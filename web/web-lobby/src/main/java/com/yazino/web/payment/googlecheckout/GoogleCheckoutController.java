package com.yazino.web.payment.googlecheckout;

import com.yazino.platform.Platform;
import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.yazino.web.util.RequestParameterUtils.hasParameter;

@Controller("googleCheckoutController")
@AllowPublicAccess("/payments/googlecheckout/*")
@RequestMapping("/payments/googlecheckout")
public class GoogleCheckoutController {
    private static final Logger LOG = LoggerFactory.getLogger(GoogleCheckoutController.class);

    private final GoogleCheckoutService googleCheckoutService;
    private final LobbySessionCache lobbySessionCache;
    private final AndroidPromotionService androidPromotionService;
    private final AndroidInAppBillingService androidInAppBillingService;

    @Autowired
    public GoogleCheckoutController(final LobbySessionCache lobbySessionCache,
                                    final GoogleCheckoutService googleCheckoutService,
                                    final AndroidPromotionService androidPromotionService,
                                    AndroidInAppBillingService androidInAppBillingService) {
        this.googleCheckoutService = googleCheckoutService;
        this.lobbySessionCache = lobbySessionCache;
        this.androidPromotionService = androidPromotionService;
        this.androidInAppBillingService = androidInAppBillingService;
    }

    /**
     * @deprecated use /api/1.0/payments/products/{platform}    instead
     */
    @RequestMapping("/products")
    public void fetchBuyChipStoreProducts(final HttpServletRequest request,
                                          final HttpServletResponse response,
                                          @RequestParam(value = "gameType", required = false) final String gameType) throws IOException {

        if (!hasParameter("gameType", gameType, request, response)) {
            return;
        }

        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session == null) {
            LOG.warn("No active session");
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (IOException e) {
                // can't writeOk to already committed response
            }
            return;
        }

        final AndroidBuyChipStoreConfig buyChipStoreConfig =
                androidPromotionService.getBuyChipStoreConfig(session.getPlayerId(), Platform.ANDROID, gameType);


        String outputString;
        if (buyChipStoreConfig != null) {
            outputString = new JsonHelper().serialize(buyChipStoreConfig);
        } else {
            outputString = "{}";
        }

        response.setContentType(MediaType.APPLICATION_JSON.toString());
        response.getWriter().write(outputString);
        response.flushBuffer();
    }

    /**
     * Fetch all products currently available for the game type.
     */
    @ResponseBody
    public String fetchAvailableProducts(final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         @RequestParam(value = "gameType", required = false) final String gameType) {
        if (!hasParameter("gameType", gameType, request, response)) {
            return null;
        }

        logProductRequest(gameType);
        final List<String> productIds = googleCheckoutService.fetchAvailableProducts(gameType);
        response.setContentType(MediaType.APPLICATION_JSON.toString());
        return new JsonHelper().serialize(productIds);
    }

    private void logProductRequest(final String gameType) {
        LOG.debug("Fetching google play product ids for game: {}", gameType);
    }

    /**
     * Credit player with chips. Verify that the order has been authorized by google, if so, credit player with chips,
     * else return error to caller.
     *
     * @param orderJSON JSON the client received from GooglePlay. It may help distinguish whether we have client problems
     *                  when sending the wrong order number, or whether it's Google sending the wrong order number (we have
     *                  had problems with clients sending through the 'Merchant Order Number' rather than the wallet's 'order number').
     *                  It's here since the client can't log.
     *                  It is now used since Google changed the orderId return to the app. From 2012-12-05, for clients,
     *                  using latest google play app, the orderId is now the 'Merchant Order Number' NOT the
     *                  'Google Order Number'. This means using the current checkout-sdk (2.5.1) there doesn't seem to be
     *                  an easy way of retrieving the order details so that we can verify the transaction before crediting
     *                  the player with chips. As a pretty poor work around, if we get a 'Merchant Order Number' and the
     *                  {@code str} is not null, then we will use the product id in the json to credit players with chips.
     *                  NOTE this is temp solution.
     * @deprecated use completeOrders. Android clients should send the signature as well as promotion ids map.
     */
    @RequestMapping(value = "/complete", method = RequestMethod.POST)
    public void completeTransaction(HttpServletRequest request,
                                    HttpServletResponse response,
                                    @RequestParam(value = "gameType", required = false) String gameType,
                                    @RequestParam(value = "orderNumber", required = false) String orderNumber,
                                    @RequestParam(value = "orderJSON", required = false) String orderJSON) {
        if (!hasParameter("gameType", gameType, request, response)
                || !hasParameter("orderNumber", orderNumber, request, response)) {
            return;
        }
        logCompleteTransactionRequest(gameType, orderNumber, orderJSON);
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (session == null) {
            LOG.warn("No active session");
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (IOException e) {
                // can't write to already committed response
            }
            return;
        }

        final PaymentContext paymentContext = new PaymentContext(session.getPlayerId(), session.getSessionId(), session.getPlayerName(), gameType,
                session.getEmail(), null, null, session.getPartnerId());
        Order order;
        if (orderJSON == null) {
            // if the json isn't sent, we can only process the order if the order number is a google order number
            // as opposed the 'current' (as of 2012-12-05) when client gets merchant order number.
            order = googleCheckoutService.fulfillLegacyBuyChipsOrder(paymentContext, orderNumber);
        } else {
            // fix for client sometimes wrapping JSON in quotes - seems to be when app loads transactions from cookies after flash automatically flushes orders
            String orderJSONWithoutQuotes = stripLeadingAndTrailingQuotes(orderJSON);
            List<Order> orders = googleCheckoutService.fulfillBuyChipsOrder(paymentContext, orderJSONWithoutQuotes);
            // another bodge until client supports multiple orders, each order has been processed, but current client expects a single order in the response
            order = orders.get(0);
        }
        writeJsonToResponse(response, order);
    }

    private String stripLeadingAndTrailingQuotes(String str) {
        if (str != null) {
            return str.replaceAll("^\"|\"$", "");
        }
        return str;
    }

    private void writeJsonToResponse(HttpServletResponse response, Order order) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        try {
            response.getWriter().write(new JsonHelper().serialize(order));
            response.flushBuffer();
        } catch (IOException e) {
            LOG.error("failed to write GoogleCheckout order to response. Order: {}", order);
        }
    }

    private void logCompleteTransactionRequest(String gameType, String orderNumber, String orderJSON) {
        LOG.info("Verifying and fulfilling Google Checkout transaction for order {} and gameType {}. order json is: {}"
                , orderNumber, gameType, orderJSON);
    }

    /**
     * @deprecated use /api/1.0/payments/android/google/receipt instead
     */
    @RequestMapping(value = "/completeOrders", method = RequestMethod.POST)
    public void completeOrders(HttpServletRequest request,
                               HttpServletResponse response,
                               @RequestParam(value = "gameType") String gameType,
                               @RequestParam(value = "orderJSON") String orderJSON,
                               @RequestParam(value = "signature") String signature,
                               @RequestParam(value = "promoIds") String promoIds) {
        logCompleteOrdersRequest(gameType, orderJSON, signature, promoIds);

        final LobbySession session = lobbySessionCache.getActiveSession(request);
        if (sessionIsInactive(response, session)) {
            return;
        }

        final PaymentContext paymentContext = new PaymentContext(session.getPlayerId(), session.getSessionId(),
                session.getPlayerName(), gameType, session.getEmail(), null, null, session.getPartnerId());
        // fix for client sometimes wrapping JSON in quotes - seems to be when app loads transactions from cookies after flash automatically flushes orders
        String orderJSONWithoutQuotes = stripLeadingAndTrailingQuotes(orderJSON);
        String promoIdsJSONWithoutQuotes = stripLeadingAndTrailingQuotes(promoIds);

        final List<VerifiedOrder> orders = androidInAppBillingService.verifyAndCompleteTransactions(
                paymentContext, orderJSONWithoutQuotes, signature, promoIdsJSONWithoutQuotes);
        writeOrdersToResponse(response, orders);
    }

    private void writeOrdersToResponse(HttpServletResponse response, List<VerifiedOrder> orders) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        try {
            response.getWriter().write(new JsonHelper().serialize(orders));
            response.flushBuffer();
        } catch (IOException e) {
            LOG.error("failed to write processed Android orders to response. Order: {}", orders, e);
        }
    }

    private boolean sessionIsInactive(HttpServletResponse response, LobbySession session) {
        if (session == null) {
            LOG.warn("No active session");
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } catch (IOException e) {
                // can't write to already committed response
            }
            return true;
        }
        return false;
    }

    private void logCompleteOrdersRequest(String gameType, String orderJSON, String signature, String promoIds) {
        LOG.info(
                "Verifying and completing android orders. gameType: {}, order json is: {}, signature: {}, promoIds: {}",
                gameType, orderJSON, signature, promoIds);
    }
}
