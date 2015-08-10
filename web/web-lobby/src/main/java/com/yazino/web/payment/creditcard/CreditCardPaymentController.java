package com.yazino.web.payment.creditcard;

import com.google.common.base.Optional;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.util.DateUtils;
import com.yazino.web.domain.payment.CardRegistrationResult;
import com.yazino.web.domain.payment.CardRegistrationTokenResult;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.payment.creditcard.worldpay.WorldPayCreditCardRegistrationService;
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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static com.yazino.payment.worldpay.MessageCode.forCode;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.CREDITCARD;
import static java.lang.String.format;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
@RequestMapping("/payment/creditcard/")
public class CreditCardPaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(CreditCardPaymentController.class);
    private static final String VIEW_PATH = "payment/creditcard/";
    private static final String PAYMENT_OPTION_KEY = "paymentOption";
    private static final String CREDIT_CARD_FORM_KEY = "creditCardForm";
    private static final String PROMOTION_SHOWN_KEY = "promotionShown";
    private static final String IN_ERROR = "inError";
    private static final String ERROR_MESSAGES = "errorMessages";
    private static final String CREDIT_CARD_IS_TEST_REGISTRATION_KEY = "isTestCardRegistration";
    private static final String CREDIT_CARD_REGISTRATION_OTT_KEY = "cardRegistrationOTT";
    private static final String CREDIT_CARD_REGISTRATION_URL_KEY = "cardRegistrationURL";
    private static final String WEB_HOST = "strata.web.host";
    private static final String COMPLETE_WITH_CARD_REGISTERED = "completeWithCardRegistered";

    private final Map<String, String> months = createExpiryMonths();
    private final Map<String, String> years = createExpiryYears();

    private final LobbySessionCache lobbySessionCache;
    private final PurchaseOutcomeMapper purchaseOutcomeMapper;
    private final CookieHelper cookieHelper;

    private final CreditCardService creditCardService;
    private final WorldPayCreditCardRegistrationService worldPayCreditCardRegistration;
    private YazinoConfiguration yazinoConfiguration;

    @Autowired(required = true)
    public CreditCardPaymentController(
            @Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache,
            @Qualifier("purchaseOutcomeMapper") final PurchaseOutcomeMapper purchaseOutcomeMapper,
            @Qualifier("cookieHelper") final CookieHelper cookieHelper,
            final CreditCardService creditCardService,
            final WorldPayCreditCardRegistrationService worldPayCardRegistration,
            final YazinoConfiguration yazinoConfiguration) {
        notNull(creditCardService, "creditCardService is null");
        notNull(worldPayCardRegistration, "creditCardService is null");
        notNull(yazinoConfiguration, "creditCardService is null");

        this.lobbySessionCache = lobbySessionCache;
        this.purchaseOutcomeMapper = purchaseOutcomeMapper;
        this.cookieHelper = cookieHelper;
        this.creditCardService = creditCardService;
        this.worldPayCreditCardRegistration = worldPayCardRegistration;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @ModelAttribute("months")
    public Map<String, String> populateMonths() {
        return months;
    }

    @ModelAttribute("years")
    public Map<String, String> populateYears() {
        return years;
    }

    @RequestMapping(value = "/start", method = RequestMethod.GET)
    public ModelAndView viewForm(final HttpServletRequest request,
                                 final HttpServletResponse response,
                                 @RequestParam(value = "paymentOption") final String paymentOptionId,
                                 @RequestParam(required = false) final Long promoId,
                                 final ModelMap model) {
        LOG.debug("Starting creditcard payment. (query={})", request.getQueryString());

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);

        return createStartView(lobbySession.getPlayerId(),
                paymentOptionId,
                promoId,
                model,
                defaultForm(lobbySession),
                false,
                null,
                response
        );
    }

    /**
     * This URL is called by World Pay once a card has been registered
     */
    @RequestMapping(value = "/" + COMPLETE_WITH_CARD_REGISTERED, method = RequestMethod.GET)
    public ModelAndView cardRegisteredPostBack(final HttpServletRequest request,
                                               final HttpServletResponse response,
                                               @RequestParam(value = "paymentOption") final String paymentOptionId,
                                               @RequestParam(value = "OTT", required = true) final String worldPayOTT,
                                               @RequestParam(required = true) final String emailAddress,
                                               @RequestParam(value = "promotionId", required = false) final Long promoId,
                                               final ModelMap model) {
        LOG.debug("Credit card registered. Returning to payments screen. (query={})", request.getQueryString());

        final LobbySession lobbySession = lobbySessionCache.getActiveSession(request);
        final PaymentOption paymentOption = paymentOptionFor(lobbySession.getPlayerId(), paymentOptionId, promoId);

        try {
            CardRegistrationResult registeredCard = worldPayCreditCardRegistration.retrieveCardRegistrationResult(worldPayOTT);
            if (forCode(registeredCard.getMessageCode()).isSuccessful()) {
                return completePayment(CreditCardFormBuilder.valueOf()
                        .withCardId(registeredCard.getCardId())
                        .withEmailAddress(emailAddress)
                        .withPromotionId(promoId)
                        .withPaymentOptionId(paymentOption.getId())
                        .withObscuredCardNumber(registeredCard.getObscuredCardNumber())
                        .withExpirationYear(registeredCard.getExpiryYear())
                        .withExpirationMonth(registeredCard.getExpiryMonth())
                        .withCardHolderName(registeredCard.getCustomerName())
                        .withCvc2("***")
                        .build(), request, response, model);
            } else {
                List<String> errors = new ArrayList<>();
                errors.add(registeredCard.getMessage());
                return createStartView(lobbySession.getPlayerId(),
                        paymentOptionId,
                        promoId,
                        model,
                        defaultForm(lobbySession),
                        true,
                        errors,
                        response
                );
            }
        } catch (IllegalArgumentException e) {
            LOG.error("Failed to create credit card view due to invalid request data", e);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
    }

    @RequestMapping(value = "/complete", method = RequestMethod.POST)
    public ModelAndView completePayment(@ModelAttribute(CREDIT_CARD_FORM_KEY) final CreditCardForm form,
                                        final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final ModelMap model) {
        LOG.debug("Completing creditcard payment. (query={})", request.getQueryString());

        final LobbySession session = lobbySessionCache.getActiveSession(request);
        final BigDecimal playerId = session.getPlayerId();

        final List<String> errors = new ArrayList<>();
        if (!form.isValidForm(errors)) {
            return createStartView(session.getPlayerId(),
                    form.getPaymentOptionId(),
                    form.getPromotionId(),
                    model,
                    form,
                    true,
                    errors,
                    response
            );
        }
        model.addAttribute(IN_ERROR, false);

        final PaymentContext paymentContext = form.toPaymentContext(session, cookieHelper.getPaymentGameType(request.getCookies()));

        final PurchaseResult details = creditCardService.completePurchase(
                paymentContext,
                form.toCreditCardDetails(),
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

    private ModelAndView createStartView(final BigDecimal playerId,
                                         final String paymentOptionId,
                                         final Long promoId,
                                         final ModelMap model,
                                         final CreditCardForm form,
                                         final boolean hasErrors,
                                         final List<String> errorMessages,
                                         final HttpServletResponse response) {
        try {
            final PaymentOption paymentOption = paymentOptionFor(playerId, paymentOptionId, promoId);

            model.addAttribute(CREDIT_CARD_FORM_KEY, form);
            model.addAttribute(PAYMENT_OPTION_KEY, paymentOption);
            model.addAttribute(PROMOTION_SHOWN_KEY, paymentOption.hasPromotion(CREDITCARD));
            model.addAttribute(IN_ERROR, hasErrors);
            model.addAttribute(ERROR_MESSAGES, errorMessages);
            addWorldPayRegistration(playerId, model, paymentOption, promoId, form.getEmailAddress());

            return new ModelAndView(VIEW_PATH + "start", model);

        } catch (IllegalArgumentException e) {
            LOG.error("Failed to create start view due to invalid request data", e);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }
    }

    private void addWorldPayRegistration(final BigDecimal playerId, final ModelMap model, final PaymentOption paymentOption, final Long promoId, final String emailAddress) {
        final String host = yazinoConfiguration.getString(WEB_HOST);
        String forwardURL = format("https://%s/payment/creditcard/%s?paymentOption=%s", host, COMPLETE_WITH_CARD_REGISTERED, paymentOption.getId());
        if (promoId != null) {
            forwardURL += format("&promotionId=%s", promoId);
        }
        if (paymentOption.getNumChipsPerPurchase(PaymentPreferences.PaymentMethod.CREDITCARD.name()) != null) {
            forwardURL += format("&promotionChips=%s", paymentOption.getNumChipsPerPurchase(PaymentPreferences.PaymentMethod.CREDITCARD.name()));
        }
        if (StringUtils.isNotBlank(emailAddress)) {
            forwardURL += format("&emailAddress=%s", emailAddress);
        }
        Optional<CardRegistrationTokenResult> cardRegistration = worldPayCreditCardRegistration.prepareCardRegistration(playerId, forwardURL);
        if (cardRegistration.isPresent()) {
            final CardRegistrationTokenResult cardRegistrationTokenResult = cardRegistration.get();
            model.addAttribute(CREDIT_CARD_IS_TEST_REGISTRATION_KEY, cardRegistrationTokenResult.isTest());
            model.addAttribute(CREDIT_CARD_REGISTRATION_URL_KEY, cardRegistrationTokenResult.getRegistrationURL());
            model.addAttribute(CREDIT_CARD_REGISTRATION_OTT_KEY, cardRegistrationTokenResult.getToken());
        }
    }

    private CreditCardForm defaultForm(final LobbySession lobbySession) {
        final Calendar cal = Calendar.getInstance();
        return CreditCardFormBuilder.valueOf()
                .withCardHolderName(lobbySession.getPlayerName())
                .withEmailAddress(lobbySession.getEmail())
                .withExpirationMonth(String.format("%02d", cal.get(Calendar.MONTH) + 1))
                .withExpirationYear(Integer.toString(cal.get(Calendar.YEAR)))
                .build();
    }

    private void sendError(final HttpServletResponse response,
                           final int errorCode) {
        try {
            response.sendError(errorCode);
        } catch (IOException e) {
            response.setStatus(errorCode);
        }
    }

    private PaymentOption paymentOptionFor(final BigDecimal playerId, final String paymentOptionId, final Long promoId) {
        final PaymentOption paymentOption = creditCardService.resolvePaymentOption(playerId, paymentOptionId, promoId);
        if (paymentOption == null) {
            throw new IllegalArgumentException("Unable to resolve payment option " + paymentOptionId);
        }
        return paymentOption;
    }

    private static Map<String, String> createExpiryMonths() {
        final Map<String, String> map = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            final String formattedMonth = String.format("%02d", month);
            map.put(formattedMonth, formattedMonth);
        }
        return map;
    }

    private static Map<String, String> createExpiryYears() {
        final DateUtils dateUtils = new DateUtils();
        final Map<String, String> map = new LinkedHashMap<>();
        final GregorianCalendar now = new GregorianCalendar();
        final int[] years = dateUtils.getYearsUntil(now.get(Calendar.YEAR) + 10);
        for (int year : years) {
            final String value = String.valueOf(year);
            map.put(value, value);
        }
        return map;
    }
}
