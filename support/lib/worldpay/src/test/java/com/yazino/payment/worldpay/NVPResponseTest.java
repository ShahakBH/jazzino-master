package com.yazino.payment.worldpay;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class NVPResponseTest {

    // message sample is from dev guide
    private static final String SAMPLE_RESPONSE = "MerchantId^100000~TransactionType^PT~OrderNumber^7603534872~StrId^1692785"
            + "~PTTID^43153507~MOP^CC~CurrencyId^840~Amount^56.78~RequestType^A~MessageCode^2100~Message^Transaction Approved~UTC^20110223164000";

    @Test
    public void aNullResponseIsEmpty() {
        assertThat(new NVPResponse("request", null).isEmpty(), is(true));
    }

    @Test
    public void aBlankResponseIsEmpty() {
        assertThat(new NVPResponse("request", "response").isEmpty(), is(true));
    }

    @Test
    public void theRawRequestCanBeRetrieved() {
        assertThat(new NVPResponse("request", "TestString^ABC").getRequestString(), is(equalTo("request")));
    }

    @Test
    public void theRawResponseCanBeRetrieved() {
        assertThat(new NVPResponse("request", "TestString^ABC").getResponseString(), is(equalTo("TestString^ABC")));
    }

    @Test
    public void aStringValueCanBeRetrievedFromTheParsedResponse() {
        assertThat(new NVPResponse("request", "TestString^ABC").get("TestString").get(), is(equalTo("ABC")));
    }

    @Test
    public void aMissingStringValueCanBeRetrievedFromTheParsedResponse() {
        assertThat(new NVPResponse("request", "TestString^ABC").get("AnotherString").isPresent(), is(false));
    }

    @Test
    public void aLongValueCanBeRetrievedFromTheParsedResponse() {
        assertThat(new NVPResponse("request", "TestLong^123").getLong("TestLong").get(), is(equalTo(123L)));
    }

    @Test(expected = NumberFormatException.class)
    public void aMalformedLongValueCausesANumberFormatException() {
        new NVPResponse("request", "TestLong^123A").getLong("TestLong");
    }

    @Test
    public void aMissingLongValueCanBeRetrievedFromTheParsedResponse() {
        assertThat(new NVPResponse("request", "TestLong^123").getLong("AnotherLong").isPresent(), is(false));
    }

    @Test
    public void aBigDecimalValueCanBeRetrievedFromTheParsedResponse() {
        assertThat(new NVPResponse("request", "TestBD^123.45").getBigDecimal("TestBD").get(), is(equalTo(new BigDecimal("123.45"))));
    }

    @Test
    public void aMissingBigDecimalValueCanBeRetrievedFromTheParsedResponse() {
        assertThat(new NVPResponse("request", "TestBD^123").getBigDecimal("AnotherBD").isPresent(), is(false));
    }

    @Test(expected = NumberFormatException.class)
    public void aMalformedBigDecimalValueCausesANumberFormatException() {
        new NVPResponse("request", "TestBD^123A").getBigDecimal("TestBD");
    }

    @Test
    public void aResponseIsCorrectlyParsed() {
        final NVPResponse response = new NVPResponse("request", SAMPLE_RESPONSE);

        assertThat(response.getLong("MerchantId").get(), is(equalTo(100000L)));
        assertThat(response.get("TransactionType").get(), is(equalTo("PT")));
        assertThat(response.getLong("OrderNumber").get(), is(equalTo(7603534872L)));
        assertThat(response.getLong("StrId").get(), is(equalTo(1692785L)));
        assertThat(response.getLong("PTTID").get(), is(equalTo(43153507L)));
        assertThat(response.get("MOP").get(), is(equalTo("CC")));
        assertThat(response.getLong("CurrencyId").get(), is(equalTo(840L)));
        assertThat(response.getBigDecimal("Amount").get(), is(equalTo(new BigDecimal("56.78"))));
        assertThat(response.get("RequestType").get(), is(equalTo("A")));
        assertThat(response.getLong("MessageCode").get(), is(equalTo(2100L)));
        assertThat(response.get("Message").get(), is(equalTo("Transaction Approved")));
        assertThat(response.getLong("UTC").get(), is(equalTo(20110223164000L)));
    }

    @Test
    public void malformedResponseFieldsAreSkipped() {
        final NVPResponse response = new NVPResponse("request", "Correct1^1~Incorrect2~Correct3^3~");

        assertThat(response.get("Correct1").get(), is(equalTo("1")));
        assertThat(response.get("Correct3").get(), is(equalTo("3")));
        assertThat(response.get("Incorrect2").isPresent(), is(false));
    }

    @Test
    public void aPopulatedResponseIsNotEmpty() {
        // message sample is from dev guide
        final NVPResponse response = new NVPResponse("request", "MerchantId^100000~TransactionType^PT~OrderNumber^7603534872~StrId^1692785"
                + "~ PTTID^43153507~MOP^CC~CurrencyId^840~Amount^56.78~RequestType^A~Message Code^2100~Message^Transaction Approved~UTC^20110223164000");

        assertThat(response.isEmpty(), is(false));
    }

}
