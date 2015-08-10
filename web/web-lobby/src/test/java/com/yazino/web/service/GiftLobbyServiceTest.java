package com.yazino.web.service;

import com.yazino.platform.community.GiftService;
import com.yazino.platform.gifting.*;
import net.sf.ehcache.CacheManager;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import strata.server.lobby.api.promotion.GiftingPromotionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GiftLobbyServiceTest {
    private static final BigDecimal SENDING_PLAYER = BigDecimal.valueOf(100);
    private static final BigDecimal RECIPIENT_PLAYER = BigDecimal.valueOf(110);
    private static final Set<BigDecimal> RECIPIENT_PLAYERS = newHashSet(BigDecimal.valueOf(50), BigDecimal.valueOf(60));
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(6500);
    private static final BigDecimal GIFT_ID = BigDecimal.valueOf(10);
    private static final Set<BigDecimal> GIFT_IDS = newHashSet(BigDecimal.valueOf(10), BigDecimal.valueOf(20));
    private static final DateTime END_OF_GIFT_PERIOD = new DateTime(445345435);

    @Mock
    private GiftService giftService;
    @Mock
    private GiftingPromotionService giftingPromotionService;
    @Mock
    private CacheManager cacheManager;

    private GiftLobbyService underTest;
    private static final BigDecimal REWARD = BigDecimal.valueOf(1234);

    @Before
    public void setUp() {
        underTest = new GiftLobbyService(giftService, giftingPromotionService, cacheManager);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullGiftService() {
        new GiftLobbyService(null, giftingPromotionService, cacheManager);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithAGiftingPromotionService() {
        new GiftLobbyService(giftService, null, cacheManager);
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullCacheManager() {
        new GiftLobbyService(giftService, giftingPromotionService, null);
    }

    @Test
    public void serviceHasANoArgConstructorForProxying()
            throws IllegalAccessException, InstantiationException {
        GiftLobbyService.class.newInstance(); // this should avoid the test being voided by automated refactoring
    }

    @Test(expected = IllegalStateException.class)
    public void businessLogicCannotBeInvokedOnAnInstanceCreatedByTheProxyingConstructor()
            throws IllegalAccessException, InstantiationException {
        final GiftLobbyService underTest = GiftLobbyService.class.newInstance();

        underTest.getEndOfGiftPeriod();
    }

    @Test
    public void givingGiftsToAllFriendsDelegatesToTheGiftService() {
        when(giftService.giveGiftsToAllFriends(SENDING_PLAYER, SESSION_ID)).thenReturn(GIFT_IDS);

        final Set<BigDecimal> giftIds = underTest.giveGiftsToAllFriends(SENDING_PLAYER, SESSION_ID);

        assertThat(giftIds, is(equalTo(GIFT_IDS)));
    }

    @Test
    public void givingGiftsDelegatesToTheGiftService() {
        when(giftService.giveGifts(SENDING_PLAYER, RECIPIENT_PLAYERS, SESSION_ID)).thenReturn(GIFT_IDS);

        final Set<BigDecimal> giftIds = underTest.giveGifts(SENDING_PLAYER, RECIPIENT_PLAYERS, SESSION_ID);

        assertThat(giftIds, is(equalTo(GIFT_IDS)));
    }

    @Test
    public void gettingAvailableGiftsDelegatesToTheGiftService() {
        when(giftService.getAvailableGifts(RECIPIENT_PLAYER)).thenReturn(newHashSet(aGift(1), aGift(2)));

        final Set<Gift> gifts = underTest.getAvailableGifts(RECIPIENT_PLAYER);

        assertThat(gifts, is(equalTo((Set<Gift>) newHashSet(aGift(1), aGift(2)))));
    }

    @Test
    public void gettingGiftableStatusesDelegatesToTheGiftService() {
        when(giftService.getGiftableStatusForPlayers(SENDING_PLAYER, RECIPIENT_PLAYERS)).thenReturn(newHashSet(aGiftableStatus(1), aGiftableStatus(2)));

        final Set<GiftableStatus> giftableStatuses = underTest.getGiftableStatusForPlayers(SENDING_PLAYER, RECIPIENT_PLAYERS);

        assertThat(giftableStatuses, is(equalTo((Set<GiftableStatus>) newHashSet(aGiftableStatus(1), aGiftableStatus(2)))));
    }

    @Test
    public void acknowledgingViewedGiftsDelegatesToTheGiftService() {
        underTest.acknowledgeViewedGifts(RECIPIENT_PLAYER, GIFT_IDS);

        verify(giftService).acknowledgeViewedGifts(RECIPIENT_PLAYER, GIFT_IDS);
    }

    @Test
    public void collectGiftDelegatesToTheGiftService() throws GiftCollectionFailure {
        when(giftService.collectGift(RECIPIENT_PLAYER, GIFT_ID, CollectChoice.GAMBLE, SESSION_ID)).thenReturn(REWARD);

        final BigDecimal reward = underTest.collectGift(RECIPIENT_PLAYER, GIFT_ID, CollectChoice.GAMBLE, SESSION_ID);

        assertThat(reward, is(equalTo(REWARD)));
    }

    @Test
    public void gettingTheEndOfGiftPeriodDelegatesToTheGiftService() throws GiftCollectionFailure {
        when(giftService.getEndOfGiftPeriod()).thenReturn(END_OF_GIFT_PERIOD);

        final DateTime endOfGiftPeriod = underTest.getEndOfGiftPeriod();

        assertThat(endOfGiftPeriod, is(equalTo(END_OF_GIFT_PERIOD)));
    }

    @Test
    public void gettingGiftingPromotionsDelegatesToTheGiftingPromotionsService() {
        when(giftingPromotionService.getGiftingPromotions(RECIPIENT_PLAYER))
                .thenReturn(asList(anAppToUserGift()));

        final List<AppToUserGift> promotions = underTest.getGiftingPromotions(RECIPIENT_PLAYER);

        assertThat(promotions, is(equalTo(asList(anAppToUserGift()))));
    }

    @Test
    public void loggingPlayerRewardsDelegatesToTheGiftingPromotionsService() {
        when(giftingPromotionService.logPlayerReward(RECIPIENT_PLAYER, 100L, SESSION_ID)).thenReturn(true);

        final boolean result = underTest.logPlayerReward(RECIPIENT_PLAYER, 100L, SESSION_ID);

        assertThat(result, is(true));
    }

    private AppToUserGift anAppToUserGift() {
        return new AppToUserGift(10L, "aGameType", new DateTime(240203452345L), 100L, "aTitle", "aDescription");
    }

    private GiftableStatus aGiftableStatus(final int id) {
        return new GiftableStatus(BigDecimal.valueOf(id), Giftable.GIFTABLE, "http://an.image", "aDisplayName");
    }

    private Gift aGift(final int id) {
        return new Gift(BigDecimal.valueOf(id), SENDING_PLAYER, RECIPIENT_PLAYER, new DateTime(1000000000), false);
    }

}