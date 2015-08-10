package com.yazino.web.controller;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Platform;
import com.yazino.platform.account.WalletService;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerService;
import com.yazino.web.service.TopUpResultService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletResponse;
import strata.server.lobby.api.promotion.*;
import strata.server.lobby.api.promotion.message.TopUpAcknowledgeRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.yazino.platform.Platform.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static strata.server.lobby.api.promotion.TopUpStatus.ACKNOWLEDGED;
import static strata.server.lobby.api.promotion.TopUpStatus.CREDITED;

@RunWith(MockitoJUnitRunner.class)
public class DailyAwardControllerTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final DateTime LAST_TOP_UP_DATE = new DateTime();
    public static final List<BigDecimal> PROMOTION_VALUE_LIST = Arrays.asList(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO);
    public static final BigDecimal ANDROID_PLAYER_BALANCE = BigDecimal.valueOf(354623547);
    public static final int CONSECUTIVE_DAYS_PLAYED = 3;
    public static final String MAIN_IMAGE = "mainImage.jpg";
    public static final String MAIN_IMAGE_LINK = "http://main.image.link";
    public static final String SECONDARY_IMAGE = "secondaryImage.jpg";
    public static final String SECONDARY_IMAGE_LINK = "http://secondary/image.jpg";
    public static final BigDecimal TOTAL_TOP_UP_AMOUNT = BigDecimal.valueOf(2000);
    public static final String IOS_IMAGE = "iosImage";
    public static final String ANDROID_IMAGE = "androidImage";
    public static final BigDecimal IOS_PLAYER_BALANCE = BigDecimal.valueOf(8976);
    public static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(897635636);

    @Mock
    private LobbySessionCache lobbySessionCache;

    @Mock
    private HttpServletRequest request;

    @Mock
    private TopUpResultService topUpResultService;

    @Mock
    private YazinoConfiguration yazinoConfiguration;

    @Mock
    private PlayerService playerService;

    @Mock
    private WalletService walletService;

    private DailyAwardController underTest;
    private MockHttpServletResponse response;

    @Before
    public void init() {
        response = new MockHttpServletResponse();
        underTest = new DailyAwardController(lobbySessionCache, topUpResultService, new TopUpResultViewHelper(), walletService, playerService);
    }

    @Test
    public void responseHasCorrectContentType() throws IOException {
        createDefaultLobbySession(IOS);

        underTest.dailyAward(request, response);

        assertThat(response.getContentType(), is("application/json"));
    }

    @Test
    public void responseShouldBeEmptyJsonWhenLastTopUpAlreadyAcknowledged() throws IOException {
        createDefaultLobbySession(IOS);
        given(topUpResultService.getTopUpResult(PLAYER_ID, IOS)).willReturn(new TopUpResult(PLAYER_ID, TopUpStatus.ACKNOWLEDGED, new DateTime()));

        underTest.dailyAward(request, response);

        assertThat(response.getContentAsString(), is("{}"));
        verify(topUpResultService).getTopUpResult(PLAYER_ID, IOS);
    }

    @Test
    public void whenSessionPlatformIsWEBThenErrorResponseIsSent() throws IOException {
        createDefaultLobbySession(WEB);

        underTest.dailyAward(request, response);

        assertThat(response.getStatus(), is(HttpServletResponse.SC_FORBIDDEN));
    }

    @Test
    public void whenSessionPlatformIsFACEBOOKThenErrorResponseIsSent() throws IOException {
        createDefaultLobbySession(FACEBOOK_CANVAS);

        underTest.dailyAward(request, response);

        assertThat(response.getStatus(), is(HttpServletResponse.SC_FORBIDDEN));
    }

    @Test
    public void responseShouldHaveDailyAwardResultForIOSWhenPlayerHasBeenToppedUp() throws IOException, WalletServiceException {
        createDefaultLobbySession(IOS);
        DailyAwardResult dailyAwardResult = createDailyAwardResult(IOS);

        given(topUpResultService.getTopUpResult(PLAYER_ID, IOS)).willReturn(createIOSTopUpResult(TopUpStatus.CREDITED));
        given(playerService.getAccountId(PLAYER_ID)).willReturn(ACCOUNT_ID);
        given(walletService.getBalance(ACCOUNT_ID)).willReturn(IOS_PLAYER_BALANCE);

        underTest.dailyAward(request, response);

        String expectedContent = new JsonHelper().serialize(dailyAwardResult);
        assertThat(response.getContentAsString(), is(expectedContent));
    }

    @Test
    public void responseShouldHaveDailyAwardResultForANDROIDWhenPlayerHasBeenToppedUp() throws IOException, WalletServiceException {
        createDefaultLobbySession(ANDROID);
        DailyAwardResult dailyAwardResult = createDailyAwardResult(ANDROID);
        assertThat(dailyAwardResult.getBalance(), is(ANDROID_PLAYER_BALANCE));

        given(topUpResultService.getTopUpResult(PLAYER_ID, ANDROID)).willReturn(createANDROIDTopUpResult());
        given(playerService.getAccountId(PLAYER_ID)).willReturn(ACCOUNT_ID);
        given(walletService.getBalance(ACCOUNT_ID)).willReturn(ANDROID_PLAYER_BALANCE);

        underTest.dailyAward(request, response);

        String expectedContent = new JsonHelper().serialize(dailyAwardResult);
        assertThat(response.getContentAsString(), is(expectedContent));
    }

    @Test
    public void ifTopUpHasBeenCreditedThenDailyAwardShouldAcknowledgeTheTopUp() throws IOException {
        createDefaultLobbySession(IOS);
        createDailyAwardResult(IOS);
        given(topUpResultService.getTopUpResult(PLAYER_ID, IOS)).willReturn(createIOSTopUpResult(TopUpStatus.CREDITED));

        underTest.dailyAward(request, response);

        verify(topUpResultService).acknowledgeTopUpResult(new TopUpAcknowledgeRequest(PLAYER_ID, LAST_TOP_UP_DATE));
    }

    @Test
    public void ifTopUpHasAlreadyBeenAcknowledgedThenDailyAwardShouldNotAcknowledgeTheTopUp() throws IOException {
        createDefaultLobbySession(IOS);
        createDailyAwardResult(IOS);
        given(topUpResultService.getTopUpResult(PLAYER_ID, IOS)).willReturn(createIOSTopUpResult(TopUpStatus.ACKNOWLEDGED));

        underTest.dailyAward(request, response);

        verify(topUpResultService).getTopUpResult(PLAYER_ID, IOS);
        verifyNoMoreInteractions(topUpResultService);
    }

    private DailyAwardResult createDailyAwardResult(Platform platform) {
        DailyAwardResult dailyAwardResult = new DailyAwardResult();
        dailyAwardResult.setTopupAmount(TOTAL_TOP_UP_AMOUNT);
        DailyAwardConfig cfg = new DailyAwardConfig();
        dailyAwardResult.setDailyAwardConfig(cfg);
        dailyAwardResult.setConsecutiveDaysPlayed(CONSECUTIVE_DAYS_PLAYED);
        if (platform == IOS) {
            cfg.setIosImage(IOS_IMAGE);
            dailyAwardResult.setBalance(IOS_PLAYER_BALANCE);
        } else if (platform == ANDROID) {
            cfg.setIosImage(ANDROID_IMAGE);
            dailyAwardResult.setBalance(ANDROID_PLAYER_BALANCE);
        }
        return dailyAwardResult;
    }

    @Test
    public void dailyAwardResponseShouldBeEmptyWhenNoActiveSession() throws IOException {
        given(lobbySessionCache.getActiveSession(request)).willReturn(null);

        underTest.dailyAward(request, response);

        assertThat(response.getContentAsString(), is("{}"));
    }

    @Test
    public void topUpResultShouldWriteEmptyJsonWhenPlayerNotLoggedIn() throws IOException {
        underTest.topUpResult(request, response);
        assertThat(response.getContentAsString(), is("{}"));
    }

    @Test
    public void shouldWriteTopUpResultToResponseWithStatusACKNOWLEDGEDWhenPopupAlreadyAcknowledged() throws IOException {
        createDefaultLobbySession(WEB);
        given(topUpResultService.getTopUpResult(PLAYER_ID, WEB)).willReturn(new TopUpResult(PLAYER_ID, ACKNOWLEDGED,
                LAST_TOP_UP_DATE));

        underTest.topUpResult(request, response);

        assertThat(response.getContentAsString(), is("{\"status\":\"ACKNOWLEDGED\"}"));
    }

    @Test
    public void shouldWriteTopUpResultToResponseWithStatusCREDITEDAndWithPopupConfigWhenPlayerHasBeenToppedUpAndHasNotAcknowledged() throws IOException {
        createDefaultLobbySession(WEB);
        final TopUpResult webTopUpResult = createWebTopUpResult();
        given(topUpResultService.getTopUpResult(PLAYER_ID, WEB)).willReturn(webTopUpResult);

        underTest.topUpResult(request, response);

        final String expectedJson = new TopUpResultViewHelper().serialiseAsJson(webTopUpResult);
        assertThat(response.getContentAsString(), is(equalTo(expectedJson)));
    }

    @Test
    public void shouldWriteTopUpResultToResponseWithStatusACKNOWLEDGEDWhenLastTopUpIsNotTodayAndHasBeenAcknowledged() throws IOException {
        createDefaultLobbySession(WEB);
        final DateTime lastTopUpDate = LAST_TOP_UP_DATE.minusDays(2);
        given(topUpResultService.getTopUpResult(PLAYER_ID, WEB))
                .willReturn(new TopUpResult(PLAYER_ID, ACKNOWLEDGED, lastTopUpDate));

        underTest.topUpResult(request, response);

        assertThat(response.getContentAsString(), is("{\"status\":\"ACKNOWLEDGED\"}"));
    }

    @Test
    public void shouldWriteMobileTopUpResultToResponseWithStatusCREDITEDWhenPlayerHasBeenToppedUpAndHasNotAcknowledgedForIOS() throws IOException {
        createDefaultLobbySession(IOS);
        final TopUpResult topUpResult = createIOSTopUpResult(CREDITED);
        final DateTime lastTopUpDate = topUpResult.getLastTopUpDate();
        given(topUpResultService.getTopUpResult(PLAYER_ID, IOS)).willReturn(topUpResult);

        underTest.topUpResult(request, response);

        String actualJson = response.getContentAsString();

        HashMap<String, Object> actualValues = (HashMap<String, Object>) new JsonHelper().deserialize(HashMap.class, actualJson);
        assertEquals(actualValues.get("imageUrl"), IOS_IMAGE);
        assertEquals(actualValues.get("totalAmount"), TOTAL_TOP_UP_AMOUNT.intValue());
        assertEquals(actualValues.get("status"), TopUpStatus.CREDITED.name());
        assertEquals(actualValues.get("date"), lastTopUpDate.getMillis());
    }

    @Test
    public void acknowledgeTopUpShouldReturnErrorJSONResponseIfNoSessionExists() throws IOException {
        underTest.acknowledgeTopUp(request, response, new DateTime().getMillis());

        assertThat(response.getContentAsString(), is("{\"error\":\"No Session for Player\"}"));
    }

    @Test
    public void acknowledgeTopUpShouldReturnInvalidRequestIfLastTopUpDateIsAbsent() throws IOException {
        createDefaultLobbySession(WEB);

        underTest.acknowledgeTopUp(request, response, null);

        assertThat(response.getStatus(), is(equalTo(HttpServletResponse.SC_BAD_REQUEST)));
    }

    @Test
    public void acknowledgeTopUpShouldDelegateToTheTopUpResultServiceToPerformAcknowledgement() throws IOException {
        createDefaultLobbySession(WEB);
        final DateTime topUpDate = new DateTime();
        underTest.acknowledgeTopUp(request, response, topUpDate.getMillis());
        verify(topUpResultService).acknowledgeTopUpResult(new TopUpAcknowledgeRequest(PLAYER_ID, topUpDate));
    }

    @Test
    public void acknowledgeTopUpShouldWriteAcknowledgeResponse() throws IOException {
        createDefaultLobbySession(WEB);
        final DateTime topUpDate = new DateTime();
        underTest.acknowledgeTopUp(request, response, topUpDate.getMillis());
        assertThat(response.getContentAsString(), is("{\"acknowledgement\":true}"));
    }

    private TopUpResult createWebTopUpResult() {
        final WebTopUpResult webTopUpResult = new WebTopUpResult(PLAYER_ID, TopUpStatus.CREDITED,
                LAST_TOP_UP_DATE);
        webTopUpResult.setTotalTopUpAmount(TOTAL_TOP_UP_AMOUNT);
        webTopUpResult.setPromotionValueList(PROMOTION_VALUE_LIST);
        webTopUpResult.setConsecutiveDaysPlayed(CONSECUTIVE_DAYS_PLAYED);
        webTopUpResult.setMainImage(MAIN_IMAGE);
        webTopUpResult.setMainImageLink(MAIN_IMAGE_LINK);
        webTopUpResult.setSecondaryImage(SECONDARY_IMAGE);
        webTopUpResult.setSecondaryImageLink(SECONDARY_IMAGE_LINK);
        return webTopUpResult;
    }

    private TopUpResult createIOSTopUpResult(TopUpStatus topUpStatus) {
        if (topUpStatus == CREDITED) {
            final MobileTopUpResult topUpResult = new MobileTopUpResult(PLAYER_ID, topUpStatus, LAST_TOP_UP_DATE);
            topUpResult.setTotalTopUpAmount(TOTAL_TOP_UP_AMOUNT);
            topUpResult.setConsecutiveDaysPlayed(CONSECUTIVE_DAYS_PLAYED);
            topUpResult.setImageUrl(IOS_IMAGE);
            return topUpResult;
        }
        return new TopUpResult(PLAYER_ID, topUpStatus, LAST_TOP_UP_DATE);
    }

    private TopUpResult createANDROIDTopUpResult() {
        final MobileTopUpResult topUpResult = new MobileTopUpResult(PLAYER_ID, TopUpStatus.CREDITED,
                LAST_TOP_UP_DATE);
        topUpResult.setTotalTopUpAmount(TOTAL_TOP_UP_AMOUNT);
        topUpResult.setConsecutiveDaysPlayed(CONSECUTIVE_DAYS_PLAYED);
        topUpResult.setImageUrl(ANDROID_IMAGE);
        return topUpResult;
    }

    private void createDefaultLobbySession(Platform platform) {
        LobbySession session = new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, null, null, null, null, null, null, false, platform, AuthProvider.YAZINO);
        given(lobbySessionCache.getActiveSession(request)).willReturn(session);
    }


}
