package com.yazino.platform.account;

import com.yazino.platform.Platform;
import org.joda.time.DateTime;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.Assert.assertEquals;

public class ExternalTransactionTest {
	@Test
	public void testTransactionLogTypeIsGeneratedCorrectly() {
        ExternalTransaction externalTransaction = ExternalTransaction.newExternalTransaction(new BigDecimal(1))
                .withInternalTransactionId("1I")
                .withExternalTransactionId("1E")
                .withMessage("<xml>Hello</xml>", new DateTime())
                .withAmount(Currency.getInstance("USD"), new BigDecimal("10.00"))
                .withPaymentOption(new BigDecimal("10.00"), null)
                .withCreditCardNumber("4200XXXXXXXX0000")
                .withCashierName("Wirecard")
                .withStatus(ExternalTransactionStatus.REQUEST)
                .withType(ExternalTransactionType.REVERSAL)
                .withGameType("ROULETTE")
                .withPlayerId(BigDecimal.TEN)
                .withPromotionId(null)
                .withPlatform(Platform.WEB)
                .build();
        assertEquals("Wirecard Reversal", externalTransaction.getTransactionLogType());
    }
}
