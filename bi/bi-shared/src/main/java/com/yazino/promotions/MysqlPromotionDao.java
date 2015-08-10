package com.yazino.promotions;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.promotions.filter.*;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import strata.server.lobby.api.promotion.*;
import strata.server.lobby.api.promotion.util.DailyAwardPromotionComparator;
import strata.server.lobby.api.promotion.util.PromotionPriorityDateComparator;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.Validate.notNull;
import static strata.server.lobby.api.promotion.BuyChipsPromotion.PAYMENT_METHODS_KEY;
import static strata.server.lobby.api.promotion.DailyAwardPromotion.MAX_REWARDS_KEY;

@Repository
public class MysqlPromotionDao implements PromotionDao {
    private static final Logger LOG = LoggerFactory.getLogger(MysqlPromotionDao.class);

    private static final String INSERT_SQL = "insert into PROMOTION(type, name, target_clients, all_players, "
            + "player_count, start_date, end_date, priority,seed,control_group_percentage,cg_function) "
            + "values(?,?,?,?,?,?,?,?,?,?,?)";

    private static final String UPDATE_SQL = "update PROMOTION set name=?, target_clients=?, all_players=?, "
            + "player_count = ? , start_date=?,"
            + "end_date=?, priority=?, seed=?, control_group_percentage=?, cg_function=? where promo_id=?";

    private static final String DELETE_SQL = "delete from PROMOTION where promo_id = ?";

    private static final String DELETE_CONFIGURATION_SQL = "delete from PROMOTION_CONFIG where promo_id=?";

    private static final String DELETE_PROMOTION_PLAYERS_SQL = "delete from PROMOTION_PLAYER where promo_id = ?";

    private static final String DELETE_PROMOTION_PLAYER_REWARDS_SQL =
            "delete from PROMOTION_PLAYER_REWARD where promo_id = ?";

    public static final String CREATE_CONFIGURATION_SQL =
            "insert into PROMOTION_CONFIG(promo_id, config_key, config_value) values (?,?,?)";

    public static final String SELECT_PROMOTIONS_BY_PLAYER_SQL =
            "select p.promo_id, p.type, p.name, p.target_clients, p.all_players, p.player_count, p.start_date, "
                    + "p.end_date, p.priority, "
                    + "p.seed, p.control_group_percentage, p.cg_function,"
                    + "pc.config_key, pc.config_value from PROMOTION p left join "
                    + "PROMOTION_CONFIG pc on p.promo_id = pc.promo_id "
                    + "where ? between p.start_date and p.end_date "
                    + "and (p.all_players = 1 or (p.is_player_list_inclusive = "
                    + "(select count(1) from PROMOTION_PLAYER where promo_id = p.promo_id and player_id=?))) "
                    + "order by 1";

    public static final String SELECT_PROMOTIONS_BY_PLAYER_SQL_ORDERED_BY_PRIORITY =
            "select p.promo_id, p.type, p.name, p.target_clients, p.all_players, p.player_count, p.start_date, "
                    + "p.end_date, p.priority, "
                    + "p.seed, p.control_group_percentage, p.cg_function,"
                    + "pc.config_key, pc.config_value from PROMOTION p left join "
                    + "PROMOTION_CONFIG pc on p.promo_id = pc.promo_id "
                    + "where ? between p.start_date and p.end_date "
                    + "and (p.all_players = 1 or (p.is_player_list_inclusive = "
                    + "(select count(1) from PROMOTION_PLAYER where promo_id = p.promo_id and player_id=?))) "
                    + "order by p.priority desc, 1";

    public static final String SELECT_PROMOTIONS_BY_PLAYER_TYPE_SQL =
            "select p.promo_id, p.type, p.name, p.target_clients, p.all_players, p.player_count, p.start_date, "
                    + "p.end_date, p.priority, "
                    + "p.seed, p.control_group_percentage, p.cg_function,"
                    + "pc.config_key, pc.config_value from PROMOTION p left join "
                    + "PROMOTION_CONFIG pc on p.promo_id = pc.promo_id "
                    + "where ? between p.start_date and p.end_date and type=? "
                    + "and (p.all_players = 1 or (p.is_player_list_inclusive = "
                    + "(select count(1) from PROMOTION_PLAYER where promo_id = p.promo_id and player_id=?))) "
                    + "order by 1";

    public static final String SELECT_PROMOTIONS_BY_ID_SQL = "select p.promo_id, p.type, p.name, p.target_clients, "
            + "p.all_players, p.player_count, p.start_date, p.end_date, p.priority, p.seed, "
            + "p.control_group_percentage, p.cg_function, "
            + "pc.config_key, pc.config_value from PROMOTION p left join "
            + "PROMOTION_CONFIG pc on p.promo_id = pc.promo_id where p.promo_id = ?";

    public static final String ADD_PROMOTION_PLAYER_SQL =
            "insert ignore into PROMOTION_PLAYER(promo_id, player_id) values(?,?)";

    public static final String UPDATE_PLAYER_COUNT_SQL = "UPDATE PROMOTION SET PLAYER_COUNT = "
            + "(select count(*) from PROMOTION_PLAYER where promo_id = ?) where promo_id = ?";

    public static final String SELECT_PROGRESSIVE_DAILY_AWARD_PROMOTION = "SELECT p.PROMO_ID, p.TYPE, pc.CONFIG_VALUE, "
            + "pc.CONFIG_KEY FROM PROMOTION p left join PROMOTION_CONFIG pc on p.PROMO_ID = pc.PROMO_ID WHERE p.TYPE=? "
            + "AND pc.CONFIG_KEY='reward.chips'";
    public static final String SELECT_PROMOTION_VALUES_SQL = "SELECT pc.config_value from PROMOTION p left join "
            + "PROMOTION_CONFIG pc on p.promo_id = pc.promo_id "
            + "where p.name='Default Daily Award' and pc.config_key='reward.chips' "
            + "and NOW() between p.start_date and p.end_date "
            + "and (p.all_players = 1 or (p.is_player_list_inclusive = "
            + "(select count(1) from PROMOTION_PLAYER where promo_id = p.promo_id and player_id=100))) "
            + "order by pc.config_value ASC";
    public static final String SELECT_PROMOTION_PLAYER_REWARD = "select promo_id, player_id, control_group, "
            + "rewarded_date, details from PROMOTION_PLAYER_REWARD where player_id = ? and rewarded_date = ?";

    private final Map<PromotionType, List<? extends Predicate<Promotion>>> promotionConfigItemsFilter;
    private final Map<Platform, List<? extends Predicate<Promotion>>> promotionPlatformFilter;

    // THIS SHOULD POINT TO STRATAPROD
    private final JdbcTemplate jdbcTemplate;


    @Autowired
    public MysqlPromotionDao(@Qualifier("marketingJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate is null");
        this.jdbcTemplate = jdbcTemplate;

        promotionConfigItemsFilter = new HashMap<>();
        promotionConfigItemsFilter.put(PromotionType.DAILY_AWARD,
                Arrays.asList(new NoRewardChipsFilter(), new NoMaximumAwardsFilter()));
        promotionConfigItemsFilter.put(PromotionType.BUY_CHIPS,
                Arrays.asList(new NoChipPackageFilter(), new NoMaximumAwardsFilter()));

        promotionPlatformFilter = new HashMap<>();
        promotionPlatformFilter.put(Platform.WEB, Arrays.asList(new NoWebPromotionsFilter()));
        promotionPlatformFilter.put(Platform.IOS, Arrays.asList(new NoIosPromotionsFilter()));
        promotionPlatformFilter.put(Platform.ANDROID, Arrays.asList(new NoAndroidPromotionsFilter()));
        promotionPlatformFilter.put(Platform.FACEBOOK_CANVAS, Arrays.asList(new NoFacebookCanvasPromotionsFilter()));
    }

    @Override
    @Transactional
    public Long create(final Promotion promo) {
        notNull(promo, "Promotion cannot be null");

        LOG.debug("Creating promotion: {}", promo);

        final KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(final Connection connection) throws SQLException {
                final PreparedStatement stmt = connection.prepareStatement(
                        INSERT_SQL, PreparedStatement.RETURN_GENERATED_KEYS);
                int index = 0;
                stmt.setString(++index, promo.getPromotionType().name());
                stmt.setString(++index, promo.getName());
                stmt.setString(++index, promo.getPlatformsAsString());
                stmt.setBoolean(++index, promo.isAllPlayers());
                stmt.setInt(++index, promo.getPlayerCount());
                stmt.setTimestamp(++index, new Timestamp(promo.getStartDate().getMillis()));
                stmt.setTimestamp(++index, new Timestamp(promo.getEndDate().getMillis()));
                if (promo.getPriority() == null) {
                    stmt.setNull(++index, Types.INTEGER);
                } else {
                    stmt.setInt(++index, promo.getPriority());
                }
                stmt.setInt(++index, promo.getSeed());
                stmt.setInt(++index, promo.getControlGroupPercentage());
                stmt.setString(++index, promo.getControlGroupFunction().name());
                return stmt;
            }
        }, keyHolder);

        if (promo.getConfiguration() != null) {
            createPromotionConfiguration((Long) keyHolder.getKey(), promo.getConfiguration());
        }

        return (Long) keyHolder.getKey();
    }

    private void createPromotionConfiguration(final Long promotionId,
                                              final PromotionConfiguration promotionConfiguration) {
        LOG.debug("Creating promotionConfiguration for promo[{}]: {}", promotionId, promotionConfiguration);

        final Map<String, String> configurationMap = promotionConfiguration.getConfiguration();
        final List<String> keys = new ArrayList<>(configurationMap.keySet());
        if (!keys.isEmpty()) {
            jdbcTemplate.batchUpdate(CREATE_CONFIGURATION_SQL, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(final PreparedStatement ps, final int i) throws SQLException {
                    int index = 0;
                    ps.setLong(++index, promotionId);
                    ps.setString(++index, keys.get(i));
                    ps.setString(++index, configurationMap.get(keys.get(i)));
                }

                @Override
                public int getBatchSize() {
                    return keys.size();
                }
            });
        }
    }

    @Override
    @Transactional
    public void update(final Promotion promo) {
        notNull(promo, "Promotion cannot be null");

        LOG.debug("Updating promotion: {}", promo);

        jdbcTemplate.update(UPDATE_SQL, promo.getName(), promo.getPlatformsAsString(), promo.isAllPlayers(),
                promo.getPlayerCount(),
                promo.getStartDate().toDate(), promo.getEndDate().toDate(), promo.getPriority(), promo.getSeed(),
                promo.getControlGroupPercentage(), promo.getControlGroupFunction().name(), promo.getId());

        final PromotionConfiguration config = promo.getConfiguration();
        jdbcTemplate.update("delete from PROMOTION_CONFIG where promo_id=?", promo.getId());
        if (config != null && config.hasConfigItems()) {
            createPromotionConfiguration(promo.getId(), config);
        }
        if (promo.isAllPlayers()) {
            jdbcTemplate.update(DELETE_PROMOTION_PLAYERS_SQL, promo.getId());
        }
    }

    @Override
    @Transactional
    public void delete(final Long promoId) {
        notNull(promoId, "Promotion id cannot be null");

        LOG.info("Deleting promotion: {}", promoId);

        jdbcTemplate.update(DELETE_PROMOTION_PLAYER_REWARDS_SQL, promoId);
        jdbcTemplate.update(DELETE_PROMOTION_PLAYERS_SQL, promoId);
        jdbcTemplate.update(DELETE_CONFIGURATION_SQL, promoId);
        jdbcTemplate.update(DELETE_SQL, promoId);
    }

    @Override
    public void addPlayersTo(final Long promoId, final Set<BigDecimal> playerIds) {
        notNull(promoId, "Promotion id cannot be null");
        notNull(playerIds, "playerIds  cannot be null");

        if (!isPromotionForAllPlayers(promoId)) {
            LOG.debug("Adding players [{}] to promotion[{}].", playerIds, promoId);

            final BigDecimal[] playerIdsToUpload = playerIds.toArray(new BigDecimal[playerIds.size()]);
            jdbcTemplate.batchUpdate(ADD_PROMOTION_PLAYER_SQL,
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(final PreparedStatement ps, final int i) throws SQLException {
                            ps.setLong(1, promoId);
                            ps.setLong(2, playerIdsToUpload[i].longValue());
                        }

                        @Override
                        public int getBatchSize() {
                            return playerIds.size();
                        }
                    });
        }
    }

    private boolean isPromotionForAllPlayers(final Long promoId) {
        return findById(promoId).isAllPlayers();
    }

    @Override
    public void updatePlayerCountInPromotion(final Long promoId) {

        notNull(promoId, "promoId cannot be null");
        jdbcTemplate.update(UPDATE_PLAYER_COUNT_SQL, promoId, promoId);
    }

    @Override
    public List<Promotion> findWebPromotions(final BigDecimal playerId, final DateTime applicableDate) {

        List<Promotion> candidates = jdbcTemplate.query(SELECT_PROMOTIONS_BY_PLAYER_SQL,
                new PromotionResultSetExtractor(),
                applicableDate.toDate(),
                playerId);
        candidates = removePromotionsNotForThisPlatform(Platform.WEB, candidates);
        candidates = filterPromotionsWherePlayerAlreadyHasMaximumAwards(playerId, candidates);

        return candidates;
    }

    @Override
    public List<Promotion> findWebPromotionsOrderedByPriority(final BigDecimal playerId,
                                                              final DateTime applicableDate) {

        List<Promotion> candidates = jdbcTemplate.query(SELECT_PROMOTIONS_BY_PLAYER_SQL_ORDERED_BY_PRIORITY,
                new PromotionResultSetExtractor(),
                applicableDate.toDate(),
                playerId);
        candidates = removePromotionsNotForThisPlatform(Platform.WEB, candidates);
        candidates = filterPromotionsWherePlayerAlreadyHasMaximumAwards(playerId, candidates);

        return candidates;
    }

    @Override
    public List<Promotion> findPromotionsForCurrentTime(final BigDecimal playerId,
                                                        final PromotionType type,
                                                        final Platform platform) {
        return findPromotionsFor(playerId, type, platform, new DateTime());
    }

    @Override
    public List<Promotion> findPromotionsFor(final BigDecimal playerId,
                                             final PromotionType type,
                                             final Platform platform,
                                             final DateTime currentTime) {

        List<Promotion> candidates = jdbcTemplate.query(SELECT_PROMOTIONS_BY_PLAYER_TYPE_SQL,
                new PromotionResultSetExtractor(),
                currentTime.toDate(),
                type.name(),
                playerId);
        // For type = BUY_CHIPS, removePromotionsNotOfThisType() uses NoChipPackageFilter to ensure
        // that each promotion has at least one PROMOTION_CONFIG entry that begins with 'buy.chips.package'.
        // This *should* be true, in any case. It seems to exist due to some previous data integrity problem.

        // Similarly, for DAILY_AWARD, removePromotionsNotOfThisType() uses NoRewardChipsFilter
        // to ensure there's at least one PROMOTION_CONFIG entry that begins with 'reward.chips'
        // It is, as far as I can see, not really required

        candidates = removePromotionsNotOfThisType(type, candidates);
        if (platform != null) {
            candidates = removePromotionsNotForThisPlatform(platform, candidates);
        }
        candidates = filterPromotionsWherePlayerAlreadyHasMaximumAwards(playerId, candidates);
        return candidates;
    }

    private class PromotionResultSetExtractor implements ResultSetExtractor<List<Promotion>> {
        @Override
        public List<Promotion> extractData(final ResultSet rs) throws SQLException, DataAccessException {
            final List<Promotion> promotions = new ArrayList<>();
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
                    final List<Platform> platforms = new ArrayList<>();
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
                    promo.setPriority(rs.getInt("priority"));
                    if (rs.wasNull()) {
                        promo.setPriority(null);
                    }
                    promo.setSeed(rs.getInt("seed"));
                    promo.setControlGroupPercentage(rs.getInt("control_group_percentage"));
                    promo.addConfigurationItem(rs.getString("config_key"), rs.getString("config_value"));
                    promo.setControlGroupFunction(ControlGroupFunctionType.valueOf(rs.getString("cg_function")));
                    lastId = currentId;
                }
            }
            return promotions;
        }
    }

    private List<Promotion> filterPromotionsWherePlayerAlreadyHasMaximumAwards(final BigDecimal playerId,
                                                                               final List<Promotion> candidates) {
        if (!candidates.isEmpty()) {
            // now remove candidates where players have already received maximum number of awards
            final StringBuilder idClause = new StringBuilder("pc.promo_id in (");
            final Iterator<Promotion> promotionIterator = candidates.iterator();
            while (promotionIterator.hasNext()) {
                idClause.append(promotionIterator.next().getId());
                if (promotionIterator.hasNext()) {
                    idClause.append(',');
                }
            }
            idClause.append(')');

            final List<Long> promotionsThatCanBeAwardedToPlayer = jdbcTemplate.query(
                    "select pc.promo_id, (config_value + 0) as maxRewards "
                            + "from PROMOTION_CONFIG pc left join PROMOTION_PLAYER_REWARD ppr on pc.promo_id = "
                            + "ppr.promo_id and " + idClause + " and player_id = ? where config_key = ? group by "
                            + "pc.promo_id having maxRewards > count(ppr.player_id)",
                    new RowMapper<Long>() {
                        @Override
                        public Long mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                            return rs.getLong("promo_id");
                        }
                    }, playerId, MAX_REWARDS_KEY);

            return filterNonApplicablePromotions(candidates, promotionsThatCanBeAwardedToPlayer);
        }
        return candidates;
    }

    private List<Promotion> removePromotionsNotOfThisType(final PromotionType type,
                                                          final List<Promotion> candidates) {
        List<Promotion> filteredCandidates = newArrayList(candidates);
        // remove promotions with missing required attributes
        final List<? extends Predicate<Promotion>> predicates = promotionConfigItemsFilter.get(type);
        if (predicates != null) {
            for (Predicate<Promotion> predicate : predicates) {
                // If the predicate returns false, remove the element.
                filteredCandidates = newArrayList(Collections2.filter(filteredCandidates, predicate));
            }
        }
        return filteredCandidates;
    }

    private List<Promotion> removePromotionsNotForThisPlatform(final Platform platform,
                                                               final List<Promotion> candidates) {
        List<Promotion> filteredCandidates = newArrayList(candidates);
        // remove promotions with missing required attributes
        final List<? extends Predicate<Promotion>> predicates = promotionPlatformFilter.get(platform);
        if (predicates != null) {
            for (Predicate<Promotion> predicate : predicates) {
                // If the predicate returns false, remove the element.
                filteredCandidates = newArrayList(Collections2.filter(filteredCandidates, predicate));
            }
        }
        return filteredCandidates;
    }

    @Override
    public List<DailyAwardPromotion> getWebDailyAwardPromotions(final BigDecimal playerId,
                                                                final DateTime currentTime) {
        return getDailyAwardPromotionsForPlayerAndPlatform(playerId, Platform.WEB, currentTime);
    }

    @Override
    public List<ProgressiveDailyAwardPromotion> getProgressiveDailyAwardPromotion(
            final BigDecimal playerId,
            final DateTime currentTime,
            final ProgressiveAwardEnum progressiveAward) {
        final String promotionType = "PROGRESSIVE_DAY_" + progressiveAward.getDailyAwardNumber();

        return jdbcTemplate.query(
                SELECT_PROGRESSIVE_DAILY_AWARD_PROMOTION, new RowMapper<ProgressiveDailyAwardPromotion>() {
                    @Override
                    public ProgressiveDailyAwardPromotion mapRow(final ResultSet resultSet, final int i) throws SQLException {

                        return new ProgressiveDailyAwardPromotion(PromotionType.getPromotionTypeFromString(promotionType),
                                resultSet.getLong("PROMO_ID"),
                                resultSet.getBigDecimal("CONFIG_VALUE"));
                    }
                }, promotionType);
    }

    @Override
    public List<DailyAwardPromotion> getIosDailyAwardPromotions(final BigDecimal playerId,
                                                                final DateTime currentTime) {
        return getDailyAwardPromotionsForPlayerAndPlatform(playerId, Platform.IOS, currentTime);
    }

    private List<DailyAwardPromotion> getDailyAwardPromotionsForPlayerAndPlatform(
            final BigDecimal playerId,
            final Platform platform,
            final DateTime currentTime) {

        final List<Promotion> promotions = findPromotionsFor(
                playerId, PromotionType.DAILY_AWARD, platform, currentTime);

        if (LOG.isDebugEnabled()) {
            for (Promotion promotion : promotions) {
                LOG.debug("found promotion: {}", promotion);
            }
        }
        if (promotions.isEmpty()) {
            return null;
        }

        sortDailyAwardPromosByRewardThenPriority(promotions);
        if (LOG.isDebugEnabled()) {
            for (Promotion promotion : promotions) {
                LOG.debug("sorted found promotion: {}", promotion);
            }
        }

        final List<DailyAwardPromotion> mostRelevantPromotions = new ArrayList<>();
        // we only required the first promotion and the default one (if different)
        final DailyAwardPromotion mostRelevant = (DailyAwardPromotion) promotions.get(0);
        mostRelevantPromotions.add(mostRelevant);
        if (!mostRelevant.isDefaultPromotion()) {
            final DailyAwardPromotion defaultDailyAward = findDefaultDailyAwardPromotion(promotions);
            if (defaultDailyAward != null) {
                mostRelevantPromotions.add(defaultDailyAward);
            }
        }
        return mostRelevantPromotions;
    }

    private DailyAwardPromotion findDefaultDailyAwardPromotion(final List<Promotion> promotions) {
        DailyAwardPromotion defaultCandidate = null;
        for (int i = 1; i < promotions.size(); i++) {
            defaultCandidate = (DailyAwardPromotion) promotions.get(i);
            if (defaultCandidate.isDefaultPromotion()) {
                break;
            }
        }
        return defaultCandidate;
    }

    private void sortDailyAwardPromosByRewardThenPriority(final List<Promotion> promotions) {
        Collections.sort(promotions, new DailyAwardPromotionComparator());
    }

    private void sortDailyAwardPromosByPriorityThenDate(final List<Promotion> promotions) {
        Collections.sort(promotions, new PromotionPriorityDateComparator());
    }

    private List<Promotion> filterNonApplicablePromotions(final List<Promotion> candidates,
                                                          final List<Long> promotionsThatCanBeAwardedToPlayer) {
        return newArrayList(Collections2.filter(candidates, new Predicate<Promotion>() {
            @Override
            public boolean apply(final Promotion promotion) {
                return promotionsThatCanBeAwardedToPlayer.contains(promotion.getId());
            }
        }));
    }

    @Override
    public void addLastReward(final PromotionPlayerReward promotionPlayerReward) {
        jdbcTemplate.update("insert into PROMOTION_PLAYER_REWARD (promo_id, player_id, control_group, rewarded_date, "
                        + "details) values (?,?,?,?,?)",
                promotionPlayerReward.getPromoId(), promotionPlayerReward.getPlayerId(),
                promotionPlayerReward.isControlGroup(), promotionPlayerReward.getRewardDate().toDate(),
                promotionPlayerReward.getDetails());
    }

    @Override
    public Map<PaymentPreferences.PaymentMethod, Promotion> getBuyChipsPromotions(
            final BigDecimal playerId,
            final Platform platform,
            final DateTime currentTime) {
        List<Promotion> promotions = findPromotionsFor(
                playerId, PromotionType.BUY_CHIPS, platform, currentTime);
        promotions = removePromotionsNotForThisPlatform(platform, promotions);
        return filterPromotions(promotions);    //it's not really clear what this does
    }

    /*
    Build a map with the highest priority promotion for each payment method.
    For each payment method, Promos in list are ordered. The highest priority is first, lowest priority is last.
     */
    private Map<PaymentPreferences.PaymentMethod, Promotion> filterPromotions(final List<Promotion> promotions) {

        int maxNumberOfPaymentMethods = PaymentPreferences.PaymentMethod.values().length;
        final Map<PaymentPreferences.PaymentMethod, Promotion> promotionsMap =
                new HashMap<>(maxNumberOfPaymentMethods);

        if (!promotions.isEmpty()) {
            // sort by priority, start date
            Collections.sort(promotions, new PromotionPriorityDateComparator());

            // pick promotion with highest priority for each active payment type
            for (Promotion promotion : promotions) {
                final String paymentMethodsStr =
                        promotion.getConfiguration().getConfigurationValue(PAYMENT_METHODS_KEY);
                for (String methodName : paymentMethodsStr.split(",")) {
                    final PaymentPreferences.PaymentMethod paymentMethod =
                            PaymentPreferences.PaymentMethod.valueOf(methodName);
                    if (!promotionsMap.containsKey(paymentMethod)) {
                        promotionsMap.put(paymentMethod, promotion);
                        LOG.debug("Adding Promo [id={}, type={}] to applicable promotions", promotion.getId(), paymentMethod);
                    }
                }
                if (promotionsMap.size() == maxNumberOfPaymentMethods) {
                    break;
                }
            }
        }
        return promotionsMap;
    }

    @Override
    public Promotion findById(final Long promoId) {
        final List<Promotion> promotions = jdbcTemplate.query(SELECT_PROMOTIONS_BY_ID_SQL,
                new PromotionResultSetExtractor(), promoId);
        if (promotions.size() > 0) {
            return promotions.get(0);
        }
        return null;
    }

    @Override
    public List<Promotion> findPromotionsByTypeOrderByPriority(final BigDecimal playerId,
                                                               final PromotionType type,
                                                               final Platform platform,
                                                               final DateTime currentTime) {
        final List<Promotion> promotions = findPromotionsFor(playerId, type, platform, currentTime);
        sortDailyAwardPromosByPriorityThenDate(promotions);
        return promotions;
    }

    @Override
    public List<BigDecimal> getProgressiveAwardPromotionValueList() {

        return jdbcTemplate.query(SELECT_PROMOTION_VALUES_SQL,
                new RowMapper<BigDecimal>() {
                    @Override
                    public BigDecimal mapRow(final ResultSet resultSet, final int i) throws SQLException {

                        return new BigDecimal(resultSet.getLong("config_value"));
                    }
                });
    }

    @Override
    public List<PromotionPlayerReward> findPromotionPlayerRewards(final BigDecimal playerId, final DateTime topUpDate) {
        return jdbcTemplate.query(SELECT_PROMOTION_PLAYER_REWARD,
                new RowMapper<PromotionPlayerReward>() {
                    @Override
                    public PromotionPlayerReward mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        return new PromotionPlayerReward(rs.getLong("PROMO_ID"),
                                playerId,
                                rs.getBoolean("CONTROL_GROUP"),
                                topUpDate,
                                rs.getString("DETAILS"));
                    }
                }, playerId, topUpDate.toDate());
    }

    @Override
    public void associateMarketingGroupMembersWithPromotion(final int marketingGroupId, final Long promoId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
