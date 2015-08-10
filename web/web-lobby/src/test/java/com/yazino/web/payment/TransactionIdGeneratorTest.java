package com.yazino.web.payment;

import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TimeZone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class TransactionIdGeneratorTest {

    @Before
    public void setUp() {
        final DateTimeZone zone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/London"));
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime("2009-11-07T17:00:00.123", zone).getMillis());
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testTransactionIdGeneration() throws UnknownHostException {
        final String localhost = InetAddress.getLocalHost().getHostAddress();

        final String actualId = new TransactionIdGenerator().generateTransactionId(BigDecimal.valueOf(1223344L));

        assertThat(actualId, is(equalTo(String.format("1223344_20091107T170000123_%s_0", localhost))));
    }
}
