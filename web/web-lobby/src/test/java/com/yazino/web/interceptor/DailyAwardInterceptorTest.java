package com.yazino.web.interceptor;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.web.controller.TopUpResultViewHelper;
import com.yazino.web.service.TopUpResultService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.ModelAndView;
import strata.server.lobby.api.promotion.TopUpResult;
import strata.server.lobby.api.promotion.TopUpStatus;
import strata.server.lobby.api.promotion.WebTopUpResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Arrays;

import static com.yazino.platform.Platform.WEB;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static strata.server.lobby.api.promotion.TopUpStatus.ACKNOWLEDGED;
import static strata.server.lobby.api.promotion.TopUpStatus.NEVER_CREDITED;


public class DailyAwardInterceptorTest {
    private static final String TOP_UP_RESULT = "topUpResult";
    public static final BigDecimal PLAYER_ID = BigDecimal.valueOf(10);
    public static final DateTime LAST_TOP_UP_DATE = new DateTime();

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Object handler;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    YazinoConfiguration yazinoConfiguration;
    @Mock
    TopUpResultService topUpResultService;

    private ModelAndView modelAndView;

    private DailyAwardInterceptor underTest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        modelAndView = new ModelAndView();

        underTest = new DailyAwardInterceptor(lobbySessionCache, yazinoConfiguration, topUpResultService,
                new TopUpResultViewHelper());
    }

    @Test
    public void whenNoActiveSessionThenEmptyTopUpRequestIsAddedToTheModel() throws Exception {
        modelAndView.addObject(TOP_UP_RESULT, "a top up result");
        underTest.postHandle(request, response, handler, modelAndView);

        assertThat((String) modelAndView.getModel().get(TOP_UP_RESULT), is("{}"));
    }

    @Test
    public void whenPlayerHasAcknowledgedTheLastTopUpThenACKNOWLEDGEDIsAddedToModel() throws Exception {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(anExistingSession());
        when(topUpResultService.getTopUpResult(PLAYER_ID, WEB))
                .thenReturn(new TopUpResult(PLAYER_ID, ACKNOWLEDGED, LAST_TOP_UP_DATE));

        underTest.postHandle(request, response, handler, modelAndView);

        final String expectedResult = "{\"status\":\"ACKNOWLEDGED\"}";
        assertThat((String) modelAndView.getModel().get(TOP_UP_RESULT), is(expectedResult));
    }

    @Test
    public void whenPlayerHasNeverBeenCreditedWithATopUpThenNEVER_CREDITEDIsAddedToModel() throws Exception {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(anExistingSession());
        when(topUpResultService.getTopUpResult(PLAYER_ID, WEB))
                .thenReturn(new TopUpResult(PLAYER_ID, NEVER_CREDITED, null));

        underTest.postHandle(request, response, handler, modelAndView);

        final String expectedResult = "{\"status\":\"NEVER_CREDITED\"}";
        assertThat((String) modelAndView.getModel().get(TOP_UP_RESULT), is(expectedResult));
    }


    @Test
    public void whenPlayerHasBeenCreditedThenTopUpResultWithPopUpInfoShouldBeAddedToModel() throws Exception {
        when(lobbySessionCache.getActiveSession(request)).thenReturn(anExistingSession());

        final TopUpResult webTopUpResult = createWebTopUpResult();
        given(topUpResultService.getTopUpResult(PLAYER_ID, WEB)).willReturn(webTopUpResult);

        underTest.postHandle(request, response, handler, modelAndView);

        assertThat((String) modelAndView.getModel().get(TOP_UP_RESULT), is(new TopUpResultViewHelper().serialiseAsJson(webTopUpResult)));
    }

    private LobbySession anExistingSession() {
        return new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, "aName", "aSessionKey", Partner.YAZINO, "aPicture", "anEmail",
                null, false, WEB, AuthProvider.YAZINO);
    }

    private TopUpResult createWebTopUpResult() {
        final WebTopUpResult webTopUpResult = new WebTopUpResult(PLAYER_ID, TopUpStatus.CREDITED,
                LAST_TOP_UP_DATE);
        webTopUpResult.setPromotionValueList(Arrays.asList(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO));
        webTopUpResult.setConsecutiveDaysPlayed(0);
        webTopUpResult.setMainImage("mainImage.jpg");
        webTopUpResult.setMainImageLink("http://main.image.link");
        webTopUpResult.setSecondaryImage("secondaryImage.jpg");
        webTopUpResult.setSecondaryImageLink("http://secondary/image.jpg");
        webTopUpResult.setTotalTopUpAmount(BigDecimal.valueOf(1234));
        return webTopUpResult;
    }

}
