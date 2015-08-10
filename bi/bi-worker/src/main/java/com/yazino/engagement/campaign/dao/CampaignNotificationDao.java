package com.yazino.engagement.campaign.dao;

import com.yazino.bi.messaging.SegmentSelectionCustomDataHelper;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.ChannelType;
import com.yazino.engagement.EmailTarget;
import com.yazino.engagement.PlayerTarget;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.platform.util.BigDecimals;
import com.yazino.yaps.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.operations.repository.GameTypeRepository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Repository
public class CampaignNotificationDao {
    /**
     * This is the legacy way of sending messages through the campaign tools, you should look at how the Amazon messaging
     * works if you wish to implement a new channel. This should be moved to fit that pattern when we get a chance. So we
     * can break up this DAO so that it does not contain so much domain logic from the different providers.
     */

    private static final Logger LOG = LoggerFactory.getLogger(CampaignNotificationDao.class);

    public static final String SELECT_FACEBOOK_PLAYERS =
            "SELECT :gameType AS GAME_TYPE, SS.*, LU.EXTERNAL_ID FROM SEGMENT_SELECTION SS  "
                    + "LEFT JOIN FACEBOOK_EXCLUSIONS FE ON SS.PLAYER_ID = FE.PLAYER_ID AND FE.GAME_TYPE=:gameType "
                    + "JOIN LOBBY_USER LU ON SS.PLAYER_ID = LU.PLAYER_ID "
                    + "WHERE SS.CAMPAIGN_RUN_ID= :campaignRunId  "
                    + "AND LU.EXTERNAL_ID IS NOT NULL "
                    + "AND upper(LU.PROVIDER_NAME)='FACEBOOK' "
                    + "AND FE.GAME_TYPE IS NULL";

    public static final String SELECT_EV_PLAYERS =
            "SELECT LU.EMAIL_ADDRESS, LU.DISPLAY_NAME, SS.CONTENT "
                    + "FROM SEGMENT_SELECTION SS, LOBBY_USER LU "
                    + "WHERE SS.CAMPAIGN_RUN_ID= :campaignRunId "
                    + "AND SS.PLAYER_ID = LU.PLAYER_ID "
                    + "AND upper(LU.PROVIDER_NAME)='FACEBOOK' "
                    + "AND LU.EMAIL_ADDRESS IS NOT NULL "
                    + "UNION SELECT LU.EMAIL_ADDRESS, LU.DISPLAY_NAME, SS.CONTENT "
                    + "FROM SEGMENT_SELECTION SS, LOBBY_USER LU "
                    + "JOIN EMAIL_VALIDATION EV ON LU.EMAIL_ADDRESS = EV.EMAIL_ADDRESS "
                    + "WHERE SS.CAMPAIGN_RUN_ID= :campaignRunId "
                    + "AND SS.PLAYER_ID = LU.PLAYER_ID "
                    + "AND upper(LU.PROVIDER_NAME) <> 'FACEBOOK' "
                    + "AND EV.STATUS = 'V' "
                    + "AND LU.EMAIL_ADDRESS IS NOT NULL";

    public static final String EMPTY_STRING = "";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final GameTypeRepository gameTypeRepository;
    private final HashSet<String> gameTypesExcludedFromFacebookRequests = new HashSet<String>();

    @Autowired
    public CampaignNotificationDao(@Qualifier("externalDwNamedJdbcTemplate") NamedParameterJdbcTemplate jdbcTemplate,
                                   GameTypeRepository gameTypeRepository,
                                   YazinoConfiguration yazinoConfiguration) {
        this.jdbcTemplate = jdbcTemplate;
        this.gameTypeRepository = gameTypeRepository;
        final String[] gameTypes = yazinoConfiguration.getStringArray("strata.worker.campaign.fb.excluded-game-types");
        if (gameTypes != null) {
            gameTypesExcludedFromFacebookRequests.addAll(Arrays.asList(gameTypes));
        }
    }

    public List<PlayerTarget> getEligiblePlayerTargets(Long campaignRunId, ChannelType channel) {

        switch (channel) {
            case FACEBOOK_APP_TO_USER_REQUEST:
            case FACEBOOK_APP_TO_USER_NOTIFICATION:
                return eligiblePlayerTargetsForFacebook(campaignRunId);
            default:
                throw new IllegalArgumentException("Unknown Channel Type: " + channel);
        }
    }

    public List<EmailTarget> getEligibleEmailTargets(final Long campaignRunId) {
        return eligiblePlayerTargetsForEmailVision(campaignRunId);
    }

    private List<EmailTarget> eligiblePlayerTargetsForEmailVision(final Long campaignRunId) {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("campaignRunId", campaignRunId);

        final List<EmailTarget> query = jdbcTemplate.query(SELECT_EV_PLAYERS, paramMap, new RowMapper<EmailTarget>() {
            @SuppressWarnings("unchecked")
            @Override
            public EmailTarget mapRow(ResultSet rs, int rowNum) throws SQLException {
                final String contentString = rs.getString("CONTENT");
                Map<String, Object> content = null;
                try {
                    if (!isBlank(contentString)) {
                        content = new JsonHelper().deserialize(Map.class, contentString);
                    }
                } catch (Exception e) {
                    LOG.warn("failed to deserialise content from campaign run {} : {}", campaignRunId, contentString);
                }
                return new EmailTarget(rs.getString("EMAIL_ADDRESS"), rs.getString("DISPLAY_NAME"), content);
            }
        });
        return query;

    }

    private List<PlayerTarget> eligiblePlayerTargetsForFacebook(Long campaignRunId) {
        Map<String, Object> paramMap = new LinkedHashMap<String, Object>();
        paramMap.put("campaignRunId", campaignRunId);

        final List<PlayerTarget> results = newArrayList();
        final Map<String, GameTypeInformation> gameConfigurationsMap = gameTypeRepository.getGameTypes();
        for (String gameType : gameConfigurationsMap.keySet()) {
            if (gameTypesExcludedFromFacebookRequests.contains(gameType)) {
                LOG.debug("Ignoring {} for facebook requests", gameType);
                continue;
            }
            LOG.debug("Retrieving facebook request targets for {}", gameType);
            paramMap.put("gameType", gameType);
            final List<PlayerTarget> resultForGameType = jdbcTemplate.query(SELECT_FACEBOOK_PLAYERS,
                    paramMap,
                    new ResultSetExtractor<List<PlayerTarget>>() {
                        @Override
                        public List<PlayerTarget> extractData(ResultSet rs) throws SQLException, DataAccessException {
                            List<PlayerTarget> playerTargets = new ArrayList<>();
                            while (rs.next()) {
                                String externalId = rs.getString("EXTERNAL_ID");
                                String gameType = rs.getString("GAME_TYPE");
                                BigDecimal playerId = BigDecimals.strip(rs.getBigDecimal("PLAYER_ID"));
                                playerTargets.add(new PlayerTarget(gameType,
                                        externalId,
                                        playerId,
                                        EMPTY_STRING,
                                        EMPTY_STRING,
                                        SegmentSelectionCustomDataHelper.getCustomData(rs)));
                            }
                            return playerTargets;
                        }
                    });
            results.addAll(resultForGameType);
            LOG.debug("found {} players for gameType {} for campaignRun {}", resultForGameType.size(), gameType, campaignRunId);

        }

        return results;
    }

}
