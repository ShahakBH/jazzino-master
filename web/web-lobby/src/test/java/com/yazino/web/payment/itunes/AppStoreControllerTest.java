package com.yazino.web.payment.itunes;

import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class AppStoreControllerTest {

    private final LobbySessionCache mSessionCache = mock(LobbySessionCache.class);
    private final AppStoreService mService = mock(AppStoreService.class);
    private final HttpServletRequest mRequest = mock(HttpServletRequest.class);
    private final HttpServletResponse mResponse = mock(HttpServletResponse.class);
    private final WebApiResponses mResponseWriter = mock(WebApiResponses.class);
    private final AppStoreChipPurchaseTransformer mTransformer = mock(AppStoreChipPurchaseTransformer.class);
    private final AppStoreConfiguration mAppStoreConfiguration = mock(AppStoreConfiguration.class);

    private final AppStoreController mController = new AppStoreController(mSessionCache, mService);
    private final String mGameType = "SLOTS";
    private final BigDecimal mPlayerId = BigDecimal.valueOf(90);

    private final AppStoreOrder mOrder = new AppStoreOrder(0);

    @Before
    public void setup() {
        mController.setResponseWriter(mResponseWriter);
        mController.setChipPurchaseTransformer(mTransformer);
    }

    @Test
    public void shouldReturn401WhenNoLobbySession() throws Exception {
        when(mSessionCache.getActiveSession(mRequest)).thenReturn(null);
        mController.processPayment(mRequest, mResponse, mGameType, "USD", BigDecimal.TEN.toPlainString(),
                "USD8_BUYS_123", "1323213fdsfds", "1234567890");
        verify(mResponse).sendError(401);
        verifyZeroInteractions(mService);
        verifyZeroInteractions(mResponseWriter);
    }

    @Test
    public void shouldCallServiceWithCorrectContext() throws Exception {
        setupSession();
        String currencyCode = "USD";
        BigDecimal amountCash = BigDecimal.TEN;
        String productIdentifier = "USD8_BUYS_123";
        String transactionIdentifier = "1323213fdsfds";
        String receipt = "1234567890";
        mController.processPayment(mRequest, mResponse, mGameType, currencyCode, amountCash.toPlainString(),
                productIdentifier, transactionIdentifier, receipt);
        ArgumentCaptor<AppStorePaymentContext> captor = ArgumentCaptor.forClass(AppStorePaymentContext.class);
        verify(mService).fulfilOrder(captor.capture());
        AppStorePaymentContext context = captor.getValue();
        assertEquals(mGameType, context.getGameType());
        assertEquals(currencyCode, context.getCurrency().getCurrencyCode());
        assertEquals(amountCash, context.getCashAmount());
        assertEquals(productIdentifier, context.getProductIdentifier());
        assertEquals(transactionIdentifier, context.getTransactionIdentifier());
        assertEquals(receipt, context.getReceipt());
    }

    @Test
    public void shouldReturnBadRequestIfAmountOfCashIsInvalid() throws Exception {
        setupSession();
        String currencyCode = "USD";
        String productIdentifier = "USD8_BUYS_123";
        String transactionIdentifier = "1323213fdsfds";
        String receipt = "1234567890";

        mController.processPayment(mRequest, mResponse, mGameType, currencyCode, "invalidCash",
                productIdentifier, transactionIdentifier, receipt);

        verify(mResponse).sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void shouldGiveTransformerOrderFromService() throws Exception {
        setupSession();
        when(mService.fulfilOrder(any(AppStorePaymentContext.class))).thenReturn(mOrder);
        mController.processPayment(mRequest, mResponse, mGameType, "USD", BigDecimal.TEN.toPlainString(),
                "USD8_BUYS_123", "1323213fdsfds", "1234567890");
        verify(mTransformer).transform(mOrder);
    }

    @Test
    public void chipPackagesShouldReturnsNoPackagesWhenGivenAnInvalidPlayerId() throws IOException {
        when(mService.paymentOptionsForPlayer(BigDecimal.ZERO)).thenThrow(new IllegalArgumentException("Invalid player ID"));
        when(mService.getConfiguration()).thenReturn(mAppStoreConfiguration);
        when(mAppStoreConfiguration.promotionProductsForGame("aGameIdentifier")).thenReturn(Collections.singleton("aProduct"));

        mController.fetchChipPackages(mResponse, "aGameIdentifier", BigDecimal.ZERO.toPlainString());

        Map<String, List<AppStoreChipPackage>> expectedChipPackages = new HashMap<String, List<AppStoreChipPackage>>();
        expectedChipPackages.put(AppStoreConstants.CHIP_PACKAGES, Collections.<AppStoreChipPackage>emptyList());
        verify(mResponseWriter).writeOk(mResponse, expectedChipPackages);
    }

    private void setupSession() {
        LobbySession session = mock(LobbySession.class);
        when(session.getPlayerId()).thenReturn(mPlayerId);
        when(mSessionCache.getActiveSession(mRequest)).thenReturn(session);
    }
}
