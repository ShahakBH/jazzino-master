package com.yazino.web.payment.creditcard.worldpay;

import com.yazino.bi.payment.PaymentOption;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.MessageCode;
import com.yazino.payment.worldpay.NVPResponse;
import com.yazino.payment.worldpay.STLink;
import com.yazino.payment.worldpay.nvp.NVPMessage;
import com.yazino.payment.worldpay.nvp.PaymentTrustAuthorisationMessage;
import com.yazino.payment.worldpay.nvp.RiskGuardianMessage;
import com.yazino.platform.account.*;
import com.yazino.platform.player.PlayerProfileStatus;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.payment.CustomerData;
import com.yazino.web.payment.TransactionIdGenerator;
import com.yazino.web.payment.creditcard.CreditCardPaymentService;
import com.yazino.web.payment.creditcard.PurchaseOutcome;
import com.yazino.web.payment.creditcard.PurchaseRequest;
import com.yazino.web.payment.creditcard.PurchaseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.yazino.platform.Platform.WEB;
import static com.yazino.platform.community.PaymentPreferences.PaymentMethod.CREDITCARD;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.left;
import static org.apache.commons.lang3.Validate.notNull;

@Service("creditCardPaymentService")
public class WorldPayCreditCardPaymentService implements CreditCardPaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(WorldPayCreditCardPaymentService.class);

    private static final String MERCHANT_ID = "WorldPay";
    private static final String DUMP_FORMAT = "Req=%s;Res=%s";
    private static final String TEST_MODE_PROPERTY = "payment.worldpay.stlink.testmode";
    private static final String RISK_GUARDIAN_ENABLED_PROPERTY = "payment.worldpay.stlink.riskguardian.enabled";
    private static final String BLOCK_OUTCOMES_PROPERTY = "payment.worldpay.stlink.auto-block.message-codes";
    private static final List<Object> DEFAULT_BLOCK_OUTCOMES
            = asList((Object) "2380", "2382", "2401", "2402", "2634", "2648", "2832", "2847", "2952", "2954");
    private static final int EXPECTED_EXPIRATION_DATE_LENGTH = 6;
    private static final int RISK_GUARDIAN_LITE = 10;
    private static final int MAX_ORDER_NUMBER_LENGTH = 35;
    private static final int MAX_FAILURE_REASON_LENGTH = 255;
    private static final String SYSTEM_USER = "system";

    private final STLink stLink;
    private final TransactionIdGenerator transactionIdGenerator;
    private final WalletService walletService;
    private final PlayerProfileService playerProfileService;
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public WorldPayCreditCardPaymentService(final STLink stLink,
                                            final TransactionIdGenerator transactionIdGenerator,
                                            final WalletService walletService,
                                            final PlayerProfileService playerProfileService,
                                            final YazinoConfiguration yazinoConfiguration) {
        notNull(stLink, "stLink may not be null");
        notNull(transactionIdGenerator, "transactionIdGenerator may not be null");
        notNull(walletService, "walletService may not be null");
        notNull(playerProfileService, "playerProfileService may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.stLink = stLink;
        this.transactionIdGenerator = transactionIdGenerator;
        this.walletService = walletService;
        this.playerProfileService = playerProfileService;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @Override
    public PurchaseResult purchase(final PurchaseRequest purchaseRequest) {
        notNull(purchaseRequest, "purchaseRequest may not be null");

        LOG.debug("Attempting purchase: {}", purchaseRequest);

        final String transactionId = Long.toHexString(transactionIdGenerator.generateNumericTransactionId()).toUpperCase();

        final NVPResponse authoriseResponse;
        try {
            recordExternalTransactionFor(purchaseRequest, null, transactionId);

            if (riskGuardianIsEnabled()) {
                final NVPResponse rgResponse = stLink.send(riskGuardianRequestFor(purchaseRequest, transactionId));
                recordExternalTransactionFor(purchaseRequest, rgResponse, transactionId);

                if (aboveRiskThreshold(rgResponse, transactionId)) {
                    LOG.debug("Purchase {} refused for violating risk threshold", transactionId);
                    return processResultOf(purchaseRequest, rgResponse, transactionId);
                }
            }

            authoriseResponse = stLink.send(paymentTrustAuthoriseRequestFor(purchaseRequest, transactionId));

            recordExternalTransactionFor(purchaseRequest, authoriseResponse, transactionId);

        } catch (Exception e) {
            LOG.error("An exception occurred when processing a transaction: {}", transactionId, e);

            recordExternalTransactionFor(purchaseRequest, transactionId, e);
            return exceptionResultOf(purchaseRequest, transactionId, e);
        }

        final PurchaseResult purchaseResult = processResultOf(purchaseRequest, authoriseResponse, transactionId);
        LOG.debug("Purchase {} completed with result {}", transactionId, purchaseResult);
        return purchaseResult;
    }

    private boolean riskGuardianIsEnabled() {
        return yazinoConfiguration.getBoolean(RISK_GUARDIAN_ENABLED_PROPERTY, true);
    }

    private NVPMessage paymentTrustAuthoriseRequestFor(final PurchaseRequest purchaseRequest,
                                                       final String internalTransactionId) {
        final CustomerData customerData = purchaseRequest.getCustomerData();

        NVPMessage ptaMessage = new PaymentTrustAuthorisationMessage()
                .withValue("OrderNumber", left(internalTransactionId, MAX_ORDER_NUMBER_LENGTH))
                .withValue("Email", customerData.getEmailAddress())
                .withValue("CurrencyId", customerData.getCurrency().getNumericCode())
                .withValue("Amount", purchaseRequest.getPaymentOption().getAmountRealMoneyPerPurchase())
                .withValue("CountryCode", customerData.getTransactionCountryISO3166())
                .withValue("REMOTE_ADDR", customerNetworkAddressFrom(customerData));

        if (isNotBlank(customerData.getCardId())) {
            ptaMessage = ptaMessage
                    .withValue("CustomerId", purchaseRequest.getPlayerId())
                    .withValue("CardId", customerData.getCardId());
        } else {
            ptaMessage = ptaMessage
                    .withValue("AcctName", customerData.getCardHolderName())
                    .withValue("AcctNumber", customerData.getCreditCardNumber())
                    .withValue("ExpDate", expirationDateFor(customerData))
                    .withValue("CVN", customerData.getCvc2());
        }

        if (yazinoConfiguration.getBoolean(TEST_MODE_PROPERTY)) {
            return ptaMessage.withValue("IsTest", 1);
        }
        return ptaMessage;
    }

    private String customerNetworkAddressFrom(final CustomerData customerData) {
        if (customerData.getCustomerIPAddress() != null) {
            return customerData.getCustomerIPAddress().getHostAddress();
        }
        return null;
    }

    private NVPMessage riskGuardianRequestFor(final PurchaseRequest purchaseRequest,
                                              final String internalTransactionId) {
        final CustomerData customerData = purchaseRequest.getCustomerData();
        NVPMessage rgMessage = new RiskGuardianMessage()
                .withValue("TRXSource", RISK_GUARDIAN_LITE)
                .withValue("OrderNumber", left(internalTransactionId, MAX_ORDER_NUMBER_LENGTH))
                .withValue("AcctName", customerData.getCardHolderName())
                .withValue("Email", customerData.getEmailAddress())
                .withValue("ExpDate", expirationDateFor(customerData))
                .withValue("CurrencyId", customerData.getCurrency().getNumericCode())
                .withValue("Amount", purchaseRequest.getPaymentOption().getAmountRealMoneyPerPurchase())
                .withValue("CountryCode", customerData.getTransactionCountryISO3166())
                .withValue("REMOTE_ADDR", customerNetworkAddressFrom(customerData));

        if (isNotBlank(customerData.getCardId())) {
            LOG.debug("Performing risk guardian with registered card");
            rgMessage = rgMessage.withValue("CardId", customerData.getCardId())
                    .withValue("CustomerId", purchaseRequest.getPlayerId());
        } else {
            rgMessage = rgMessage.withValue("AcctNumber", customerData.getCreditCardNumber())
                    .withValue("CVN", customerData.getCvc2());
        }

        if (yazinoConfiguration.getBoolean(TEST_MODE_PROPERTY)) {
            return rgMessage.withValue("IsTest", 1);
        }
        return rgMessage;
    }

    private String expirationDateFor(final CustomerData customerData) {
        if (customerData.getExpirationMonth() == null || customerData.getExpirationYear() == null) {
            return null;
        }

        String expirationYear = customerData.getExpirationYear();
        if (expirationYear.length() == 2) {
            expirationYear = "20" + expirationYear;
        } else if (expirationYear.length() == 1) {
            expirationYear = "201" + expirationYear;
        }

        final String expirationDate = customerData.getExpirationMonth() + expirationYear;
        if (expirationDate.length() != EXPECTED_EXPIRATION_DATE_LENGTH) {
            throw new IllegalArgumentException("Invalid expiration month or year; expected 6 digits in total:" + expirationDate);
        }
        return expirationDate;
    }

    private boolean aboveRiskThreshold(final NVPResponse rgResponse,
                                       final String localTransactionId) {
        if (rgResponse == null) {
            return true;
        }

        final MessageCode rgResponseCode = MessageCode.forCode(rgResponse.get("MessageCode").orNull());
        if (rgResponseCode != MessageCode.OKAY) {
            LOG.error("RiskGuardian query failed for transaction {} with error code {}", localTransactionId, rgResponseCode);
            return false;
        }

        if (!rgResponse.get("tScore").isPresent() || !rgResponse.get("tRisk").isPresent()) {
            LOG.error("RiskGuardian query failed for transaction {} with missing tScore or tRisk", localTransactionId, rgResponseCode);
            return false;
        }

        final BigDecimal score = new BigDecimal(rgResponse.get("tScore").get());
        final BigDecimal threshold = new BigDecimal(rgResponse.get("tRisk").get());

        return score.compareTo(threshold) > 0;
    }

    @Override
    public boolean accepts(final PurchaseRequest purchaseRequest) {
        return true;
    }

    private PurchaseResult exceptionResultOf(final PurchaseRequest purchaseRequest,
                                             final String localTransactionId,
                                             final Throwable exception) {
        final CustomerData customerData = purchaseRequest.getCustomerData();
        final PaymentOption paymentOption = purchaseRequest.getPaymentOption();

        return new PurchaseResult(MERCHANT_ID,
                PurchaseOutcome.SYSTEM_FAILURE,
                customerData.getEmailAddress(),
                "Transaction Failed.",
                customerData.getCurrency(),
                paymentOption.getAmountRealMoneyPerPurchase(),
                paymentOption.getNumChipsPerPurchase(CREDITCARD.name()),
                customerData.getObscureMiddleCardNumbers(),
                localTransactionId,
                null,
                dump(exception));
    }

    private PurchaseResult processResultOf(final PurchaseRequest purchaseRequest,
                                           final NVPResponse txResponse,
                                           final String localTransactionId) {
        final CustomerData customerData = purchaseRequest.getCustomerData();
        final PaymentOption paymentOption = purchaseRequest.getPaymentOption();

        final PurchaseOutcome purchaseOutcome;
        if (blockPlayerIfRequired(txResponse, purchaseRequest.getPlayerId())) {
            purchaseOutcome = PurchaseOutcome.PLAYER_BLOCKED;
        } else {
            purchaseOutcome = outcomeFor(txResponse);
        }

        return new PurchaseResult(MERCHANT_ID,
                purchaseOutcome,
                customerData.getEmailAddress(),
                defaultIfNull(valueOf("Message", txResponse), ""),
                customerData.getCurrency(),
                paymentOption.getAmountRealMoneyPerPurchase(),
                paymentOption.getNumChipsPerPurchase(CREDITCARD.name()),
                customerData.getObscureMiddleCardNumbers(),
                localTransactionId,
                externalTransactionIdFor(txResponse),
                dumpRequestAndResponse(txResponse));
    }

    private String valueOf(final String fieldName, final NVPResponse txResponse) {
        return txResponse.get(fieldName).orNull();
    }

    private PurchaseOutcome outcomeFor(final NVPResponse txResponse) {
        if (txResponse == null) {
            return PurchaseOutcome.UNKNOWN;
        }

        if ("RG".equals(txResponse.get("TransactionType").orNull())) {
            return outcomeForRiskGuardianResponse(txResponse);
        }
        return outcomeForPaymentTrustResponse(txResponse);
    }

    private PurchaseOutcome outcomeForRiskGuardianResponse(final NVPResponse txResponse) {
        final MessageCode responseCode = MessageCode.forCode(txResponse.get("MessageCode").orNull());
        if (responseCode == null) {
            return PurchaseOutcome.UNKNOWN;

        } else if (responseCode == MessageCode.OKAY) {
            return PurchaseOutcome.RISK_FAILED;
        }

        return PurchaseOutcome.SYSTEM_FAILURE;
    }

    private PurchaseOutcome outcomeForPaymentTrustResponse(final NVPResponse txResponse) {
        final MessageCode responseCode = MessageCode.forCode(txResponse.get("MessageCode").orNull());
        if (responseCode == null) {
            return PurchaseOutcome.UNKNOWN;
        }

        switch (responseCode) {
            case APPROVED:
                return PurchaseOutcome.APPROVED;
            case NOT_AUTHORISED:
            case DO_NOT_HONOUR:
                return PurchaseOutcome.DECLINED;
            case CALL_BANK:
                return PurchaseOutcome.REFERRED;
            case INVALID_ACCOUNT_NUMBER:
            case INVALID_CARD_NUMBER:
                return PurchaseOutcome.INVALID_ACCOUNT;
            case INVALID_CARD_EXPIRATION:
            case INVALID_CREDIT_CARD_EXPIRY:
                return PurchaseOutcome.INVALID_EXPIRY;
            case INSUFFICIENT_FUNDS:
                return PurchaseOutcome.INSUFFICIENT_FUNDS;
            case OVER_CREDIT_LIMIT:
                return PurchaseOutcome.EXCEEDS_TRANSACTION_LIMIT;
            case CVN_FAILURE:
            case INVALID_CVN_VALUE:
                return PurchaseOutcome.CSC_CHECK_FAILED;
            case CARD_STOLEN:
                return PurchaseOutcome.LOST_OR_STOLEN_CARD;
            case UNABLE_TO_PROCESS_SYSTEM_MALFUNCTION:
            case SYSTEM_ERROR:
                return PurchaseOutcome.SYSTEM_FAILURE;
            default:
                return PurchaseOutcome.UNKNOWN;
        }
    }

    private void recordExternalTransactionFor(final PurchaseRequest purchaseRequest,
                                              final String localTransactionId,
                                              final Throwable exception) {
        final PaymentOption paymentOption = purchaseRequest.getPaymentOption();
        final CustomerData customerData = purchaseRequest.getCustomerData();

        final ExternalTransaction externalTx = builderFor(purchaseRequest, localTransactionId, paymentOption, customerData)
                .withExternalTransactionId(null)
                .withMessage(dump(exception), purchaseRequest.getDateTime())
                .withStatus(ExternalTransactionStatus.ERROR)
                .withFailureReason(left(exception.getMessage(), MAX_FAILURE_REASON_LENGTH))
                .build();

        try {
            walletService.record(externalTx);

        } catch (Exception e) {
            LOG.error("Unable to record external transaction {}", externalTx, e);
        }
    }

    private void recordExternalTransactionFor(final PurchaseRequest purchaseRequest,
                                              final NVPResponse txResponse,
                                              final String internalTransactionId) {
        final PaymentOption paymentOption = purchaseRequest.getPaymentOption();
        final CustomerData customerData = purchaseRequest.getCustomerData();

        final ExternalTransactionStatus status = transactionStatusFor(txResponse, internalTransactionId);
        ExternalTransactionBuilder externalTxBuilder = builderFor(purchaseRequest, internalTransactionId, paymentOption, customerData)
                .withExternalTransactionId(externalTransactionIdFor(txResponse))
                .withMessage(dumpRequestAndResponse(txResponse), purchaseRequest.getDateTime())
                .withStatus(status);

        if (status == ExternalTransactionStatus.FAILURE) {
            externalTxBuilder = externalTxBuilder.withFailureReason(failureReasonFrom(txResponse));
        }

        final ExternalTransaction externalTransaction = externalTxBuilder.build();
        try {
            walletService.record(externalTransaction);

        } catch (Exception e) {
            LOG.error("Unable to record external transaction {}", externalTransaction, e);
        }
    }

    private String failureReasonFrom(final NVPResponse txResponse) {
        if ("RG".equals(txResponse.get("TransactionType").or(""))) {
            return "RiskGuardian rejection";
        }
        return left(txResponse.get("Message").orNull(), MAX_FAILURE_REASON_LENGTH);
    }

    private ExternalTransactionBuilder builderFor(final PurchaseRequest purchaseRequest,
                                                  final String localTransactionId,
                                                  final PaymentOption paymentOption,
                                                  final CustomerData customerData) {
        return ExternalTransaction.newExternalTransaction(purchaseRequest.getAccountId())
                .withInternalTransactionId(localTransactionId)
                .withAmount(currencyFor(paymentOption.getRealMoneyCurrency()), paymentOption.getAmountRealMoneyPerPurchase())
                .withPaymentOption(paymentOption.getNumChipsPerPurchase(CREDITCARD.name()), paymentOption.getId())
                .withCreditCardNumber(customerData.getObscureMiddleCardNumbers())
                .withCashierName(MERCHANT_ID)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType(customerData.getGameType())
                .withPlayerId(purchaseRequest.getPlayerId())
                .withSessionId(purchaseRequest.getSessionId())
                .withPromotionId(purchaseRequest.getPromotionId())
                .withPlatform(WEB)
                .withForeignExchange(currencyFor(paymentOption.getBaseCurrencyCode()),
                        paymentOption.getBaseCurrencyPrice(), paymentOption.getExchangeRate());
    }

    private String dump(final Throwable exception) {
        if (exception == null) {
            return null;
        }

        final StringWriter exceptionOutput = new StringWriter();
        exception.printStackTrace(new PrintWriter(exceptionOutput));
        return exceptionOutput.toString();
    }

    private String dumpRequestAndResponse(final NVPResponse txResponse) {
        if (txResponse != null) {
            return String.format(DUMP_FORMAT, txResponse.getRequestString(), txResponse.getResponseString());
        }
        return "";
    }

    private String externalTransactionIdFor(final NVPResponse txResponse) {
        if (txResponse != null) {
            final String transactionType = txResponse.get("TransactionType").or("PT");
            if ("PT".equals(transactionType)) {
                return txResponse.get("PTTID").orNull();
            }
            if ("RG".equals(transactionType)) {
                return txResponse.get("GttId").or(txResponse.get("RGID")).orNull();
            }
        }
        return null;
    }

    private java.util.Currency currencyFor(final String currencyCode) {
        if (currencyCode != null) {
            return java.util.Currency.getInstance(currencyCode);
        }
        return null;
    }

    private ExternalTransactionStatus transactionStatusFor(final NVPResponse txResponse,
                                                           final String internalTransactionId) {
        if (txResponse == null) {
            return ExternalTransactionStatus.REQUEST;
        }

        if (MessageCode.isSuccessful(txResponse.get("MessageCode"))) {
            if ("RG".equals(txResponse.get("TransactionType").or(""))) {
                if (!aboveRiskThreshold(txResponse, internalTransactionId)) {
                    return ExternalTransactionStatus.REQUEST;
                }
            } else {
                return ExternalTransactionStatus.AUTHORISED;
            }
        }

        return ExternalTransactionStatus.FAILURE;
    }

    private boolean blockPlayerIfRequired(final NVPResponse response,
                                          final BigDecimal playerId) {
        final MessageCode messageCode = messageCodeFrom(response);
        if (messageCode != null && messageCodesToBlock().contains(messageCode)) {
            LOG.debug("Response code {} is marked as auto-blocked; blocking player {}", messageCode.getCode(), playerId);

            try {
                playerProfileService.updateStatus(playerId, PlayerProfileStatus.BLOCKED, SYSTEM_USER,
                        String.format("Blocked on WorldPay response %s:%s", messageCode.getCode(), messageCode.getDescription()));
                return true;

            } catch (Exception e) {
                LOG.error("Auto-block: Failed to block player {}", playerId, e);
            }
        }
        return false;
    }

    private MessageCode messageCodeFrom(final NVPResponse authoriseResponse) {
        return MessageCode.forCode(authoriseResponse.get("MessageCode").orNull());
    }

    private List<MessageCode> messageCodesToBlock() {
        final List<MessageCode> messageCodes = new ArrayList<>();
        for (Object messageCodesId : yazinoConfiguration.getList(BLOCK_OUTCOMES_PROPERTY, DEFAULT_BLOCK_OUTCOMES)) {
            try {
                messageCodes.add(MessageCode.forCode(messageCodesId.toString()));
            } catch (Exception e) {
                LOG.error("Auto-block: Failed to parse message code " + messageCodesId);
            }
        }
        return messageCodes;
    }
}
