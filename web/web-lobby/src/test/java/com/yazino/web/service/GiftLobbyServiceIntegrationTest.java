package com.yazino.web.service;

import com.yazino.platform.community.GiftService;
import com.yazino.platform.gifting.*;
import net.sf.ehcache.CacheManager;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import strata.server.lobby.api.promotion.GiftingPromotionService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class GiftLobbyServiceIntegrationTest {
    private static final BigDecimal SENDING_PLAYER = BigDecimal.valueOf(100);
    private static final BigDecimal RECIPIENT_PLAYER = BigDecimal.valueOf(110);
    private static final Set<BigDecimal> RECIPIENT_PLAYERS = newHashSet(BigDecimal.valueOf(50), BigDecimal.valueOf(60));
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(6500);
    private static final BigDecimal GIFT_ID = BigDecimal.valueOf(10);
    private static final DateTime END_OF_GIFT_PERIOD = new DateTime(445345435);

    @Autowired
    private CacheManager ehCacheManager;
    @Autowired
    private GiftService giftService;
    @Autowired
    private GiftingPromotionService giftingPromotionService;
    @Autowired
    private GiftLobbyService underTest;

    @Before
    public void setUp() {
        when(giftService.getGiftableStatusForPlayers(SENDING_PLAYER, RECIPIENT_PLAYERS)).thenReturn(newHashSet(aGiftableStatus(1), aGiftableStatus(2)));
        when(giftService.getGiftableStatusForPlayers(RECIPIENT_PLAYER, RECIPIENT_PLAYERS)).thenReturn(newHashSet(aGiftableStatus(1), aGiftableStatus(2)));
        when(giftService.getEndOfGiftPeriod()).thenReturn(END_OF_GIFT_PERIOD);
        when(giftService.getAvailableGifts(RECIPIENT_PLAYER)).thenReturn(newHashSet(aGift(1), aGift(2)));
        when(giftingPromotionService.getGiftingPromotions(RECIPIENT_PLAYER)).thenReturn(asList(anAppToUserGift()));
    }

    @Test
    public void givingGiftsToAllFriendsClearsAllCachesForTheSendingPlayer() {
        when(giftService.getGiftableStatusForPlayers(SENDING_PLAYER, RECIPIENT_PLAYERS)).thenReturn(newHashSet(aGiftableStatus(1), aGiftableStatus(2)));
        underTest.getGiftableStatusForPlayers(SENDING_PLAYER, RECIPIENT_PLAYERS);

        assertThat(keysFromCache("giftingStatusCache"), hasItem(aKeyWithValue(SENDING_PLAYER, RECIPIENT_PLAYERS)));

        underTest.giveGiftsToAllFriends(SENDING_PLAYER, SESSION_ID);

        assertThat(keysFromCache("giftingStatusCache"), not(hasItem(aKeyWithValue(SENDING_PLAYER, RECIPIENT_PLAYERS))));
    }

    @Test
    public void givingGiftsToAllFriendsDoesNotClearCachesForOtherPlayers() {
        underTest.getGiftableStatusForPlayers(SENDING_PLAYER, RECIPIENT_PLAYERS);
        underTest.getGiftableStatusForPlayers(RECIPIENT_PLAYER, RECIPIENT_PLAYERS);

        assertThat(keysFromCache("giftingStatusCache"), hasItem(aKeyWithValue(RECIPIENT_PLAYER, RECIPIENT_PLAYERS)));

        underTest.giveGiftsToAllFriends(SENDING_PLAYER, SESSION_ID);

        assertThat(keysFromCache("giftingStatusCache"), hasItem(aKeyWithValue(RECIPIENT_PLAYER, RECIPIENT_PLAYERS)));
    }

    @Test
    public void givingGiftsClearsAllCachesForTheSendingPlayer() {
        underTest.getGiftableStatusForPlayers(SENDING_PLAYER, RECIPIENT_PLAYERS);

        assertThat(keysFromCache("giftingStatusCache"), hasItem(aKeyWithValue(SENDING_PLAYER, RECIPIENT_PLAYERS)));

        underTest.giveGifts(SENDING_PLAYER, RECIPIENT_PLAYERS, SESSION_ID);

        assertThat(keysFromCache("giftingStatusCache"), not(hasItem(aKeyWithValue(SENDING_PLAYER, RECIPIENT_PLAYERS))));
    }

    @Test
    public void givingGiftsDoesNotClearCachesForOtherPlayers() {
        underTest.getGiftableStatusForPlayers(SENDING_PLAYER, RECIPIENT_PLAYERS);
        underTest.getGiftableStatusForPlayers(RECIPIENT_PLAYER, RECIPIENT_PLAYERS);

        assertThat(keysFromCache("giftingStatusCache"), hasItem(aKeyWithValue(RECIPIENT_PLAYER, RECIPIENT_PLAYERS)));

        underTest.giveGifts(SENDING_PLAYER, RECIPIENT_PLAYERS, SESSION_ID);

        assertThat(keysFromCache("giftingStatusCache"), hasItem(aKeyWithValue(RECIPIENT_PLAYER, RECIPIENT_PLAYERS)));
    }

    @Test
    public void theEndOfTheGiftingPeriodIsCached() {
        assertThat(keysFromCache("giftPeriodCache"), is(empty()));

        underTest.getEndOfGiftPeriod();

        assertThat(keysFromCache("giftPeriodCache").size(), is(equalTo(1)));
    }

    @Test
    public void retrievingAGiftClearsTheGiftCache() throws GiftCollectionFailure {
        underTest.getAvailableGifts(RECIPIENT_PLAYER);

        assertThat(keysFromCache("giftCache").size(), is(equalTo(1)));

        underTest.collectGift(RECIPIENT_PLAYER, GIFT_ID, CollectChoice.GAMBLE, SESSION_ID);

        assertThat(keysFromCache("giftCache"), is(empty()));
    }

    @Test
    public void giftingPromotionsAreCached() {
        assertThat(keysFromCache("giftingPromotionsCache"), is(empty()));

        underTest.getGiftingPromotions(RECIPIENT_PLAYER);

        assertThat(keysFromCache("giftingPromotionsCache").size(), is(equalTo(1)));
    }

    @Test
    public void loggingPlayersRewardsClearsThePromotionCacheForTheRecipient() {
        underTest.getGiftingPromotions(RECIPIENT_PLAYER);

        assertThat(keysFromCache("giftingPromotionsCache").size(), is(equalTo(1)));

        underTest.logPlayerReward(RECIPIENT_PLAYER, 100L, SESSION_ID);

        assertThat(keysFromCache("giftingPromotionsCache"), is(empty()));
    }

    @SuppressWarnings("unchecked")
    private List<Object> keysFromCache(final String cacheName) {
        return (List<Object>) ehCacheManager.getCache(cacheName).getKeys();
    }

    private List<Object> aKeyWithValue(final BigDecimal sender, final Set<BigDecimal> recipients) {
        return asList(GiftLobbyService.class, "getGiftableStatusForPlayers", Set.class,
                asList(BigDecimal.class, Set.class), asList(sender, new ArrayList<>(recipients)));
    }

    private GiftableStatus aGiftableStatus(final int id) {
        return new GiftableStatus(BigDecimal.valueOf(id), Giftable.GIFTABLE, "http://an.image", "aDisplayName");
    }

    private Gift aGift(final int id) {
        return new Gift(BigDecimal.valueOf(id), SENDING_PLAYER, RECIPIENT_PLAYER, new DateTime(1000000000), false);
    }

    private AppToUserGift anAppToUserGift() {
        return new AppToUserGift(10L, "aGameType", new DateTime(240203452345L), 100L, "aTitle", "aDescription");
    }
}