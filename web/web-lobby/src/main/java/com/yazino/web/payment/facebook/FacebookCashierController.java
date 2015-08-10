package com.yazino.web.payment.facebook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.web.domain.facebook.SignedRequest;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.web.payment.facebook.FacebookPayment.Status.completed;
import static com.yazino.web.payment.facebook.FacebookPayment.Status.failed;
import static com.yazino.web.payment.facebook.FacebookPayment.Type.charge;
import static java.lang.String.format;
import static org.apache.commons.lang3.Validate.notNull;

@Controller("facebookCashierController")
@RequestMapping("/payment/facebook/")
public class FacebookCashierController {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookCashierController.class);

    private static final String FACEBOOK_CALLBACK_SUBSCRIPTION_TOKEN = "yaz1norules";

    private final FacebookPaymentService facebookService;
    private final FacebookConfiguration facebookConfiguration;
    private final SignedRequestValidator signedRequestValidator;
    private final LobbySessionCache lobbySessionCache;
    private final FacebookPaymentIntegrationService facebookPaymentIntegrationService;

    @Autowired
    public FacebookCashierController(final FacebookPaymentService facebookService,
                                     @Qualifier("facebookConfiguration") final FacebookConfiguration facebookConfiguration,
                                     final SignedRequestValidator signedRequestValidator,
                                     LobbySessionCache lobbySessionCache,
                                     final FacebookPaymentIntegrationService facebookPaymentIntegrationService) {
        notNull(facebookService, "facebookService may not be null");
        notNull(facebookConfiguration, "facebookConfiguration may not be null");
        notNull(signedRequestValidator, "signedRequestValidator may not be null");
        notNull(lobbySessionCache, "lobbySessionCache may not be null");
        notNull(facebookPaymentIntegrationService, "facebookPaymentIntegrationService may not be null");

        this.facebookService = facebookService;
        this.facebookConfiguration = facebookConfiguration;
        this.signedRequestValidator = signedRequestValidator;
        this.lobbySessionCache = lobbySessionCache;
        this.facebookPaymentIntegrationService = facebookPaymentIntegrationService;
    }

    @RequestMapping(value = "/callback/{gameType}", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getItemPrice(@RequestParam(value = "method", required = true) final String method,
                                            @RequestParam(value = "product", required = true) final String product,
                                            @RequestParam(value = "signed_request", required = true) final String signedRequest,
                                            @PathVariable String gameType,
                                            HttpServletRequest request) throws IOException {
        LOG.info("Handling a facebook callback request {} with method {}", request.getRequestURL(), method);
        if ("payments_get_item_price".equals(method)) {
            return handlePaymentsGetItemPrice(product, signedRequest, gameType);
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> handlePaymentsGetItemPrice(final String product,
                                                           final String signedRequest,
                                                           final String gameType) {
        final FacebookAppConfiguration appConfigForGameType = facebookConfiguration.getAppConfigFor(gameType,
                FacebookConfiguration.ApplicationType.CANVAS, FacebookConfiguration.MatchType.STRICT);
        if (!signedRequestValidator.validate(signedRequest, appConfigForGameType.getSecretKey())) {
            return null;
        }
        FacebookProductUrl productUrl;
        try {
            productUrl = new FacebookProductUrl(product);

            final SignedRequest facebookRequest = getSignedRequest(signedRequest, appConfigForGameType);
            final BigDecimal playerId = facebookService.getPlayerId((String) facebookRequest.get("user_id"));
            final PaymentOption paymentOption = facebookService.resolvePaymentOption(playerId,
                    productUrl.getWebPackage(),
                    productUrl.getPromoId());
            facebookService.logTransactionAttempt(playerId,
                    ((Map<String, String>) facebookRequest.get("payment")).get("request_id"),
                    paymentOption,
                    gameType,
                    productUrl.getPromoId());
            Map<String, Object> response = newHashMap();
            Map<String, Object> content = newHashMap();
            final String rebuiltUrl = rebuildProductFromPaymentOption(paymentOption, productUrl);
            if (!rebuiltUrl.equals(productUrl.toUrl())) {
                //someone's been tampering!
                LOG.warn("{} is not the same as {} for user {}", rebuiltUrl, productUrl.toUrl(), playerId);

            }
            content.put("product", rebuiltUrl);
            content.put("amount", paymentOption.getAmountRealMoneyPerPurchase());
            content.put("currency", paymentOption.getRealMoneyCurrency());
            response.put("method", "payments_get_item_price");
            response.put("content", content);
            return response;

        } catch (Exception e) {
            LOG.error("Invalid FaceBook OpenGraph product: {}", product, e);
            return null;
        }
    }

    protected SignedRequest getSignedRequest(final String signedRequest,
                                             final FacebookAppConfiguration appConfigForGameType) {
        return new SignedRequest(signedRequest, appConfigForGameType.getSecretKey());
    }

    private String rebuildProductFromPaymentOption(final PaymentOption paymentOption,
                                                   final FacebookProductUrl product) {
        return new FacebookProductUrl(paymentOption, product).toUrl();
    }

    @RequestMapping("/fail")
    public void logFailedPurchase(HttpServletRequest request,
                                  HttpServletResponse response,
                                  @RequestParam String gameType,
                                  @RequestParam String productUrl,
                                  @RequestParam String requestId,
                                  @RequestParam String errorCode,
                                  @RequestParam String errorMessage) throws IOException {
        final LobbySession activeSession = lobbySessionCache.getActiveSession(request);
        if (activeSession == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        LOG.debug("Logging failed txn - playerId={}, gameType={}, productUrl={}, requestId={}, errorCode={}, errorMessage={}",
                activeSession.getPlayerId(), gameType, productUrl, requestId, errorCode, errorMessage);

        final BigDecimal playerId = activeSession.getPlayerId();
        try {
            final FacebookProductUrl url = new FacebookProductUrl(productUrl);
            final String paymentOptionId = url.getWebPackage();
            final Long promoId = url.getPromoId();
            PaymentOption paymentOption = facebookService.resolvePaymentOption(playerId, paymentOptionId, promoId);
            if (paymentOption == null) {
                LOG.error("Could not resolve payment option for playerId={}, paymentOptionId={}, promoId={}",
                        playerId,
                        paymentOptionId,
                        promoId);
                writeJsonToResponse(response, "{}");
                return;
            }
            facebookService.logFailedTransaction(errorCode, errorMessage, playerId, null, requestId, paymentOption,
                    gameType, promoId);
        } catch (WalletServiceException e) {
            LOG.error("Invalid");
        }
        writeJsonToResponse(response, "{}");
    }

    /**
     * Used while registering payment subscriptions in App Settings -> Real-time updates.
     * For payments we are interested in the 'payments' object, and 'actions' and 'disputes' fields
     * this is just used in setup of the app, not in production use, but is needed to setup production
     */
    @RequestMapping(value = "/notifications/{gameType}",
            method = RequestMethod.GET,
            params = {"hub.mode", "hub.challenge", "hub.verify_token"})
    public void registerSubscription(HttpServletResponse response, @RequestParam("hub.mode") String hubMode,
                                     @RequestParam("hub.challenge") String hubChallenge,
                                     @RequestParam("hub.verify_token") String verifyToken,
                                     @PathVariable("gameType") String gameType) throws IOException {
        LOG.info("Handling a facebook verification callback for {}", gameType);
        if ("subscribe".equals(hubMode) && FACEBOOK_CALLBACK_SUBSCRIPTION_TOKEN.equals(verifyToken)) {
            final PrintWriter writer = response.getWriter();
            writer.write(hubChallenge);
            writer.flush();
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/notifications/{gameType}", method = RequestMethod.POST)
    public void confirmPayment(HttpServletResponse response,
                               @RequestBody String requestBody,
                               @PathVariable("gameType") String gameType) throws IOException {
        final FacebookPayment facebookPayment;
        try {
            String paymentId = getPaymentId(requestBody);
            facebookPayment = facebookPaymentIntegrationService.retrievePayment(gameType, paymentId);

            LOG.info("Received a facebook payment. Request body is: {}, facebookPayment is: {}", requestBody, facebookPayment);

            final BigDecimal playerId = facebookService.getPlayerId(facebookPayment.getFacebookUserId());

            final PaymentOption paymentOption = facebookService.resolvePaymentOption(playerId,
                    facebookPayment.getProductId(),
                    facebookPayment.getPromoId());

            if (facebookPayment.getStatus() == completed) {
                handleCompletedPayment(requestBody, gameType, facebookPayment, paymentId, playerId, paymentOption);

            } else if (facebookPayment.getStatus() == failed) {
                facebookService.logFailedTransaction(
                        null,
                        format("Received notification for %s with failed status", facebookPayment.getType()),
                        playerId,
                        paymentId,
                        facebookPayment.getRequestId(),
                        paymentOption,
                        gameType,
                        facebookPayment.getPromoId());
            } else {
                LOG.warn("Confirmation was not a success or failure for: {}", requestBody);
            }

        } catch (WalletServiceException e) {
            LOG.error("Failed to confirm payment: {}", requestBody, e);
        }

        writeJsonToResponse(response, "{\"1\":\"1\"}");
    }

    private void handleCompletedPayment(final String requestBody,
                                        final String gameType,
                                        final FacebookPayment facebookPayment,
                                        final String paymentId,
                                        final BigDecimal playerId,
                                        final PaymentOption paymentOption) throws IOException {
        if (facebookPayment.getType() != charge) {
            LOG.warn("Confirmation was not for a charge or a dispute. No action will be taken. External transaction ID: {}",
                    paymentId);
            return;

        }
        if (isADispute(requestBody)) {
            if (facebookPayment.getDisputeDate() != null) {
                facebookService.disputePurchase(facebookPayment.getRequestId(),
                        paymentId,
                        playerId,
                        paymentOption,
                        gameType,
                        facebookPayment.getPromoId(),
                        facebookPayment.getDisputeReason(),
                        facebookPayment.getDisputeDate());
            } else {
                LOG.debug("Confirmation changed dispute but does not have pending status. External transaction ID: {}",
                        paymentId);
            }

        } else {
            facebookService.completePurchase(
                    playerId,
                    gameType,
                    paymentOption,
                    paymentId,
                    facebookPayment.getRequestId(),
                    facebookPayment.getCurrencyCode(),
                    facebookPayment.getAmount(),
                    facebookPayment.getPromoId());
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isADispute(final String requestBody) throws IOException {
        final Map<String, Object> request = objectMapper().readValue(requestBody, Map.class);
        final List<Map<String, Object>> entries = (List) request.get("entry");
        final List<String> changedFields = (List<String>) entries.get(0).get("changed_fields");
        return changedFields != null
                && changedFields.contains("disputes");
    }

    private ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        return objectMapper;
    }

    @SuppressWarnings("unchecked")
    public String getPaymentId(String requestBody) throws IOException, WalletServiceException {
        Map<String, Object> map = objectMapper().readValue(requestBody, Map.class);
        if (!"payments".equals(map.get("object"))) {
            LOG.error("Notification '" + map.get("object") + "' ignored");
            LOG.error("Body=" + requestBody);
            throw new WalletServiceException("Not a payment object");
        }
        List<Map<String, Object>> entries = (List) map.get("entry");
        return (String) entries.get(0).get("id");

    }

    @RequestMapping(value = "/earnedChips", params = {"player_id"})
    @ResponseBody
    public String getEarnChipsToday(@RequestParam(value = "player_id", required = true) BigDecimal playerId) {
        return Integer.toString(facebookService.getEarnedChipsToday(playerId).intValue());
    }

    public Map getEarnChipsData(final String earnChipsJson) {
        final Map modified = new JsonHelper().deserialize(Map.class, earnChipsJson);
        return (Map) modified.get("modified");
    }

    private void writeJsonToResponse(final HttpServletResponse response,
                                     final String jsonResponse) {
        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            LOG.debug("Writing out to payment response: {}", jsonResponse);
            response.getWriter().write(jsonResponse);

        } catch (IOException e) {
            LOG.error("Cannot write response for payment callback", e);
            handleErrorResponse(response);
        }
    }

    private void handleErrorResponse(final HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (IOException ignored) {
            // ignored
        }
    }

}
