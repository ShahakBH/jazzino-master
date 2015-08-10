package com.yazino.payment.worldpay.emis;

import com.google.common.base.Charsets;
import com.yazino.payment.worldpay.fx.ParseException;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class WorldPayChargebacksParserTest {

    private static final String VALID_FILE = "0,,,C  ,,,,3509,,,,180311,,,\n"
            + "5,3.54252E+22,549123******9900,MSC,15966663,6085553901,161210,20330,826,002WNBG1,No Signature,171210,D,20330,37\n"
            + "5,3.54252E+22,530124******8001,MSC,66466662,6124963761,140111,13780,826,002WNG8H,No Signature,150111,D,13780,37\n"
            + "5,3.54252E+22,543458******2203,MSC,66466662,6143438439,270111,23186,826,002WNBHC,No Signature,280111,D,23186,37\n"
            + "5,3.54252E+22,549113******0865,MSC,15966663,6139932705,250111,50092,826,002WNG8R,No Signature,260111,D,50092,37\n"
            + "5,4.54252E+22,552157******5211,MSC,36066662,6108919040,311210,350,826,002WNGD2,No Signature,50111,D,350,37\n"
            + "5,7.46786E+22,492942******9002,VIS,36066662,6169750010,120211,400,826,002WNGXX,Fraud - Card Absent Environment,160211,D,400,83\n"
            + "99,,,,,,,108138,,,,,,6,\n";
    private static final String VALID_FILE_WITH_BLANK_LINES = "0,,,C  ,,,,3509,,,,180311,,,\n"
            + "5,3.54252E+22,549123******9900,MSC,15966663,6085553901,161210,20330,826,002WNBG1,No Signature,171210,D,20330,37\n"
            + " \n"
            + "99,,,,,,,20330,,,,,,1,\n"
            + "   ";
    private static final String VALID_FILE_NO_RECORDS = "0,,,C  ,,,,3509,,,,180311,,,\n"
            + "99,,,,,,,0,,,,,,0,\n";
    private static final String INCORRECT_TYPE = "0,,,R  ,,,,3509,,,,180311,,,\n"
            + "5,3.54252E+22,549123******9900,MSC,15966663,6085553901,161210,20330,826,002WNBG1,No Signature,171210,D,20330,37\n"
            + "99,,,,,,,20330,,,,,,1,\n";
    private static final String INCORRECT_RECORD_COUNT = "0,,,C  ,,,,3509,,,,180311,,,\n"
            + "5,3.54252E+22,549123******9900,MSC,15966663,6085553901,161210,20330,826,002WNBG1,No Signature,171210,D,20330,37\n"
            + "99,,,,,,,20330,,,,,,2,\n";
    private static final String INCORRECT_TOTAL_AMOUNT = "0,,,C  ,,,,3509,,,,180311,,,\n"
            + "5,3.54252E+22,549123******9900,MSC,15966663,6085553901,161210,20330,826,002WNBG1,No Signature,171210,D,20330,37\n"
            + "99,,,,,,,20331,,,,,,1,\n";
    private static final String INCORRECT_RECORD_ID = "0,,,C  ,,,,3509,,,,180311,,,\n"
            + "6,3.54252E+22,549123******9900,MSC,15966663,6085553901,161210,20330,826,002WNBG1,No Signature,171210,D,20330,37\n"
            + "99,,,,,,,20330,,,,,,1,\n";
    private static final String MALFORMED_DATE = "0,,,C  ,,,,3509,,,,180311,,,\n"
            + "5,3.54252E+22,549123******9900,MSC,15966663,6085553901,161310,20330,826,002WNBG1,No Signature,171210,D,20330,37\n"
            + "99,,,,,,,20330,,,,,,1,\n";
    private static final String MALFORMED_AMOUNT = "0,,,C  ,,,,3509,,,,180311,,,\n"
            + "5,3.54252E+22,549123******9900,MSC,15966663,6085553901,161310,2033x,826,002WNBG1,No Signature,171210,D,20330,37\n"
            + "99,,,,,,,20330,,,,,,1,\n";
    private static final String MALFORMED_CURRENCY = "0,,,C  ,,,,3509,,,,180311,,,\n"
            + "5,3.54252E+22,549123******9900,MSC,15966663,6085553901,161310,2033x,GBP,002WNBG1,No Signature,171210,D,20330,37\n"
            + "99,,,,,,,20330,,,,,,1,\n";
    private static final String INVALID_CURRENCY = "0,,,C  ,,,,3509,,,,180311,,,\n"
            + "5,3.54252E+22,549123******9900,MSC,15966663,6085553901,161310,2033x,99999,002WNBG1,No Signature,171210,D,20330,37\n"
            + "99,,,,,,,20330,,,,,,1,\n";
    private static final String MALFORMED_CHARGEBACK = "0,,,C  ,,,,3509,,,,180311,,,\n"
            + "5,3.54252E+22,549123******9900,MSC,161210,20330,826,002WNBG1,No Signature,171210,D,20330,37\n"
            + "99,,,,,,,20330,,,,,,1,\n";
    private static final String MALFORMED_HEADER = "0,,,C  ,3509,,,,180311\n"
            + "5,3.54252E+22,549123******9900,MSC,15966663,6085553901,161210,20330,826,002WNBG1,No Signature,171210,D,20330,37\n"
            + "99,,,,,,,20330,,,,,,1,\n";
    private static final String MALFORMED_TRAILER = "0,,,C  ,,,,3509,,,,180311,,,\n"
            + "5,3.54252E+22,549123******9900,MSC,15966663,6085553901,161210,20330,826,002WNBG1,No Signature,171210,D,20330,37\n"
            + "99,,,,,20330,,,,,,1,\n";

    private WorldPayChargebacksParser underTest = new WorldPayChargebacksParser();

    @Test
    public void aValidFileIsParsedCorrectly() throws IOException {
        final WorldPayChargebacks parsed = underTest.parse(streamOf(VALID_FILE));

        assertThat(parsed, is(not(nullValue())));
        assertThat(parsed.getDate(), is(equalTo(new DateTime(2011, 3, 18, 0, 0, 0))));
        assertThat(parsed.getChargebacks().size(), is(equalTo(6)));
        assertThat(parsed.getChargebacks(), contains(
                new Chargeback(new BigDecimal("3.54252E+22"), "549123******9900", CardScheme.MASTERCARD, "15966663",
                        "6085553901", new DateTime(2010, 12, 16, 0, 0, 0), new BigDecimal("203.30"), Currency.getInstance("GBP"),
                        "002WNBG1", "No Signature", new DateTime(2010, 12, 17, 0, 0, 0), ChargebackTransactionType.SALE, new BigDecimal("203.30"), "37"),
                new Chargeback(new BigDecimal("3.54252E+22"), "530124******8001", CardScheme.MASTERCARD, "66466662",
                        "6124963761", new DateTime(2011, 1, 14, 0, 0, 0), new BigDecimal("137.80"), Currency.getInstance("GBP"),
                        "002WNG8H", "No Signature", new DateTime(2011, 1, 15, 0, 0, 0), ChargebackTransactionType.SALE, new BigDecimal("137.80"), "37"),
                new Chargeback(new BigDecimal("3.54252E+22"), "543458******2203", CardScheme.MASTERCARD, "66466662",
                        "6143438439", new DateTime(2011, 1, 27, 0, 0, 0), new BigDecimal("231.86"), Currency.getInstance("GBP"),
                        "002WNBHC", "No Signature", new DateTime(2011, 1, 28, 0, 0, 0), ChargebackTransactionType.SALE, new BigDecimal("231.86"), "37"),
                new Chargeback(new BigDecimal("3.54252E+22"), "549113******0865", CardScheme.MASTERCARD, "15966663",
                        "6139932705", new DateTime(2011, 1, 25, 0, 0, 0), new BigDecimal("500.92"), Currency.getInstance("GBP"),
                        "002WNG8R", "No Signature", new DateTime(2011, 1, 26, 0, 0), ChargebackTransactionType.SALE, new BigDecimal("500.92"), "37"),
                new Chargeback(new BigDecimal("4.54252E+22"), "552157******5211", CardScheme.MASTERCARD, "36066662",
                        "6108919040", new DateTime(2010, 12, 31, 0, 0, 0), new BigDecimal("3.50"), Currency.getInstance("GBP"),
                        "002WNGD2", "No Signature", new DateTime(2011, 1, 5, 0, 0, 0), ChargebackTransactionType.SALE, new BigDecimal("3.50"), "37"),
                new Chargeback(new BigDecimal("7.46786E+22"), "492942******9002", CardScheme.VISA, "36066662",
                        "6169750010", new DateTime(2011, 2, 12, 0, 0, 0), new BigDecimal("4.00"), Currency.getInstance("GBP"),
                        "002WNGXX", "Fraud - Card Absent Environment", new DateTime(2011, 2, 16, 0, 0, 0), ChargebackTransactionType.SALE, new BigDecimal("4.00"), "83")
        ));
    }

    @Test
    public void aValidFileWithBlankLinesIsParsedCorrectly() throws IOException {
        final WorldPayChargebacks parsed = underTest.parse(streamOf(VALID_FILE_WITH_BLANK_LINES));

        assertThat(parsed, is(not(nullValue())));
        assertThat(parsed.getDate(), is(equalTo(new DateTime(2011, 3, 18, 0, 0, 0))));
        assertThat(parsed.getChargebacks().size(), is(equalTo(1)));
        assertThat(parsed.getChargebacks(), contains(
                new Chargeback(new BigDecimal("3.54252E+22"), "549123******9900", CardScheme.MASTERCARD, "15966663",
                        "6085553901", new DateTime(2010, 12, 16, 0, 0, 0), new BigDecimal("203.30"), Currency.getInstance("GBP"),
                        "002WNBG1", "No Signature", new DateTime(2010, 12, 17, 0, 0, 0), ChargebackTransactionType.SALE, new BigDecimal("203.30"), "37")));
    }

    @Test
    public void aValidFileWithNoRecordsIsParsedCorrectly() throws IOException {
        final WorldPayChargebacks parsed = underTest.parse(streamOf(VALID_FILE_NO_RECORDS));

        assertThat(parsed, is(not(nullValue())));
        assertThat(parsed.getDate(), is(equalTo(new DateTime(2011, 3, 18, 0, 0, 0))));
        assertThat(parsed.getChargebacks().size(), is(equalTo(0)));
    }


    @Test(expected = ParseException.class)
    public void anEmptyFileThrowsAnIOException() throws IOException {
        underTest.parse(streamOf(""));
    }

    @Test(expected = ParseException.class)
    public void aNonChargebackFileIsRejected() throws IOException {
        underTest.parse(streamOf(INCORRECT_TYPE));
    }

    @Test(expected = ParseException.class)
    public void aFileWithAnIncorrectRecordCountIsRejected() throws IOException {
        underTest.parse(streamOf(INCORRECT_RECORD_COUNT));
    }

    @Test(expected = ParseException.class)
    public void aFileWithAnIncorrectTotalAmountIsRejected() throws IOException {
        underTest.parse(streamOf(INCORRECT_TOTAL_AMOUNT));
    }

    @Test(expected = ParseException.class)
    public void aFileWithAnIncorrectRecordIDIsRejected() throws IOException {
        underTest.parse(streamOf(INCORRECT_RECORD_ID));
    }

    @Test(expected = ParseException.class)
    public void aFileWithAMalformedHeaderIsRejected() throws IOException {
        underTest.parse(streamOf(MALFORMED_HEADER));
    }

    @Test(expected = ParseException.class)
    public void aFileWithAMalformedChargebackIsRejected() throws IOException {
        underTest.parse(streamOf(MALFORMED_CHARGEBACK));
    }

    @Test(expected = ParseException.class)
    public void aFileWithAMalformedTrailerIsRejected() throws IOException {
        underTest.parse(streamOf(MALFORMED_TRAILER));
    }

    @Test(expected = ParseException.class)
    public void aFileWithAMalformedDateIsRejected() throws IOException {
        underTest.parse(streamOf(MALFORMED_DATE));
    }

    @Test(expected = ParseException.class)
    public void aFileWithAMalformedAmountIsRejected() throws IOException {
        underTest.parse(streamOf(MALFORMED_AMOUNT));
    }

    @Test(expected = ParseException.class)
    public void aFileWithAMalformedCurrencyIsRejected() throws IOException {
        underTest.parse(streamOf(MALFORMED_CURRENCY));
    }

    @Test(expected = ParseException.class)
    public void aFileWithAnInvalidCurrencyIsRejected() throws IOException {
        underTest.parse(streamOf(INVALID_CURRENCY));
    }

    private ByteArrayInputStream streamOf(final String source) {
        return new ByteArrayInputStream(source.getBytes(Charsets.UTF_8));
    }

}
