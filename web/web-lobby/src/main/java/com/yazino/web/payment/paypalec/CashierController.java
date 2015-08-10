package com.yazino.web.payment.paypalec;

import com.yazino.platform.Platform;
import com.yazino.platform.account.*;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.web.domain.PaymentEmailBodyTemplate;
import com.yazino.web.domain.email.BoughtChipsEmailBuilder;
import com.yazino.web.payment.TransactionIdGenerator;
import com.yazino.web.service.PurchaseTracking;
import com.yazino.web.service.QuietPlayerEmailer;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import static com.yazino.platform.account.ExternalTransactionStatus.*;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.PAYPAL;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Cashier controller for PayPal Express Checkout.
 *
 * @see "https://cms.paypal.com/uk/cgi-bin/?cmd=_render-content&content_ID=developer/e_howto_api_ECGettingStarted"
 */
@Controller("paypalEcCashierController")
@RequestMapping("/payment/paypal-ec/")
public class CashierController implements InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(CashierController.class);
    private static final String CASHIER_NAME = "PayPal";

    private final PaypalRequester paypalRequester;
    private final LobbySessionCache lobbySessionCache;
    private final WalletService walletService;
    private final CommunityService communityService;
    private final CookieHelper cookieHelper;
    private final CashierConfig cashierConfig;
    private final PlayerService playerService;
    private final BuyChipsPromotionService buyChipsPromotionService;
    private final QuietPlayerEmailer emailer;
    private final PurchaseTracking purchaseTracking;
    private final TransactionIdGenerator transactionIdGenerator;

    private String assetUrl;
    private String paypalEnvironment;

    @Autowired
    public CashierController(@Qualifier("safeBuyChipsPromotionService")
                             final BuyChipsPromotionService buyChipsPromotionService,
                             final PaypalRequester paypalRequester,
                             final LobbySessionCache lobbySessionCache,
                             final WalletService walletService,
                             final CommunityService communityService,
                             final PlayerService playerService,
                             final CookieHelper cookieHelper,
                             @Qualifier("paypalEcCashierConfig") final CashierConfig cashierConfig,
                             final PurchaseTracking purchaseTracking,
                             final QuietPlayerEmailer emailer,
                             final TransactionIdGenerator transactionIdGenerator) {
        notNull(buyChipsPromotionService, "buyChipsPromotionService may not be null");
        notNull(transactionIdGenerator, "transactionIdGenerator may not be null");
        notNull(paypalRequester, "paypalRequester may not be null");

        this.paypalRequester = paypalRequester;
        this.lobbySessionCache = lobbySessionCache;
        this.playerService = playerService;
        this.walletService = walletService;
        this.communityService = communityService;
        this.cookieHelper = cookieHelper;
        this.cashierConfig = cashierConfig;
        this.buyChipsPromotionService = buyChipsPromotionService;
        this.purchaseTracking = purchaseTracking;
        this.emailer = emailer;
        this.transactionIdGenerator = transactionIdGenerator;
    }

    @ModelAttribute("config")
    public CashierConfig getCashierConfig() {
        return cashierConfig;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        assetUrl = cashierConfig.getAssetUrl();
        paypalEnvironment = cashierConfig.getPaypalApiEnvironment();
    }

    private void incrementBalance(final BigDecimal playerId,
                                  final BigDecimal sessionId,
                                  final BigDecimal accountId,
                                  final String externalTransactionId,
                                  final String internalTransactionId,
                                  final PaymentOption paymentOption,
                                  final DateTime timeStamp,
                                  String gameType,
                                  final ExpressCheckoutPayment requestResult,
                                  final Long promoId)
            throws WalletServiceException {
        LOG.debug("Processing chip deposit for account id {}; int tx id = {}; ext tx id = {}",
                accountId, internalTransactionId, externalTransactionId);

        walletService.record(anExternalTransactionWithStatus(
                SUCCESS, accountId, internalTransactionId, externalTransactionId, gameType,
                requestResult.getCurrency(), requestResult.getAmount(), paymentOption.getNumChipsPerPurchase(PAYPAL.name()), timeStamp,
                playerId, sessionId, promoId));

        logPromotionPlayerReward(playerId, paymentOption, timeStamp);
        communityService.asyncPublishBalance(playerId);
        updatePreferredPaymentMethod(playerId, PAYPAL);
    }

    private DateTime dateTimeFromPayPal(final String timeStamp) {
        final DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        return fmt.parseDateTime(timeStamp);
    }

    private ExternalTransaction anExternalTransactionWithStatus(final ExternalTransactionStatus status,
                                                                final BigDecimal accountId,
                                                                final String internalTransactionId,
                                                                final String externalTransactionId,
                                                                final String gameType,
                                                                final String currencyCode,
                                                                final String amount,
                                                                final BigDecimal numberOfChips,
                                                                final DateTime timestamp,
                                                                final BigDecimal playerId,
                                                                final BigDecimal sessionId,
                                                                final Long promoId) {
        return ExternalTransaction.newExternalTransaction(accountId)
                .withInternalTransactionId(internalTransactionId)
                .withExternalTransactionId(externalTransactionId)
                .withMessage("x-x-x", timestamp)
                .withAmount(currencyFor(currencyCode), bigDecimalFor(amount))
                .withPaymentOption(numberOfChips, null)
                .withCreditCardNumber("x-x-x")
                .withCashierName(CASHIER_NAME)
                .withStatus(status)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(gameType)
                .withPlayerId(playerId)
                .withSessionId(sessionId)
                .withPromotionId(promoId)
                .withPlatform(Platform.WEB)
                .build();
    }

    private BigDecimal bigDecimalFor(final String amount) {
        if (amount == null) {
            return null;
        }
        return new BigDecimal(amount);
    }

    private Currency currencyFor(final String currencyCode) {
        if (currencyCode == null) {
            return null;
        }
        return Currency.getInstance(currencyCode);
    }

    private void logPromotionPlayerReward(final BigDecimal playerId,
                                          final PaymentOption paymentOption,
                                          final DateTime dateTime) {
        if (paymentOption.hasPromotion(PAYPAL)) {
            final PromotionPaymentOption promotion = paymentOption.getPromotion(PAYPAL);
            buyChipsPromotionService.logPlayerReward(playerId, promotion.getPromoId(), PAYPAL,
                    paymentOption.getId(), dateTime);
        }
    }

    private void updatePreferredPaymentMethod(final BigDecimal playerId,
                                              final PaymentPreferences.PaymentMethod paymentMethod) {
        PaymentPreferences paymentPreferences = playerService.getPaymentPreferences(playerId);

        if (paymentPreferences != null) {
            paymentPreferences = paymentPreferences.withPaymentMethod(paymentMethod);
        } else {
            paymentPreferences = new PaymentPreferences(paymentMethod);
        }

        playerService.updatePaymentPreferences(playerId, paymentPreferences);
    }

    @RequestMapping("/return")
    public ModelAndView returnFromPaypal(@RequestParam("token") final String token,
                                         @RequestParam(value = "paymentOptionId", required = false) final String paymentOptionId,
                                         @RequestParam(required = false) final Long promoId,
                                         final HttpServletRequest request)
            throws IOException, WalletServiceException {
        final String gameType = cookieHelper.getPaymentGameType(request.getCookies());
        final LobbySession activeSession = lobbySessionCache.getActiveSession(request);

        final BigDecimal playerId = activeSession.getPlayerId();

        ExpressCheckoutDetails checkoutDetails;
        try {
            checkoutDetails = paypalRequester.getExpressCheckoutDetails(token);
            if (checkoutDetails.getCheckoutStatus().isCompleted()) {
                LOG.error("Attempting to reprocess already processed transaction with external ID {}", checkoutDetails.getTransactionId());
                return getErrorModelAndView(gameType);
            }

        } catch (Exception e) {
            LOG.error("Failed to retrieve checkout details with token {}", token, e);
            return getErrorModelAndView(gameType);
        }

        final PaymentOption paymentOption = getPaymentOption(playerId, paymentOptionId, promoId);
        if (paymentOption == null) {
            throw new IllegalArgumentException("Could not find paymentOption for ID " + paymentOptionId);
        }
        final BigDecimal numberOfChips = paymentOption.getNumChipsPerPurchase(PAYPAL.name());
        final BigDecimal accountId = playerService.getAccountId(playerId);

        ExpressCheckoutPayment requestResult;
        String externalTransactionId = null;
        try {
            requestResult = paypalRequester.doExpressCheckoutPayment(checkoutDetails.getToken(),
                    checkoutDetails.getPayerId(), checkoutDetails.getInvoiceId(),
                    checkoutDetails.getAmount(), checkoutDetails.getCurrency(), numberOfChips);
            externalTransactionId = requestResult.getTransactionId();

            if (!requestResult.isSuccessful()) {
                LOG.info("Checkout failed for player {}; invoice: {}; errors were {}",
                        playerId, checkoutDetails.getInvoiceId(), requestResult.getErrors());
                return logAndShowErrorView(gameType, checkoutDetails, externalTransactionId,
                        numberOfChips, accountId, playerId, activeSession.getSessionId(), promoId);
            }

        } catch (Exception e) {
            LOG.error("Failed to invoke checkout payment for int tx: {}", checkoutDetails.getInvoiceId(), e);
            return logAndShowErrorView(gameType, checkoutDetails, externalTransactionId,
                    numberOfChips, accountId, playerId, activeSession.getSessionId(), promoId);
        }

        LOG.debug("Successful transaction; int:{} / ext:{}: {} {}", checkoutDetails.getInvoiceId(),
                externalTransactionId, requestResult.getCurrency(), requestResult.getAmount());

        final DateTime timeStamp = dateTimeFromPayPal(requestResult.getTimestamp());
        incrementBalance(playerId, activeSession.getSessionId(), accountId, externalTransactionId, checkoutDetails.getInvoiceId(),
                paymentOption, timeStamp, gameType, requestResult, promoId);

        sendConfirmationEmail(activeSession, requestResult.getAmount(), requestResult.getCurrency(), externalTransactionId, numberOfChips);
        purchaseTracking.trackSuccessfulPurchase(playerId);

        final ModelAndView mav = new ModelAndView("payment/paypal-ec/success");
        mav.addObject("numberOfChips", numberOfChips);
        mav.addObject("assetUrl", assetUrl);
        mav.addObject("gameType", gameType);
        return mav;
    }

    private ModelAndView logAndShowErrorView(final String gameType,
                                             final ExpressCheckoutDetails checkoutDetails,
                                             final String externalTransactionId,
                                             final BigDecimal numberOfChips,
                                             final BigDecimal accountId,
                                             final BigDecimal playerId,
                                             final BigDecimal sessionId,
                                             final Long promoId) {
        record(anExternalTransactionWithStatus(
                FAILURE, accountId, checkoutDetails.getInvoiceId(), externalTransactionId, gameType,
                checkoutDetails.getCurrency(), checkoutDetails.getAmount(), numberOfChips, new DateTime(), playerId, sessionId, promoId));

        return getErrorModelAndView(gameType);
    }

    private void sendConfirmationEmail(final LobbySession activeSession,
                                       final String amt,
                                       final String currencyCode,
                                       final String transactionId,
                                       final BigDecimal numChipsPerPurchase) {
        final BoughtChipsEmailBuilder builder = new BoughtChipsEmailBuilder();
        builder.withCardNumber("");
        builder.withCost(bigDecimalFor(amt));
        builder.withEmailAddress(activeSession.getEmail());
        builder.withCurrency(currencyFor(currencyCode));
        builder.withFirstName(activeSession.getPlayerName());
        builder.withPaymentDate(new DateTime().toDate());
        builder.withPurchasedChips(numChipsPerPurchase);
        builder.withPaymentEmailBodyTemplate(PaymentEmailBodyTemplate.Paypal);
        builder.withPaymentId(transactionId);

        emailer.quietlySendEmail(builder);
    }

    private ModelAndView getErrorModelAndView(final String gameType) {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("assetUrl", assetUrl);
        parameters.put("gameType", gameType);
        return new ModelAndView("payment/paypal-ec/error", parameters);
    }

    @RequestMapping("/cancel")
    public ModelAndView cancel(final HttpServletRequest request) {
        LOG.debug("Transaction cancelled");
        final ModelAndView mav = new ModelAndView();
        mav.setViewName("payment/paypal-ec/cancel");
        mav.addObject("assetUrl", assetUrl);
        mav.addObject("gameType", cookieHelper.getPaymentGameType(request.getCookies()));
        return mav;
    }

    @RequestMapping("/process")
    public ModelAndView processPayment(@RequestParam("paymentOption") final String paymentOptionId,
                                       @RequestParam(required = false) final Long promoId,
                                       final HttpServletRequest request)
            throws IOException {
        LOG.debug("Processing payment: paymentOptionId={}, promoId={};", paymentOptionId, promoId);

        final LobbySession activeSession = lobbySessionCache.getActiveSession(request);
        final String gameType = cookieHelper.getPaymentGameType(request.getCookies());

        if (invalidRequest(paymentOptionId)) {
            return getErrorModelAndView(gameType);
        }

        final BigDecimal playerId = lobbySessionCache.getActiveSession(request).getPlayerId();
        final PaymentOption paymentOption = getPaymentOption(playerId, paymentOptionId, promoId);
        if (paymentOption == null) {
            LOG.error("Payment option {} was not recognized. (playerId={}, promoId={})",
                    paymentOptionId, playerId, promoId);
            final ModelAndView errorModelAndView = getErrorModelAndView(gameType);
            errorModelAndView.addObject("errorCode", "unknown.paymentOption");
            return errorModelAndView;
        }

        final BigDecimal accountId = playerService.getAccountId(playerId);
        final String internalTransactionId = transactionIdGenerator.generateTransactionId(accountId);
        final String purchaseAmount = String.valueOf(paymentOption.getAmountRealMoneyPerPurchase());
        final String purchaseCurrency = paymentOption.getRealMoneyCurrency();
        final BigDecimal numberOfChips = paymentOption.getNumChipsPerPurchase(PAYPAL.name());

        final ExternalTransaction transaction = anExternalTransactionWithStatus(
                REQUEST, accountId, internalTransactionId, null, gameType,
                purchaseCurrency, purchaseAmount, numberOfChips, new DateTime(), playerId, activeSession.getSessionId(), promoId);
        record(transaction);

        final String token;
        try {
            final String baseUrl = buildBaseUrl(request);
            final String urlParams = urlParametersFor(paymentOptionId, promoId, paymentOption);
            token = paypalRequester.setExpressCheckout(baseUrl + "/return" + urlParams, baseUrl + "/cancel" + urlParams,
                    internalTransactionId, purchaseAmount, purchaseCurrency, numberOfChips);

        } catch (PaypalRequestException e) {
            return getErrorModelAndView(gameType);
        }

        LOG.debug("paypalEnvironment == {}", paypalEnvironment);

        return new ModelAndView(new RedirectView("https://www." + environmentPrefix()
                + "paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=" + token, false, true, false));
    }

    private void record(final ExternalTransaction transaction) {
        try {
            walletService.record(transaction);
        } catch (WalletServiceException e) {
            LOG.error("Failed to record transaction: {}", e);
        }
    }

    private String environmentPrefix() {
        final String web;
        if ("live".equals(paypalEnvironment)) {
            web = "";
        } else {
            web = paypalEnvironment + ".";
        }
        return web;
    }

    private String urlParametersFor(final String paymentOptionId,
                                    final Long promoId,
                                    final PaymentOption paymentOption) {
        String urlParams = String.format("?paymentOptionId=%s", paymentOptionId);
        if (promoId != null) {
            urlParams = String.format("%s&promoId=%s&promoChips=%s", urlParams, promoId,
                    paymentOption.getNumChipsPerPurchase(PAYPAL.name()));
        }
        return urlParams;
    }

    private boolean invalidRequest(final String paymentOptionId) {
        if (StringUtils.isBlank(paymentOptionId)) {
            LOG.error("Payment OptionId must be specified");
            return true;
        }
        return false;
    }

    private PaymentOption getPaymentOption(final BigDecimal playerId, final String paymentOptionId,
                                           final Long promoId) {
        PaymentOption paymentOption = null;
        if (promoId != null) {
            paymentOption = buyChipsPromotionService.getPaymentOptionFor(playerId, promoId,
                    PAYPAL, paymentOptionId);
            if (paymentOption == null) {
                LOG.error("Failed to load buy chips payment option for player={}, promoId={}, "
                        + "paymentOptionId={}, paymentMethod={}",
                        playerId, promoId, paymentOptionId, PAYPAL.name());
            }
        }
        if (paymentOption == null) {
            LOG.debug("Using default PaymentOption for paymentOptionId={}", paymentOptionId);
            paymentOption = buyChipsPromotionService.getDefaultPaymentOptionFor(paymentOptionId, Platform.WEB);
        }
        return paymentOption;
    }

    private String buildBaseUrl(final HttpServletRequest request) {
        final StringBuilder url = new StringBuilder();

        if (request.isSecure()) {
            url.append("https://");
        } else {
            url.append("http://");
        }
        url.append(request.getServerName());
        url.append(":");
        url.append(request.getServerPort());
        url.append(request.getContextPath());
        url.append("/payment/paypal-ec");

        return url.toString();
    }
}
