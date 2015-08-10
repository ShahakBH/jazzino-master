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
import com.yazino.platform.account.ExternalTransactionType;
import com.yazino.platform.model.account.PaymentSettlement;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Currency;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorldPayPaymentSettlementProcessorTest {
    private static final BigDecimal PRICE = new BigDecimal("56.78");
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(200);
    private static final BigDecimal CHIPS = BigDecimal.valueOf(2000);
    private static final String EXTERNAL_TRANSACTION_ID = "00000000001";

    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private STLink stLink;

    private WorldPayPaymentSettlementProcessor underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(67000000000L);

        when(stLink.send(aDepositMessage())).thenReturn(aSuccessfulDepositResponse());
        when(stLink.send(aTestDepositMessage())).thenReturn(aSuccessfulDepositResponse());
        when(stLink.send(aCancellationMessage())).thenReturn(aSuccessfulCancellationResponse());
        when(stLink.send(aTestCancellationMessage())).thenReturn(aSuccessfulCancellationResponse());

        underTest = new WorldPayPaymentSettlementProcessor(yazinoConfiguration, stLink);
    }

    @After
    public void resetTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = NullPointerException.class)
    public void theProcessorCannotBeCreatedWithANullYazinoConfiguration() {
        new WorldPayPaymentSettlementProcessor(null, stLink);
    }

    @Test(expected = NullPointerException.class)
    public void theProcessorCannotBeCreatedWithANullSTLink() {
        new WorldPayPaymentSettlementProcessor(yazinoConfiguration, null);
    }

    @Test(expected = NullPointerException.class)
    public void settlingANullSettlementThrowsANullPointerException() {
        underTest.settle(null);
    }

    @Test(expected = NullPointerException.class)
    public void cancellingANullSettlementThrowsANullPointerException() {
        underTest.cancel(null);
    }

    @Test
    public void settlingSendsASettlementRequestToSTLink() {
        underTest.settle(aPaymentSettlement());

        verify(stLink).send(aDepositMessage());
    }

    @Test
    public void cancellingSendsACancellationRequestToSTLink() {
        underTest.cancel(aPaymentSettlement());

        verify(stLink).send(aCancellationMessage());
    }

    @Test
    public void settlingSendsATestSettlementRequestToSTLinkWhenTestIsActive() {
        when(yazinoConfiguration.getBoolean("payment.worldpay.stlink.testmode", false)).thenReturn(true);

        underTest.settle(aPaymentSettlement());

        verify(stLink).send(aTestDepositMessage());
    }

    @Test
    public void cancellingSendsATestCancellationRequestToSTLinkWhenTestIsActive() {
        when(yazinoConfiguration.getBoolean("payment.worldpay.stlink.testmode", false)).thenReturn(true);

        underTest.settle(aPaymentSettlement());

        verify(stLink).send(aTestDepositMessage());
    }

    @Test
    public void aSuccessfulSettlementReturnsASettledExternalTransaction() {
        final ExternalTransaction externalTransaction = underTest.settle(aPaymentSettlement());

        assertThat(externalTransaction, is(equalTo(aSettledExternalTransaction())));
    }

    @Test
    public void aSuccessfulCancellationReturnsACancellationExternalTransaction() {
        final ExternalTransaction externalTransaction = underTest.cancel(aPaymentSettlement());

        assertThat(externalTransaction, is(equalTo(aCancelledExternalTransaction())));
    }

    @Test
    public void anUnsuccessfulSettlementReturnsAFailedExternalTransaction() {
        reset(stLink);
        when(stLink.send(aDepositMessage())).thenReturn(anUnsuccessfulResponse());

        final ExternalTransaction externalTransaction = underTest.settle(aPaymentSettlement());

        assertThat(externalTransaction, is(equalTo(aFailedExternalTransaction())));
    }

    @Test
    public void anUnsuccessfulCancellationReturnsAFailedExternalTransaction() {
        reset(stLink);
        when(stLink.send(aCancellationMessage())).thenReturn(anUnsuccessfulResponse());

        final ExternalTransaction externalTransaction = underTest.cancel(aPaymentSettlement());

        assertThat(externalTransaction, is(equalTo(aFailedExternalTransaction())));
    }

    @Test
    public void aSettlementThatCausesAnExceptionFromSTLinkReturnsAFailedExternalTransaction() {
        final RuntimeException testException = new RuntimeException("aTestException");
        reset(stLink);
        when(stLink.send(aDepositMessage())).thenThrow(testException);

        final ExternalTransaction externalTransaction = underTest.settle(aPaymentSettlement());

        assertThat(externalTransaction, is(equalTo(anExceptionExternalTransactionFor(testException))));
    }

    @Test
    public void aCancellationThatCausesAnExceptionFromSTLinkReturnsAFailedExternalTransaction() {
        final RuntimeException testException = new RuntimeException("aTestException");
        reset(stLink);
        when(stLink.send(aCancellationMessage())).thenThrow(testException);

        final ExternalTransaction externalTransaction = underTest.cancel(aPaymentSettlement());

        assertThat(externalTransaction, is(equalTo(anExceptionExternalTransactionFor(testException))));
    }

    private ExternalTransaction anExceptionExternalTransactionFor(final Exception e) {
        return anExternalTransactionBuilderFor()
                .withMessage(dump(e), new DateTime())
                .withStatus(ExternalTransactionStatus.ERROR)
                .withFailureReason(e.getMessage())
                .build();
    }

    private ExternalTransaction aFailedExternalTransaction() {
        return anExternalTransactionBuilderFor()
                .withMessage(format("Req=aRequest;Res=%s", anUnsuccessfulResponse().getResponseString()), new DateTime())
                .withStatus(ExternalTransactionStatus.FAILURE)
                .build();
    }

    private ExternalTransaction aSettledExternalTransaction() {
        return anExternalTransactionBuilderFor()
                .withMessage(format("Req=aRequest;Res=%s", aSuccessfulDepositResponse().getResponseString()), new DateTime())
                .withStatus(ExternalTransactionStatus.SETTLED)
                .build();
    }

    private ExternalTransaction aCancelledExternalTransaction() {
        return anExternalTransactionBuilderFor()
                .withMessage(format("Req=aRequest;Res=%s", aSuccessfulCancellationResponse().getResponseString()), new DateTime())
                .withStatus(ExternalTransactionStatus.CANCELLED)
                .build();
    }

    private ExternalTransactionBuilder anExternalTransactionBuilderFor() {
        return ExternalTransaction.newExternalTransaction(ACCOUNT_ID)
                .withAmount(Currency.getInstance("GBP"), PRICE)
                .withCashierName("WorldPay")
                .withCreditCardNumber("anAccountNumber")
                .withExternalTransactionId(EXTERNAL_TRANSACTION_ID)
                .withInternalTransactionId("anInternalTransactionId")
                .withPaymentOption(CHIPS, null)
                .withPlayerId(PLAYER_ID)
                .withType(ExternalTransactionType.DEPOSIT);
    }

    private NVPResponse aSuccessfulDepositResponse() {
        return new NVPResponse("aRequest", format("PTTID^%s~OrderNumber^%s~MessageCode^%d",
                EXTERNAL_TRANSACTION_ID, "anInternalTransactionId", MessageCode.APPROVED.getCode()));
    }

    private NVPResponse aSuccessfulCancellationResponse() {
        return new NVPResponse("aRequest", format("PTTID^%s~OrderNumber^%s~MessageCode^%d",
                EXTERNAL_TRANSACTION_ID, "anInternalTransactionId", MessageCode.CANCELLED_SUCCESSFULLY_BY_REQUEST.getCode()));
    }

    private NVPResponse anUnsuccessfulResponse() {
        return new NVPResponse("aRequest", format("PTTID^%s~OrderNumber^%s~MessageCode^%d",
                EXTERNAL_TRANSACTION_ID, "anInternalTransactionId", MessageCode.SYSTEM_ERROR.getCode()));
    }

    private NVPMessage aTestDepositMessage() {
        return aDepositMessage().withValue("IsTest", 1);
    }

    private NVPMessage aTestCancellationMessage() {
        return aCancellationMessage().withValue("IsTest", 1);
    }

    private NVPMessage aDepositMessage() {
        return new PaymentTrustDepositMessage()
                .withValue("OrderNumber", "anInternalTransactionId")
                .withValue("PTTID", EXTERNAL_TRANSACTION_ID)
                .withValue("CurrencyId", 826)
                .withValue("Amount", PRICE.toPlainString());
    }

    private NVPMessage aCancellationMessage() {
        return new PaymentTrustCancellationMessage()
                .withValue("OrderNumber", "anInternalTransactionId")
                .withValue("PTTID", EXTERNAL_TRANSACTION_ID);
    }

    private PaymentSettlement aPaymentSettlement() {
        return PaymentSettlement.newSettlement("anInternalTransactionId",
                EXTERNAL_TRANSACTION_ID,
                PLAYER_ID,
                ACCOUNT_ID,
                "WorldPay",
                new DateTime(),
                "anAccountNumber",
                PRICE,
                Currency.getInstance("GBP"),
                CHIPS,
                ExternalTransactionType.DEPOSIT)
                .build();
    }

    private String dump(final Throwable exception) {
        final StringWriter exceptionOutput = new StringWriter();
        exception.printStackTrace(new PrintWriter(exceptionOutput));
        return exceptionOutput.toString();
    }
}
