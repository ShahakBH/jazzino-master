package com.yazino.bi.operations.persistence;

import com.yazino.platform.Platform;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import strata.server.lobby.api.promotion.ControlGroupFunctionType;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.PromotionFactory;
import strata.server.lobby.api.promotion.PromotionType;
import com.yazino.bi.operations.model.PromotionPlayer;
import com.yazino.bi.operations.view.PromotionSearchType;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class JdbcBackOfficePromotionDao {
    private static final Logger LOG = LoggerFactory.getLogger(JdbcBackOfficePromotionDao.class);

    public static final String SELECT_PROMOTION_SQL = "select p.promo_id, p.type, p.name, p.target_clients, "
            + "p.all_players, p.player_count, p.start_date, p.end_date, p.priority, p.seed, "
            + "p.control_group_percentage, p.cg_function, pc.config_key, pc.config_value "
            + "from %s p left join %s pc on p.promo_id = pc.promo_id where %s order by p.start_date";
    public static final String SELECT_PROMOTION_PLAYER_SQL =
            "select promo_id, player_id " + "from %s where promo_id=? order by player_id limit ?,? ";
    public static final String SELECT_PLAYER_COUNT_SQL = "select count(*) from %s where promo_id = ?";
    public static final String SELECT_EXPIRED_PROMOTIONS_SQL =
            "select promo_id from strataprod.PROMOTION where end_date < ?";
    public static final String PROMOTION_ARCHIVE_SQL =
            "insert ignore into PROMOTION_ARCHIVE (promo_id, type, name, target_clients,"
                    + " all_players, player_count, start_date, "
                    + "end_date, priority, seed, control_group_percentage, cg_function) "
                    + "select promo_id, type, name, target_clients, all_players,"
                    + " player_count, start_date, end_date, priority, seed, "
                    + "control_group_percentage, cg_function from strataprod.PROMOTION where promo_id = ?";
    public static final String PROMOTION_CONFIG_ARCHIVE_SQL =
            "insert ignore into PROMOTION_CONFIG_ARCHIVE (promo_id, config_key, config_value) "
                    + "select promo_id, config_key, config_value from strataprod.PROMOTION_CONFIG  where promo_id = ?";
    public static final String PROMOTION_PLAYER_ARCHIVE_SQL =
            "insert ignore into PROMOTION_PLAYER_ARCHIVE (promo_id, player_id) "
                    + "select promo_id, player_id from strataprod.PROMOTION_PLAYER where promo_id = ?";
    public static final String PROMOTION_PLAYER_REWARD_ARCHIVE =
            "insert ignore into PROMOTION_PLAYER_REWARD_ARCHIVE (promo_id, player_id, rewarded_date, details) "
                    + "select promo_id, player_id, rewarded_date, details from strataprod.PROMOTION_PLAYER_REWARD "
                    + "where promo_id = ?";

    private JdbcTemplate dwJdbcTemplate;

    // CGLIB default constructor
    public JdbcBackOfficePromotionDao() {
    }

    @Autowired(required = true)
    public JdbcBackOfficePromotionDao(@Qualifier("dwJdbcTemplate") final JdbcTemplate dwJdbcTemplate) {
        this.dwJdbcTemplate = dwJdbcTemplate;
    }

    public List<Promotion> find(final PromotionSearchType searchType, final PromotionType promotionType) {
        notNull(searchType, "searchType cannot be null");
        notNull(promotionType, "promotionType cannot be null");
        final String selectSql = getSelectPromotionSql(searchType, false);
        return dwJdbcTemplate.query(selectSql, promotionResultSetExtractor(), promotionType.name());
    }

    public Promotion findById(final PromotionSearchType searchType, final Long promotionId) {
        notNull(promotionId, "promotionId cannot be null");
        notNull(searchType, "searchType cannot be null");
        final String selectSql = getSelectPromotionSql(searchType, true);
        final List<Promotion> promotions = dwJdbcTemplate.query(selectSql, promotionResultSetExtractor(), promotionId);
        if (promotions.size() == 0) {
            return null;
        }
        return promotions.get(0);
    }


    private String getSelectPromotionSql(final PromotionSearchType searchType, final boolean byId) {
        final String promoIdClause;
        if (byId) {
            promoIdClause = "p.promo_id=?";
        } else {
            promoIdClause = "p.type=?";
        }
        if (searchType == PromotionSearchType.LIVE) {
            return String.format(SELECT_PROMOTION_SQL, "strataprod.PROMOTION",
                    "strataprod.PROMOTION_CONFIG", promoIdClause);
        }
        return String.format(SELECT_PROMOTION_SQL, "PROMOTION_ARCHIVE", "PROMOTION_CONFIG_ARCHIVE", promoIdClause);

    }

    private ResultSetExtractor<List<Promotion>> promotionResultSetExtractor() {
        return new ResultSetExtractor<List<Promotion>>() {
            @Override
            public List<Promotion> extractData(final ResultSet rs) throws SQLException, DataAccessException {
                final List<Promotion> promotions = new ArrayList<Promotion>();
                Long lastId = null;
                Promotion promo = null;
                while (rs.next()) {
                    final Long currentId = rs.getLong("promo_id");
                    if (currentId.equals(lastId)) {
                        promo.addConfigurationItem(rs.getString("config_key"), rs.getString("config_value"));
                    } else {
                        final PromotionType type = PromotionType.valueOf(rs.getString("type"));
                        promo = PromotionFactory.createPromotion(type);
                        promotions.add(promo);
                        promo.setId(currentId);
                        promo.setName(rs.getString("name"));

                        final String platformsString = rs.getString("target_clients");
                        final List<Platform> platforms = new ArrayList<Platform>();
                        if (StringUtils.isNotBlank(platformsString)) {
                            for (String platform : platformsString.split(",")) {
                                platforms.add(Platform.valueOf(platform));
                            }
                        }
                        promo.setPlatforms(platforms);

                        promo.setAllPlayers(rs.getBoolean("all_players"));
                        promo.setPlayerCount(rs.getInt("player_count"));
                        promo.setStartDate(new DateTime(rs.getTimestamp("start_date")));
                        promo.setEndDate(new DateTime(rs.getTimestamp("end_date")));
                        promo.addConfigurationItem(rs.getString("config_key"), rs.getString("config_value"));
                        final int priority = rs.getInt("priority");
                        if (!rs.wasNull()) {
                            promo.setPriority(priority);
                        }
                        promo.setSeed(rs.getInt("seed"));
                        promo.setControlGroupPercentage(rs.getInt("control_group_percentage"));
                        promo.setControlGroupFunction(ControlGroupFunctionType.valueOf(rs.getString("cg_function")));
                        lastId = currentId;
                    }
                }
                return promotions;
            }
        };
    }

    public Integer countPlayersInPromotion(final PromotionSearchType searchType, final Long promotionId) {
        notNull(promotionId, "promotionId cannot be null");
        final String selectSql;
        if (searchType == PromotionSearchType.LIVE) {
            selectSql = String.format(SELECT_PLAYER_COUNT_SQL,
                    "strataprod.PROMOTION_PLAYER");
        } else {
            selectSql = String.format(SELECT_PLAYER_COUNT_SQL,
                    "PROMOTION_PLAYER_ARCHIVE");
        }
        return dwJdbcTemplate.queryForInt(selectSql, promotionId);
    }

    public List<PromotionPlayer> findPlayers(final PromotionSearchType searchType,
                                             final Long promotionId,
                                             final Integer offset,
                                             final Integer numberOfPlayers) {
        notNull(promotionId, "promotionId is null");
        notNull(offset, "firstPlayer is null");
        notNull(numberOfPlayers, "numberOfPlayers is null");
        final String selectSql;
        if (PromotionSearchType.LIVE == searchType) {
            selectSql = String.format(SELECT_PROMOTION_PLAYER_SQL,
                    "strataprod.PROMOTION_PLAYER");
        } else {
            selectSql = String.format(SELECT_PROMOTION_PLAYER_SQL,
                    "PROMOTION_PLAYER_ARCHIVE");
        }
        return dwJdbcTemplate.query(selectSql, new RowMapper<PromotionPlayer>() {
            @Override
            public PromotionPlayer mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final PromotionPlayer player = new PromotionPlayer();
                player.setPromotionId(rs.getLong(1));
                player.setPlayerId(BigInteger.valueOf(rs.getLong(2)));
                return player;
            }
        }, promotionId, offset, numberOfPlayers);
    }

    public List<Long> findPromotionsOlderThan(final int numberOfDays) {
        final DateTime currentTime = new DateTime().minusDays(numberOfDays);
        return dwJdbcTemplate.queryForList(SELECT_EXPIRED_PROMOTIONS_SQL, Long.class, currentTime.toDate());
    }

    @Transactional
    public void archivePromotion(final Long promotionId) {
        notNull(promotionId, "promotionId is null");
        if (LOG.isInfoEnabled()) {
            LOG.info(String.format("Copying promotion[%s] data to archive tables", promotionId));
        }
        dwJdbcTemplate.update(PROMOTION_ARCHIVE_SQL, promotionId);
        dwJdbcTemplate.update(PROMOTION_CONFIG_ARCHIVE_SQL, promotionId);
        dwJdbcTemplate.update(PROMOTION_PLAYER_ARCHIVE_SQL, promotionId);
        dwJdbcTemplate.update(PROMOTION_PLAYER_REWARD_ARCHIVE, promotionId);
    }
}
