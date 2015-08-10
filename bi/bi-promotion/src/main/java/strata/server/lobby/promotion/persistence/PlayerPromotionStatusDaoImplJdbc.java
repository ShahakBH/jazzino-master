package strata.server.lobby.promotion.persistence;

import com.yazino.platform.util.BigDecimals;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import strata.server.lobby.api.promotion.domain.PlayerPromotionStatus;
import strata.server.lobby.api.promotion.domain.builder.PlayerPromotionStatusBuilder;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.lang3.Validate.notNull;

@Repository("playerPromotionStatusDao")
public class PlayerPromotionStatusDaoImplJdbc implements PlayerPromotionStatusDao {

    public static final String SELECT_FROM_PLAYER_PROMOTION_STATUS_WHERE_PLAYER_ID =
            "SELECT PLAYER_ID, LAST_TOPUP_DATE, LAST_PLAYED_DATE, CONSECUTIVE_PLAY_DAYS, TOP_UP_ACKNOWLEDGED "
                    + "FROM PLAYER_PROMOTION_STATUS "
                    + "WHERE PLAYER_ID = ? FOR UPDATE";

    public static final String INSERT_OR_UPDATE_PLAYER_PROMOTION_SQL = "INSERT INTO PLAYER_PROMOTION_STATUS "
            + "(PLAYER_ID, LAST_TOPUP_DATE, LAST_PLAYED_DATE, CONSECUTIVE_PLAY_DAYS, TOP_UP_ACKNOWLEDGED) "
            + "VALUES (:playerId, :lastTopup, :lastPlayed, :consecutiveDaysPlayed, :topUpAcknowledged) "
            + "ON DUPLICATE KEY UPDATE LAST_TOPUP_DATE=:lastTopup ,LAST_PLAYED_DATE=:lastPlayed, "
            + "CONSECUTIVE_PLAY_DAYS=:consecutiveDaysPlayed, TOP_UP_ACKNOWLEDGED=:topUpAcknowledged";

    private static final String UPDATE_ACKNOWLEDGE_FLAG_FOR_PLAYER = "UPDATE PLAYER_PROMOTION_STATUS "
            + "SET TOP_UP_ACKNOWLEDGED=true WHERE PLAYER_ID=:playerId and LAST_TOPUP_DATE=:lastTopUpDate";

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    // THIS SHOULD POINT TO STRATAPROD which it does
    @Autowired
    public PlayerPromotionStatusDaoImplJdbc(@Qualifier("marketingJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public PlayerPromotionStatus get(final BigDecimal playerId) {
        try {
            return jdbcTemplate.queryForObject(SELECT_FROM_PLAYER_PROMOTION_STATUS_WHERE_PLAYER_ID,
                    new RowMapper<PlayerPromotionStatus>() {

                        @Override
                        public PlayerPromotionStatus mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                            boolean topUpAcknowledged = rs.getBoolean("TOP_UP_ACKNOWLEDGED");
                            if (rs.wasNull()) {
                                topUpAcknowledged = true;
                            }
                            return new PlayerPromotionStatusBuilder().withPlayerId(BigDecimals.strip(rs.getBigDecimal("PLAYER_ID")))
                                    .withLastPlayedDateAsTimestamp(rs.getTimestamp("LAST_PLAYED_DATE"))
                                    .withLastTopupDateAsTimestamp(rs.getTimestamp("LAST_TOPUP_DATE"))
                                    .withConsecutiveDaysPlayed(rs.getInt("CONSECUTIVE_PLAY_DAYS"))
                                    .withTopUpAcknowledged(topUpAcknowledged)
                                    .build();
                        }
                    }, playerId);
        } catch (EmptyResultDataAccessException ex) {
            return new PlayerPromotionStatusBuilder().withPlayerId(playerId).build();
        }
    }

    @Override
    @Transactional
    public PlayerPromotionStatus save(final PlayerPromotionStatus playerPromotionStatus) {

        final Map<String, Object> arguments = newHashMap();
        arguments.put("playerId", playerPromotionStatus.getPlayerId());

        if (playerPromotionStatus.getLastPlayed() != null) {
            arguments.put("lastPlayed", new Timestamp(playerPromotionStatus.getLastPlayed().getMillis()));
        } else {
            arguments.put("lastPlayed", null);
        }

        if (playerPromotionStatus.getLastTopup() != null) {
            arguments.put("lastTopup", new Timestamp(playerPromotionStatus.getLastTopup().getMillis()));
        } else {
            arguments.put("lastTopup", null);
        }
        arguments.put("consecutiveDaysPlayed", playerPromotionStatus.getConsecutiveDaysPlayed());
        arguments.put("topUpAcknowledged", playerPromotionStatus.isTopUpAcknowledged());
        namedJdbcTemplate.update(INSERT_OR_UPDATE_PLAYER_PROMOTION_SQL, arguments);

        return playerPromotionStatus;
    }

    @Override
    public void saveAcknowledgeTopUpForPlayer(final BigDecimal playerId, final DateTime acknowledgeDate) {
        final Map<String, Object> arguments = newHashMap();
        arguments.put("playerId", playerId);
        arguments.put("lastTopUpDate", acknowledgeDate.toDate());
        namedJdbcTemplate.update(UPDATE_ACKNOWLEDGE_FLAG_FOR_PLAYER, arguments);
    }
}
