package com.yazino.web.controller;

import com.google.common.collect.ImmutableMap;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.gifting.*;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.service.GiftLobbyService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.remoting.RemoteInvocationFailureException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Sets.newHashSet;
import static java.math.BigDecimal.valueOf;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class GiftingControllerTest {
    public static final BigDecimal SESSION_ID = BigDecimal.valueOf(32123214874324l);
    public static final BigDecimal PLAYER_ID = valueOf(666L);

    public static final BigDecimal GIFT_ONE = BigDecimal.valueOf(123);
    public static final BigDecimal GIFT_TWO = BigDecimal.valueOf(456);
    public static final BigDecimal GIFT_THREE = BigDecimal.valueOf(789);
    private static final String HOST_URL = "http://host.url";

    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private WebApiResponses responseWriter;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpServletRequest request;
    @Mock
    private GiftLobbyService giftService;
    @Mock
    private PlayerProfileService playerProfileService;
    @Mock
    private LobbySession lobbySession;
    @Mock
    private SiteConfiguration siteConfiguration;
    @Mock
    private CommunityService communityService;

    private GiftingController underTest;


    @Before
    public void setUp() throws Exception {

        underTest = new GiftingController(responseWriter, lobbySessionCache, giftService, playerProfileService, siteConfiguration, communityService);
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID);
        when(lobbySession.getSessionId()).thenReturn(SESSION_ID);
        when(siteConfiguration.getHostUrl()).thenReturn(HOST_URL);
    }

    @Test
    public void getGiftsShouldContainBothGiftTypes() throws IOException {
        Gift giftOne = new Gift(GIFT_ONE, BigDecimal.ONE, PLAYER_ID, null, false);
        Gift giftTwo = new Gift(GIFT_TWO, BigDecimal.TEN, PLAYER_ID, null, false);

        Set<Gift> expectedList = newHashSet(giftOne, giftTwo);
        when(giftService.getAvailableGifts(PLAYER_ID)).thenReturn(expectedList);

        final AppToUserGift appToUserGift1 = new AppToUserGift(123l, null, DateTime.now().plusHours(2), 6000l, "bonus4u", "6k bonuss@!!");
        final AppToUserGift appToUserGift2 = new AppToUserGift(999l, "SLOTS", DateTime.now().plusHours(26), 200l, "nother bonus4u", "200 bonuss yeys@!!");
        final List<AppToUserGift> appToUserGifts = of(appToUserGift1, appToUserGift2);
        when(giftService.getGiftingPromotions(PLAYER_ID)).thenReturn(appToUserGifts);

        underTest.getAllGiftTypes(request, response);

        ArgumentCaptor<AllGifts> captor = ArgumentCaptor.forClass(AllGifts.class);
        verify(responseWriter).writeOk(eq(response), captor.capture());
        final AllGifts gifts = captor.getValue();
        assertThat(gifts.getUserToUserGifts().size(), is(2));
        assertThat(gifts.getAppToUserGifts().size(), is(2));

        assertThat(appToUserGift1, equalTo(appToUserGift1));

        assertThat(gifts.getAppToUserGifts(), containsInAnyOrder(appToUserGift1, appToUserGift2));
        assertThat(gifts.getUserToUserGifts(), containsInAnyOrder(giftOne, giftTwo));
    }

    @Test
    public void getGiftsShouldBeResilientToBrokenBackOffice() throws IOException {
        Gift giftOne = new Gift(GIFT_ONE, BigDecimal.ONE, PLAYER_ID, null, false);
        Set<Gift> expectedList = newHashSet(giftOne);
        when(giftService.getAvailableGifts(PLAYER_ID)).thenReturn(expectedList);

        when(giftService.getGiftingPromotions(PLAYER_ID)).thenThrow(new RemoteInvocationFailureException("no server", new NullPointerException()));

        underTest.getAllGiftTypes(request, response);

        ArgumentCaptor<AllGifts> captor = ArgumentCaptor.forClass(AllGifts.class);
        verify(responseWriter).writeOk(eq(response), captor.capture());
        final AllGifts gifts = captor.getValue();
        assertThat(gifts.getUserToUserGifts().size(), is(1));
        assertThat(gifts.getUserToUserGifts(), containsInAnyOrder(giftOne));

        assertThat(gifts.getAppToUserGifts().size(), is(0));
    }

    @Test
    public void collectGiftShouldHandleBothAppToUser() throws IOException {
        when(giftService.logPlayerReward(PLAYER_ID, 123L, SESSION_ID)).thenReturn(true);

        underTest.collectAppToUserGift("123", request, response);
        verify(responseWriter).writeOk(response, ImmutableMap.of("result", "success"));
    }

    @Test
    public void invalidGiftShouldReturnForbidden() throws IOException {
        when(giftService.logPlayerReward(PLAYER_ID, 123L, null)).thenReturn(false);
        underTest.collectAppToUserGift("123", request, response);
        verify(responseWriter).writeError(response, 403, "gift collection failure: either the gift is already collected or it has expired");
    }

    @Test
    public void invalidPromoIdShouldReturnBadRequest() throws IOException {
        underTest.collectAppToUserGift("not an id", request, response);
        verify(responseWriter).writeError(response, 400, "could not collect app to user gift with promoId 'not an id'");
    }

    @Test
    public void getGiftableStatusShouldCallRemoteService() throws IOException {
        final String csvPlayerIds = "1,2,3";
        underTest.getGiftingStatus(request, response, csvPlayerIds);
        final HashSet<BigDecimal> receivers = newHashSet(valueOf(1l), valueOf(2l), valueOf(3l));
        verify(giftService).getGiftableStatusForPlayers(PLAYER_ID, receivers);
    }

    @Test
    public void getGiftableStatusShouldCallRemoteServiceWithOnePlayerId() throws IOException {
        final String csvPlayerIds = "1";
        underTest.getGiftingStatus(request, response, csvPlayerIds);
        final HashSet<BigDecimal> receivers = newHashSet(valueOf(1l));
        verify(giftService).getGiftableStatusForPlayers(PLAYER_ID, receivers);
    }

    @Test
    public void remotingFailureShouldNotBreakGetGifts() throws IOException {
        when(giftService.getGiftingPromotions((BigDecimal) any())).thenThrow(new RuntimeException("Ahh!"));
        final HashSet<Gift> gifts = newHashSet();
        when(giftService.getAvailableGifts(PLAYER_ID)).thenReturn(gifts);
        underTest.getAllGiftTypes(request, response);
        verify(responseWriter).writeOk(eq(response), any(AllGifts.class));
    }

    @Test
    public void getGiftableStatusShouldReturnGiftableStatus() throws IOException {
        final GiftableStatus gridGiftOne = new GiftableStatus(BigDecimal.ONE, Giftable.GIFTABLE, null, null);
        final GiftableStatus gridGiftTwo = new GiftableStatus(BigDecimal.TEN, Giftable.GIFTABLE, null, null);
        final GiftableStatus populatedGiftOne = new GiftableStatus(BigDecimal.ONE, Giftable.GIFTABLE, HOST_URL + "/api/v1.0/player/picture?playerid=1", "player1");
        final GiftableStatus populatedGiftTwo = new GiftableStatus(BigDecimal.TEN, Giftable.GIFTABLE, HOST_URL + "/api/v1.0/player/picture?playerid=10", "player10");
        when(giftService.getGiftableStatusForPlayers(PLAYER_ID, newHashSet(valueOf(1), valueOf(10)))).thenReturn(newHashSet(gridGiftOne, gridGiftTwo));
        final Map<BigDecimal, String> displayNames = new HashMap<>();
        displayNames.put(valueOf(1), "player1");
        displayNames.put(valueOf(10), "player10");
        when(playerProfileService.findDisplayNamesById(newHashSet(valueOf(1), valueOf(10)))).thenReturn(displayNames);

        underTest.getGiftingStatus(request, response, "1,10");

        verify(responseWriter).writeOk(response, newHashSet(populatedGiftOne, populatedGiftTwo));
    }

    @Test
    public void getGiftableStatusShouldReturnEmptyJsonIfCsvPlayerIdsIsNull() throws IOException {
        HashSet<GiftableStatus> emptyGiftableStatusSet = newHashSet();

        underTest.getGiftingStatus(request, response, null);
        verifyNoMoreInteractions(giftService);
        verify(responseWriter).writeOk(response, emptyGiftableStatusSet);
    }

    @Test
    public void sendGiftsShouldCallServiceToSendGifts() throws IOException {
        final String csvPlayerIds = "1,2,3";

        when(giftService.giveGifts(PLAYER_ID, newHashSet(valueOf(1l), valueOf(2l), valueOf(3l)), SESSION_ID))
                .thenReturn(newHashSet(GIFT_ONE, GIFT_TWO, GIFT_THREE));

        underTest.sendGifts(request, response, csvPlayerIds);

        verify(responseWriter).writeOk(response, newHashSet(GIFT_ONE, GIFT_TWO, GIFT_THREE));
    }

    @Test
    public void getGiftsShouldGetGiftsFromService() throws IOException {
        BigDecimal receiver = BigDecimal.valueOf(23l);
        Gift giftOne = new Gift(GIFT_ONE, BigDecimal.ONE, receiver, null, false);
        Gift giftTwo = new Gift(GIFT_TWO, BigDecimal.TEN, receiver, null, false);

        Set<Gift> expectedList = newHashSet(giftOne, giftTwo);
        when(giftService.getAvailableGifts(PLAYER_ID)).thenReturn(expectedList);
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        underTest.getGifts(request, response);
        verify(responseWriter).writeOk(response, expectedList);
    }

    @Test
    public void getGiftsShouldReturn401OnNoSession() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(null);
        underTest.getGifts(request, response);
        verifyNoMoreInteractions(giftService);
        verify(responseWriter).writeError(response, 401, "no session");
    }

    @Test
    public void acknowledgeGiftsViewedShouldNotifyServer() throws IOException {
        underTest.acknowledgeExpiredGifts(request, response, "123,456");
        verify(giftService).acknowledgeViewedGifts(PLAYER_ID, newHashSet(GIFT_ONE, GIFT_TWO));
        verify(responseWriter).writeOk(response, emptyMap());
    }

    @Test
    public void invalidGiftIdShouldReturn200() throws IOException {
        underTest.acknowledgeExpiredGifts(request, response, "123,your mum");
        verify(responseWriter).writeError(response, HttpStatus.BAD_REQUEST.value(), "could not parse gift ids. check log for details");
    }

    @Test
    public void sendGiftsReturnBadRequest() throws IOException {
        final String csvPlayerIds = "2asdasd";

        underTest.sendGifts(request, response, csvPlayerIds);

        verifyNoMoreInteractions(giftService);
        verify(responseWriter).writeError(response, 400, "could not parse player id. check log for details");
    }

    @Test
    public void invalidRequestToGiftingStatusShouldReturn400() throws IOException {
        final String csvPlayerIds = "1,2,your mum";
        underTest.getGiftingStatus(request, response, csvPlayerIds);
        verify(responseWriter).writeError(response, 400, "could not parse player id. check log for details");
        verifyNoMoreInteractions(giftService);
    }

    @Test
    public void getEndOfGiftingPeriodShouldWriteResponseToClient() throws IOException {
        DateTime endOfGiftingTime = new DateTime(2012, 10, 15, 5, 0);
        when(giftService.getEndOfGiftPeriod()).thenReturn(endOfGiftingTime);

        underTest.getEndOfGiftingPeriod(response);
        verify(responseWriter).writeOk(response, new GiftingController.GiftingPeriod(endOfGiftingTime));
    }

    @Test
    public void invalidRequestToSendGiftsShouldReturn400() throws IOException {
        final String csvPlayerIds = "1,2,your mum";
        underTest.sendGifts(request, response, csvPlayerIds);
        verify(responseWriter).writeError(response, 400, "could not parse player id. check log for details");
        verifyNoMoreInteractions(giftService);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void exceptionsThrownFromGiftingServiceShouldBeLoggedAndReturned() throws IOException {
        when(giftService.giveGifts(any(BigDecimal.class), anySet(), eq(SESSION_ID))).thenThrow(new RuntimeException("BOOM"));
        final String csvPlayerIds = "1,2";
        underTest.sendGifts(request, response, csvPlayerIds);

        verify(responseWriter).writeError(response, 500, "problem with sending gifts to recipients");
    }

    @Test
    public void exceptionsThrownFromGiftingServiceWhenSendingToAllFriendsShouldBeLoggedAndReturned() throws IOException {
        when(giftService.giveGiftsToAllFriends(any(BigDecimal.class), eq(SESSION_ID)))
                .thenThrow(new RuntimeException("BOOM"));

        underTest.sendGiftsToAll(request, response);

        verify(responseWriter).writeError(response, 500, "Problem with sending gifts to all friends");
    }

    @Test
    public void giftsCanBeSentToAllFriends() throws IOException {
        when(giftService.giveGiftsToAllFriends(any(BigDecimal.class), eq(SESSION_ID)))
                .thenReturn(newHashSet(GIFT_ONE, GIFT_TWO, GIFT_THREE));

        underTest.sendGiftsToAll(request, response);

        verify(responseWriter).writeOk(response, newHashSet(GIFT_ONE, GIFT_TWO, GIFT_THREE));
    }

    @Test
    public void pushGiftCollectionsRemainingToClientShouldShouldReturn401OnNoSession() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(null);
        underTest.pushPlayerCollectionStatusToClient(request, response);
        verifyNoMoreInteractions(giftService);
        verify(responseWriter).writeError(response, 401, "no session");
    }

    @Test
    public void pushGiftCollectionsRemainingToClientShouldCallGiftingService() throws IOException {
        PlayerCollectionStatus status = new PlayerCollectionStatus(1, 2);
        when(giftService.pushPlayerCollectionStatus(PLAYER_ID)).thenReturn(status);
        underTest.pushPlayerCollectionStatusToClient(request, response);
        verify(responseWriter).writeOk(response, status);
    }

    @Test
    public void collectGiftShouldPassThroughSessionId() throws IOException, GiftCollectionFailure {
        underTest.collectGift(GIFT_ONE.toPlainString(), "GAMBLE", request, response);
        verify(giftService).collectGift(eq(PLAYER_ID), eq(GIFT_ONE), eq(CollectChoice.GAMBLE), eq(SESSION_ID));
    }
}
