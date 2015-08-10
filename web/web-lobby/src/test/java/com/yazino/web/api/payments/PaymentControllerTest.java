package com.yazino.web.api.payments;

import com.yazino.platform.Platform;
import com.yazino.platform.account.WalletService;
import com.yazino.web.payment.TransactionIdGenerator;
import com.yazino.web.payment.amazon.AmazonInitiatePurchaseProcessor;
import com.yazino.web.payment.amazon.InitiatePurchaseProcessor;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PaymentControllerTest {
    private static final long TRANSACTION_ID = 23344L;
    public static final String GAME_TYPE = "SLOTS";
    public static final String PRODUCT_ID = "product id";
    public static final String PROMOTION_ID = "957463";
    public static final BigDecimal ACCOUNT_ID = BigDecimal.TEN;
    public static final BigDecimal PLAYER_ID = BigDecimal.valueOf(2345l);
    public static final Platform PLATFORM = Platform.AMAZON;

    @Mock
    private TransactionIdGenerator transactionIdGenerator;
    @Mock
    private WebApiResponses webApiResponses;

    @Mock
    private WalletService walletService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private LobbySessionCache lobbySessionCache;
    @Mock
    private LobbySession lobbySession;
    @Mock
    AmazonInitiatePurchaseProcessor purchaseProcessor;

    private List<InitiatePurchaseProcessor> initiatePurchaseProcessorList;

    private PaymentController underTest;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        initiatePurchaseProcessorList = new ArrayList<>();
        initiatePurchaseProcessorList.add(purchaseProcessor);

        when(purchaseProcessor.getPlatform()).thenReturn(PLATFORM);
        when(lobbySession.getPlatform()).thenReturn(PLATFORM);

        underTest = new PaymentController(
                webApiResponses,
                lobbySessionCache,
                initiatePurchaseProcessorList);
    }

    @Test
    public void initiatePurchaseShouldRejectRequestIfPromoIdIsInvalid() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID);

        underTest.initiatePurchase(request, response, GAME_TYPE, PRODUCT_ID, "bad promo id");

        verify(webApiResponses).writeError(response, 400, "bad promo id, is not a valid integer for promotion id");
    }

    @Test
    public void initiatePurchaseShouldNotRejectRequestIfPromoIdIsNull() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID);

        underTest.initiatePurchase(request, response, GAME_TYPE, PRODUCT_ID, null);

        verify(webApiResponses).writeOk(eq(response), any());
    }

    @Test
    public void initiatePurchaseShouldRejectRequestIfPlatformIsInvalid() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID);

        underTest.initiatePurchase(request, response, GAME_TYPE, PRODUCT_ID, PROMOTION_ID);

        verify(webApiResponses).writeError(response, 403, "error: platform AMAZON is unsupported");
    }

    @Test
    public void initiatePurchaseShouldLogTransactionWithWalletService() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        when(lobbySession.getPlayerId()).thenReturn(PLAYER_ID);

        underTest.initiatePurchase(request, response, GAME_TYPE, PRODUCT_ID, PROMOTION_ID);

        verify(purchaseProcessor).initiatePurchase(PLAYER_ID, PRODUCT_ID, Long.parseLong(PROMOTION_ID), GAME_TYPE, PLATFORM);
    }

    @Test
    public void initiatePurchaseShouldReturnA403IfUnsupportedPlatformType() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        when(lobbySession.getPlatform()).thenReturn(Platform.IOS);

        underTest.initiatePurchase(request, response, GAME_TYPE, PRODUCT_ID, PROMOTION_ID);

        verify(webApiResponses).writeError(response, 403, format("error: platform %s is unsupported", Platform.IOS));
    }

    @Test
    public void initiatePurchaseShouldSenBadRequestWhenGameTypeIsMissing() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        when(lobbySession.getPlatform()).thenReturn(PLATFORM);

        underTest.initiatePurchase(request, response, "", PRODUCT_ID, PROMOTION_ID);

        verify(webApiResponses).writeError(response, 400, "error: parameter 'gameType' is missing");
    }

    @Test
    public void initiatePurchaseShouldSenBadRequestWhenProductIdIsMissing() throws IOException {
        when(lobbySessionCache.getActiveSession(any(HttpServletRequest.class))).thenReturn(lobbySession);
        when(lobbySession.getPlatform()).thenReturn(PLATFORM);

        underTest.initiatePurchase(request, response, GAME_TYPE, "", PROMOTION_ID);

        verify(webApiResponses).writeError(response, 400, "error: parameter 'productId' is missing");
    }
}
