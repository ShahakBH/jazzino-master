package com.yazino.engagement.campaign.dao;

import com.yazino.bi.persistence.BatchResultSetExtractor;
import com.yazino.bi.persistence.BatchVisitor;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.campaign.domain.PlayerWithContent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;
import static org.joda.time.DateTime.now;

@Repository
public class PostgresSegmentSelectorDao implements SegmentSelectorDao {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresSegmentSelectorDao.class);

    private static final DateFormat SQL_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_BATCH_SIZE = 10000;
    private static final String PROPERTY_BATCH_SIZE = "strata.worker.campaign.batch-size";

    private final NamedParameterJdbcTemplate template;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public PostgresSegmentSelectorDao(
            @Qualifier("externalDwNamedJdbcTemplate") final NamedParameterJdbcTemplate template,
            final YazinoConfiguration yazinoConfiguration) {
        notNull(template, "template can not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration");

        this.template = template;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @Override
    public int fetchSegment(final String segmentSelectionQuery,
                            final DateTime reportTime,
                            final BatchVisitor<PlayerWithContent> visitor) {
        final String sql = setDateIn(segmentSelectionQuery, reportTime);
        LOG.debug("sql is : {}", sql);

        return template.query(
                sql, new BatchResultSetExtractor<>(new PlayerWithContentRowMapper(), visitor, batchSize()));
    }

    @Override
    public void updateSegmentDelaysForCampaignRuns(final Set<Long> campaignRunIds, final DateTime now) {
        if (campaignRunIds == null || campaignRunIds.size() == 0) {
            return;
        }
        final MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("ids", campaignRunIds);
        paramSource.addValue("now", new Timestamp(now.getMillis()), Types.TIMESTAMP);
        final DateTime from = now.withZone(DateTimeZone.UTC).minusHours(1);
        final DateTime to = now.withZone(DateTimeZone.UTC);
        paramSource.addValue("frm", new Timestamp(from.getMillisOfDay()), Types.TIMESTAMP);
        paramSource.addValue("tuu", new Timestamp(to.getMillisOfDay()), Types.TIMESTAMP);

        final String sql = "update segment_selection s set valid_from=:now from last_login l where "
                + getTimeComparison(from, to)
                + " and s.player_id= l.player_id and campaign_run_id in (:ids) "
                + " and s.valid_from is null";
        LOG.debug("updating segment selection with: {}", sql);

        final int numberOfRows = template.update(sql, paramSource);

        LOG.debug("updated {} segments for {} with date {}", numberOfRows, campaignRunIds, now());

    }

    private String getTimeComparison(DateTime from, DateTime to) {
        if (from.isAfter(to)) {
            return "l.last_login::time <= :tuu::time or l.last_login::time > :frm::time ";
        } else {
            return "l.last_login::time <= :tuu::time and l.last_login::time > :frm::time ";
        }
    }

    private int batchSize() {
        return yazinoConfiguration.getInt(PROPERTY_BATCH_SIZE, DEFAULT_BATCH_SIZE);
    }

    private String setDateIn(final String segmentSelectionQuery, final DateTime currentDate) {
        return replaceDateWithQueryRunTime(segmentSelectionQuery, currentDate);
    }

    private String replaceDateWithQueryRunTime(final String segmentSelectionQuery, final DateTime currentDate) {

        final String query = segmentSelectionQuery.replaceAll(
                "(?i)getDate\\(\\)",
                "'" + SQL_DATE_FORMAT.format(currentDate.toDate()) + "'" + "::timestamp");
        LOG.info("replacing date if needed resulting in query {}", query);
        return query;
    }

    private static class PlayerWithContentRowMapper implements RowMapper<PlayerWithContent> {
        @Override
        public PlayerWithContent mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final PlayerWithContent playerWithContent = new PlayerWithContent();

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int index = 1; index <= columnCount; index++) {
                String columnName = metaData.getColumnName(index);
                if (columnName.equals("player_id")) {
                    playerWithContent.setPlayerId(rs.getBigDecimal(index));
                } else {
                    playerWithContent.getContent().put(columnName.toUpperCase(), rs.getString(index));
                }
            }

            return playerWithContent;
        }
    }

}
