package com.yazino.promotions;

import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import helper.BuyChipsPromotionTestBuilder;
import helper.DailyAwardPromotionBuilder;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import strata.server.lobby.api.promotion.*;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.platform.Platform.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.*;
import static strata.server.lobby.api.promotion.DailyAwardPromotion.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class MysqlPromotionDaoTest {

    @Autowired
    @Qualifier("marketingJdbcTemplate")
    private JdbcTemplate marketingJdbcTemplate;

    private final BigDecimal playerId = BigDecimal.valueOf(666);

    @Autowired
    private PromotionDao underTest;

    @Before
    public void setUp() throws Exception {
        marketingJdbcTemplate.update("delete from PROMOTION_CONFIG where PROMO_ID > 10");
        marketingJdbcTemplate.update("delete from PROMOTION_PLAYER where PROMO_ID > 10");
        marketingJdbcTemplate.update("delete from PROMOTION_PLAYER_REWARD where PROMO_ID > 10");
        marketingJdbcTemplate.update("delete from PROMOTION where PROMO_ID > 10");


    }

    @Test(expected = java.lang.NullPointerException.class)
    public void marketingJdbcTemplateCannotBeNull() {
        new MysqlPromotionDao(null);
    }

    @Test(expected = java.lang.NullPointerException.class)
    public void createWithNullPromotionShouldFail() {
        underTest.create(null);
    }

    @Test
    @Transactional
    public void shouldCreatePromotionWithPriority() {
        // GIVEN the new promotion
        Promotion promo = new DailyAwardPromotionBuilder().build();

        // WHEN saving
        Long id = underTest.create(promo);

        // THEN the promotion should be persisted
        promo.setId(id);
        Promotion actual = findById(id);
        assertThat(actual, equalTo(promo));
    }

    @Test
    @Transactional
    public void shouldCreatePromotionNoPriority() {
        // GIVEN the new promotion
        Promotion promo = new DailyAwardPromotionBuilder().build();
        promo.setPriority(null);

        // WHEN saving
        Long id = underTest.create(promo);

        // THEN the promotion should be persisted
        promo.setId(id);
        Promotion actual = findById(id);
        assertThat(actual, equalTo(promo));
    }

    @Test
    @Transactional
    public void shouldCreatePromotionWithNoConfiguration() {
        // GIVEN the new promotion
        Promotion promo = new DailyAwardPromotionBuilder().build();
        promo.setConfiguration(null);

        // WHEN saving
        Long id = underTest.create(promo);

        // THEN the promotion should be persisted
        promo.setId(id);
        Promotion actual = findById(id);
        assertThat(actual, equalTo(promo));
    }

    @Test(expected = NullPointerException.class)
    public void updateWithNullPromotionShouldFail() {
        underTest.update(null);
    }

    @Test
    @Transactional
    public void shouldUpdatePromotion() {
        // GIVEN the existing promotion
        Promotion promo = new DailyAwardPromotionBuilder().build();
        Long id = underTest.create(promo);
        promo.setId(id);

        // WHEN updated
        promo.setName(promo.getName() + "a change");
        promo.setStartDate(promo.getStartDate().plusMinutes(200));
        promo.setEndDate(promo.getEndDate().plusMinutes(310));
        promo.setAllPlayers(false);
        promo.setPriority(88);

        PromotionConfiguration config = new PromotionConfiguration();
        config.addConfigurationItem(MAIN_IMAGE_KEY, DailyAwardPromotionBuilder.MAIN_IMAGE_VALUE + "updated");
        config.addConfigurationItem(MAIN_IMAGE_LINK_KEY, DailyAwardPromotionBuilder.MAIN_IMAGE_LINK_VALUE + "updated");
        config.addConfigurationItem(SECONDARY_IMAGE_KEY, DailyAwardPromotionBuilder.SECONDARY_IMAGE_VALUE + "updated");
        config.addConfigurationItem(SECONDARY_IMAGE_LINK_KEY, DailyAwardPromotionBuilder.SECONDARY_IMAGE_LINK_VALUE + "updated");
        config.addConfigurationItem(REWARD_CHIPS_KEY, DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE + "3");
        config.addConfigurationItem(MAX_REWARDS_KEY, DailyAwardPromotionBuilder.MAX_REWARDS_VALUE + "45");
        promo.setConfiguration(config);

        underTest.update(promo);

        // THEN the promotion should have all fields updated, except type
        Promotion actual = findById(id);
        assertThat(actual, equalTo(promo));
    }

    @Test
    @Transactional
    public void shouldUpdatePromotionAndDeletedConfiguration() {
        // GIVEN the existing promotion
        Promotion promo = new DailyAwardPromotionBuilder().build();
        Long id = underTest.create(promo);
        promo.setId(id);

        // WHEN updated
        promo.setName(promo.getName() + "a change");
        promo.setStartDate(promo.getStartDate().plusMinutes(200));
        promo.setEndDate(promo.getEndDate().plusMinutes(310));
        promo.setAllPlayers(false);

        promo.setConfiguration(null);

        underTest.update(promo);

        // THEN the promotion should have all fields updated, except type
        Promotion actual = findById(id);
        assertThat(actual, equalTo(promo));
    }

    @Test
    @Transactional
    public void shouldFindPromotion() {
        // GIVEN a promotion
        Promotion promo = new BuyChipsPromotionTestBuilder().build();
        Long id = underTest.create(promo);
        promo.setId(id);

        // WHEN searching by id
        Promotion actual = underTest.findById(id);

        // THEN promotion should be loaded
        assertThat(actual, is(promo));
    }

    @Test
    @Transactional
    public void shouldReturnNullWhenPromotionDoesNotExist() {
        // WHEN searching by an unknown id
        Promotion actual = underTest.findById(-6755232L);

        // THEN null is returned
        assertNull(actual);
    }

    @Test
    @Transactional
    public void shouldAddConfigOnUpdate() {
        // GIVEN the existing promotion without a daily award configuration
        Promotion promo = new DailyAwardPromotionBuilder().build();
        promo.setConfiguration(null);
        Long id = underTest.create(promo);
        promo.setId(id);

        // WHEN updated
        promo.setName(promo.getName() + "a change");
        promo.setStartDate(promo.getStartDate().plusMinutes(200));
        promo.setEndDate(promo.getEndDate().plusMinutes(310));
        promo.setAllPlayers(false);

        PromotionConfiguration config = new PromotionConfiguration();
        config.addConfigurationItem(MAIN_IMAGE_KEY, DailyAwardPromotionBuilder.MAIN_IMAGE_VALUE + "updated");
        config.addConfigurationItem(MAIN_IMAGE_LINK_KEY, DailyAwardPromotionBuilder.MAIN_IMAGE_LINK_VALUE + "updated");
        config.addConfigurationItem(SECONDARY_IMAGE_KEY, DailyAwardPromotionBuilder.SECONDARY_IMAGE_VALUE + "updated");
        config.addConfigurationItem(SECONDARY_IMAGE_LINK_KEY, DailyAwardPromotionBuilder.SECONDARY_IMAGE_LINK_VALUE + "updated");
        config.addConfigurationItem(REWARD_CHIPS_KEY, DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE + "3");
        config.addConfigurationItem(MAX_REWARDS_KEY, DailyAwardPromotionBuilder.MAX_REWARDS_VALUE + "45");
        promo.setConfiguration(config);
        underTest.update(promo);

        // THEN the promotion should have a daily award cfg
        Promotion actual = findById(id);
        assertThat(actual, equalTo(promo));
    }

    @Test(expected = NullPointerException.class)
    public void addPlayersToWithNullPromotionShouldFail() {
        underTest.addPlayersTo(null, new HashSet<BigDecimal>());
    }

    @Test(expected = NullPointerException.class)
    public void addPlayersToWithNullPlayersShouldFail() {
        underTest.addPlayersTo(1L, null);
    }

    @Test
    @Transactional
    public void shouldAddPlayersToPromotion() {
        // GIVEN the existing promotion
        Promotion promo = new DailyAwardPromotionBuilder().withAllPlayers(false).build();
        Long id = underTest.create(promo);
        promo.setId(id);
        // AND set of players
        Set<BigDecimal> playersInPromotion = new HashSet<BigDecimal>(Arrays.asList(BigDecimal.ONE, BigDecimal.TEN));

        // WHEN adding players to promotion
        underTest.addPlayersTo(id, playersInPromotion);

        // THEN the promotion players should be saved
        Set<BigDecimal> actualPlayerIds = findPromotionPlayers(id);
        assertThat(actualPlayerIds, hasItems(BigDecimal.ONE.setScale(2), BigDecimal.TEN.setScale(2)));
    }

    @Test
    @Transactional
    public void shouldIgnoreDuplicatePlayersWhenAddingPlayersToPromotion() {
        // GIVEN the existing promotion
        Promotion promo = new DailyAwardPromotionBuilder().withAllPlayers(false).build();
        Long id = underTest.create(promo);
        promo.setId(id);
        // AND set of players to add
        Set<BigDecimal> playersInPromotion = new HashSet<BigDecimal>(Arrays.asList(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN));

        // WHEN adding players to promotion
        underTest.addPlayersTo(id, playersInPromotion);

        // THEN the promotion players should be saved
        Set<BigDecimal> actualPlayerIds = findPromotionPlayers(id);
        assertTrue(actualPlayerIds.size() == 2);
        assertThat(actualPlayerIds, hasItems(BigDecimal.ONE.setScale(2), BigDecimal.TEN.setScale(2)));
    }

    @Test
    @Transactional
    public void shouldIgnoreDuplicatePlayersWhenAddingPlayersASecondTimeToPromotion() {
        // GIVEN the existing promotion
        Promotion promo = new DailyAwardPromotionBuilder().withAllPlayers(false).build();
        Long id = underTest.create(promo);
        promo.setId(id);
        // AND set of players already in promotion
        Set<BigDecimal> playersInPromotion = new HashSet<BigDecimal>(Arrays.asList(BigDecimal.ONE, BigDecimal.TEN));
        underTest.addPlayersTo(id, playersInPromotion);

        // WHEN adding players including already exiting players to promotion
        Set<BigDecimal> newPlayersInPromotion = new HashSet<BigDecimal>(Arrays.asList(BigDecimal.ONE, BigDecimal.valueOf(3l), BigDecimal.TEN));
        underTest.addPlayersTo(id, newPlayersInPromotion);

        // THEN the promotion players should be saved
        Set<BigDecimal> actualPlayerIds = findPromotionPlayers(id);
        assertTrue(actualPlayerIds.size() == 3);
        assertThat(actualPlayerIds, hasItems(new BigDecimal("1.00"), new BigDecimal("10.00"), new BigDecimal("3.00")));
    }

    @Test(expected = NullPointerException.class)
    public void deleteWithNullPromotionShouldFail() {
        underTest.delete(null);
    }

    @Test
    @Transactional
    public void shouldUpdatePromotionAndRemoveAssociatedPlayers() {
        // GIVEN the existing promotion and players
        Promotion promo = new DailyAwardPromotionBuilder().withAllPlayers(false).build();
        Long id = underTest.create(promo);
        promo.setId(id);
        // AND set of players
        Set<BigDecimal> playersInPromotion = new HashSet<BigDecimal>(Arrays.asList(BigDecimal.ONE, BigDecimal.TEN));
        underTest.addPlayersTo(id, playersInPromotion);

        // WHEN updating promotion and changing type to all players
        promo.setAllPlayers(true);
        underTest.update(promo);

        // THEN the promotion should have all fields updated
        Promotion actual = findById(id);
        assertThat(actual, equalTo(promo));
        // AND associated players should be deleted
        Set<BigDecimal> actualPlayerIds = findPromotionPlayers(id);
        assertTrue(actualPlayerIds.isEmpty());
    }

    @Test
    @Transactional
    public void shouldDeletePromotion() {
        // GIVEN the existing promotion
        Promotion promo = new DailyAwardPromotionBuilder().build();
        Long id = underTest.create(promo);
        promo.setId(id);

        // WHEN deleting
        underTest.delete(id);

        // THEN the promotion should be zapped
        assertFalse(promotionExists(id));
        // AND associated configuration should be dead as well
        assertFalse(configurationExists(id));
    }

    @Test
    @Transactional
    public void shouldDeletePromotionAndPromotionPlayers() {
        // GIVEN the existing promotion
        Promotion promo = new DailyAwardPromotionBuilder().build();
        Long id = underTest.create(promo);
        promo.setId(id);
        // AND set of players
        Set<BigDecimal> playersInPromotion = new HashSet<BigDecimal>(Arrays.asList(BigDecimal.ONE, BigDecimal.TEN));
        underTest.addPlayersTo(id, playersInPromotion);

        // WHEN deleting the promotion
        underTest.delete(id);

        // THEN the promotion, daily award and promotion players should be zapped
        assertFalse(promotionExists(id));
        assertTrue(findPromotionPlayers(id).isEmpty());
        assertFalse(configurationExists(id));
    }

    @Test
    @Transactional
    public void shouldAddLastRewardedDateWithDetails() {
        // given a promotion
        Promotion promo = new DailyAwardPromotionBuilder().build();
        Long id = underTest.create(promo);
        promo.setId(id);

        // when adding last reward for player
        DateTime rewardDate = new DateTime().withMillisOfSecond(0);
        BigDecimal playerId = BigDecimal.ONE;
        String details = "detrails";
        boolean controlGroup = false;
        PromotionPlayerReward promotionPlayerReward =
                new PromotionPlayerReward(id, playerId, controlGroup, rewardDate, details);
        underTest.addLastReward(promotionPlayerReward);

        // then is should be persisted
        int exists = marketingJdbcTemplate.queryForInt("select count(*) from PROMOTION_PLAYER_REWARD where promo_id=? " +
                        "and player_id=? and rewarded_date=? and details=? and control_group=?",
                id, playerId, rewardDate.toDate(), details, controlGroup
        );
        assertEquals(1, exists);
    }

    @Test
    @Transactional
    public void shouldDeletePromotionAndPromotionPlayersAndPromotionPlayerReward() {
        // GIVEN the existing promotion
        Promotion promo = new DailyAwardPromotionBuilder().build();
        Long id = underTest.create(promo);
        promo.setId(id);
        // AND set of players
        Set<BigDecimal> playersInPromotion = new HashSet<BigDecimal>(Arrays.asList(BigDecimal.ONE, BigDecimal.TEN));
        underTest.addPlayersTo(id, playersInPromotion);

        DateTime rewardDate = new DateTime();
        BigDecimal playerId = BigDecimal.ONE;
        boolean controlGroup = true;
        PromotionPlayerReward promotionPlayerReward =
                new PromotionPlayerReward(id, playerId, controlGroup, rewardDate, "reward=" + DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE);
        underTest.addLastReward(promotionPlayerReward);

        // WHEN deleting the promotion
        underTest.delete(id);

        // THEN the promotion, daily award, promotion players, and promotion player rewards should be zapped
        assertFalse(promotionExists(id));
        assertTrue(findPromotionPlayers(id).isEmpty());
        assertFalse(configurationExists(id));
        assertFalse(promotionPlayerRewardsExist(id));
    }

    @Test
    @Transactional
    public void shouldUpdatePlayerCountCorrectly() {
        // GIVEN the existing promotion
        Promotion promo = new DailyAwardPromotionBuilder().withAllPlayers(false).build();
        Long id = underTest.create(promo);
        promo.setId(id);
        // AND set of players
        Set<BigDecimal> playersInPromotion = new HashSet<BigDecimal>(Arrays.asList(BigDecimal.ONE, BigDecimal.TEN));
        underTest.addPlayersTo(id, playersInPromotion);

        // WHEN PlayerCount updated
        underTest.updatePlayerCountInPromotion(id);
        // THEN playerCount should be correct
        assertEquals(2, underTest.findById(id).getPlayerCount());
    }

    @Test(expected = NullPointerException.class)
    public void updatePlayerCountWithNullPromoIdShouldFail() {
        underTest.updatePlayerCountInPromotion(null);
    }

    @Test
    @Transactional
    public void shouldReturnPromotionOfAllTypes() {
        // GIVEN a set of promotions with 5 daily promos and 1 buy chip promo
        // and a player
        BigDecimal playerId = BigDecimal.ONE;
        DateTime testDate = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion dailyAwardPromotion = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(500)
                .withControlGroupPercentage(0)
                .build();
        Long id = underTest.create(dailyAwardPromotion);
        dailyAwardPromotion.setId(id);

        Promotion buyChipsPromotion = new BuyChipsPromotionTestBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(10)
                .build();
        id = underTest.create(buyChipsPromotion);
        buyChipsPromotion.setId(id);

        List<Promotion> promotions = underTest.findWebPromotions(playerId, testDate);
        assertThat(promotions, hasItems(dailyAwardPromotion, buyChipsPromotion));
    }

    @Test
    @Transactional
    public void shouldReturnPromotionOfAllTypesOrderedByPriority() {
        // GIVEN a set of promotions with 5 daily promos and 1 buy chip promo
        // and a player
        deleteAllPromotions();
        BigDecimal playerId = BigDecimal.ONE;
        DateTime testDate = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion dailyAwardPromotion = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(10)
                .withControlGroupPercentage(0)
                .build();
        Long id = underTest.create(dailyAwardPromotion);
        dailyAwardPromotion.setId(id);

        Promotion buyChipsPromotion = new BuyChipsPromotionTestBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(500)
                .build();
        id = underTest.create(buyChipsPromotion);
        buyChipsPromotion.setId(id);

        List<Promotion> promotions = underTest.findWebPromotionsOrderedByPriority(playerId, testDate);
        assertEquals(promotions.get(0), buyChipsPromotion);
        assertEquals(promotions.get(1), dailyAwardPromotion);
    }

    @Test
    @Transactional
    public void shouldReturnPromosSpanningGivenDate() {
        // GIVEN a set of promotions with 5 daily promos and 1 buy chip promo
        // and a player
        BigDecimal playerId = BigDecimal.ONE;
        DateTime testDate = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion promoAllPlayers = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .build();
        Long id = underTest.create(promoAllPlayers);
        promoAllPlayers.setId(id);

        Promotion promoInFuture = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 19, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 4, 10, 26, 30, 0))
                .withConfiguration(null).build();
        id = underTest.create(promoInFuture);
        promoInFuture.setId(id);

        Promotion promoIncludesPlayer1 = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 12, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 4, 10, 26, 30, 0))
                .withAllPlayers(false).build();
        id = underTest.create(promoIncludesPlayer1);
        promoIncludesPlayer1.setId(id);
        underTest.addPlayersTo(id, new HashSet<BigDecimal>(Arrays.asList(playerId)));
        // this buy chips promo should be ignored even though it spans the current date
        Promotion promoBuyChipsAllPlayers = new BuyChipsPromotionTestBuilder()
                .withStartDate(new DateTime(2007, 10, 12, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 4, 10, 26, 30, 0))
                .build();
        id = underTest.create(promoBuyChipsAllPlayers);
        promoBuyChipsAllPlayers.setId(id);

        Promotion promoIncludesPlayer10 = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 12, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 4, 10, 26, 30, 0))
                .withAllPlayers(false).build();
        id = underTest.create(promoIncludesPlayer10);
        promoIncludesPlayer10.setId(id);
        underTest.addPlayersTo(id, new HashSet<BigDecimal>(Arrays.asList(BigDecimal.TEN)));

        // WHEN query proms spanning date
        List<Promotion> promotionsSpanningDate = underTest.findPromotionsFor(playerId, PromotionType.DAILY_AWARD, WEB, testDate);

        // THEN only proms spanning date and with correct type are loaded
        assertThat(promotionsSpanningDate, hasItems(promoAllPlayers, promoIncludesPlayer1));
        // AND buy chips promo spanning date is NOT loaded
        assertThat(promotionsSpanningDate, not(hasItems(promoBuyChipsAllPlayers, promoIncludesPlayer10, promoInFuture)));
    }

    @Test
    @Transactional
    public void shouldReturnPromosWithEndDateEqualToNow() {
        // GIVEN an all player promotion end date equal to now
        DateTime now = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion promo1 = new DailyAwardPromotionBuilder().withStartDate(now.minusDays(12)).withEndDate(now).build();
        Long id = underTest.create(promo1);
        promo1.setId(id);

        // WHEN query promos spanning date
        List<Promotion> promotionsSpanningDate = underTest.findPromotionsFor(BigDecimal.ONE, PromotionType.DAILY_AWARD,
                WEB, now);

        // THEN only proms spanning date and with correct type are loaded
        assertThat(promotionsSpanningDate, hasItems(promo1));
    }

    @Test
    @Transactional
    public void shouldReturnPromosForCorrectPlatform() {
        // GIVEN an all player promotion end date equal to now
        DateTime now = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion promo1 = new DailyAwardPromotionBuilder().withStartDate(now.minusDays(12)).withEndDate(now)
                .withPlatforms(Arrays.asList(WEB, IOS)).build();
        Long id = underTest.create(promo1);
        promo1.setId(id);

        Promotion promo2 = new DailyAwardPromotionBuilder().withStartDate(now.minusDays(12)).withEndDate(now)
                .withPlatforms(Arrays.asList(IOS)).build();
        id = underTest.create(promo2);
        promo2.setId(id);

        Promotion promo3 = new DailyAwardPromotionBuilder().withStartDate(now.minusDays(12)).withEndDate(now)
                .withPlatforms(Arrays.asList(WEB)).build();
        id = underTest.create(promo3);
        promo3.setId(id);

        // WHEN query promos spanning date
        List<Promotion> promotionsForIos = underTest.findPromotionsFor(BigDecimal.ONE, PromotionType.DAILY_AWARD, IOS, now);

        // THEN only proms spanning date and with correct type are loaded
        assertThat(promotionsForIos, hasItems(promo1));
        assertThat(promotionsForIos, hasItems(promo2));
        assertThat(promotionsForIos, not(hasItem(promo3)));
    }

    @Test
    @Transactional
    public void shouldReturnPromosWithStartDateEqualToNow() {
        // GIVEN an all player promotion start date equal to now
        DateTime now = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion promo1 = new DailyAwardPromotionBuilder()
                .withStartDate(now).withEndDate(now.plusDays(12))
                .build();
        Long id = underTest.create(promo1);
        promo1.setId(id);
        // and a player
        BigDecimal playerId = BigDecimal.ONE;

        // WHEN query proms spanning date
        List<Promotion> promotionsSpanningDate = underTest.findPromotionsFor(playerId, PromotionType.DAILY_AWARD, WEB,
                now);

        // THEN only proms spanning date and with correct type are loaded
        assertThat(promotionsSpanningDate, hasItems(promo1));
    }

    @Test
    @Transactional
    public void shouldSetPriorityToNullForPromotionsFoundWithPriorityNotSet() {
        // GIVEN an all player promotion end date equal to now
        DateTime now = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion promo1 = new DailyAwardPromotionBuilder().withStartDate(now.minusDays(
                12)).withEndDate(now).build();
        promo1.setPriority(null);
        Long id = underTest.create(promo1);
        promo1.setId(id);

        // WHEN query promos spanning date
        List<Promotion> promotionsSpanningDate = underTest.findPromotionsFor(BigDecimal.ONE, PromotionType.DAILY_AWARD, WEB, now);

        // THEN promotions returned should have null priority
        assertThat(promotionsSpanningDate, hasItems(promo1));
    }

    @Test
    @Transactional
    public void shouldIgnoreDailyAwardPromosWithNoRewardChips() {
        // GIVEN an all player promotion end date equal to now
        DateTime now = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion promoWithNoRewardChips = new DailyAwardPromotionBuilder().withReward(null)
                .withStartDate(now.minusDays(1)).withEndDate(now.plusDays(1))
                .build();
        Long id = underTest.create(promoWithNoRewardChips);
        promoWithNoRewardChips.setId(id);
        Promotion promoWithReward = new DailyAwardPromotionBuilder().withStartDate(now.minusDays(12)).withEndDate(now.plusDays(1)).build();
        id = underTest.create(promoWithReward);
        promoWithReward.setId(id);

        // WHEN query promos spanning date
        List<Promotion> promotionsWithRewards = underTest.findPromotionsFor(BigDecimal.ONE, PromotionType.DAILY_AWARD, WEB, now);

        // THEN only promotions with reward chips are returned
        assertThat(promotionsWithRewards, hasItem(promoWithReward));
        assertThat(promotionsWithRewards, not(hasItem(promoWithNoRewardChips)));
    }

    @Test
    @Transactional
    public void shouldIgnoreBuyChipPromosWithNoPackageOverride() {
        // GIVEN an all player promotion end date equal to now
        DateTime now = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion promoWithNoPackageOverride = new BuyChipsPromotionTestBuilder().withChips(
                BuyChipsPromotionTestBuilder.CHIP_DEFAULT_PACKAGE_VALUE, null, WEB)
                .withStartDate(now.minusDays(1)).withEndDate(now.plusDays(1))
                .build();
        Long id = underTest.create(promoWithNoPackageOverride);
        promoWithNoPackageOverride.setId(id);
        Promotion promoWithPackageOverride = new BuyChipsPromotionTestBuilder().withChips("10000", "45000", WEB)
                .withStartDate(now.minusDays(12)).withEndDate(now.plusDays(1)).build();
        id = underTest.create(promoWithPackageOverride);
        promoWithPackageOverride.setId(id);

        // WHEN query promos spanning date
        List<Promotion> promotionsWithPackages = underTest.findPromotionsFor(BigDecimal.ONE, PromotionType.BUY_CHIPS, WEB, now);

        // THEN only promotions with reward chips are returned
        assertThat(promotionsWithPackages, hasItem(promoWithPackageOverride));
        assertThat(promotionsWithPackages, not(hasItem(promoWithNoPackageOverride)));
    }

    @Test
    @Transactional
    public void shouldIgnoreBuyChipPromosWithNoMaximumAwards() {
        // GIVEN an all player promotion end date equal to now
        DateTime now = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion promoWithNoPackageOverride = new BuyChipsPromotionTestBuilder().withMaxRewards(null)
                .withStartDate(now.minusDays(1)).withEndDate(now.plusDays(1))
                .build();
        Long id = underTest.create(promoWithNoPackageOverride);
        promoWithNoPackageOverride.setId(id);
        Promotion promoWithPackageOverride = new BuyChipsPromotionTestBuilder().withChips("10000", "45000", WEB)
                .withStartDate(now.minusDays(12)).withEndDate(now.plusDays(1)).build();
        id = underTest.create(promoWithPackageOverride);
        promoWithPackageOverride.setId(id);

        // WHEN query promos spanning date
        List<Promotion> promotionsWithRewards = underTest.findPromotionsFor(BigDecimal.ONE, PromotionType.BUY_CHIPS, WEB, now);

        // THEN only promotions with reward chips are returned
        assertThat(promotionsWithRewards, hasItem(promoWithPackageOverride));
        assertThat(promotionsWithRewards, not(hasItem(promoWithNoPackageOverride)));
    }

    @Test
    @Transactional
    public void shouldReturnDefaultDailyAwardWhenOtherPromotionsHaveExpired() {
        // given known set of promotions including the ever present default
        deleteAllPromotions();
        ensureDefaultDailyPromotionExists(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE_FOR_DEFAULT_AWARD);
        DateTime thePast = new DateTime().minusYears(1000);
        Promotion promo1 = new DailyAwardPromotionBuilder().withId(-1l).withReward(Integer.MAX_VALUE)
                .withStartDate(thePast).withEndDate(thePast.plusDays(1)).build();
        underTest.create(promo1);
        // and a player
        BigDecimal playerId = BigDecimal.ONE;

        // when querying the promotion to apply
        Promotion applicableDailyPromotion = underTest.getWebDailyAwardPromotions(playerId, new DateTime()).get(0);

        // then the promotion should be default one
        assertThat(applicableDailyPromotion.getName(), is(PromotionType.DAILY_AWARD.getDefaultPromotionName()));
    }

    @Test
    @Transactional
    public void shouldReturnDefaultDailyAwardWhenDefaultHasHighestReward() {
        // given known set of promotions including the ever present default
        deleteAllPromotions();
        ensureDefaultDailyPromotionExists(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE_FOR_DEFAULT_AWARD);
        DateTime now = new DateTime();
        // ensure player is not a control
        Promotion promo1 = new DailyAwardPromotionBuilder().withReward(Integer.parseInt(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE_FOR_DEFAULT_AWARD) - 1)
                .withStartDate(now.minusDays(1)).withEndDate(now.plusDays(1))
                .withPriority(Integer.MAX_VALUE)
                .withControlGroupPercentage(0)
                .build();
        underTest.create(promo1);
        // and a player
        BigDecimal playerId = BigDecimal.ONE;

        // when query the promotion to apply
        Promotion applicableDailyPromotion = underTest.getWebDailyAwardPromotions(playerId, new DateTime()).get(0);

        // then the promotion should be default one
        assertThat(applicableDailyPromotion.getName(), is(PromotionType.DAILY_AWARD.getDefaultPromotionName()));
    }

    private void deleteAllPromotions() {
        List<Long> promotionIds = marketingJdbcTemplate.queryForList("select promo_id from PROMOTION", Long.class);
        for (Long promotionId : promotionIds) {
            underTest.delete(promotionId);
        }
    }

    private void ensureDefaultDailyPromotionExists(String reward) {
        boolean exists = marketingJdbcTemplate.queryForObject("select count(*) > 0 from PROMOTION where name = ?",
                Boolean.class, PromotionType.DAILY_AWARD.getDefaultPromotionName());
        if (!exists) {
            Promotion defaultPromo = new DailyAwardPromotionBuilder()
                    .withStartDate(new DateTime().minusYears(1000))
                    .withEndDate(new DateTime().plusYears(1000))
                    .withName("Default Daily Award")
                    .withPriority(null)
                    .withReward(Integer.valueOf(reward))
                    .withMainImage(DailyAwardPromotionBuilder.MAIN_IMAGE_VALUE_FOR_DEFAULT)
                    .withMainImageLink(DailyAwardPromotionBuilder.MAIN_IMAGE_LINK_VALUE_FOR_DEFAULT)
                    .withSecImage(DailyAwardPromotionBuilder.SECONDARY_IMAGE_VALUE_FOR_DEFAULT)
                    .withSecImageLink(DailyAwardPromotionBuilder.SECONDARY_IMAGE_LINK_VALUE_FOR_DEFAULT)
                    .build();
            underTest.create(defaultPromo);
        }
    }

    @Test
    @Transactional
    public void shouldReturnPromotionWithHighestPriorityAndDefault() {
        // given known set of promotions with same reward including the ever present default
        deleteAllPromotions();
        // same reward as other promotions
        ensureDefaultDailyPromotionExists(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE);
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion promo1 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(10)).withEndDate(now.plusHours(23))
                .withPriority(null).build();
        Long id = underTest.create(promo1);
        promo1.setId(id);
        Promotion promo2 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(1)).withEndDate(now.plusHours(18))
                .withPriority(999).build();
        id = underTest.create(promo2);
        promo2.setId(id);
        Promotion promo3 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(14)).withEndDate(now.plusHours(2))
                .withPriority(10).build();
        id = underTest.create(promo3);
        promo3.setId(id);

        // and a player not previously rewarded for a promotion
        BigDecimal playerId = BigDecimal.ONE;

        // when querying promotion for date inside all promotions
        List<DailyAwardPromotion> applicableDailyPromotions = underTest.getWebDailyAwardPromotions(playerId, now);

        // then promotion with highest reward and  the default should be returned
        assertThat(applicableDailyPromotions.get(0), is(promo2));
        assertTrue(applicableDailyPromotions.get(1).isDefaultPromotion());
    }

    @Test
    @Transactional
    public void shouldReturnPromotionWithHighestPriorityWhenSeveralPromotionsShareHighestRewardAndTheDefault() {
        // given known set of promotions including the ever present default
        deleteAllPromotions();
        ensureDefaultDailyPromotionExists(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE);
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion promo1 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(10)).withEndDate(now.plusHours(23))
                .withPriority(10).withReward(10000).build();
        Long id = underTest.create(promo1);
        promo1.setId(id);
        Promotion promo2 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(10)).withEndDate(now.plusHours(23))
                .withPriority(10).withReward(10000).build();
        id = underTest.create(promo2);
        promo2.setId(id);
        Promotion promo3 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(10)).withEndDate(now.plusHours(23))
                .withPriority(1).withReward(10000).build();
        id = underTest.create(promo3);
        promo3.setId(id);

        // and a player not previously rewarded for a promotion
        BigDecimal playerId = BigDecimal.ONE;

        // when querying promotion for date inside all promotions
        List<DailyAwardPromotion> applicableDailyPromotions = underTest.getWebDailyAwardPromotions(playerId, now);

        // then promotion with highest priority and the default should be returned
        assertThat(applicableDailyPromotions.get(0), is(promo1));
        assertTrue(applicableDailyPromotions.get(1).isDefaultPromotion());
    }

    @Test
    @Transactional
    public void shouldReturnPromotionWithHighestRewardWhenAllPromotionsHaveNoPrioritySetAndTheDefault() {
        // given known set of promotions including the ever present default
        deleteAllPromotions();
        ensureDefaultDailyPromotionExists(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE_FOR_DEFAULT_AWARD);
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion promo1 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(10)).withEndDate(now.plusHours(23))
                .withPriority(null).withReward(10000).build();
        Long id = underTest.create(promo1);
        promo1.setId(id);
        Promotion promo2 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(10)).withEndDate(now.plusHours(23))
                .withPriority(null).withReward(20000).build();
        id = underTest.create(promo2);
        promo2.setId(id);
        Promotion promo3 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(10)).withEndDate(now.plusHours(23))
                .withPriority(null).withReward(30000).build();
        id = underTest.create(promo3);
        promo3.setId(id);

        // and a player not previously rewarded for a promotion
        BigDecimal playerId = BigDecimal.ONE;

        // when querying promotion for date inside all promotions
        List<DailyAwardPromotion> applicableDailyPromotions = underTest.getWebDailyAwardPromotions(playerId, now);

        // then promotion with highest reward and the default should be returned
        assertThat(applicableDailyPromotions.get(0), is(promo3));
        assertTrue(applicableDailyPromotions.get(1).isDefaultPromotion());
    }

    @Test
    @Transactional
    public void shouldReturnPromotionWithEarliestStartDateAndTheDefault() {
        // given known set of promotions with same priority reward and including the ever present default
        deleteAllPromotions();
        ensureDefaultDailyPromotionExists(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE_FOR_DEFAULT_AWARD);
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion promo1 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(100)).withEndDate(now.plusHours(23))
                .withPriority(0).withReward(10000).build();
        Long id = underTest.create(promo1);
        promo1.setId(id);

        Promotion promo2 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(12)).withEndDate(now.plusHours(23))
                .withPriority(10).withReward(10000).withPlatforms(Arrays.asList(IOS)).build();
        id = underTest.create(promo2);
        promo2.setId(id);

        Promotion promo3 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(11)).withEndDate(now.plusHours(23))
                .withPriority(1).withReward(10000).build();
        id = underTest.create(promo3);
        promo3.setId(id);

        Promotion promo4 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(12)).withEndDate(now.plusHours(23))
                .withPriority(1).withReward(10000).build();
        id = underTest.create(promo4);
        promo4.setId(id);

        // and a player not previously rewarded for a promotion
        BigDecimal playerId = BigDecimal.ONE;

        // when querying promotion for date inside all promotions
        List<DailyAwardPromotion> applicableDailyPromotions = underTest.getWebDailyAwardPromotions(playerId, now);

        // then promotion with highest reward, highest priority and earliest start date and the default should be returned
        assertThat(applicableDailyPromotions.get(0), is(promo4));
        assertTrue(applicableDailyPromotions.get(1).isDefaultPromotion());
    }

    @Test
    @Transactional
    public void whenPlayerIsInSelectivePromotionThatPromotionShouldBeReturnedIfItHasHighestPriority() {
        // given known set of promotions with different priorities
        deleteAllPromotions();
        ensureDefaultDailyPromotionExists(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE_FOR_DEFAULT_AWARD);
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion promo1 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(11)).withEndDate(now.plusHours(23))
                .withPriority(12).withReward(20000).build();
        Long id = underTest.create(promo1);
        promo1.setId(id);
        Promotion promo2 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(12)).withEndDate(now.plusHours(23))
                .withPriority(120).withReward(20000).build();
        id = underTest.create(promo2);
        promo2.setId(id);
        Promotion promo3 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(14)).withEndDate(now.plusHours(23))
                .withPriority(1200).withReward(20000).withAllPlayers(false).build();
        id = underTest.create(promo3);
        promo3.setId(id);

        // and a player in promo 3
        BigDecimal playerId = BigDecimal.ONE;
        underTest.addPlayersTo(promo3.getId(), new HashSet<BigDecimal>(Arrays.asList(playerId)));

        // when querying promotion for date inside all promotions
        List<DailyAwardPromotion> applicableDailyPromotions = underTest.getWebDailyAwardPromotions(playerId, now);

        // then promotion with highest reward, highest priority and earliest start date, and player and the default should be returned
        assertThat(applicableDailyPromotions.get(0), is(promo3));
        assertTrue(applicableDailyPromotions.get(1).isDefaultPromotion());
    }

    @Test
    @Transactional
    public void whenPlayerIsInSelectivePromotionThatPromotionShouldBeReturnedIfItHasHighestReward() {
        // given known set of promotions with same priority but different rewards and including the ever present default
        deleteAllPromotions();
        ensureDefaultDailyPromotionExists(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE_FOR_DEFAULT_AWARD);
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion promo1 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(120)).withEndDate(now.plusHours(23))
                .withReward(10000).build();
        Long id = underTest.create(promo1);
        promo1.setId(id);
        Promotion promo2 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(60)).withEndDate(now.plusHours(23))
                .withReward(20000).build();
        id = underTest.create(promo2);
        promo2.setId(id);
        Promotion promo3 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(12)).withEndDate(now.plusHours(23))
                .withReward(30000).withAllPlayers(false).build();
        id = underTest.create(promo3);
        promo3.setId(id);

        // and a player that has in promo 3
        BigDecimal playerId = BigDecimal.ONE;
        underTest.addPlayersTo(promo3.getId(), new HashSet<BigDecimal>(Arrays.asList(playerId)));

        // when querying promotion for date inside all promotions
        List<DailyAwardPromotion> applicableDailyPromotions = underTest.getWebDailyAwardPromotions(playerId, now);

        // then promotion with highest reward, highest priority and earliest start date, and player and the default should be returned
        assertThat(applicableDailyPromotions.get(0), is(promo3));
        assertTrue(applicableDailyPromotions.get(1).isDefaultPromotion());
    }

    @Test
    @Transactional
    public void whenPlayerIsInSelectivePromotionButHasReceivedMaxRewardsThatPromotionShouldBeIgnored() {
        // given known set of promotions
        deleteAllPromotions();
        ensureDefaultDailyPromotionExists(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE_FOR_DEFAULT_AWARD);
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion promo1 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(12)).withEndDate(now.plusHours(23))
                .withPriority(200).withReward(10000).build();
        Long id = underTest.create(promo1);
        promo1.setId(id);
        Promotion promo2 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(12)).withEndDate(now.plusHours(23))
                .withPriority(100).withReward(20000).build();
        id = underTest.create(promo2);
        promo2.setId(id);
        Promotion promo3 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(12)).withEndDate(now.plusHours(23))
                .withPriority(999).withReward(30000).withAllPlayers(false).withMaxRewards(1).build();
        id = underTest.create(promo3);
        promo3.setId(id);

        // and a player in promo 3 with max rewards
        BigDecimal playerId = BigDecimal.ONE;
        underTest.addPlayersTo(promo3.getId(), new HashSet<BigDecimal>(Arrays.asList(playerId)));
        underTest.addLastReward(new PromotionPlayerReward(promo3.getId(), playerId,
                false, new DateTime(), null));

        // when querying promotion for date inside all promotions
        List<DailyAwardPromotion> applicableDailyPromotions = underTest.getWebDailyAwardPromotions(playerId, now);

        // promo3 is ignored since player has max rewards for this promotion, promo 2 is returned since highest priority and the default
        assertThat(applicableDailyPromotions.get(0), is(promo2));
        assertTrue(applicableDailyPromotions.get(1).isDefaultPromotion());
    }

    @Test
    @Transactional
    public void whenPlayerHasReceivedMaxRewardsForANonSelectivePromotionThatPromotionShouldBeIgnored() {
        // given known set of promotions
        deleteAllPromotions();
        ensureDefaultDailyPromotionExists(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE_FOR_DEFAULT_AWARD);
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion promo1 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(12)).withEndDate(now.plusHours(23))
                .withPriority(200).withReward(10000).build();
        Long id = underTest.create(promo1);
        promo1.setId(id);
        Promotion promo2 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(12)).withEndDate(now.plusHours(23))
                .withPriority(100).withReward(20000).build();
        id = underTest.create(promo2);
        promo2.setId(id);
        Promotion promo3 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(12)).withEndDate(now.plusHours(23))
                .withPriority(999).withReward(30000).withMaxRewards(1).build();
        id = underTest.create(promo3);
        promo3.setId(id);

        // and a player in promo 3 with max rewards
        BigDecimal playerId = BigDecimal.ONE;
        underTest.addPlayersTo(promo3.getId(), new HashSet<BigDecimal>(Arrays.asList(playerId)));
        underTest.addLastReward(new PromotionPlayerReward(promo3.getId(), playerId,
                false, new DateTime(), "reward=7867"));

        // when querying promotion for date inside all promotions
        List<DailyAwardPromotion> applicableDailyPromotions = underTest.getWebDailyAwardPromotions(playerId, now);

        // promo3 is ignored since player has max rewards for this promotion, promo 2 is returned since highest priority and the default
        assertThat(applicableDailyPromotions.get(0), is(promo2));
        assertTrue(applicableDailyPromotions.get(1).isDefaultPromotion());
    }

    @Test
    @Transactional
    public void whenPlayerIsInFuturePromotionThenPromotionShouldBeIgnored() {
        // given known set of promotions with including the ever present default
        deleteAllPromotions();
        ensureDefaultDailyPromotionExists(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE_FOR_DEFAULT_AWARD);
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion promo2 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(12)).withEndDate(now.plusHours(23))
                .withReward(20000).build();
        long id = underTest.create(promo2);
        promo2.setId(id);
        Promotion promo3 = new DailyAwardPromotionBuilder()
                .withStartDate(now.plusDays(12)).withEndDate(now.plusDays(20))
                .withReward(30000).withAllPlayers(false).build();
        id = underTest.create(promo3);
        promo3.setId(id);

        // and a player that has already received max rewards for a promotion
        BigDecimal playerId = BigDecimal.ONE;
        underTest.addPlayersTo(promo3.getId(), new HashSet<BigDecimal>(Arrays.asList(playerId)));

        // when querying promotion for date inside all promotions
        List<DailyAwardPromotion> applicableDailyPromotions = underTest.getWebDailyAwardPromotions(playerId, now);

        // promo3 is ignored since it's in the future, promo 2  and the default are returned
        assertThat(applicableDailyPromotions.get(0), is(promo2));
        assertTrue(applicableDailyPromotions.get(1).isDefaultPromotion());
    }

    @Test
    @Transactional
    public void whenASelectivePromotionExcludesPlayerThenThatPromotionIsIgnore() {
        // given known set of promotions with same reward and including the ever present default
        deleteAllPromotions();
        ensureDefaultDailyPromotionExists(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE_FOR_DEFAULT_AWARD);
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion promo1 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(12)).withEndDate(now.plusHours(23))
                .withPriority(100).withAllPlayers(true).build();
        Long id = underTest.create(promo1);
        promo1.setId(id);

        Promotion promo2 = new DailyAwardPromotionBuilder()
                .withStartDate(now.minusDays(12)).withEndDate(now.plusHours(23))
                .withPriority(200).withAllPlayers(false).build();
        Long promo2Id = underTest.create(promo2);
        promo2.setId(promo2Id);

        BigDecimal playerId = BigDecimal.ONE;

        // when querying promotion for date inside all promotions
        List<DailyAwardPromotion> applicableDailyPromotions = underTest.getWebDailyAwardPromotions(playerId, now);

        // promo2 is ignored since player is not in this promo even though it has higher priority, so promo1 and default are returned
        assertThat(applicableDailyPromotions.get(0), is(promo1));
        assertTrue(applicableDailyPromotions.get(1).isDefaultPromotion());
    }

    @Test
    @Transactional
    public void shouldReturnEmptyMapWhenNoBuyChipPromotionsApply() {
        // GIVEN a set of buy chip promotions in the past and future
        deleteAllPromotions();
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion buyChipPromoInPast = new BuyChipsPromotionTestBuilder().withStartDate(now.minusDays(2)).withEndDate(now.minusDays(1)).build();
        Long id = underTest.create(buyChipPromoInPast);
        buyChipPromoInPast.setId(id);
        Promotion buyChipPromoInFuture = new BuyChipsPromotionTestBuilder().withStartDate(now.plusDays(1)).withEndDate(now.plusDays(2)).build();
        id = underTest.create(buyChipPromoInFuture);
        buyChipPromoInFuture.setId(id);

        // when requesting promos for player
        final Map<PaymentPreferences.PaymentMethod, Promotion> applicableBuyChipsPromotions = underTest.getBuyChipsPromotions(BigDecimal.ONE, WEB, now);

        // THEN no map is returned
        assertEquals(applicableBuyChipsPromotions.size(), 0);
    }

    @Test
    @Transactional
    public void whenMostApplicablePromotionAppliesToMultiplePaymentMethodsMapReturnedShouldContainEntryForEachMethod() {
        // GIVEN a creditcard and wirecard promo with highest priority
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion buyChipPromoWithHighestPriority = new BuyChipsPromotionTestBuilder()
                .withStartDate(now.minusDays(1)).withEndDate(now.plusDays(1))
                .withPriority(Integer.MAX_VALUE)
                .build();
        Long id = underTest.create(buyChipPromoWithHighestPriority);
        buyChipPromoWithHighestPriority.setId(id);

        // WHEN requesting promos for player
        final Map<PaymentPreferences.PaymentMethod, Promotion> applicableBuyChipsPromotions = underTest.getBuyChipsPromotions(
                BigDecimal.ONE, WEB, now);

        // THEN map should have credit card entry and a paypal entry
        assertThat(applicableBuyChipsPromotions.get(PaymentPreferences.PaymentMethod.CREDITCARD), is(
                buyChipPromoWithHighestPriority));
        assertThat(applicableBuyChipsPromotions.get(PaymentPreferences.PaymentMethod.PAYPAL), is(
                buyChipPromoWithHighestPriority));
    }

    @Test
    @Transactional
    public void shouldUsePromotionWithHighestPriorityForEachPaymentMethod() {
        // GIVEN a creditcard and wirecard promo with second highest priority
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion buyChipPromoForMultipleProviders = new BuyChipsPromotionTestBuilder()
                .withStartDate(now.minusDays(1)).withEndDate(now.plusDays(1))
                .withPriority(Integer.MAX_VALUE - 1)
                .build();
        Long id = underTest.create(buyChipPromoForMultipleProviders);
        buyChipPromoForMultipleProviders.setId(id);
        Promotion buyChipPromoForSingleProviderWithHighestPriority = new BuyChipsPromotionTestBuilder()
                .withStartDate(now.minusDays(1)).withEndDate(now.plusDays(1))
                .withPaymentMethod(PaymentPreferences.PaymentMethod.PAYPAL)
                .withPriority(Integer.MAX_VALUE).build();
        id = underTest.create(buyChipPromoForSingleProviderWithHighestPriority);
        buyChipPromoForSingleProviderWithHighestPriority.setId(id);
        Promotion lowPriorityPromotion = new BuyChipsPromotionTestBuilder()
                .withStartDate(now.minusDays(1)).withEndDate(now.plusDays(1))
                .withPriority(1).build();
        id = underTest.create(lowPriorityPromotion);
        lowPriorityPromotion.setId(id);

        // WHEN requesting promos for player
        final Map<PaymentPreferences.PaymentMethod, Promotion> applicableBuyChipsPromotions = underTest.getBuyChipsPromotions(BigDecimal.ONE, WEB, now);

        // THEN map should have correct promotions for payment type
        assertThat(applicableBuyChipsPromotions.get(PaymentPreferences.PaymentMethod.CREDITCARD), is(buyChipPromoForMultipleProviders));
        assertThat(applicableBuyChipsPromotions.get(PaymentPreferences.PaymentMethod.PAYPAL), is(
                buyChipPromoForSingleProviderWithHighestPriority));
    }

    @Test
    @Transactional
    public void shouldUsePromotionWithHighestPriorityForEachPaymentMethodWhenPromotionsHaveSinglePaymentMethod() {
        // GIVEN a creditcard and wirecard promo with second highest priority
        DateTime now = new DateTime().withMillisOfDay(0);
        Promotion creditCardPromo = new BuyChipsPromotionTestBuilder()
                .withStartDate(now.minusDays(1)).withEndDate(now.plusDays(1))
                .withPaymentMethod(PaymentPreferences.PaymentMethod.CREDITCARD)
                .withPriority(Integer.MAX_VALUE - 1)
                .build();
        Long id = underTest.create(creditCardPromo);
        creditCardPromo.setId(id);
        Promotion paypalPromo = new BuyChipsPromotionTestBuilder()
                .withStartDate(now.minusDays(1)).withEndDate(now.plusDays(1))
                .withPaymentMethod(PaymentPreferences.PaymentMethod.PAYPAL)
                .withPriority(Integer.MAX_VALUE).build();
        id = underTest.create(paypalPromo);
        paypalPromo.setId(id);
        Promotion lowPriorityPromotion = new BuyChipsPromotionTestBuilder()
                .withStartDate(now.minusDays(1)).withEndDate(now.plusDays(1))
                .withPriority(1).build();
        id = underTest.create(lowPriorityPromotion);
        lowPriorityPromotion.setId(id);

        // WHEN requesting promos for player
        final Map<PaymentPreferences.PaymentMethod, Promotion> applicableBuyChipsPromotions = underTest.getBuyChipsPromotions(BigDecimal.ONE, WEB, now);

        // THEN map should have correct promotions for payment type
        assertThat(applicableBuyChipsPromotions.get(PaymentPreferences.PaymentMethod.CREDITCARD), is(creditCardPromo));
        assertThat(applicableBuyChipsPromotions.get(PaymentPreferences.PaymentMethod.PAYPAL), is(paypalPromo));
    }

    @Test
    @Transactional
    public void getProgressiveDailyAwardPromotionShouldReturnListOfProgressiveDailyAwardPromotions() {
        BigDecimal playerId = new BigDecimal(String.valueOf(-12));
        final Long promoId = marketingJdbcTemplate.queryForLong("SELECT PROMO_ID FROM PROMOTION WHERE TYPE = 'PROGRESSIVE_DAY_1'");
        ProgressiveDailyAwardPromotion progressiveDailyAwardPromotion = new ProgressiveDailyAwardPromotion(PromotionType.PROGRESSIVE_DAY_1, promoId, new BigDecimal(2500));
        final List<ProgressiveDailyAwardPromotion> progressiveDailyAwardPromotionList = underTest.getProgressiveDailyAwardPromotion(playerId, new DateTime(), ProgressiveAwardEnum.AWARD_1);
        assertEquals(Arrays.asList(progressiveDailyAwardPromotion), progressiveDailyAwardPromotionList);
    }

    private Promotion findById(final Long id) {
        Promotion promo = marketingJdbcTemplate.queryForObject("select p.promo_id, p.type, p.name, p.target_clients, p.start_date, p.end_date, " +
                "p.priority, p.all_players, p.player_count, p.seed, p.control_group_percentage, p.cg_function from PROMOTION p " +
                "where p.promo_id = ?", new RowMapper<Promotion>() {
            @Override
            public Promotion mapRow(ResultSet resultSet, int i) throws SQLException {
                final PromotionType type = PromotionType.valueOf(resultSet.getString("type"));
                Promotion promo = PromotionFactory.createPromotion(type);
                promo.setId(id);
                promo.setName(resultSet.getString("name"));
                final String platformsString = resultSet.getString("target_clients");
                final List<Platform> platforms = new ArrayList<Platform>();
                if (StringUtils.isNotBlank(platformsString)) {
                    for (String platform : platformsString.split(",")) {
                        platforms.add(Platform.valueOf(platform));
                    }
                }
                promo.setPlatforms(platforms);
                promo.setStartDate(new DateTime(resultSet.getTimestamp("start_date")));
                promo.setEndDate(new DateTime(resultSet.getTimestamp("end_date")));
                promo.setAllPlayers(resultSet.getBoolean("all_players"));
                promo.setPlayerCount(resultSet.getInt("player_count"));
                promo.setPriority(resultSet.getInt("priority"));
                if (resultSet.wasNull()) {
                    promo.setPriority(null);
                }
                promo.setSeed(resultSet.getInt("seed"));
                promo.setControlGroupPercentage(resultSet.getInt("control_group_percentage"));
                promo.setControlGroupFunction(ControlGroupFunctionType.valueOf(resultSet.getString("cg_function")));
                return promo;
            }
        }, id);
        PromotionConfiguration config = marketingJdbcTemplate.query(
                "select config_key, config_value from PROMOTION_CONFIG where promo_id=?",
                new Object[]{id}, new ResultSetExtractor<PromotionConfiguration>() {
                    @Override
                    public PromotionConfiguration extractData(ResultSet rs) throws SQLException, DataAccessException {
                        PromotionConfiguration promotionConfiguration = new PromotionConfiguration();
                        while (rs.next()) {
                            promotionConfiguration.addConfigurationItem(rs.getString(1), rs.getString(2));
                        }
                        if (promotionConfiguration.getConfiguration().isEmpty()) {
                            return null;
                        } else {
                            return promotionConfiguration;
                        }
                    }
                }
        );
        promo.setConfiguration(config);
        return promo;
    }

    @Test
    @Transactional
    public void getPromotionByTypeOrderByPriorityShouldReturnCorrectPromotionsForDailyAwardAndWeb() {
        deleteAllPromotions();
        BigDecimal playerId = new BigDecimal(-937);
        PromotionType type = PromotionType.DAILY_AWARD;
        Platform platform = WEB;

        DateTime testDate = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion dailyAwardPromotionPriority10 = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(10)
                .withControlGroupPercentage(0)
                .build();
        Long id = underTest.create(dailyAwardPromotionPriority10);
        dailyAwardPromotionPriority10.setId(id);

        Promotion dailyAwardPromotionPriority20 = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(20)
                .withControlGroupPercentage(0)
                .build();
        id = underTest.create(dailyAwardPromotionPriority20);
        dailyAwardPromotionPriority20.setId(id);

        Promotion buyChipsPromotion = new BuyChipsPromotionTestBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(500)
                .build();
        id = underTest.create(buyChipsPromotion);
        buyChipsPromotion.setId(id);

        final List<Promotion> promotions = underTest.findPromotionsByTypeOrderByPriority(playerId, type, platform, testDate);

        assertEquals(2, promotions.size());
        assertEquals(dailyAwardPromotionPriority20, promotions.get(0));
        assertEquals(dailyAwardPromotionPriority10, promotions.get(1));
    }

    @Test
    @Transactional
    public void getPromotionByTypeOrderByPriorityShouldReturnCorrectPromotionsForWeb() {
        deleteAllPromotions();
        BigDecimal playerId = new BigDecimal(-937);
        Platform platform = WEB;
        List<Platform> webPlatform = new ArrayList<Platform>();
        List<Platform> iosPlatform = new ArrayList<Platform>();
        webPlatform.add(WEB);
        iosPlatform.add(IOS);

        DateTime testDate = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion dailyAwardPromotionPriority10 = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(10)
                .withControlGroupPercentage(0)
                .withPlatforms(webPlatform)
                .build();
        Long id = underTest.create(dailyAwardPromotionPriority10);
        dailyAwardPromotionPriority10.setId(id);

        Promotion dailyAwardPromotionPriority20 = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(20)
                .withControlGroupPercentage(0)
                .build();
        id = underTest.create(dailyAwardPromotionPriority20);
        dailyAwardPromotionPriority20.setId(id);

        Promotion iosDailyAwardPromotionPriority30 = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(20)
                .withControlGroupPercentage(0)
                .withPlatforms(iosPlatform)
                .build();
        id = underTest.create(iosDailyAwardPromotionPriority30);
        iosDailyAwardPromotionPriority30.setId(id);


        Promotion buyChipsPromotionPriority100 = new BuyChipsPromotionTestBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(100)
                .build();
        id = underTest.create(buyChipsPromotionPriority100);
        buyChipsPromotionPriority100.setId(id);


        Promotion buyChipsPromotionPriority200 = new BuyChipsPromotionTestBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(200)
                .build();
        id = underTest.create(buyChipsPromotionPriority200);
        buyChipsPromotionPriority200.setId(id);


        final List<Promotion> buyChipPromotions = underTest.findPromotionsByTypeOrderByPriority(playerId, PromotionType.BUY_CHIPS, platform, testDate);
        final List<Promotion> dailyAwardPromotions = underTest.findPromotionsByTypeOrderByPriority(playerId, PromotionType.DAILY_AWARD, platform, testDate);


        assertEquals(2, buyChipPromotions.size());
        assertEquals(buyChipsPromotionPriority200, buyChipPromotions.get(0));
        assertEquals(buyChipsPromotionPriority100, buyChipPromotions.get(1));

        assertEquals(2, dailyAwardPromotions.size());
        assertEquals(dailyAwardPromotionPriority20, dailyAwardPromotions.get(0));
        assertEquals(dailyAwardPromotionPriority10, dailyAwardPromotions.get(1));
    }

    @Test
    @Transactional
    public void getPromotionByTypeOrderByPriorityShouldReturnCorrectPromotionsForIos() {
        deleteAllPromotions();
        BigDecimal playerId = new BigDecimal(-937);
        Platform platform = IOS;
        List<Platform> webPlatform = new ArrayList<Platform>();
        List<Platform> iosPlatform = new ArrayList<Platform>();
        webPlatform.add(WEB);
        iosPlatform.add(IOS);

        DateTime testDate = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion dailyAwardPromotionPriority10 = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(10)
                .withControlGroupPercentage(0)
                .withPlatforms(webPlatform)
                .build();
        Long id = underTest.create(dailyAwardPromotionPriority10);
        dailyAwardPromotionPriority10.setId(id);

        Promotion dailyAwardPromotionPriority20 = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(20)
                .withControlGroupPercentage(0)
                .build();
        id = underTest.create(dailyAwardPromotionPriority20);
        dailyAwardPromotionPriority20.setId(id);

        Promotion iosDailyAwardPromotionPriority30 = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(30)
                .withControlGroupPercentage(0)
                .withPlatforms(iosPlatform)
                .build();
        id = underTest.create(iosDailyAwardPromotionPriority30);
        iosDailyAwardPromotionPriority30.setId(id);


        Promotion buyChipsPromotionPriority100 = new BuyChipsPromotionTestBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26,
                        30, 0))
                .withPriority(100)
                .withPlatforms(webPlatform)
                .build();
        id = underTest.create(buyChipsPromotionPriority100);
        buyChipsPromotionPriority100.setId(id);


        Promotion buyChipsPromotionPriority200 = new BuyChipsPromotionTestBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26,
                        30, 0))
                .withPriority(200)
                .withPlatforms(iosPlatform)
                .build();
        id = underTest.create(buyChipsPromotionPriority200);
        buyChipsPromotionPriority200.setId(id);


        final List<Promotion> buyChipPromotions = underTest.findPromotionsByTypeOrderByPriority(playerId, PromotionType.BUY_CHIPS, platform, testDate);
        final List<Promotion> dailyAwardPromotions = underTest.findPromotionsByTypeOrderByPriority(playerId, PromotionType.DAILY_AWARD, platform, testDate);


        assertEquals(1, buyChipPromotions.size());
        assertEquals(buyChipsPromotionPriority200, buyChipPromotions.get(0));

        assertEquals(2, dailyAwardPromotions.size());
        assertEquals(iosDailyAwardPromotionPriority30, dailyAwardPromotions.get(0));
        assertEquals(dailyAwardPromotionPriority20, dailyAwardPromotions.get(1));
    }

    @Test
    @Transactional
    public void getPromotionByTypeOrderByPriorityShouldReturnCorrectPromotionsForAndroid() {
        deleteAllPromotions();
        BigDecimal playerId = new BigDecimal(-937);
        Platform platform = ANDROID;
        List<Platform> webPlatform = new ArrayList<Platform>();
        List<Platform> iosPlatform = new ArrayList<Platform>();
        List<Platform> androidPlatform = new ArrayList<Platform>();
        webPlatform.add(WEB);
        iosPlatform.add(IOS);
        androidPlatform.add(ANDROID);

        DateTime testDate = new DateTime(2007, 10, 13, 10, 9, 8, 0);
        Promotion dailyAwardPromotionPriority10 = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(10)
                .withControlGroupPercentage(0)
                .withPlatforms(webPlatform)
                .build();
        Long id = underTest.create(dailyAwardPromotionPriority10);
        dailyAwardPromotionPriority10.setId(id);

        Promotion dailyAwardPromotionPriority20 = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(20)
                .withControlGroupPercentage(0)
                .withPlatforms(iosPlatform)
                .build();
        id = underTest.create(dailyAwardPromotionPriority20);
        dailyAwardPromotionPriority20.setId(id);

        Promotion androidDailyAwardPromotion = new DailyAwardPromotionBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(30)
                .withControlGroupPercentage(0)
                .withPlatforms(androidPlatform)
                .build();
        id = underTest.create(androidDailyAwardPromotion);
        androidDailyAwardPromotion.setId(id);


        Promotion buyChipsPromotionPriority100 = new BuyChipsPromotionTestBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(100)
                .withPlatforms(webPlatform)
                .build();
        id = underTest.create(buyChipsPromotionPriority100);
        buyChipsPromotionPriority100.setId(id);


        Promotion buyChipsPromotionPriority200 = new BuyChipsPromotionTestBuilder()
                .withStartDate(new DateTime(2007, 10, 9, 10, 26, 30, 0)).withEndDate(new DateTime(2007, 11, 9, 10, 26, 30, 0))
                .withPriority(200)
                .withPlatforms(androidPlatform)
                .build();
        id = underTest.create(buyChipsPromotionPriority200);
        buyChipsPromotionPriority200.setId(id);


        final List<Promotion> buyChipPromotions = underTest.findPromotionsByTypeOrderByPriority(playerId, PromotionType.BUY_CHIPS, platform, testDate);
        final List<Promotion> dailyAwardPromotions = underTest.findPromotionsByTypeOrderByPriority(playerId, PromotionType.DAILY_AWARD, platform, testDate);

        assertEquals(1, buyChipPromotions.size());
        assertEquals(buyChipsPromotionPriority200, buyChipPromotions.get(0));

        assertEquals(1, dailyAwardPromotions.size());
        assertEquals(androidDailyAwardPromotion, dailyAwardPromotions.get(0));
    }

    @Test
    @Transactional
    public void getProgressiveAwardPromotionValueMapReturnsMapOfPromotionValues() {
        assertEquals(Arrays.asList(new BigDecimal(2500),
                new BigDecimal(3000),
                new BigDecimal(3500),
                new BigDecimal(4000),
                new BigDecimal(5000)), underTest.getProgressiveAwardPromotionValueList());
    }

    @Test
    @Transactional
    public void shouldReturnNoPromotionPlayerRewardsWhenNoRewardsExistOnTopUpDate() {
        // GIVEN the promotion and player awards
        Promotion promo = new DailyAwardPromotionBuilder().build();
        Long promoId = underTest.create(promo);
        DateTime awardedDate = new DateTime();
        PromotionPlayerReward ppr1 = new PromotionPlayerReward(promoId, BigDecimal.valueOf(2222), false, awardedDate, "details");
        underTest.addLastReward(ppr1);

        final List<PromotionPlayerReward> promotionPlayerRewards = underTest.findPromotionPlayerRewards(
                BigDecimal.valueOf(2222), awardedDate.minusMinutes(1));

        assertTrue(promotionPlayerRewards.isEmpty());
    }

    @Test
    @Transactional
    public void shouldReturnPromotionPlayerRewardsForPlayerAndTopUpDate() {
        // GIVEN the promotion and player awards
        Promotion promo = new DailyAwardPromotionBuilder().build();
        Long promoId = underTest.create(promo);
        final Long progressivePromoId = marketingJdbcTemplate.queryForLong(
                "SELECT PROMO_ID FROM PROMOTION WHERE TYPE = 'PROGRESSIVE_DAY_1'");

        DateTime awardedDate = new DateTime().withMillisOfSecond(0);

        final BigDecimal playerId = BigDecimal.valueOf(2222L);
        // ppr1 does not match topUpdate
        PromotionPlayerReward ppr1 = new PromotionPlayerReward(promoId, playerId, false, awardedDate.minusMinutes(1), "details");
        underTest.addLastReward(ppr1);

        // these do match topUpdate and player id
        PromotionPlayerReward ppr2 = new PromotionPlayerReward(progressivePromoId, playerId, false, awardedDate, "details");
        underTest.addLastReward(ppr2);
        PromotionPlayerReward ppr3 = new PromotionPlayerReward(promoId, playerId, false, awardedDate, "details");
        underTest.addLastReward(ppr3);

        // this one does not match the player id
        PromotionPlayerReward ppr4 = new PromotionPlayerReward(promoId, playerId.add(BigDecimal.TEN), false, awardedDate, "details");
        underTest.addLastReward(ppr4);

        final List<PromotionPlayerReward> promotionPlayerRewards = underTest.findPromotionPlayerRewards(
                playerId, awardedDate);

        assertThat(promotionPlayerRewards.size(), is(2));
        assertThat(promotionPlayerRewards, hasItems(ppr2, ppr3));
    }

    @Test
    @Transactional
    public void addPlayersToPromoShouldNotForAllPlayerPromos() {

        Promotion promo = new helper.DailyAwardPromotionBuilder().withAllPlayers(true).build();

        final Long promoId = underTest.create(promo);

        final HashSet<BigDecimal> playerIds = newHashSet();
        playerIds.add(playerId);

        underTest.addPlayersTo(promoId, playerIds);
        final int promoPlayers = marketingJdbcTemplate.queryForInt("select count(*) from PROMOTION_PLAYER where promo_id =?", promoId);
        assertThat(promoPlayers, is(0));

    }

    @Test
    @Transactional
    public void addPlayersToPromoShouldForNonAllPlayerPromos() {

        Promotion promo = new helper.DailyAwardPromotionBuilder().withAllPlayers(false).build();

        final Long promoId = underTest.create(promo);

        final HashSet<BigDecimal> playerIds = newHashSet();
        playerIds.add(playerId);

        underTest.addPlayersTo(promoId, playerIds);
        final int promoPlayers = marketingJdbcTemplate.queryForInt("select count(*) from PROMOTION_PLAYER where promo_id =?", promoId);
        assertThat(promoPlayers, is(1));

    }

    @Test
    @Transactional
    public void allPlayerPromoShouldLoadForAllPlatforms() throws InterruptedException {
        final String slots = "SLOTS";
        final GiftingPromotion promo = new GiftingPromotion(
                now().minusMinutes(1), now().plusDays(1), 5000L, true, "GIFTING PROMO", "GIFT TITLE", "GIFT DESCRIPTION", slots);
        final Long promoId = underTest.create(promo);
        final List<Promotion> promotionList = underTest.findPromotionsFor(playerId, PromotionType.GIFTING, null, now());
        assertThat(promotionList.size(), is(1));
        final GiftingPromotion promotion = (GiftingPromotion) promotionList.get(0);
        assertThat(promotion.getName(), is(equalTo("GIFTING PROMO")));
        assertThat(promotion.getId(), is(promoId));
        assertThat(promotion.getGameTypes(), is(equalTo(slots)));
        assertThat(promotion.getGiftDescription(), is(equalTo("GIFT DESCRIPTION")));
        assertThat(promotion.getGiftTitle(), is(equalTo("GIFT TITLE")));

        underTest.addLastReward(new PromotionPlayerReward(promoId, playerId, false, now(), ""));
        final List<Promotion> newPromotionList = underTest.findPromotionsFor(playerId, PromotionType.GIFTING, null, now());
        assertThat(newPromotionList.size(), is(0));


    }

    private Set<BigDecimal> findPromotionPlayers(final Long promoId) {
        return new HashSet<BigDecimal>(marketingJdbcTemplate.queryForList("select player_id from PROMOTION_PLAYER where promo_id = ?", BigDecimal.class, promoId));
    }

    private boolean promotionExists(final Long id) {
        return marketingJdbcTemplate.queryForInt("select count(*) from PROMOTION where promo_id = ?", id) == 1;
    }

    private boolean configurationExists(Long id) {
        return marketingJdbcTemplate.queryForInt("select count(*) from PROMOTION_CONFIG where promo_id = ?", id) == 1;
    }

    private boolean promotionPlayerRewardsExist(Long promotionId) {
        return marketingJdbcTemplate.queryForInt("select count(*) from PROMOTION_PLAYER_REWARD where promo_id = ?", promotionId) == 1;
    }
}
