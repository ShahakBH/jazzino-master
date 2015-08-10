package com.yazino.web.payment.creditcard;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.platform.Platform;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.email.AsyncEmailService;
import com.yazino.web.domain.email.BoughtChipsEmailBuilder;
import com.yazino.web.payment.CustomerDataBuilder;
import com.yazino.web.payment.PaymentContext;
import com.yazino.web.service.PurchaseTracking;
import com.yazino.web.service.QuietPlayerEmailer;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.Currency;
import java.util.Date;
import java.util.Map;

import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.CREDITCARD;
import static com.yazino.web.domain.PaymentEmailBodyTemplate.CreditCard;
import static com.yazino.web.payment.CustomerData.obscureMiddleCardNumbers;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service("creditCardService")
public class CreditCardService {
    private static final Logger LOG = LoggerFactory.getLogger(CreditCardService.class);

    private static final String TX_COUNTRY = "GB";
    private static final String TRANSACTION_EMAIL_SUBJECT_FAIL = "Your chip purchase at Yazino";
    private static final String TRANSACTION_EMAIL_SUBJECT_FAIL_INTERNAL = "Failed chip purchase at Yazino";
    private static final String WIRECARD_EMAIL_ADDRESS = "wirecard@yazino.com";

    private final BuyChipsPromotionService buyChipsPromotionService;
    private final PlayerService playerService;
    private final CreditCardPaymentService creditCardPaymentService;
    private final AsyncEmailService emailService;
    private final CommunityService communityService;
    private final PurchaseTracking purchaseTracking;
    private final String sender;
    private final QuietPlayerEmailer emailer;

    @Autowired
    public CreditCardService(@Qualifier("safeBuyChipsPromotionService")
                                 final BuyChipsPromotionService buyChipsPromotionService,
                             final PlayerService playerService,
                             final CreditCardPaymentService creditCardPaymentService,
                             final AsyncEmailService emailService,
                             final CommunityService communityService,
                             final PurchaseTracking purchaseTracking,
                             final QuietPlayerEmailer emailer,
                             @Value("${strata.email.from-address}") final String sender) {
        this.buyChipsPromotionService = buyChipsPromotionService;
        this.playerService = playerService;
        this.creditCardPaymentService = creditCardPaymentService;
        this.emailer = emailer;
        this.emailService = emailService;
        this.communityService = communityService;
        this.purchaseTracking = purchaseTracking;
        this.sender = sender;
    }

    public PaymentOption resolvePaymentOption(final BigDecimal playerId,
                                              final String paymentOptionId,
                                              final Long promoId) {
        PaymentOption paymentOption = null;
        if (promoId != null) {
            paymentOption = buyChipsPromotionService.getPaymentOptionFor(playerId,
                    promoId,
                    CREDITCARD,
                    paymentOptionId);
            if (paymentOption == null) {
                LOG.error("Failed to load buy chips payment option for player={}, promoId={}, paymentOptionId={}, paymentMethod={}",
                        playerId, promoId, paymentOptionId, CREDITCARD.name());
            }
        }
        if (paymentOption == null) {
            paymentOption = buyChipsPromotionService.getDefaultPaymentOptionFor(paymentOptionId, Platform.WEB);
            LOG.debug("Using default PaymentOption for paymentOptionId={}", paymentOptionId);
        }
        return paymentOption;
    }

    public PurchaseResult completePurchase(final PaymentContext context,
                                           final CreditCardDetails creditCardDetails,
                                           final InetAddress ipAddress) {
        LOG.debug("Attempting to complete purchase");
        final PaymentOption paymentOption = resolvePaymentOption(context.getPlayerId(),
                context.getPaymentOptionId(),
                context.getPromotionId());
        if (paymentOption == null) {
            return new PurchaseResult(PurchaseOutcome.INVALID_PAYMENT_OPTION,
                    context.getPaymentOptionId() + " is invalid", context.getEmailAddress());
        }

        final BigDecimal accountId = playerService.getAccountId(context.getPlayerId());
        CustomerDataBuilder customerDataBuilder = new CustomerDataBuilder()
                .withAmount(paymentOption.getAmountRealMoneyPerPurchase())
                .withCurrency(Currency.getInstance(paymentOption.getRealMoneyCurrency()))
                .withTransactionCountry(TX_COUNTRY)
                .withCustomerIPAddress(ipAddress)
                .withEmailAddress(context.getEmailAddress())
                .withGameType(context.getGameType())
                .withCvc2(creditCardDetails.getCvc2())
                .withExpirationMonth(creditCardDetails.getExpirationMonth())
                .withExpirationYear(creditCardDetails.getExpirationYear())
                .withCardHolderName(creditCardDetails.getCardHolderName());

        if (isNotBlank(creditCardDetails.getCardId())) {
            LOG.debug("Payment being made with a registered card");
            customerDataBuilder = customerDataBuilder
                    .withCardId(creditCardDetails.getCardId())
                    .withObscureMiddleCardNumbers(creditCardDetails.getObscuredCardNumber());
        } else {
            customerDataBuilder = customerDataBuilder
                    .withObscureMiddleCardNumbers(obscureMiddleCardNumbers(creditCardDetails.getCreditCardNumber()))
                    .withCreditCardNumber(creditCardDetails.getCreditCardNumber());
        }
        final DateTime paymentTime = new DateTime();
        final PurchaseRequest purchaseRequest = new PurchaseRequest(customerDataBuilder.build(),
                accountId,
                paymentOption,
                paymentTime,
                context.getPlayerId(),
                context.getSessionId(),
                context.getPromotionId());

        LOG.debug("Purchasing with purchase request: {}", purchaseRequest);
        final PurchaseResult result = creditCardPaymentService.purchase(purchaseRequest);
        sendTransactionDetails(result, context);
        if (result.getOutcome() == PurchaseOutcome.APPROVED) {
            communityService.asyncPublishBalance(context.getPlayerId());
            logPromotionPlayerReward(context.getPlayerId(), paymentOption, paymentTime);
            updatePreferredPaymentMethod(context.getPlayerId(), paymentOption.getRealMoneyCurrency());
            purchaseTracking.trackSuccessfulPurchase(context.getPlayerId());
        }
        return result;
    }

    private void updatePreferredPaymentMethod(final BigDecimal playerId,
                                              final String realMoneyCurrency) {
        PaymentPreferences paymentPreferences = playerService.getPaymentPreferences(playerId);

        if (paymentPreferences != null) {
            paymentPreferences = paymentPreferences.withPaymentMethod(CREDITCARD);
        } else {
            paymentPreferences = new PaymentPreferences(CREDITCARD);
        }
        paymentPreferences = paymentPreferences.withCurrency(getCurrency(realMoneyCurrency));

        playerService.updatePaymentPreferences(playerId, paymentPreferences);
    }

    private com.yazino.platform.reference.Currency getCurrency(final String realMoneyCurrency) {
        for (com.yazino.platform.reference.Currency currency : com.yazino.platform.reference.Currency.values()) {
            if (currency.getCode().equals(realMoneyCurrency)) {
                return currency;
            }
        }
        return com.yazino.platform.reference.Currency.USD;
    }


    private void logPromotionPlayerReward(final BigDecimal playerId,
                                          final PaymentOption paymentOption,
                                          final DateTime dateTime) {
        if (paymentOption.hasPromotion(CREDITCARD)) {
            final PromotionPaymentOption promotion = paymentOption.getPromotion(CREDITCARD);
            buyChipsPromotionService.logPlayerReward(playerId, promotion.getPromoId(),
                    CREDITCARD, paymentOption.getId(), dateTime);
        }
    }

    private void sendTransactionDetails(final PurchaseResult purchaseResult,
                                        final PaymentContext context) {
        notNull(purchaseResult, "purchaseResult is null");
        LOG.debug("Sending transaction details for purchase result: {}", purchaseResult);
        final boolean successful = purchaseResult.getOutcome() == PurchaseOutcome.APPROVED;

        final String displayName = context.getPlayerName();
        try {
            if (successful) {
                final BoughtChipsEmailBuilder builder = new BoughtChipsEmailBuilder();
                builder.withEmailAddress(purchaseResult.getCustomerEmail());
                builder.withFirstName(displayName);
                builder.withPurchasedChips(purchaseResult.getChips());
                builder.withCurrency(java.util.Currency.getInstance(purchaseResult.getCurrency().getCurrencyCode()));
                builder.withCost(purchaseResult.getMoney());
                builder.withPaymentDate(new Date());
                builder.withCardNumber(purchaseResult.getCardNumberObscured());
                builder.withPaymentId(purchaseResult.getExternalTransactionId());
                builder.withPaymentEmailBodyTemplate(CreditCard);

                emailer.quietlySendEmail(builder);

            } else {
                final Map<String, Object> templateProperties = purchaseResult.buildArgumentMap();
                templateProperties.put("displayName", displayName);

                final String template = String.format("%s-transaction-%s",
                        purchaseResult.getMerchant().toLowerCase(), "fail");
                emailService.send(purchaseResult.getCustomerEmail(),
                        sender,
                        TRANSACTION_EMAIL_SUBJECT_FAIL,
                        "lobby/" + template,
                        templateProperties);
                emailService.send(WIRECARD_EMAIL_ADDRESS,
                        sender,
                        TRANSACTION_EMAIL_SUBJECT_FAIL_INTERNAL,
                        "lobby/" + template + "-internal",
                        templateProperties);
            }
        } catch (final Exception e) {
            LOG.error(String.format("Could not send transaction response to %s with message %s",
                    purchaseResult.getCustomerEmail(), purchaseResult.getMessage()), e);
        }
    }

}
