package com.yazino.engagement.campaign.dao;

import com.yazino.platform.audit.message.SessionKey;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

@Repository
public class FacebookExclusionsDao {
    private static final String INSERT_EXCLUSIONS =
            "INSERT INTO FACEBOOK_EXCLUSIONS (PLAYER_ID, GAME_TYPE, REASON) VALUES (:playerId, :gameType, :reason)";

    static final String BOUNCED = "BOUNCED";

    private static final Logger LOG = LoggerFactory.getLogger(FacebookExclusionsDao.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public FacebookExclusionsDao(final NamedParameterJdbcTemplate externalDwNamedJdbcTemplate) {
        Validate.notNull(externalDwNamedJdbcTemplate, "externalDwJdbcTemplate CanNotBeNull");
        this.jdbcTemplate = externalDwNamedJdbcTemplate;

    }

    public void logFailureInSendingFacebookNotification(final BigDecimal playerId, final String gameType) {
        Map<String, Object> params = newHashMap();
        LOG.debug("adding player {} to facebook exclusions for game: {}", playerId, gameType);
        params.put("playerId", playerId);
        params.put("gameType", gameType);
        params.put("reason", BOUNCED);
        try {
            jdbcTemplate.update(INSERT_EXCLUSIONS, params);
        } catch (DuplicateKeyException e) {
            jdbcTemplate.update("UPDATE FACEBOOK_EXCLUSIONS SET REASON=:reason WHERE PLAYER_ID= :playerId AND GAME_TYPE= :gameType",
                    params);
            //don't care
        }
    }


    public void resetFacebookExclusions(final List<SessionKey> sessionKeys) {

        SqlParameterSource[] params = SqlParameterSourceUtils.createBatch(sessionKeys.toArray());
        jdbcTemplate.batchUpdate("delete from FACEBOOK_EXCLUSIONS where player_id=:playerId", params);

    }
}
