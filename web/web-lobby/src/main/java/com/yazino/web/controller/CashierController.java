package com.yazino.web.controller;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.reference.ReferenceService;
import com.yazino.web.data.CurrencyRepository;
import com.yazino.web.domain.CashierConfiguration;
import com.yazino.web.domain.CashierConfigurationContainer;
import com.yazino.web.domain.SiteConfiguration;
import com.yazino.web.domain.facebook.FacebookUserCurrency;
import com.yazino.web.domain.payment.RegisteredCardQueryResult;
import com.yazino.web.domain.payment.RegisteredCreditCardDetails;
import com.yazino.web.payment.PaymentContextBuilder;
import com.yazino.web.payment.creditcard.*;
import com.yazino.web.payment.creditcard.worldpay.WorldPayCreditCardQueryService;
import com.yazino.web.payment.facebook.FacebookPaymentOptionHelper;
import com.yazino.web.service.FacebookCurrencyService;
import com.yazino.web.service.GeolocationLookup;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.CookieHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.util.*;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.platform.Platform.FACEBOOK_CANVAS;
import static com.yazino.platform.Platform.WEB;
import static com.yazino.web.payment.creditcard.PurchaseOutcome.INVALID_ACCOUNT;
import static com.yazino.web.util.RequestParameterUtils.hasParameter;
import static java.math.BigDecimal.TEN;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class CashierController {
    private static final Logger LOG = LoggerFactory.getLogger(CashierController.class);
    public static final String REGISTERED_CARDS_PARAMETER = "registeredCards";
    private static final String VIEW_PATH = "payment/creditcard/";
    private static final String IN_ERROR = "inError";
    private static final int OFFSET_100 = 100;
    private static final int OFFSET_10 = 10;

    private static final FacebookUserCurrency DEFAULT_USER_CURRENCY = new FacebookUserCurrency(10.0,
            "USD",
            100,
            0.1); // see https://www.facebook.com/help/156911951040843/

    private final CashierConfigurationContainer cashierConfiguration;
    private final LobbySessionCache lobbySessionCache;
    private final CookieHelper cookieHelper;
    private final SiteConfiguration siteConfiguration;
    private final BuyChipsPromotionService buyChipsPromotionService;
    private final PlayerService playerService;
    private final ReferenceService referenceService;
    private final CurrencyRepository currencyRepository;
    private Map<String, String> currencies;
    private final FacebookPaymentOptionHelper facebookPaymentOptionHelper;
    private final FacebookCurrencyService facebookCurrencyService;
    private final WorldPayCreditCardQueryService worldPayCreditCardQueryService;
    private final CreditCardService creditCardService;
    private final PurchaseOutcomeMapper purchaseOutcomeMapper;

    @Autowired
    public CashierController(
            @Qualifier("cashierConfiguration") final CashierConfigurationContainer cashierConfiguration,
            PlayerService playerService,
            @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
            @Qualifier("cookieHelper") final CookieHelper cookieHelper,
            @Qualifier("siteConfiguration") final SiteConfiguration siteConfiguration,
            @Qualifier("safeBuyChipsPromotionService") final BuyChipsPromotionService buyChipsPromotionService,
            ReferenceService referenceService,
            CurrencyRepository currencyRepository,
            FacebookPaymentOptionHelper facebookPaymentOptionHelper,
            FacebookCurrencyService facebookCurrencyService,
            final WorldPayCreditCardQueryService worldPayCreditCardQueryService,
            final CreditCardService creditCardService,
            final PurchaseOutcomeMapper purchaseOutcomeMapper) {
        notNull(cookieHelper, "cookieHelper may not be null");
        notNull(siteConfiguration, "siteConfiguration may not be null");
        notNull(buyChipsPromotionService, "dailyAwardPromotionService may not be null");
        notNull(playerService, "playerService may not be null");
        notNull(referenceService, "referenceService may not be null");
        notNull(currencyRepository, "currencyRepository may not be null");
        notNull(worldPayCreditCardQueryService, "worldPayCreditCardQueryService may not be null");
        notNull(creditCardService, "creditCardService may not be null");

        this.cashierConfiguration = cashierConfiguration;
        this.lobbySessionCache = lobbySessionCache;
        this.cookieHelper = cookieHelper;
        this.siteConfiguration = siteConfiguration;
        this.buyChipsPromotionService = buyChipsPromotionService;
        this.playerService = playerService;
        this.referenceService = referenceService;
        this.currencyRepository = currencyRepository;
        this.facebookPaymentOptionHelper = facebookPaymentOptionHelper;
        this.facebookCurrencyService = facebookCurrencyService;
        this.worldPayCreditCardQueryService = worldPayCreditCardQueryService;
        this.creditCardService = creditCardService;
        this.purchaseOutcomeMapper = purchaseOutcomeMapper;
    }

    private Map<String, String> createAcceptedCurrenciesMap() {
        final Map<String, String> acceptedCurrencies = new LinkedHashMap<String, String>();
        for (Currency acceptedCurrency : currencyRepository.getAcceptedCurrencies()) {
            acceptedCurrencies.put(acceptedCurrency.getCode(), acceptedCurrency.getDescription());
        }
        return acceptedCurrencies;
    }

    @RequestMapping({"/lobby/closeCashier", "/lobbyPartials/closeCashier"})
    public String closeCashier() {
        return "closeCashier";
    }

    @RequestMapping({"/lobby/cashier", "/lobbyPartials/cashier"})
    public ModelAndView view(final ModelMap modelMap,
                             final HttpServletRequest request,
                             final HttpServletResponse response,
                             @RequestParam(value = "updatedCurrency", required = false) final String updatedCurrency,
                             @RequestParam(value = "gameType", required = false) final String gameType,
                             @RequestParam(value = "paymentMethodType", required = false) final String paymentMethodType) {
        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (lobbySession == null) {
            throw new RuntimeException("No lobby session found");
        }
        final BigDecimal playerId = lobbySession.getPlayerId();

        final boolean isOnCanvas = cookieHelper.isOnCanvas(request, response);

        PaymentPreferences.PaymentMethod paymentMethod = getPaymentMethod(paymentMethodType, playerId, isOnCanvas);

        final Map<String, String> acceptedCurrencies = createAcceptedCurrenciesMap();
        final String preferredCurrency = getPreferredCurrency(request, updatedCurrency, gameType, isOnCanvas, acceptedCurrencies);

        updatePaymentGameType(request, response, gameType);

        final Map<Currency, List<PaymentOption>> paymentOptions = getPaymentOptions(modelMap, lobbySession, isOnCanvas, gameType);

        RegisteredCardQueryResult registeredCardQueryResult = worldPayCreditCardQueryService.retrieveCardsFor(playerId);

        modelMap.addAttribute(REGISTERED_CARDS_PARAMETER, registeredCardQueryResult);
        modelMap.addAttribute("paymentOptions", paymentOptions);
        modelMap.addAttribute("cashierConfiguration", cashierConfiguration);
        modelMap.addAttribute("acceptedCurrencies", acceptedCurrencies);
        modelMap.addAttribute("preferredCurrency", preferredCurrency);
        modelMap.addAttribute("preferedPaymentType", paymentMethod);
        modelMap.addAttribute("gameType", gameType);
        modelMap.addAttribute("controllerUrl", "cashier");

        return new ModelAndView("cashierFrame", modelMap);
    }

    private Map<Currency, List<PaymentOption>> getPaymentOptions(final ModelMap modelMap,
                                                                 final LobbySession lobbySession,
                                                                 final boolean onCanvas,
                                                                 final String gameType) {
        final Map<Currency, List<PaymentOption>> paymentOptionsPerCurrency;
        if (onCanvas) {
            paymentOptionsPerCurrency = buyChipsPromotionService.getBuyChipsPaymentOptionsFor(lobbySession.getPlayerId(), FACEBOOK_CANVAS);
            facebookPaymentOptionHelper.modifyPaymentOptionIdsIn(gameType, paymentOptionsPerCurrency);
            modelMap.addAttribute("requestId", UUID.randomUUID().toString());
        } else {
            paymentOptionsPerCurrency = buyChipsPromotionService.getBuyChipsPaymentOptionsFor(lobbySession.getPlayerId(), WEB);
        }

        for (List<PaymentOption> paymentOptions : paymentOptionsPerCurrency.values()) {
            Collections.sort(paymentOptions, new Comparator<PaymentOption>() {
                @Override
                public int compare(final PaymentOption p1, final PaymentOption p2) {
                    return p1.getNumChipsPerPurchase().compareTo(p2.getNumChipsPerPurchase());
                }
            });
        }
        return paymentOptionsPerCurrency;
    }

    private String getPreferredCurrency(final HttpServletRequest request,
                                        final String updatedCurrency,
                                        final String gameType,
                                        final boolean onCanvas,
                                        Map<String, String> acceptedCurrencies) {
        final String preferredCurrency;
        if (onCanvas) {
            preferredCurrency = facebookCurrencyService.getPreferredCurrencyFromFacebook(request, gameType).getCode();
        } else if (updatedCurrency != null) {
            preferredCurrency = Currency.valueOf(updatedCurrency).toString();
        } else {
            preferredCurrency = getDefaultCurrencyFromIP(request).getCode();
        }

        if (acceptedCurrencies.get(preferredCurrency) == null) {
            return "USD";
        } else {
            return preferredCurrency;
        }

    }

    private PaymentPreferences.PaymentMethod getPaymentMethod(final String paymentMethodType,
                                                              final BigDecimal playerId,
                                                              final boolean onCanvas) {
        if (onCanvas) {
            return PaymentPreferences.PaymentMethod.FACEBOOK;

        } else if (!isBlank(paymentMethodType)) {
            return PaymentPreferences.PaymentMethod.valueOf(paymentMethodType);
        } else {
            final PaymentPreferences paymentPreferences = playerService.getPaymentPreferences(playerId);
            if (paymentPreferences != null && paymentPreferences.getPaymentMethod() != null) {
                return paymentPreferences.getPaymentMethod();
            } else {
                return PaymentPreferences.PaymentMethod.CREDITCARD;
            }
        }
    }

    protected Currency getDefaultCurrencyFromIP(final HttpServletRequest request) {
        LOG.debug("Got request from IP {}", request.getRemoteAddr());
        final String countryCode = new GeolocationLookup().lookupCountryCodeByIp(request.getRemoteAddr());
        LOG.debug("Got request from country {}", countryCode);
        return referenceService.getPreferredCurrency(countryCode);
    }

    private void updatePaymentGameType(final HttpServletRequest request,
                                       final HttpServletResponse response,
                                       final String gameType) {
        if (gameType == null) {
            cookieHelper.setPaymentGameType(response, cookieHelper.getLastGameType(
                    request.getCookies(), siteConfiguration.getDefaultGameType()));
        } else {
            cookieHelper.setPaymentGameType(response, gameType);
        }
    }

    @RequestMapping({"/lobby/cashierProcess", "/lobbyPartials/cashierProcess"})
    public ModelAndView process(
            @RequestParam(value = "paymentMethod", required = false) final String paymentMethodStr,
            @RequestParam(value = "paymentOption", required = false) final String paymentOption,
            @RequestParam(value = "promotionId", required = false) final Long promotionId,
            @RequestParam(value = "promotionChips", required = false) final String promotionChipsAsString,
            final HttpServletRequest request,
            final HttpServletResponse response)
            throws IOException {
        final BigDecimal promotionChips = parsePromotionChips(promotionChipsAsString);

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        if (!validateProcessRequest(lobbySession.getPlayerId(),
                paymentMethodStr,
                paymentOption,
                promotionId,
                promotionChips,
                request,
                response)) {
            return null;
        }

        CashierConfiguration config = cashierConfiguration.getCashierConfiguration(paymentMethodStr.toLowerCase());

        final String redirectUrl = generateRedirectUrl(paymentOption, promotionId, promotionChips, config);

        if (LOG.isDebugEnabled()) {
            LOG.debug("redirectUrl=" + redirectUrl);
            LOG.debug("paymentMethod=" + paymentMethodStr);
        }

        if (config.getCashierId().equalsIgnoreCase("paypal")) {
            final ModelAndView mav = new ModelAndView("paypal");
            mav.addObject("redirectUrl", redirectUrl);
            return mav;

        } else {
            return new ModelAndView(new RedirectView(redirectUrl, false, true, false));
        }
    }

    @RequestMapping(value = "/lobbyPartials/complete", method = POST)
    public ModelAndView completePayment(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final ModelMap model,
                                        @RequestParam(value = "paymentOption", required = false) final String paymentOptionId,
                                        @RequestParam(value = "promotionId", required = false) final Long promotionId,
                                        @RequestParam(value = "registeredCardSelection", required = true) final String cardId) {
        LOG.debug("Completing credit card payment. (query={})", request.getQueryString());
        notNull(cardId, "Card ID cannot be null");
        notNull(paymentOptionId, "Payment option cannot be null");

        final LobbySession session = lobbySessionCache.getActiveSession(request);
        final BigDecimal playerId = session.getPlayerId();

        RegisteredCreditCardDetails cardDetails = readCardDetails(cardId, playerId);
        if (cardDetails == null) {
            LOG.error("Unable to locate card for card ID {} for player {}", cardId, playerId);
            model.addAttribute("ResultMessage", purchaseOutcomeMapper.getErrorMessage(INVALID_ACCOUNT));
            return new ModelAndView(VIEW_PATH + "error", model);
        }

        model.addAttribute(IN_ERROR, false);

        final PurchaseResult details = creditCardService.completePurchase(
                PaymentContextBuilder.builder()
                        .withPlayerId(session.getPlayerId())
                        .withSessionId(session.getSessionId())
                        .withPlayerName(session.getPlayerName())
                        .withGameType(cookieHelper.getPaymentGameType(request.getCookies()))
                        .withEmailAddress(session.getEmail())
                        .withPaymentOptionId(paymentOptionId)
                        .withPartnerId(session.getPartnerId())
                        .withPromotionId(promotionId).build(),
                CreditCardDetailsBuilder.builder().withCreditCardNumber(cardDetails.getObscuredNumber())
                        .withCvc2(cardDetails.getCvc2())
                        .withExpirationMonth(cardDetails.getExpiryMonth())
                        .withExpirationYear(cardDetails.getExpiryYear())
                        .withCardHolderName(cardDetails.getAccountName())
                        .withCardId(cardId)
                        .withObscuredCardNumber(cardDetails.getObscuredNumber()).build(),
                IpAddressResolver.resolveFor(request));

        switch (details.getOutcome()) {
            case APPROVED:
                LOG.debug("Transaction {} successful for player: {}", details.getInternalTransactionId(), playerId);
                model.addAttribute("transactionId", details.getInternalTransactionId());
                model.addAttribute("transactionValue", details.getChips());
                return new ModelAndView(VIEW_PATH + "success", model);

            case PLAYER_BLOCKED:
                LOG.debug("Transaction {} resulted in player being blocked: {}", details.getInternalTransactionId(), playerId);
                return new ModelAndView(new RedirectView("/blocked?reason=payment", false, true, false));

            default:
                LOG.debug("Transaction {} failed for player {}. Outcome was {}. Trace is: {}) ",
                        details.getInternalTransactionId(), playerId, details.getOutcome(), details.getTrace());
                model.addAttribute("ResultMessage", purchaseOutcomeMapper.getErrorMessage(details.getOutcome()));
                return new ModelAndView(VIEW_PATH + "error", model);
        }
    }

    private BigDecimal parsePromotionChips(final String promotionChipsAsString) {
        BigDecimal promotionChips = null;
        try {
            if (StringUtils.isNotBlank(promotionChipsAsString)) {
                promotionChips = new BigDecimal(promotionChipsAsString);
            }
        } catch (NumberFormatException e) {
            LOG.warn("Invalid promotion chips received: {}", promotionChipsAsString);
        }
        return promotionChips;
    }

    boolean validateProcessRequest(final BigDecimal playerId,
                                   final String paymentMethodStr,
                                   final String paymentOption,
                                   final Long promotionId,
                                   final BigDecimal promotionChips,
                                   final HttpServletRequest request,
                                   final HttpServletResponse response) {
        if (!hasParameter("paymentMethod", paymentMethodStr, request, response)
                || !hasParameter("paymentOption", paymentOption, request, response)) {
            return false;
        }

        if (promotionId == null && promotionChips != null) {
            LOG.warn(String.format("Both promotionId=%s and promotionChips=%s are required",
                    promotionId, promotionChips));
            return false;
        }

        if (!cashierConfiguration.cashierExists(paymentMethodStr.toLowerCase())) {
            LOG.warn("Payment method " + paymentMethodStr + " is not supported");
            return false;
        }

        final PaymentPreferences.PaymentMethod paymentMethod = PaymentPreferences.PaymentMethod.valueOf(
                paymentMethodStr.toUpperCase());
        if (promotionId != null
                && !isPromotionValid(playerId, promotionId, paymentMethod, paymentOption, promotionChips)) {
            LOG.warn(
                    String.format("Promotion not valid, promoId=%s, paymentOption=%s, promotionChips=%s",
                            promotionId, paymentOption, promotionChips));
            return false;
        }

        return true;
    }

    private String generateRedirectUrl(final String paymentOption,
                                       final Long promoId,
                                       final BigDecimal promoChips,
                                       final CashierConfiguration config) throws UnsupportedEncodingException {
        if (promoId != null) {
            return String.format("%s?paymentOption=%s&promoId=%s&promoChips=%s",
                    config.getCashierUrl(),
                    URLEncoder.encode(paymentOption, "UTF-8"),
                    promoId.toString(),
                    promoChips.toPlainString());
        } else {
            return String.format("%s?paymentOption=%s",
                    config.getCashierUrl(), URLEncoder.encode(paymentOption, "UTF-8"));
        }
    }

    private boolean isPromotionValid(final BigDecimal playerId,
                                     final Long promoId,
                                     final PaymentPreferences.PaymentMethod paymentMethod,
                                     final String optionId,
                                     final BigDecimal requestedPromotionChips) {
        final PaymentOption paymentOption = buyChipsPromotionService.getPaymentOptionFor(
                playerId, promoId, paymentMethod, optionId);
        if (paymentOption != null) {
            final BigDecimal promotionChipsPerPurchase
                    = paymentOption.getPromotion(paymentMethod).getPromotionChipsPerPurchase();
            if (promotionChipsPerPurchase != null) {
                return promotionChipsPerPurchase.equals(requestedPromotionChips);
            }
        }
        return false;
    }

    private RegisteredCreditCardDetails readCardDetails(final String cardId, final BigDecimal playerId) {
        RegisteredCardQueryResult registeredCardQueryResult = worldPayCreditCardQueryService.retrieveCardsFor(playerId);
        RegisteredCreditCardDetails cardDetails = null;
        for (RegisteredCreditCardDetails registeredCreditCardDetails : registeredCardQueryResult.getCreditCardDetailList()) {
            if (registeredCreditCardDetails.getCardId().equals(cardId)) {
                cardDetails = registeredCreditCardDetails;
                break;
            }
        }
        return cardDetails;
    }

    private String mapCurrency(final String userCurrency) {
        if (currencies == null) {
            currencies = newHashMap();
            currencies.put("USD", "$");
            currencies.put("CAD", "C$");
            currencies.put("AUD", "A$");
            currencies.put("EUR", "€");
            currencies.put("GBP", "£");
        }

        final String symbol = currencies.get(userCurrency);
        if (symbol != null) {
            return symbol;
        }
        return userCurrency;
    }

    BigDecimal calculateLocalCurrency(final FacebookUserCurrency fbUserCurrency, BigDecimal amountRealMoneyPerPurchase) {
        int rnd;
        if (fbUserCurrency.getCurrencyOffset().intValue() == OFFSET_100) {
            rnd = 2;
        } else if (fbUserCurrency.getCurrencyOffset().intValue() == OFFSET_10) {
            rnd = 1;
        } else {
            rnd = 0;
        }

        return fbUserCurrency.getCurrencyExchangeInverse()
                .multiply(amountRealMoneyPerPurchase.divide(TEN)) // credits
                .multiply(fbUserCurrency.getCurrencyOffset()).setScale(rnd, RoundingMode.HALF_EVEN);
    }

    private void filterAcceptableCurrencies(final Map<Currency, List<PaymentOption>> prefilteredPaymentOptionsMap,
                                            final Map<Currency, List<PaymentOption>> paymentOptionsMap) {
        for (Currency currency : prefilteredPaymentOptionsMap.keySet()) {
            if (currencyRepository.getAcceptedCurrencies().contains(currency)) {
                final List<PaymentOption> paymentOptions = prefilteredPaymentOptionsMap.get(currency);
                paymentOptionsMap.put(currency, paymentOptions);
            }
        }
    }
}
