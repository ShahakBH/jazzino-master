package com.yazino.engagement.campaign.dao;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.engagement.campaign.domain.CampaignRun;
import com.yazino.engagement.campaign.domain.PlayerWithContent;
import com.yazino.yaps.JsonHelper;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.bi.persistence.InsertStatementBuilder.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;
import static org.joda.time.DateTime.now;

@Repository
public class CampaignRunDaoImpl implements CampaignRunDao {
    private static final Logger LOG = LoggerFactory.getLogger(CampaignRunDaoImpl.class);

    public static final String INSERT_INTO_CAMPAIGN_RUN_CAMPAIGN_ID_RUN_TS_VALUES =
            "INSERT INTO CAMPAIGN_RUN (CAMPAIGN_ID, RUN_TS) VALUES (?,?)";
    private static final String GET_CAMPAIGN_RUN =
            "SELECT ID, CAMPAIGN_ID, RUN_TS FROM CAMPAIGN_RUN WHERE ID = ?";

    public static final String SELECT_PLAYER_FROM_SEGMENT_SELECTION_WHERE_CAMPAIGN_RUN_ID =
            "SELECT PLAYER_ID, CONTENT, VALID_FROM FROM SEGMENT_SELECTION WHERE CAMPAIGN_RUN_ID =?";

    private static final String SQL_CREATE_TEMP_TABLE = "CREATE TEMPORARY TABLE IF NOT EXISTS TMP_STG_SEGMENT_SELECTION "
            + "(CAMPAIGN_RUN_ID BIGINT NOT NULL, PLAYER_ID NUMERIC(16,2) NOT NULL, CONTENT VARCHAR(10000), VALID_FROM TIMESTAMP)";
    public static final String SQL_INSERT_UNIQUE_FROM_STAGING = "INSERT INTO SEGMENT_SELECTION (CAMPAIGN_RUN_ID, PLAYER_ID, CONTENT, VALID_FROM)  "
            + "SELECT stg.* FROM TMP_STG_SEGMENT_SELECTION stg "
            + "LEFT JOIN SEGMENT_SELECTION ss ON "
            + "stg.campaign_run_id = ss.campaign_run_id AND stg.player_id = ss.player_id "
            + "WHERE ss.campaign_run_id IS NULL AND ss.player_id IS NULL";
    private static final String SQL_DROP_TEMP_TABLE = "DROP TABLE IF EXISTS TMP_STG_SEGMENT_SELECTION";

    private final JdbcTemplate dwJdbcTemplate;
    private JdbcTemplate externalJdbcTemplate;
    private final JsonHelper jsonHelper;

    @Autowired
    public CampaignRunDaoImpl(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate externalJdbcTemplate,
                              @Qualifier("dwJdbcTemplate") final JdbcTemplate dwJdbcTemplate) {
        this.dwJdbcTemplate = dwJdbcTemplate;
        notNull(externalJdbcTemplate, "jdbcTemplate should not be null");
        this.jsonHelper = new JsonHelper();
        this.externalJdbcTemplate = externalJdbcTemplate;
    }

    @Override
    public Long createCampaignRun(final Long campaignId, final DateTime dateTime) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        dwJdbcTemplate.update(
                new PreparedStatementCreator() {
                    @Override
                    public PreparedStatement createPreparedStatement(final Connection con) throws SQLException {
                        PreparedStatement ps = con.prepareStatement(
                                INSERT_INTO_CAMPAIGN_RUN_CAMPAIGN_ID_RUN_TS_VALUES,
                                Statement.RETURN_GENERATED_KEYS);

                        ps.setLong(1, campaignId);
                        ps.setTimestamp(2, new Timestamp(dateTime.withMillisOfSecond(0).getMillis()));

                        return ps;
                    }
                }, keyHolder
        );

//        final long campaignRunId = keyHolder.getKeys().get("id");
        final long campaignRunId = keyHolder.getKey().longValue();

        LOG.debug("creating runid: {} created for campaign {}", campaignRunId, campaignId);
        return campaignRunId;
    }

    @Override
    public void purgeSegmentSelection(final Long campaignRunId) {
        notNull(campaignRunId, "campaignRunId may not be null");

        LOG.debug("Purging segment selection for campaignRunId {}", campaignRunId);

        externalJdbcTemplate.update("DELETE FROM SEGMENT_SELECTION WHERE campaign_run_id = ?", campaignRunId);
    }

    @Override
    @Transactional("externalDwTransactionManager")
    public void addPlayers(final Long campaignRunId,
                           final Collection<PlayerWithContent> recipientPlayers,
                           final boolean delayNotifications) {
        try {
            final Set<BigDecimal> processedPlayers = new HashSet<>();
            InsertStatementBuilder insertStmt = new InsertStatementBuilder(
                    "TMP_STG_SEGMENT_SELECTION", "CAMPAIGN_RUN_ID", "PLAYER_ID", "CONTENT", "VALID_FROM");
            for (PlayerWithContent recipientPlayer : recipientPlayers) {
                if (processedPlayers.contains(recipientPlayer.getPlayerId())) {
                    continue;
                }

                insertStmt = insertStmt.withValues(
                        sqlLong(campaignRunId),
                        sqlBigDecimal(recipientPlayer.getPlayerId()),
                        sqlString(jsonHelper.serialize(recipientPlayer.getContent())),
                        timestamp(delayNotifications)
                );

                processedPlayers.add(recipientPlayer.getPlayerId());
            }

            LOG.debug("Adding {} players for campaignRunId {}", processedPlayers.size(), campaignRunId);

            externalJdbcTemplate.batchUpdate(
                    new String[]{
                            SQL_DROP_TEMP_TABLE,
                            SQL_CREATE_TEMP_TABLE,
                            insertStmt.toSql(),
                            SQL_INSERT_UNIQUE_FROM_STAGING,
                            SQL_DROP_TEMP_TABLE
                    }
            );

        } catch (DataAccessException e) {
            try {
                externalJdbcTemplate.execute(SQL_DROP_TEMP_TABLE);
            } catch (DataAccessException ignored) {
                // ignored
            }

            throw e;
        }
    }

    private String timestamp(final boolean delayNotifications) {
        if (delayNotifications) {
            return "null";
        }
        return sqlTimestamp(new Timestamp(now().getMillis()));
    }

    @Override
    public List<PlayerWithContent> fetchPlayers(final Long campaignRunId) {
        LOG.debug("fetching players list for {}", campaignRunId);
        return externalJdbcTemplate.query(
                SELECT_PLAYER_FROM_SEGMENT_SELECTION_WHERE_CAMPAIGN_RUN_ID, playerMapper(), campaignRunId);
    }

    private RowMapper<PlayerWithContent> playerMapper() {
        return new RowMapper<PlayerWithContent>() {
            @SuppressWarnings("unchecked")
            @Override
            public PlayerWithContent mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final PlayerWithContent playerWithContent = new PlayerWithContent(rs.getBigDecimal(1));
                final String contentString = rs.getString(2);
                if (!isBlank(contentString)) {
                    playerWithContent.setContent(
                            (Map<String, String>) jsonHelper.deserialize(Map.class, contentString));
                }
                return playerWithContent;
            }
        };
    }

    @Override
    public CampaignRun getCampaignRun(final Long campaignRunId) {
        return dwJdbcTemplate.queryForObject(
                GET_CAMPAIGN_RUN, new RowMapper<CampaignRun>() {
                    @Override
                    public CampaignRun mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        return new CampaignRun(
                                rs.getLong("ID"), rs.getLong("CAMPAIGN_ID"), new DateTime(rs.getTimestamp("RUN_TS")));
                    }
                }, campaignRunId
        );
    }

    @Override
    public Map<Long, Long> getLatestDelayedCampaignRunsInLast24Hours() {
        final Map<Long, Long> campaignRunIds = newHashMap();
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(dwJdbcTemplate.getDataSource());
        //easier to do with this template

        final List<Map<String, Object>> results = namedTemplate.queryForList(
                "select max(cr.id) id, campaign_id from CAMPAIGN_RUN cr join CAMPAIGN_DEFINITION cd on cr.campaign_id = cd.id "
                        + "where run_ts>= :now and delay_notifications=true group by campaign_id",
                of("now", new Timestamp(now().minusDays(1).getMillis())
                ));

        for (Map<String, Object> result : results) {
            campaignRunIds.put(
                    Long.valueOf((Integer) result.get("id")), Long.valueOf((Integer) result.get("campaign_id")));
        }
        return campaignRunIds;
    }

    @Override
    @Transactional
    public DateTime getLastRuntimeForCampaignRunIdAndResetTo(final Long campaignRunId, final DateTime now) {
        LOG.debug("getting last runtime for campaign run id{} ", campaignRunId);
        final Map<String, Object> rerun = dwJdbcTemplate.queryForMap(
                "select run_ts, last_rerun_ts from CAMPAIGN_RUN where id=?", campaignRunId);

        LOG.debug("setting rerun time for campaign run id{} to {}", campaignRunId, now);
        dwJdbcTemplate.update(
                "update CAMPAIGN_RUN set last_rerun_ts=? where id=?",
                new Timestamp(now.getMillis()), campaignRunId);

        if (rerun.get("last_rerun_ts") == null) {
            return new DateTime(rerun.get("run_ts"));
        }

        return new DateTime(rerun.get("last_rerun_ts"));
    }
}
