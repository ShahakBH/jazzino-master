package com.yazino.platform.processor.account;

import com.yazino.platform.account.Amount;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.audit.AuditService;
import com.yazino.platform.model.account.AccountStatement;
import com.yazino.platform.model.account.ExternalTransactionPersistenceRequest;
import com.yazino.platform.model.account.PaymentSettlement;
import com.yazino.platform.persistence.account.JDBCAccountStatementDAO;
import com.yazino.platform.persistence.account.JDBCPaymentSettlementDAO;
import org.apache.commons.lang3.ObjectUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import static com.yazino.platform.account.ExternalTransaction.copyExternalTransaction;
import static com.yazino.platform.model.account.PaymentSettlement.newSettlement;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
public class ExternalTransactionPersistenceRequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalTransactionPersistenceRequestProcessor.class);

    private static final ExternalTransactionPersistenceRequest TEMPLATE = new ExternalTransactionPersistenceRequest();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssSSS");
    private static final AtomicLong ATOMIC_LONG = new AtomicLong();

    private final AuditService auditService;
    private final JDBCAccountStatementDAO accountStatementDao;
    private final JDBCPaymentSettlementDAO paymentSettlementDao;

    private InetAddress localhost;

    protected ExternalTransactionPersistenceRequestProcessor() {
        // CGLib constructor

        this.auditService = null;
        this.accountStatementDao = null;
        this.paymentSettlementDao = null;
    }

    @Autowired
    public ExternalTransactionPersistenceRequestProcessor(final AuditService auditService,
                                                          final JDBCAccountStatementDAO accountStatementDao,
                                                          final JDBCPaymentSettlementDAO paymentSettlementDao) {
        notNull(auditService, "auditService may not be null");
        notNull(accountStatementDao, "accountStatementDao may not be null");
        notNull(paymentSettlementDao, "paymentSettlementDao may not be null");

        this.auditService = auditService;
        this.accountStatementDao = accountStatementDao;
        this.paymentSettlementDao = paymentSettlementDao;

        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            LOG.warn("Unable to lookup hostname for localhost", e);
        }
    }

    @EventTemplate
    public ExternalTransactionPersistenceRequest getEventTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void process(final ExternalTransactionPersistenceRequest request) {
        checkInitialisation();

        if (request == null || request.getExternalTransaction() == null) {
            LOG.error("Invalid request received: {}");
            return;
        }

        ExternalTransaction externalTransaction = request.getExternalTransaction();
        if (isBlank(externalTransaction.getInternalTransactionId())) {
            LOG.warn("Received external transaction with invalid internal tx ID: {}", externalTransaction);
            externalTransaction = withGeneratedInternalTransactionId(externalTransaction);
        }

        if (externalTransaction.getStatus() == ExternalTransactionStatus.AUTHORISED) {
            LOG.debug("Added transaction to settlement list: {}", externalTransaction.getInternalTransactionId());
            addToSettlementList(externalTransaction);
        }

        audit(externalTransaction);
        addToAccountStatement(externalTransaction);
    }

    private void addToSettlementList(final ExternalTransaction externalTransaction) {
        paymentSettlementDao.save(settlementFor(externalTransaction));
    }

    private PaymentSettlement settlementFor(final ExternalTransaction externalTransaction) {
        PaymentSettlement.PaymentSettlementBuilder settlementBuilder = newSettlement(
                externalTransaction.getInternalTransactionId(),
                externalTransaction.getExternalTransactionId(),
                externalTransaction.getPlayerId(),
                externalTransaction.getAccountId(),
                externalTransaction.getCashierName(),
                externalTransaction.getMessageTimeStamp(),
                externalTransaction.getObscuredCreditCardNumber(),
                externalTransaction.getAmount().getQuantity(),
                externalTransaction.getAmount().getCurrency(),
                externalTransaction.getAmountChips(),
                externalTransaction.getType())
                .withExchangeRate(externalTransaction.getExchangeRate())
                .withGameType(externalTransaction.getGameType())
                .withPaymentOptionId(externalTransaction.getPaymentOptionId())
                .withPlatform(externalTransaction.getPlatform())
                .withPromotionId(externalTransaction.getPromoId());

        if (externalTransaction.getBaseCurrencyAmount() != null) {
            settlementBuilder = settlementBuilder
                    .withBaseCurrencyAmount(externalTransaction.getBaseCurrencyAmount().getQuantity())
                    .withBaseCurrency(externalTransaction.getBaseCurrencyAmount().getCurrency());
        }

        return settlementBuilder.build();
    }

    private ExternalTransaction withGeneratedInternalTransactionId(final ExternalTransaction externalTransaction) {
        return copyExternalTransaction(externalTransaction)
                .withInternalTransactionId(newTransactionIdFor(externalTransaction.getAccountId()))
                .build();
    }

    private String newTransactionIdFor(final BigDecimal accountId) {
        final DateTime dtLondon = new DateTime(DateTimeZone.forID("Europe/London"));

        return String.format("int_%s_%s_%s_%d", accountId.toPlainString(), DATE_TIME_FORMATTER.print(dtLondon),
                localhost.getHostAddress(), ATOMIC_LONG.getAndIncrement());
    }

    private void checkInitialisation() {
        if (auditService == null || accountStatementDao == null || paymentSettlementDao == null) {
            throw new IllegalStateException("Class has been initialised via CGLib constructor");
        }
    }

    private void addToAccountStatement(final ExternalTransaction externalTransaction) {
        try {
            accountStatementDao.save(asAccountStatement(externalTransaction));

        } catch (Exception e) {
            LOG.error("Failed to add to account statement: {}", externalTransaction, e);
        }
    }

    private void audit(final ExternalTransaction externalTransaction) {
        try {
            auditService.externalTransactionProcessed(asWorkerTransaction(externalTransaction));

        } catch (Exception e) {
            LOG.error("Exception saving External Transaction: {}", externalTransaction, e);
        }
    }

    private AccountStatement asAccountStatement(final ExternalTransaction externalTransaction) {
        return AccountStatement.forAccount(externalTransaction.getAccountId())
                .withInternalTransactionId(externalTransaction.getInternalTransactionId())
                .withCashierName(externalTransaction.getCashierName())
                .withGameType(externalTransaction.getGameType())
                .withTransactionStatus(externalTransaction.getStatus())
                .withTimestamp(new DateTime())
                .withPurchaseCurrency(externalTransaction.getAmount().getCurrency())
                .withPurchaseAmount(externalTransaction.getAmount().getQuantity())
                .withChipsAmount(externalTransaction.getAmountChips())
                .asStatement();
    }

    private com.yazino.platform.audit.message.ExternalTransaction asWorkerTransaction(
            final ExternalTransaction externalTransaction) {
        return new com.yazino.platform.audit.message.ExternalTransaction(
                externalTransaction.getAccountId(),
                externalTransaction.getInternalTransactionId(),
                externalTransaction.getExternalTransactionId(),
                externalTransaction.getCreditCardObscuredMessage(),
                dateOf(externalTransaction),
                currencyFrom(externalTransaction.getAmount()),
                amountFrom(externalTransaction.getAmount()),
                externalTransaction.getAmountChips(),
                externalTransaction.getObscuredCreditCardNumber(),
                externalTransaction.getCashierName(),
                externalTransaction.getGameType(),
                ObjectUtils.toString(externalTransaction.getStatus()),
                externalTransaction.getTransactionLogType(),
                externalTransaction.getPlayerId(),
                externalTransaction.getPromoId(),
                externalTransaction.getPlatform(),
                externalTransaction.getPaymentOptionId(),
                currencyFrom(externalTransaction.getBaseCurrencyAmount()),
                amountFrom(externalTransaction.getBaseCurrencyAmount()),
                externalTransaction.getExchangeRate(),
                externalTransaction.getFailureReason());
    }

    private String currencyFrom(final Amount amount) {
        if (amount != null) {
            return amount.getCurrency().getCurrencyCode();
        }
        return null;
    }

    private BigDecimal amountFrom(final Amount amount) {
        if (amount != null) {
            return amount.getQuantity();
        }
        return null;
    }

    private Date dateOf(final ExternalTransaction externalTransaction) {
        if (externalTransaction.getMessageTimeStamp() != null) {
            return externalTransaction.getMessageTimeStamp().toDate();
        }
        return null;
    }
}
