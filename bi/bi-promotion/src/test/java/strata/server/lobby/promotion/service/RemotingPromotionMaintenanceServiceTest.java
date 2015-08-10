package strata.server.lobby.promotion.service;

import com.yazino.promotions.PromotionDao;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.lobby.api.promotion.*;
import strata.server.lobby.api.promotion.helper.DailyAwardPromotionBuilder;
import strata.server.lobby.promotion.tools.PromotionFunctions;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static strata.server.lobby.api.promotion.DailyAwardPromotion.*;
import static strata.server.lobby.api.promotion.PromotionType.DAILY_AWARD;

@RunWith(MockitoJUnitRunner.class)
public class RemotingPromotionMaintenanceServiceTest {
    public static final String MAIN_IMAGE_VALUE = "main image";
    public static final String MAIN_IMAGE_LINK_VALUE = "main image link";
    public static final String SECONDARY_IMAGE_VALUE = "secondary image";
    public static final String SECONDARY_IMAGE_LINK_VALUE = "secondary image link";
    public static final String REWARD_CHIPS_VALUE = "4500";
    public static final String MAX_REWARDS_VALUE = "2";
    private static final Long PROMOTION_ID = 12345l;

    @Mock
    private PromotionDao promotionDao;

    @Mock
    private PromotionFunctions promotionFunctions;

    private Promotion defaultPromotion;

    private RemotingPromotionMaintenanceService underTest;

    @Before
    public void init() {
        defaultPromotion = createDefaultDailyAward();
        underTest = new RemotingPromotionMaintenanceService(promotionDao, promotionFunctions, defaultPromotion);
    }

    @Test(expected = NullPointerException.class)
    public void createWithNullPromotionShouldFail() {
        underTest.create(null);
    }

    @Test
    public void createShouldGenerateSeedForPromotion() {
        Promotion promotion = new BuyChipsPromotion();
        when(promotionFunctions.generateSeed()).thenReturn(23);

        underTest.create(promotion);

        assertThat(promotion.getSeed(), is(23));
    }

    @Test
    public void createShouldDelegatetoDao() {
        Promotion promotion = new BuyChipsPromotion();
        when(promotionDao.create(promotion)).thenReturn(1L);
        underTest.create(promotion);
        verify(promotionDao).create(promotion);
    }

    @Test
    public void createShouldReturnPromoId() {
        Promotion promotion = new BuyChipsPromotion();
        when(promotionDao.create(promotion)).thenReturn(1L);
        Long actualPromotionId = underTest.create(promotion);
        assertThat(actualPromotionId, is(1L));
    }

    @Test(expected = NullPointerException.class)
    public void updateWithNullPromotionShouldFail() {
        underTest.update(null);
    }

    @Test
    public void updateShouldDelegatetoDao() {
        Promotion promotion = new BuyChipsPromotion();
        underTest.update(promotion);
        verify(promotionDao).update(promotion);
    }

    @Test(expected = NullPointerException.class)
    public void deleteWithNullPromotionIdShouldFail() {
        underTest.delete(null);
    }

    @Test
    public void deleteShouldDelegatetoDao() {
        Promotion toDelete = new DailyAwardPromotionBuilder().withId(PROMOTION_ID).getPromotion();
        given(promotionDao.findById(PROMOTION_ID)).willReturn(toDelete);
        underTest.delete(PROMOTION_ID);
        verify(promotionDao).delete(PROMOTION_ID);
    }

    @Test(expected = NullPointerException.class)
    public void addPlayersToWithNullPromotionIdShouldFail() {
        underTest.addPlayersTo(null, new HashSet<BigDecimal>());
    }

    @Test(expected = NullPointerException.class)
    public void addPlayersToWithNullPlayerIdsShouldFail() {
        underTest.addPlayersTo(1L, null);
    }

    @Test
    public void addPlayersToShouldDelegatetoDaoMethods() {
        Long ID = 1L;
        Set<BigDecimal> PLAYERS = new HashSet<BigDecimal>(Arrays.asList(new BigDecimal [] { BigDecimal.ONE, BigDecimal.TEN }));
        underTest.addPlayersTo(ID, PLAYERS);
        verify(promotionDao).addPlayersTo(ID, PLAYERS);
        verify(promotionDao).updatePlayerCountInPromotion(ID);
    }

    @Test
    public void shouldReturnDefaultDailyAward() {
        final DailyAwardConfig defaultDailyAwardConfiguration = underTest.getDefaultDailyAwardConfiguration();
        assertThat(defaultDailyAwardConfiguration, is(new DailyAwardConfig(defaultPromotion.getConfiguration())));
    }

    private Promotion createDefaultDailyAward() {
        Promotion promotion = new DailyAwardPromotion();
        promotion.setId(PROMOTION_ID);
        promotion.setAllPlayers(false);
        promotion.setPromotionType(DAILY_AWARD);
        promotion.setStartDate(new DateTime());
        promotion.setEndDate(promotion.getStartDate().plusDays(23));

        PromotionConfiguration config = new PromotionConfiguration();
        config.addConfigurationItem(MAIN_IMAGE_KEY, MAIN_IMAGE_VALUE);
        config.addConfigurationItem(MAIN_IMAGE_LINK_KEY, MAIN_IMAGE_LINK_VALUE);
        config.addConfigurationItem(SECONDARY_IMAGE_KEY, SECONDARY_IMAGE_VALUE);
        config.addConfigurationItem(SECONDARY_IMAGE_LINK_KEY, SECONDARY_IMAGE_LINK_VALUE);
        config.addConfigurationItem(REWARD_CHIPS_KEY, REWARD_CHIPS_VALUE);
        config.addConfigurationItem(MAX_REWARDS_KEY, MAX_REWARDS_VALUE);
        promotion.setConfiguration(config);
        return promotion;
    }
}

