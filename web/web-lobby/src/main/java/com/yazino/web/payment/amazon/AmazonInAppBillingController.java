package com.yazino.web.payment.amazon;

import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.api.RequestException;
import com.yazino.web.controller.util.RequestValidationHelper;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.Purchase;
import com.yazino.web.session.LobbySession;
import com.yazino.web.util.WebApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@AllowPublicAccess("/api/1.0/payments/android/amazon/**")
public class AmazonInAppBillingController {
    private static final Logger LOG = LoggerFactory.getLogger(AmazonInAppBillingController.class);

    private static final int SERVER_ERROR_CODES = 500;

    private final InAppBillingService inAppBillingService;
    private final AmazonInAppBillingRequestValidator amazonInAppBillingRequestValidator;
    private final RequestValidationHelper requestValidationHelper;
    private final WebApiResponses responseWriter;

    @Autowired
    public AmazonInAppBillingController(InAppBillingService inAppBillingService,
                                        final AmazonInAppBillingRequestValidator amazonInAppBillingRequestValidator,
                                        final RequestValidationHelper requestValidationHelper,
                                        final WebApiResponses responseWriter) {
        this.inAppBillingService = inAppBillingService;
        this.amazonInAppBillingRequestValidator = amazonInAppBillingRequestValidator;
        this.requestValidationHelper = requestValidationHelper;
        this.responseWriter = responseWriter;
    }

    @RequestMapping(value = "/api/1.0/payments/android/amazon/receipt", method = POST)
    public void creditPurchase(HttpServletRequest request,
                               HttpServletResponse response,
                               @RequestParam(value = "userId") String userId,
                               @RequestParam(value = "orderId") String internalId,
                               @RequestParam(value = "productId") String productId,
                               @RequestParam(value = "gameType") String gameType,
                               @RequestParam(value = "purchaseToken") String externalId,
                               @RequestParam(value = "promotionId", required = false) String promotionId) throws IOException {
        LOG.debug("crediting purchase: userId={}, internalId={}, productId={}, gameType={}, externalId={}, promotionId={}",
                userId, internalId, productId, gameType, externalId, promotionId);

        try {
            verifyValidReceiptRequest(request, response, userId, internalId, productId, gameType, externalId);
            LobbySession session = requestValidationHelper.verifySession(request);
            PaymentContext paymentContext = buildPaymentContext(session, gameType, promotionId);
            Purchase purchase = inAppBillingService.creditPurchase(paymentContext, userId, externalId, productId, internalId);
            responseWriter.writeOk(response, purchase);
        } catch (RequestException e) {
            log(e);
            responseWriter.writeError(response, e.getHttpStatusCode(), e.getError());
        }
    }

    @RequestMapping(value = "/api/1.0/payments/android/amazon/failure", method = POST)
    public void logFailure(HttpServletRequest request,
                           HttpServletResponse response,
                           @RequestParam(value = "orderId") String orderId,
                           @RequestParam(value = "productId") String productId,
                           @RequestParam(value = "gameType") String gameType,
                           @RequestParam(value = "message") String message,
                           @RequestParam(value = "promotionId", required = false) String promotionId) throws IOException {
        LOG.debug("logging failed transaction: internalId={}, productId={}, gameType={}, message={}, promotionId={}",
                orderId,
                productId,
                gameType,
                message,
                promotionId);
        try {
            verifyValidLogFailureRequest(request, response, orderId, productId, gameType, message);
            LobbySession session = requestValidationHelper.verifySession(request);
            PaymentContext paymentContext = buildPaymentContext(session, gameType, promotionId);

            inAppBillingService.logFailedTransaction(paymentContext, productId, orderId, message);
            responseWriter.writeNoContent(response, SC_OK);
        } catch (RequestException e) {
            log(e);
            responseWriter.writeError(response, e.getHttpStatusCode(), e.getError());
        }
    }

    private void log(final RequestException e) {
        if (e.getHttpStatusCode() >= SERVER_ERROR_CODES) {
            LOG.error(e.getError());
        } else {
            LOG.debug(e.getError());
        }
    }

    private void verifyValidReceiptRequest(final HttpServletRequest request,
                                           final HttpServletResponse response,
                                           final String userId,
                                           final String orderId,
                                           final String productId,
                                           final String gameType,
                                           final String purchaseToken) throws RequestException {
        if (!amazonInAppBillingRequestValidator.isValidReceiptRequest(request,
                response,
                userId,
                orderId,
                productId,
                gameType,
                purchaseToken)) {
            throw new RequestException(SC_BAD_REQUEST, "userId, internalId, productId, gameType, externalId are required");
        }
    }

    private void verifyValidLogFailureRequest(final HttpServletRequest request,
                                              final HttpServletResponse response,
                                              final String orderId,
                                              final String productId,
                                              final String gameType,
                                              final String message) throws RequestException {
        if (!amazonInAppBillingRequestValidator.isValidLogFailureRequest(request, response, orderId, gameType, productId, message)) {
            throw new RequestException(SC_BAD_REQUEST, "internalId, gameType, productId, message cannot be blank");
        }

    }

    private Long getPromoIdAsLong(final String promotionId) {
        Long promoId = null;
        if (!isBlank(promotionId)) {
            try {
                promoId = Long.valueOf(promotionId);
            } catch (NumberFormatException e) {
                LOG.info("Failed to verify promotion id:{} reverting to null", promotionId, e.getMessage());
            }
        }
        return promoId;
    }

    private PaymentContext buildPaymentContext(final LobbySession session,
                                               final String gameType,
                                               final String promotionId) {
        return new PaymentContext(session.getPlayerId(),
                session.getSessionId(), session.getPlayerName(),
                gameType,
                session.getEmail(),
                null,
                getPromoIdAsLong(promotionId),
                session.getPartnerId());
    }

}
