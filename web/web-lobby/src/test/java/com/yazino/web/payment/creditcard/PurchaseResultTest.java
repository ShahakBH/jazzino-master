package com.yazino.web.payment.creditcard;

import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PurchaseResultTest {
    private static final int CURRENT_TIME = 10000;

    private PurchaseResult underTest;

    @Before
    public void init() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(CURRENT_TIME);

        underTest = new PurchaseResult("merchant", PurchaseOutcome.APPROVED, "email", "message",
                Currency.getInstance("GBP"), BigDecimal.valueOf(1), BigDecimal.valueOf(2), "xxxx",
                "internalId", "externalId", "trace");
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void should_build_correct_argument_map() throws ClassNotFoundException, IOException {
        final DateTimeFormatter dateFormatter = DateTimeFormat.forPattern("dd/MM/yyyy");
        final String expectedDate = dateFormatter.print(CURRENT_TIME);
        final DateTimeFormatter timeFormatter = DateTimeFormat.forPattern("hh:mm:ss");
        final String expectedTime = timeFormatter.print(CURRENT_TIME);
        final Map<String, Object> args = underTest.buildArgumentMap();

        assertEquals(args.get("message"), underTest.getMessage());
        assertEquals(args.get("merchant"), underTest.getMerchant());
        assertEquals(args.get("result"), underTest.getOutcome().name());
        assertEquals(args.get("customerEmail"), underTest.getCustomerEmail());
        assertEquals(args.get("money"), underTest.getMoney());
        assertEquals(args.get("chips"), underTest.getChips());
        assertEquals(args.get("currencyCode"), underTest.getCurrency().getCurrencyCode());
        assertEquals(args.get("internalTransactionId"), underTest.getInternalTransactionId());
        assertEquals(args.get("externalTransactionId"), underTest.getExternalTransactionId());
        assertEquals(args.get("cardNumberObscured"), underTest.getCardNumberObscured());
        assertEquals(args.get("trace"), underTest.getTrace());
        assertEquals(args.get("date"), expectedDate);
        assertEquals(args.get("time"), expectedTime);
    }
}
