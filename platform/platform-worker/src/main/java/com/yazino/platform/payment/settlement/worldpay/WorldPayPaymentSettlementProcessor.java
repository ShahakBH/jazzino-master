package com.yazino.platform.payment.settlement.worldpay;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.MessageCode;
import com.yazino.payment.worldpay.NVPResponse;
import com.yazino.payment.worldpay.STLink;
import com.yazino.payment.worldpay.nvp.NVPMessage;
import com.yazino.payment.worldpay.nvp.PaymentTrustCancellationMessage;
import com.yazino.payment.worldpay.nvp.PaymentTrustDepositMessage;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.ExternalTransactionBuilder;
import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.model.account.PaymentSettlement;
import com.yazino.platform.payment.settlement.PaymentSettlementProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.apache.commons.lang3.StringUtils.left;
import static org.apache.commons.lang3.Validate.notNull;

public class WorldPayPaymentSettlementProcessor implements PaymentSettlementProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(WorldPayPaymentSettlementProcessor.class);

    private static final String DUMP_FORMAT = "Req=%s;Res=%s";
    private static final int MAX_ORDER_NUMBER_LENGTH = 35;
    private static final String TEST_MODE_PROPERTY = "payment.worldpay.stlink.testmode";
    private static final int MAX_FAILURE_REASON_LENGTH = 255;

    private final YazinoConfiguration yazinoConfiguration;
    private final STLink stLink;

    @Autowired
    public WorldPayPaymentSettlementProcessor(final YazinoConfiguration yazinoConfiguration,
                                              final STLink stLink) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(stLink, "stLink may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.stLink = stLink;
    }

    @Override
    public ExternalTransaction settle(final PaymentSettlement settlement) {
        notNull(settlement, "settlement may not be null");

        LOG.debug("Processing settlement of transaction {}", settlement.getInternalTransactionId());

        try {
            final NVPResponse depositResponse = stLink.send(aDepositMessageFor(settlement));
            LOG.debug("Settlement {} completed with result {}", settlement.getInternalTransactionId(), depositResponse);

            return externalTransactionFor(settlement, depositResponse);

        } catch (Exception e) {
            LOG.error("An error occurred when settling a transaction: {}", settlement.getInternalTransactionId(), e);
            return externalTransactionFor(settlement, e);
        }
    }

    @Override
    public ExternalTransaction cancel(final PaymentSettlement settlement) {
        notNull(settlement, "settlement may not be null");

        LOG.debug("Processing cancelling of transaction {}", settlement.getInternalTransactionId());

        try {
            final NVPResponse depositResponse = stLink.send(aCancellationMessageFor(settlement));
            LOG.debug("Cancellation {} completed with result {}", settlement.getInternalTransactionId(), depositResponse);

            return externalTransactionFor(settlement, depositResponse);

        } catch (Exception e) {
            LOG.error("An error occurred when cancelling a transaction: {}", settlement.getInternalTransactionId(), e);
            return externalTransactionFor(settlement, e);
        }
    }

    private NVPMessage aCancellationMessageFor(final PaymentSettlement settlement) {
        final NVPMessage cancellationMessage = new PaymentTrustCancellationMessage()
                .withValue("OrderNumber", left(settlement.getInternalTransactionId(), MAX_ORDER_NUMBER_LENGTH))
                .withValue("PTTID", settlement.getExternalTransactionId());

        if (yazinoConfiguration.getBoolean(TEST_MODE_PROPERTY, false)) {
            return cancellationMessage.withValue("IsTest", 1);
        }

        return cancellationMessage;
    }

    private NVPMessage aDepositMessageFor(final PaymentSettlement settlement) {
        final NVPMessage depositMessage = new PaymentTrustDepositMessage()
                .withValue("OrderNumber", left(settlement.getInternalTransactionId(), MAX_ORDER_NUMBER_LENGTH))
                .withValue("PTTID", settlement.getExternalTransactionId())
                .withValue("CurrencyId", settlement.getCurrency().getNumericCode())
                .withValue("Amount", settlement.getPrice());

        if (yazinoConfiguration.getBoolean(TEST_MODE_PROPERTY, false)) {
            return depositMessage.withValue("IsTest", 1);
        }

        return depositMessage;
    }

    private ExternalTransaction externalTransactionFor(final PaymentSettlement settlement,
                                                       final Throwable exception) {
        return builderFor(settlement)
                .withExternalTransactionId(settlement.getExternalTransactionId())
                .withMessage(dump(exception), settlement.getTimestamp())
                .withStatus(ExternalTransactionStatus.ERROR)
                .withFailureReason(left(exception.getMessage(), MAX_FAILURE_REASON_LENGTH))
                .build();
    }

    private ExternalTransaction externalTransactionFor(final PaymentSettlement settlement,
                                                       final NVPResponse txResponse) {
        final ExternalTransactionStatus status = transactionStatusFor(txResponse);
        ExternalTransactionBuilder builder = builderFor(settlement)
                .withExternalTransactionId(txResponse.get("PTTID").orNull())
                .withMessage(dumpRequestAndResponse(txResponse), settlement.getTimestamp())
                .withStatus(status);

        if (status == ExternalTransactionStatus.FAILURE) {
            builder = builder.withFailureReason(left(txResponse.get("Message").orNull(), MAX_FAILURE_REASON_LENGTH));
        }

        return builder.build();
    }

    private ExternalTransactionBuilder builderFor(final PaymentSettlement settlement) {
        return ExternalTransaction.newExternalTransaction(settlement.getAccountId())
                .withInternalTransactionId(settlement.getInternalTransactionId())
                .withAmount(settlement.getCurrency(), settlement.getPrice())
                .withPaymentOption(settlement.getChips(), settlement.getPaymentOptionId())
                .withCreditCardNumber(settlement.getAccountNumber())
                .withCashierName(settlement.getCashierName())
                .withType(settlement.getTransactionType())
                .withGameType(settlement.getGameType())
                .withPlayerId(settlement.getPlayerId())
                .withPromotionId(settlement.getPromotionId())
                .withPlatform(settlement.getPlatform())
                .withForeignExchange(settlement.getBaseCurrency(), settlement.getBaseCurrencyAmount(), settlement.getExchangeRate());
    }

    private ExternalTransactionStatus transactionStatusFor(final NVPResponse txResponse) {
        if (txResponse != null) {
            final MessageCode messageCode = MessageCode.forCode(txResponse.get("MessageCode").orNull());
            if (messageCode == MessageCode.CANCELLED_SUCCESSFULLY_BY_REQUEST
                    || messageCode == MessageCode.CANCELLED_SUCCESSFULLY) {
                return ExternalTransactionStatus.CANCELLED;

            } else if (messageCode.isSuccessful()) {
                return ExternalTransactionStatus.SETTLED;
            }
        }
        return ExternalTransactionStatus.FAILURE;
    }

    private String dumpRequestAndResponse(final NVPResponse txResponse) {
        if (txResponse != null) {
            return String.format(DUMP_FORMAT, txResponse.getRequestString(), txResponse.getResponseString());
        }
        return "";
    }

    private String dump(final Throwable exception) {
        if (exception == null) {
            return null;
        }

        final StringWriter exceptionOutput = new StringWriter();
        exception.printStackTrace(new PrintWriter(exceptionOutput));
        return exceptionOutput.toString();
    }
}
