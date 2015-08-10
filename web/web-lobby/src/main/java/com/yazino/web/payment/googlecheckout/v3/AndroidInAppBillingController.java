package com.yazino.web.payment.googlecheckout.v3;

import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.payment.Purchase;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static com.yazino.platform.Platform.ANDROID;
import static com.yazino.web.util.RequestParameterUtils.hasParameter;

@Controller
@AllowPublicAccess("/api/1.0/payments/android/google/**")
public class AndroidInAppBillingController {
    private static final Logger LOG = LoggerFactory.getLogger(AndroidInAppBillingController.class);

    private LobbySessionCache lobbySessionCache;
    private AndroidInAppBillingServiceV3 billingService;
    private WebApiResponses responseWriter;
    private AndroidPromotionServiceV3 androidPromotionService;

    @Autowired
    public AndroidInAppBillingController(LobbySessionCache lobbySessionCache,
                                         AndroidInAppBillingServiceV3 billingService,
                                         WebApiResponses responseWriter,
                                         AndroidPromotionServiceV3 androidPromotionService) {
        this.lobbySessionCache = lobbySessionCache;
        this.billingService = billingService;
        this.responseWriter = responseWriter;
        this.androidPromotionService = androidPromotionService;
    }


    /**
     * @deprecated use /api/1.0/payments/products/{platform}
     */
    @RequestMapping(value = "/api/1.0/payments/android/google/products")
    public void requestProducts(HttpServletRequest request,
                                HttpServletResponse response,
                                @RequestParam(value = "gameType", required = true) final String gameType) throws IOException {
        final LobbySession session = verifyProductRequest(request, response, gameType);
        if (session != null) {
            AndroidStoreProducts products = androidPromotionService.getAvailableProducts(session.getPlayerId(), ANDROID, gameType);

            responseWriter.writeOk(response, products);
        }
    }

    /* Verifies game type and session. Returns null if no session.
    Sends error response with status SC_BAD_REQUEST if game type is missing.
    Sends error response with status SC_UNAUTHORIZED if no session.
    */
    private LobbySession verifyProductRequest(HttpServletRequest request, HttpServletResponse response, String gameType) {
        if (!hasParameter("gameType", gameType, request, response)) {
            return null;
        }

        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session == null) {
            LOG.info("No active session when requesting android products");
            sendUnauthorisedResponse(response);
            return null;
        }
        return session;
    }

    private void sendUnauthorisedResponse(final HttpServletResponse response) {
        safeSendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - no session");
    }

    @RequestMapping(value = "/api/1.0/payments/android/google/purchases", method = RequestMethod.POST)
    public void requestPurchase(HttpServletRequest request, HttpServletResponse response,
                                @RequestParam(required = false) BigDecimal playerId,
                                @RequestParam(required = false) String gameType,
                                @RequestParam(required = false) String productId,
                                @RequestParam(required = false) String promotionId) throws IOException {
        LOG.debug("requestPurchase: playerId={}, productId={}, promotionId={}", playerId, productId, promotionId);

        if (!hasParameter("playerId", playerId, request, response)
                || !hasParameter("gameType", gameType, request, response)
                || !hasParameter("productId", productId, request, response)) {
            return;
        }

        if (playerHasSession(request, response, playerId)) {
            try {
                Long promotionIdLong = safeGetPromotionId(promotionId);
                Purchase result = billingService.createPurchaseRequest(playerId, gameType, productId, promotionIdLong);
                responseWriter.writeOk(response, result);
            } catch (NumberFormatException nfe) {
                safeSendError(response, HttpServletResponse.SC_BAD_REQUEST, "cannot convert promotionId (" + promotionId + ") to Long");
            } catch (Exception e) {
                LOG.error("Failed to create purchase request. playerId={}, gameType={}, productId={}, promotionId={}",
                        playerId, gameType, productId, promotionId, e);
                responseWriter.writeOk(response, e);
            }
        } else {
            // Note: playerHasSession will already have sent an error...
        }
    }

    /**
     * Safely converts given promotion id from String to Long. Null is returned if arg is null or empty.
     *
     * @param promotionId value to convert
     * @return null if promotionId is null or empty, otherwise Long.
     * @throws NumberFormatException if promotionId fails to convert.
     */
    private Long safeGetPromotionId(String promotionId) {
        Long promotionIdLong = null;
        // TODO the "null" check is since the client sends null promoId as "null". Remove when clients stop adding promotionId to parameter map when null.
        if (StringUtils.isNotBlank(promotionId) && !"null".equalsIgnoreCase(promotionId)) {
            promotionIdLong = Long.valueOf(promotionId);
        }
        return promotionIdLong;
    }

    private boolean playerHasSession(HttpServletRequest request, HttpServletResponse response, BigDecimal playerId) {
        final LobbySession session = lobbySessionCache.getActiveSession(request);

        if (session == null) {
            LOG.info("No active session when requesting android purchase for playerId={}", playerId);
            sendUnauthorisedResponse(response);
            return false;
        }

        if (playerId.compareTo(session.getPlayerId()) != 0) {
            LOG.error("Session active for player ({}), when requesting android purchase for, playerId={}",
                    session.getPlayerId(), playerId);
            safeSendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - player id and session player id mismatch");
            return false;
        }
        return true;
    }

    private void safeSendError(HttpServletResponse response, int httpCode, String errorMessage) {
        try {
            responseWriter.writeError(response, httpCode, errorMessage);
        } catch (IOException e) {
            LOG.error("failed to send error to client. http error code: {}", httpCode);
        }
    }

    @RequestMapping(value = "/api/1.0/payments/android/google/failure", method = RequestMethod.POST)
    public void logFailure(HttpServletRequest request,
                           HttpServletResponse response,
                           @RequestParam(value = "purchaseId") String purchaseId,
                           @RequestParam(value = "message") String message,
                           @RequestParam(required = false) String userCancelled) {
        LOG.debug("logging failed transaction: purchaseId={}, message={}", purchaseId, message);
        if (!hasParameter("purchaseId", purchaseId, request, response) || !hasParameter("message", message, request, response)) {
            return;
        }

        if (Boolean.parseBoolean(userCancelled)) {
            billingService.logUserCancelledTransaction(purchaseId, message);
        } else {
            billingService.logFailedTransaction(purchaseId, message);
        }
        safeWriteEmptyResponse(response);
    }

    private void safeWriteEmptyResponse(HttpServletResponse response) {
        try {
            responseWriter.writeNoContent(response, HttpStatus.SC_OK);
        } catch (IOException e) {
            // ignored
        }
    }


    @RequestMapping(value = "/api/1.0/payments/android/google/receipt", method = RequestMethod.POST)
    public void creditPurchase(HttpServletRequest request,
                               HttpServletResponse response,
                               @RequestParam(value = "gameType") String gameType,
                               @RequestParam(value = "orderData") String orderData,
                               @RequestParam(value = "signature") String signature) throws IOException {
        LOG.debug("crediting purchase: orderData={}, signature={}", orderData, signature);
        if (!hasParameter("gameType", gameType, request, response)
                || !hasParameter("orderData", orderData, request, response)
                || !hasParameter("signature", signature, request, response)) {
            return;
        }

        Object result;
        try {
            final LobbySession session = lobbySessionCache.getActiveSession(request);
            if (session == null) {
                sendUnauthorisedResponse(response);
                return;
            }

            result = billingService.creditPurchase(gameType, orderData, signature, session.getPartnerId());

        } catch (PurchaseException e) {
            result = e;
            LOG.error("Failed to credit purchase (debug-message={}). Error follows:", e.getDebugMessage(), e);
        }

        responseWriter.writeOk(response, result);
    }
}
