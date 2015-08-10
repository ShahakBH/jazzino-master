package com.yazino.platform.payment.settlement.worldpay;

import com.yazino.payment.worldpay.NVPResponse;
import com.yazino.payment.worldpay.STLink;
import com.yazino.payment.worldpay.nvp.PaymentTrustAuthorisationMessage;
import com.yazino.platform.account.ExternalTransaction;
import com.yazino.platform.account.ExternalTransactionStatus;
import com.yazino.platform.account.ExternalTransactionType;
import com.yazino.platform.model.account.PaymentSettlement;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.Currency;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class WorldPayPaymentSettlementProcessorExternalIntegrationTest {

    private static final String ACCOUNT_NUMBER = "4200000000000000";
    private static final BigDecimal PRICE = new BigDecimal("56.78");
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(100);
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(200);
    private static final BigDecimal CHIPS = BigDecimal.valueOf(2000);

    @Autowired
    private WorldPayPaymentSettlementProcessor underTest;
    @Autowired
    private STLink stLink;

    private String currentYear;

    @Before
    public void setUp() {
        currentYear = Integer.toString(new DateTime().getYear());
    }

    @Test
    public void aPaymentCanBeSettled() {
        final String internalTransactionId = "WPPSPEIT" + new DateTime().toString("ddMMyyHHmmSS");
        final NVPResponse authResponse = stLink.send(new PaymentTrustAuthorisationMessage()
                .withValue("IsTest", 1)
                .withValue("OrderNumber", internalTransactionId)
                .withValue("AcctNumber", ACCOUNT_NUMBER)
                .withValue("ExpDate", "12" + currentYear)
                .withValue("CurrencyId", 826)
                .withValue("Amount", PRICE));
        assertThat(authResponse.get("MessageCode").get(), is(anyOf(equalTo("2050"), equalTo("2100"))));

        final ExternalTransaction externalTransaction = underTest.settle(
                aSettlementFor(internalTransactionId, authResponse.get("PTTID").get()));

        assertThat(externalTransaction.getStatus(), is(equalTo(ExternalTransactionStatus.SETTLED)));
    }

    private PaymentSettlement aSettlementFor(final String internalTransactionId,
                                             final String externalTransactionId) {
        return PaymentSettlement.newSettlement(internalTransactionId,
                externalTransactionId,
                PLAYER_ID,
                ACCOUNT_ID,
                "WorldPay",
                new DateTime(),
                ACCOUNT_NUMBER,
                PRICE,
                Currency.getInstance("GBP"),
                CHIPS,
                ExternalTransactionType.DEPOSIT).build();
    }

}
