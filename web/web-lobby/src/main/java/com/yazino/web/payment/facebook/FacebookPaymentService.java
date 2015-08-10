package com.yazino.web.payment.facebook;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.platform.account.*;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.payment.PaymentDispute;
import com.yazino.platform.payment.PaymentService;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.email.BoughtChipsEmailBuilder;
import com.yazino.web.payment.CustomerData;
import com.yazino.web.payment.CustomerDataBuilder;
import com.yazino.web.payment.creditcard.PurchaseOutcome;
import com.yazino.web.payment.creditcard.PurchaseRequest;
import com.yazino.web.payment.creditcard.PurchaseResult;
import com.yazino.web.service.PurchaseTracking;
import com.yazino.web.service.QuietPlayerEmailer;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;

import java.math.BigDecimal;
import java.util.Currency;

import static com.yazino.platform.Platform.FACEBOOK_CANVAS;
import static com.yazino.platform.account.ExternalTransactionStatus.CANCELLED;
import static com.yazino.platform.account.ExternalTransactionStatus.FAILURE;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.FACEBOOK;
import static com.yazino.web.domain.PaymentEmailBodyTemplate.Facebook;
import static org.apache.commons.lang3.Validate.notNull;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CANVAS;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.STRICT;

@Service("facebookPaymentService")
public class FacebookPaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(FacebookPaymentService.class);

    private static final String TX_COUNTRY = "GB";
    private static final String EMPTY_STRING = "";
    public static final String FACEBOOK_URL = "https://apps.facebook.com/%s";
    private static final String EARN_CHIPS_CASHIER = "FacebookEarnChips";
    public static final String USER_CANCELLED_ERROR_CODE = "1383010";

    private final FacebookConfiguration facebookConfiguration;
    private final BuyChipsPromotionService buyChipsPromotionService;
    private final PlayerService playerService;
    private final WalletService walletService;
    private final QuietPlayerEmailer emailer;
    private final CommunityService communityService;
    private final PurchaseTracking purchaseTracking;
    private final PlayerProfileService profileService;
    private final PaymentService paymentService;

    @Autowired
    public FacebookPaymentService(final FacebookConfiguration facebookConfiguration,
                                  final BuyChipsPromotionService buyChipsPromotionService,
                                  final PlayerService playerService,
                                  final WalletService walletService,
                                  final QuietPlayerEmailer emailer,
                                  final CommunityService communityService,
                                  final PurchaseTracking purchaseTracking,
                                  final PlayerProfileService profileService,
                                  final PaymentService paymentService) {
        notNull(facebookConfiguration, "facebookConfiguration may not be null");
        notNull(buyChipsPromotionService, "buyChipsPromotionService may not be null");
        notNull(playerService, "playerService may not be null");
        notNull(walletService, "walletService may not be null");
        notNull(emailer, "emailer may not be null");
        notNull(communityService, "communityService may not be null");
        notNull(purchaseTracking, "purchaseTracking may not be null");
        notNull(profileService, "profileService may not be null");
        notNull(paymentService, "paymentService may not be null");

        this.facebookConfiguration = facebookConfiguration;
        this.buyChipsPromotionService = buyChipsPromotionService;
        this.playerService = playerService;
        this.walletService = walletService;
        this.emailer = emailer;
        this.communityService = communityService;
        this.purchaseTracking = purchaseTracking;
        this.profileService = profileService;
        this.paymentService = paymentService;
    }

    public PaymentOption resolvePaymentOption(final BigDecimal playerId,
                                              final String paymentOptionId,
                                              final Long promoId) {
        PaymentOption paymentOption = null;
        if (promoId != null) {
            paymentOption = buyChipsPromotionService.getPaymentOptionFor(playerId,
                    promoId,
                    FACEBOOK,
                    paymentOptionId);
            if (paymentOption == null) {
                LOG.error(String.format("Failed to load buy chips payment option for player=%s, promoId=%s, "
                                + "paymentOptionId=%s, paymentMethod=%s",
                        playerId, promoId, paymentOptionId, FACEBOOK.name()));
            }
        }
        if (paymentOption == null) {
            paymentOption = buyChipsPromotionService.getDefaultFacebookPaymentOptionFor(paymentOptionId);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Using default PaymentOption for paymentOptionId=%s", paymentOptionId));
            }
        }
        return paymentOption;
    }

    public ExternalTransaction logTransactionAttempt(BigDecimal playerId,
                                                     String internalRef,
                                                     PaymentOption paymentOption,
                                                     String gameType,
                                                     Long promoId) {
        final BigDecimal primaryAccountId = playerService.getAccountId(playerId);
        final ExternalTransaction transaction = buildExternalTransaction(
                ExternalTransactionStatus.REQUEST, null, internalRef, paymentOption, gameType, primaryAccountId, playerId, promoId, "");
        record(transaction);
        return transaction;
    }

    private void record(final ExternalTransaction transaction) {
        try {
            walletService.record(transaction);
        } catch (WalletServiceException e) {
            LOG.error("Unable to record transaction: {}", transaction, e);
        }
    }

    private ExternalTransaction buildExternalTransaction(ExternalTransactionStatus status,
                                                         String externalRef,
                                                         String internalRef,
                                                         PaymentOption paymentOption,
                                                         String gameType,
                                                         BigDecimal primaryAccountId,
                                                         BigDecimal playerId,
                                                         Long promoId,
                                                         String message) {
        return ExternalTransaction.newExternalTransaction(primaryAccountId)
                .withInternalTransactionId(internalRef)
                .withExternalTransactionId(externalRef)
                .withMessage(message, new DateTime())
                .withAmount(Currency.getInstance(paymentOption.getRealMoneyCurrency()), paymentOption.getAmountRealMoneyPerPurchase())
                .withPaymentOption(paymentOption.getNumChipsPerPurchase(FACEBOOK.name()), paymentOption.getId())
                .withCreditCardNumber("")
                .withCashierName(FACEBOOK.name())
                .withStatus(status)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(gameType)
                .withPlayerId(playerId)
                .withPromotionId(promoId)
                .withPlatform(FACEBOOK_CANVAS)
                .withForeignExchange(currencyFor(paymentOption.getBaseCurrencyCode()),
                        paymentOption.getBaseCurrencyPrice(),
                        paymentOption.getExchangeRate())
                .build();
    }

    private Currency currencyFor(final String currencyCode) {
        if (currencyCode != null) {
            return Currency.getInstance(currencyCode);
        }
        return null;
    }

    public void earnChips(BigDecimal playerId,
                          BigDecimal amountOfCredits,
                          BigDecimal amountOfChips,
                          final String transactionRef,
                          String gameType,
                          final Long promoId) {
        final BigDecimal accountId = playerService.getAccountId(playerId);
        try {
            processExternalTransaction(accountId, transactionRef, amountOfCredits, amountOfChips, gameType, playerId, promoId);
            communityService.asyncPublishBalance(playerId);
            LOG.debug("Successfully awarded player[" + playerId + "] with [" + amountOfChips + "] chips");

            purchaseTracking.trackSuccessfulPurchase(playerId);
        } catch (WalletServiceException e) {
            LOG.error("error caught awarding player[" + playerId + "] with [" + amountOfChips + "] chips", e);
        }

    }

    private void processExternalTransaction(final BigDecimal accountId,
                                            final String transactionRef,
                                            final BigDecimal amountOfCredits,
                                            final BigDecimal amountOfChips,
                                            final String gameType,
                                            final BigDecimal playerId,
                                            final Long promoId) throws WalletServiceException {

        final ExternalTransaction externalTransaction = ExternalTransaction.newExternalTransaction(accountId)
                .withInternalTransactionId(transactionRef)
                .withExternalTransactionId(transactionRef)
                .withMessage("", new DateTime())
                .withAmount(Currency.getInstance("USD"), amountOfCredits.divide(BigDecimal.TEN))
                .withPaymentOption(amountOfChips, null)
                .withCreditCardNumber("")
                .withCashierName(EARN_CHIPS_CASHIER)
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(gameType)
                .withPlayerId(playerId)
                .withPromotionId(promoId)
                .withPlatform(FACEBOOK_CANVAS)
                .build();
        LOG.debug("Trying to log an external transaction");
        walletService.record(externalTransaction);
        LOG.debug("Successfully logged an external transaction");
    }

    public void disputePurchase(final String internalTransactionId,
                                final String externalTransactionId,
                                final BigDecimal playerId,
                                final PaymentOption paymentOption,
                                final String gameType,
                                final Long promoId,
                                final String disputeReason,
                                final DateTime disputeDate) {
        final BigDecimal accountId = playerService.getAccountId(playerId);
        paymentService.disputePayment(PaymentDispute.newDispute(internalTransactionId,
                FACEBOOK.name(),
                externalTransactionId,
                playerId,
                accountId,
                disputeDate,
                paymentOption.getAmountRealMoneyPerPurchase(),
                Currency.getInstance(paymentOption.getRealMoneyCurrency()),
                paymentOption.getNumChipsPerPurchase(FACEBOOK.name()),
                ExternalTransactionType.DEPOSIT,
                disputeReason)
                .withGameType(gameType)
                .withPaymentOptionId(paymentOption.getId())
                .withPlatform(FACEBOOK_CANVAS)
                .withPromotionId(promoId)
                .build());
    }

    public PurchaseResult completePurchase(final BigDecimal playerId,
                                           final String gameType,
                                           final PaymentOption paymentOption,
                                           final String externalReference,
                                           final String internalReference,
                                           final String currencyCode,
                                           final BigDecimal amountPaidInCurrency,
                                           final Long promoId) {

        final BigDecimal accountId = playerService.getAccountId(playerId);
        final PlayerProfile player = profileService.findByPlayerId(playerId);
        final String emailAddress = player.getEmailAddress();

        final CustomerData customerData = new CustomerDataBuilder()
                .withAmount(paymentOption.getAmountRealMoneyPerPurchase())
                .withCurrency(Currency.getInstance(paymentOption.getRealMoneyCurrency()))
                .withTransactionCountry(TX_COUNTRY)
                .withCreditCardNumber(EMPTY_STRING)
                .withCvc2(EMPTY_STRING)
                .withExpirationMonth(EMPTY_STRING)
                .withExpirationYear(EMPTY_STRING)
                .withCardHolderName(EMPTY_STRING)
                .withCustomerIPAddress(null)
                .withEmailAddress(emailAddress)
                .withGameType(gameType)
                .build();
        final DateTime paymentTime = new DateTime();
        final PurchaseRequest purchaseRequest = new PurchaseRequest(customerData,
                accountId,
                paymentOption,
                paymentTime,
                playerId,
                null,
                promoId);
        final PurchaseResult result = createPurchaseResult(gameType, purchaseRequest, externalReference, internalReference);
        FacebookAppConfiguration facebookAppConfiguration = facebookConfiguration.getAppConfigFor(gameType, CANVAS, STRICT);
        String redirectUrl = String.format(FACEBOOK_URL, facebookAppConfiguration.getAppName());
        emailTransactionDetails(result, player.getDisplayName(), currencyCode, amountPaidInCurrency, redirectUrl);
        communityService.asyncPublishBalance(playerId);
        logPromotionPlayerReward(playerId, paymentOption, paymentTime);
        updatePreferredPaymentMethod(playerId, paymentOption.getRealMoneyCurrency());
        purchaseTracking.trackSuccessfulPurchase(playerId);
        return result;
    }

    private void updatePreferredPaymentMethod(final BigDecimal playerId, final String realMoneyCurrency) {
        PaymentPreferences paymentPreferences = playerService.getPaymentPreferences(playerId);
        if (paymentPreferences != null) {
            paymentPreferences = paymentPreferences.withPaymentMethod(FACEBOOK);
        } else {
            paymentPreferences = new PaymentPreferences(FACEBOOK);
        }
        paymentPreferences = paymentPreferences.withCurrency(getCurrency(realMoneyCurrency));
        playerService.updatePaymentPreferences(playerId, paymentPreferences);
    }

    com.yazino.platform.reference.Currency getCurrency(final String realMoneyCurrency) {
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
        if (paymentOption.hasPromotion(FACEBOOK)) {
            final PromotionPaymentOption promotion = paymentOption.getPromotion(FACEBOOK);
            buyChipsPromotionService.logPlayerReward(playerId, promotion.getPromoId(),
                    FACEBOOK, paymentOption.getId(), dateTime);
        }
    }


    private PurchaseResult createPurchaseResult(String gameType,
                                                PurchaseRequest purchaseRequest,
                                                String externalRef,
                                                String internalRef) {
        TransactionResponse transactionResponse = executeTransaction(
                externalRef,
                internalRef,
                gameType,
                purchaseRequest);
        return new PurchaseResult(
                FACEBOOK.name(),
                PurchaseOutcome.APPROVED,
                purchaseRequest.getCustomerData().getEmailAddress(),
                transactionResponse.getMessage(),
                purchaseRequest.getCustomerData().getCurrency(),
                purchaseRequest.getPaymentOption().getAmountRealMoneyPerPurchase(),
                purchaseRequest.getPaymentOption().getNumChipsPerPurchase(
                        PaymentPreferences.PaymentMethod.FACEBOOK.name()),
                EMPTY_STRING,
                transactionResponse.getInternalTransactionId(),
                externalRef,
                EMPTY_STRING
        );
    }

    private void emailTransactionDetails(final PurchaseResult purchaseResult,
                                         String playerName,
                                         String currencyCode,
                                         BigDecimal amountPaidInCurrency,
                                         String targetUrl) {
        notNull(purchaseResult, "purchaseResult is null");
        try {
            final BoughtChipsEmailBuilder builder = new BoughtChipsEmailBuilder();
            builder.withEmailAddress(purchaseResult.getCustomerEmail());
            builder.withFirstName(playerName);
            builder.withPurchasedChips(purchaseResult.getChips());
            builder.withCurrency(java.util.Currency.getInstance(currencyCode));
            builder.withCost(amountPaidInCurrency);
            builder.withPaymentDate(new DateTime().toDate());
            builder.withPaymentId(purchaseResult.getExternalTransactionId());
            builder.withPaymentEmailBodyTemplate(Facebook);
            builder.withTargetUrl(targetUrl);
            builder.withIncludeGamesInFooter(false);

            emailer.quietlySendEmail(builder);
        } catch (final Exception e) {
            LOG.error(String.format("Could not send transaction response to %s with message %s",
                    purchaseResult.getCustomerEmail(), purchaseResult.getMessage()), e);
        }
    }


    private TransactionResponse executeTransaction(String externalRef,
                                                   String internalRef,
                                                   String gameType,
                                                   PurchaseRequest purchaseRequest) {
        String internalTransactionId = null;
        try {
            final ExternalTransaction transaction = buildExternalTransaction(
                    ExternalTransactionStatus.SUCCESS,
                    externalRef,
                    internalRef,
                    purchaseRequest.getPaymentOption(),
                    gameType,
                    purchaseRequest.getAccountId(),
                    purchaseRequest.getPlayerId(),
                    purchaseRequest.getPromotionId(),
                    "");
            internalTransactionId = transaction.getInternalTransactionId();
            walletService.record(transaction);
            return new TransactionResponse(internalTransactionId, FunctionResultCode.ACK);
        } catch (Exception e) {
            LOG.error("Error processing request for transaction: " + internalTransactionId, e);
            String message = e.getMessage();
            if (message == null) {
                message = e.getClass().getName();
            }
            return new TransactionResponse(internalTransactionId, FunctionResultCode.NOK, message);
        }
    }

    public BigDecimal getPlayerId(final String buyerId) {
        return profileService.findByProviderNameAndExternalId("facebook", buyerId).getPlayerId();
    }

    public BigDecimal getEarnedChipsToday(BigDecimal playerId) {
        final BigDecimal primaryAccountId = playerService.getAccountId(playerId);
        return walletService.getValueOfTodaysEarnedChips(primaryAccountId, EARN_CHIPS_CASHIER);
    }

    public void logFailedTransaction(String errorCode,
                                     String errorMessage,
                                     BigDecimal playerId,
                                     String externalTransactionId,
                                     String internalTransactionId,
                                     PaymentOption paymentOption,
                                     String gameType,
                                     Long promoId) throws WalletServiceException {
        ExternalTransactionStatus status;
        String message = "";
        if (USER_CANCELLED_ERROR_CODE.equals(errorCode)) {
            status = CANCELLED;
            message = "Cancelled by user";
        } else {
            status = FAILURE;
            if (StringUtils.isNotBlank(errorCode)) {
                message = errorCode + " - ";
            }
            message += errorMessage;
        }

        final BigDecimal primaryAccountId = playerService.getAccountId(playerId);
        ExternalTransaction transaction = buildExternalTransaction(
                status,
                externalTransactionId,
                internalTransactionId,
                paymentOption,
                gameType,
                primaryAccountId,
                playerId,
                promoId,
                message);
        walletService.record(transaction);
    }
}
