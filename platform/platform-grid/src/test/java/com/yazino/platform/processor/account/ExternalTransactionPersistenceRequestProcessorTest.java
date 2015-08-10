package com.yazino.platform.processor.account;

import com.yazino.platform.Platform;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.ExternalTransactionBuilder;
import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.account.ExternalTransactionType;
import com.yazino.platform.audit.AuditService;
import com.yazino.platform.model.account.AccountStatement;
import com.yazino.platform.model.account.ExternalTransactionPersistenceRequest;
import com.yazino.platform.model.account.PaymentSettlement;
import com.yazino.platform.persistence.account.JDBCAccountStatementDAO;
import com.yazino.platform.persistence.account.JDBCPaymentSettlementDAO;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Currency;
import java.util.concurrent.atomic.AtomicLong;

import static com.yazino.platform.model.account.PaymentSettlement.newSettlement;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExternalTransactionPersistenceRequestProcessorTest {
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(100);
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(99);

    @Mock
    private AuditService auditService;
    @Mock
    private JDBCAccountStatementDAO accountStatementDao;
    @Mock
    private JDBCPaymentSettlementDAO paymentSettlementDao;

    private String localhost;

    private ExternalTransactionPersistenceRequestProcessor underTest;

    @Before
    public void setUp() throws UnknownHostException {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(100000);

        localhost = InetAddress.getLocalHost().getHostAddress();

        underTest = new ExternalTransactionPersistenceRequestProcessor(auditService, accountStatementDao, paymentSettlementDao);
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void theProcessorCannotBeCreatedWithANullAuditService() {
        new ExternalTransactionPersistenceRequestProcessor(null, accountStatementDao, paymentSettlementDao);
    }

    @Test(expected = NullPointerException.class)
    public void theProcessorCannotBeCreatedWithANullAccountStatementDAO() {
        new ExternalTransactionPersistenceRequestProcessor(auditService, null, paymentSettlementDao);
    }

    @Test(expected = NullPointerException.class)
    public void theProcessorCannotBeCreatedWithANullPaymentSettlementDAO() {
        new ExternalTransactionPersistenceRequestProcessor(auditService, accountStatementDao, null);
    }

    @Test(expected = IllegalStateException.class)
    public void theProcessorCannotUsedWhenInitialisedViaTheProxyConstructor() {
        new ExternalTransactionPersistenceRequestProcessor().process(asRequest(anExternalTransaction()));
    }

    @Test
    public void aNullRequestIsIgnored() {
        underTest.process(null);

        verifyZeroInteractions(auditService, accountStatementDao);
    }

    @Test
    public void aRequestWithANullExternalTransactionIsIgnored() {
        underTest.process(new ExternalTransactionPersistenceRequest(null));

        verifyZeroInteractions(auditService, accountStatementDao);
    }

    @Test
    public void aNonAuthorisedRequestIsNotPassedToThePaymentSettlementDAO() {
        underTest.process(asRequest(anExternalTransaction()));

        verifyZeroInteractions(paymentSettlementDao);
    }

    @Test
    public void anAuthorisedRequestIsPassedToThePaymentSettlementDAO() {
        final ExternalTransaction externalTransaction = anExternalTransactionBuilder("anInternalTransaction")
                .withStatus(ExternalTransactionStatus.AUTHORISED)
                .withExternalTransactionId("anExternalTransactionId")
                .build();

        underTest.process(asRequest(externalTransaction));

        verify(paymentSettlementDao).save(asSettlement(externalTransaction));
    }

    @Test
    public void requestIsCorrectlyPassedToTheAuditService() {
        underTest.process(asRequest(anExternalTransaction()));

        verify(auditService, times(1)).externalTransactionProcessed(toWorkerTransaction(anExternalTransaction()));
    }

    @Test
    public void aRequestWithABlankInternalTransactionIdHasANewIdGenerated() {
        resetAtomicIdOn(underTest);

        underTest.process(asRequest(anExternalTransactionWithId("")));

        final String dateTime = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssSSS").print(
                new DateTime(DateTimeZone.forID("Europe/London")));
        verify(auditService, times(1)).externalTransactionProcessed(toWorkerTransaction(anExternalTransactionWithId(
                String.format("int_%s_%s_%s_%d", ACCOUNT_ID, dateTime, localhost, 1))));
    }


    @Test
    public void aRequestWithANullInternalTransactionIdHasANewIdGenerated() {
        resetAtomicIdOn(underTest);

        underTest.process(asRequest(anExternalTransactionWithId("")));

        final String dateTime = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssSSS").print(
                new DateTime(DateTimeZone.forID("Europe/London")));
        verify(auditService, times(1)).externalTransactionProcessed(toWorkerTransaction(anExternalTransactionWithId(
                String.format("int_%s_%s_%s_%d", ACCOUNT_ID, dateTime, localhost, 1))));
    }

    @Test
    public void requestIsCorrectlyPassedToTheAccountStatementDAO() {
        underTest.process(asRequest(anExternalTransaction()));

        verify(accountStatementDao, times(1)).save(asStatement(anExternalTransaction()));
    }

    @Test
    public void processingExceptionsFromAuditingAreSwallowed() {
        doThrow(new RuntimeException("Catch Me")).when(auditService).externalTransactionProcessed(
                toWorkerTransaction(anExternalTransaction()));

        try {
            underTest.process(asRequest(anExternalTransaction()));
        } catch (Throwable t) {
            fail("Exceptions should never propagate out of this processor.");
        }
    }

    @Test
    public void processingExceptionsFromAuditingDoNotAffectTheStatementDAO() {
        doThrow(new RuntimeException("Catch Me")).when(auditService).externalTransactionProcessed(
                toWorkerTransaction(anExternalTransaction()));

        underTest.process(asRequest(anExternalTransaction()));

        verify(accountStatementDao, times(1)).save(asStatement(anExternalTransaction()));
    }

    @Test
    public void processingExceptionsFromTheStatementDAOAreSwallowed() {
        doThrow(new RuntimeException("aTestException"))
                .when(accountStatementDao).save(asStatement(anExternalTransaction()));

        try {
            underTest.process(asRequest(anExternalTransaction()));
        } catch (Throwable t) {
            fail("Exceptions should never propagate out of this processor.");
        }
    }

    @Test
    public void processingExceptionsFromTheStatementDAODoNotAffectTheAuditService() {
        doThrow(new RuntimeException("aTestException"))
                .when(accountStatementDao).save(asStatement(anExternalTransaction()));

        underTest.process(asRequest(anExternalTransaction()));

        verify(auditService, times(1)).externalTransactionProcessed(toWorkerTransaction(anExternalTransaction()));
    }

    private com.yazino.platform.audit.message.ExternalTransaction toWorkerTransaction(
            final ExternalTransaction externalTransaction) {
        String baseCurrencyCode = null;
        BigDecimal baseCurrencyAmount = null;
        if (externalTransaction.getBaseCurrencyAmount() != null) {
            baseCurrencyCode = externalTransaction.getBaseCurrencyAmount().getCurrency().getCurrencyCode();
            baseCurrencyAmount = externalTransaction.getBaseCurrencyAmount().getQuantity();
        }
        return new com.yazino.platform.audit.message.ExternalTransaction(
                externalTransaction.getAccountId(),
                externalTransaction.getInternalTransactionId(),
                externalTransaction.getExternalTransactionId(),
                externalTransaction.getCreditCardObscuredMessage(),
                externalTransaction.getMessageTimeStamp().toDate(),
                externalTransaction.getAmount().getCurrency().getCurrencyCode(),
                externalTransaction.getAmount().getQuantity(),
                externalTransaction.getAmountChips(),
                externalTransaction.getObscuredCreditCardNumber(),
                externalTransaction.getCashierName(),
                externalTransaction.getGameType(),
                externalTransaction.getStatus().name(),
                externalTransaction.getTransactionLogType(),
                externalTransaction.getPlayerId(),
                externalTransaction.getPromoId(),
                externalTransaction.getPlatform(),
                externalTransaction.getPaymentOptionId(),
                baseCurrencyCode,
                baseCurrencyAmount,
                externalTransaction.getExchangeRate(),
                externalTransaction.getFailureReason());
    }

    private AccountStatement asStatement(final ExternalTransaction externalTransaction) {
        return AccountStatement.forAccount(externalTransaction.getAccountId())
                .withChipsAmount(externalTransaction.getAmountChips())
                .withPurchaseAmount(externalTransaction.getAmount().getQuantity())
                .withPurchaseCurrency(externalTransaction.getAmount().getCurrency())
                .withTransactionStatus(externalTransaction.getStatus())
                .withCashierName(externalTransaction.getCashierName())
                .withGameType(externalTransaction.getGameType())
                .withInternalTransactionId(externalTransaction.getInternalTransactionId())
                .withTimestamp(new DateTime())
                .asStatement();
    }

    private ExternalTransactionPersistenceRequest asRequest(final ExternalTransaction transaction) {
        return new ExternalTransactionPersistenceRequest(transaction);
    }

    private ExternalTransaction anExternalTransaction() {
        return anExternalTransactionWithId("I1");
    }

    private ExternalTransaction anExternalTransactionWithId(final String internalTransactionId) {
        return anExternalTransactionBuilder(internalTransactionId).build();
    }

    private ExternalTransactionBuilder anExternalTransactionBuilder(final String internalTransactionId) {
        return ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withInternalTransactionId(internalTransactionId)
                .withExternalTransactionId(null)
                .withMessage("<xml>hello</xml>", new DateTime())
                .withAmount(Currency.getInstance("USD"), new BigDecimal("10.00"))
                .withPaymentOption(BigDecimal.ONE, null)
                .withCreditCardNumber("4200XXXXXXXX0000")
                .withCashierName("Wirecard")
                .withStatus(ExternalTransactionStatus.SUCCESS)
                .withType(ExternalTransactionType.DEPOSIT)
                .withGameType("ROULETTE")
                .withPlayerId(PLAYER_ID)
                .withPromotionId(123l)
                .withPlatform(Platform.WEB);
    }

    private PaymentSettlement asSettlement(final ExternalTransaction externalTransaction) {
        return newSettlement(
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
                .withPromotionId(externalTransaction.getPromoId())
                .build();
    }

    private void resetAtomicIdOn(final ExternalTransactionPersistenceRequestProcessor object) {
        ((AtomicLong) ReflectionTestUtils.getField(object, "ATOMIC_LONG")).set(1);
    }
}
