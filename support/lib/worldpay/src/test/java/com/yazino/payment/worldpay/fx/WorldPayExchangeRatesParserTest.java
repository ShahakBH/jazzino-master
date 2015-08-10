package com.yazino.payment.worldpay.fx;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.DateTime;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;

import static com.google.common.collect.Sets.newHashSet;
import static com.yazino.payment.worldpay.fx.WorldPayExchangeRatesParserTest.ShallowCompanyExchangeRatesMatcher.equalToExcludingRates;
import static org.apache.commons.lang3.Validate.notNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class WorldPayExchangeRatesParserTest {
    private static final String TEST_FILE = "/com/yazino/payment/worldpay/fx/WorldPayExchangeRatesParserTest-testfile";
    private static final String BLANK_LINES = "020000001230813839055240813\n\n"
            + "490000002CHFSWISS FRANC                   0000012758722\n"
            + "490000003EUREURO                          0000010000000\n\n"
            + "490000004GBPSTERLING                      0000008856477\n"
            + "920000005003";
    private static final String OUT_OF_ORDER_RATE_RECORDS = "020000001230813839055240813\n"
            + "490000002CHFSWISS FRANC                   0000012758722\n"
            + "490000004EUREURO                          0000010000000\n"
            + "490000003GBPSTERLING                      0000008856477\n"
            + "920000005003";
    private static final String INVALID_RATE_COUNT = "020000001230813839055240813\n"
            + "490000002CHFSWISS FRANC                   0000012758722\n"
            + "490000003EUREURO                          0000010000000\n"
            + "490000004GBPSTERLING                      0000008856477\n"
            + "920000005004";
    private static final String OUT_OF_ORDER_HEADER_RECORDS = "020000002230813839055240813\n"
            + "490000002CHFSWISS FRANC                   0000012758722\n"
            + "920000003001";
    private static final String OUT_OF_ORDER_TRAILER_RECORDS = "020000001230813839055240813\n"
            + "490000002CHFSWISS FRANC                   0000012758722\n"
            + "920000004001";
    private static final String MISSING_HEADER = "490000001CHFSWISS FRANC                   0000012758722\n"
            + "920000002001";
    private static final String INVALID_RATE_CONTENT = "020000001230813839055240813\n"
            + "490000002CHFSWISS FRANC                   00000127aa722\n"
            + "920000003001";
    private static final String INVALID_RECORD_NUMBER_CONTENT = "020000001230813839055240813\n"
            + "4900aa002CHFSWISS FRANC                   0000012758722\n"
            + "920000003001";
    private static final String INVALID_COMPANY_NUMBER_CONTENT = "02000000123081383bb55240813\n"
            + "490000002CHFSWISS FRANC                   0000012758722\n"
            + "920000003001";
    private static final String INVALID_RECORD_NUMBER = "020000001230813839055240813\n"
            + "500000002CHFSWISS FRANC                   0000012758722\n"
            + "920000003001";
    private static final String INVALID_RECORD_ID_LENGTH = "0\n"
            + "490000002CHFSWISS FRANC                   0000012758722\n"
            + "920000003001";
    private static final String INVALID_AGREEMENT_DATE = "020000001239913839055240813\n"
            + "490000002CHFSWISS FRANC                   0000012758722\n"
            + "920000003001";
    private static final String INVALID_PROCESSING_DATE = "020000001230813839055990813\n"
            + "490000002CHFSWISS FRANC                   0000012758722\n"
            + "920000003001";
    private static final String INVALID_HEADER_LENGTH = "0200000012308139055240813\n"
            + "490000002CHFSWISS FRANC                   0000012758722\n"
            + "920000003001";
    private static final String INVALID_RATE_LENGTH = "020000001230813839055240813\n"
            + "4900002CHFSWISS FRANC                   0000012758722\n"
            + "920000003001";
    private static final String INVALID_TRAILER_LENGTH = "020000001230813839055240813\n"
            + "490000002CHFSWISS FRANC                   0000012758722\n"
            + "9200003001";

    private WorldPayExchangeRatesParser underTest = new WorldPayExchangeRatesParser();

    @Test(expected = NullPointerException.class)
    public void aNullStreamThrowsANullPointerException() throws IOException {
        underTest.parse(null);
    }

    @Test
    public void anEmptyStreamReturnsAbsent() throws IOException {
        final Optional<WorldPayExchangeRates> parsed = underTest.parse(new ByteArrayInputStream(new byte[0]));

        assertThat(parsed.isPresent(), is(false));
    }

    @Test(expected = ParseException.class)
    public void anInvalidStreamThrowsAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream("an invalid stream".getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithOutOfOrderExchangeRateRecordsCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(OUT_OF_ORDER_RATE_RECORDS.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithOutOfOrderHeaderRecordsCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(OUT_OF_ORDER_HEADER_RECORDS.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithOutOfOrderTrailerRecordsCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(OUT_OF_ORDER_TRAILER_RECORDS.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithAnInvalidNumberOfExchangeRateRecordsCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(INVALID_RATE_COUNT.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithAnInvalidHeaderLengthCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(INVALID_HEADER_LENGTH.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithAnInvalidExchangeRateLengthCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(INVALID_RATE_LENGTH.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithAnInvalidTrailerLengthCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(INVALID_TRAILER_LENGTH.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithAnInvalidAgreementDateCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(INVALID_AGREEMENT_DATE.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithAnInvalidProcessingDateCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(INVALID_PROCESSING_DATE.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithAnInvalidRecordNumberCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(INVALID_RECORD_NUMBER.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithAnInvalidRecordIdLengthCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(INVALID_RECORD_ID_LENGTH.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithAnInvalidRecordNumberContentCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(INVALID_RECORD_NUMBER_CONTENT.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithAnInvalidCompanyNumberContentCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(INVALID_COMPANY_NUMBER_CONTENT.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithAnInvalidExchangeRateNumberContentCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(INVALID_RATE_CONTENT.getBytes(Charsets.UTF_8)));
    }

    @Test(expected = ParseException.class)
    public void aStreamWithAMissingHeaderCausesAParseException() throws IOException {
        underTest.parse(new ByteArrayInputStream(MISSING_HEADER.getBytes(Charsets.UTF_8)));
    }

    @Test
    public void theParsedRatesContainsAllCompanyRecords() throws IOException {
        final Optional<WorldPayExchangeRates> parsed = underTest.parse(getClass().getResourceAsStream(TEST_FILE));

        assertThat(parsed.isPresent(), is(true));
        assertThat(parsed.get().getCompanyExchangeRates().size(), is(equalTo(4)));
        assertThat(parsed.get().getCompanyExchangeRates(), hasItems(
                equalToExcludingRates(companyWith(707816, date(23, 8, 2013), date(24, 8, 2013))),
                equalToExcludingRates(companyWith(827920, date(23, 8, 2013), date(24, 8, 2013))),
                equalToExcludingRates(companyWith(832202, date(23, 8, 2013), date(24, 8, 2013))),
                equalToExcludingRates(companyWith(839055, date(23, 8, 2013), date(24, 8, 2013)))));
    }

    @Test
    public void theParsedRatesContainsAllExchangeRatesForTheFirstCompanyInTheFile() throws IOException {
        final Optional<WorldPayExchangeRates> parsed = underTest.parse(getClass().getResourceAsStream(TEST_FILE));

        assertThat(parsed.isPresent(), is(true));
        assertThat(parsed.get().exchangeRatesFor(707816), is(not(equalTo(Optional.<CompanyExchangeRates>absent()))));
        assertThat(parsed.get().exchangeRatesFor(707816).get(), is(equalTo(
                companyWith(707816, date(23, 8, 2013), date(24, 8, 2013),
                        new ExchangeRate("AED", "UAE DIRHAM", new BigDecimal("5.9435916")),
                        new ExchangeRate("AUD", "AUSTRALIAN DOLLAR", new BigDecimal("1.8023265")),
                        new ExchangeRate("BRL", "BRAZILIAN REAL", new BigDecimal("3.9677974")),
                        new ExchangeRate("CAD", "CANADIAN DOLLAR", new BigDecimal("1.6986592")),
                        new ExchangeRate("CHF", "SWISS FRANC", new BigDecimal("1.4967136")),
                        new ExchangeRate("CNY", "CHINESE YUAN RENMINB", new BigDecimal("9.9824950")),
                        new ExchangeRate("DKK", "DANISH KRONE", new BigDecimal("9.0467744")),
                        new ExchangeRate("EUR", "EURO", new BigDecimal("1.2126666")),
                        new ExchangeRate("GBP", "STERLING", new BigDecimal("1.0000000")),
                        new ExchangeRate("HKD", "HONG KONG DOLLAR", new BigDecimal("12.5542036")),
                        new ExchangeRate("JPY", "YEN", new BigDecimal("159.5350185")),
                        new ExchangeRate("KRW", "KOREAN WON", new BigDecimal("1815.0624951")),
                        new ExchangeRate("KWD", "KUWAITI DINAR", new BigDecimal("0.4608789")),
                        new ExchangeRate("NOK", "NORWEGIAN KRONE", new BigDecimal("9.8455153")),
                        new ExchangeRate("NZD", "NEW ZEALAND DOLLAR", new BigDecimal("2.0703110")),
                        new ExchangeRate("QAR", "QATARI RIAL", new BigDecimal("5.8934625")),
                        new ExchangeRate("RUB", "RUSSIAN ROUBLE", new BigDecimal("53.7611532")),
                        new ExchangeRate("SAR", "SAUDI RIYAL", new BigDecimal("6.0686972")),
                        new ExchangeRate("SEK", "SWEDISH KRONA", new BigDecimal("10.5562057")),
                        new ExchangeRate("SGD", "SINGAPORE DOLLAR", new BigDecimal("2.0787760")),
                        new ExchangeRate("USD", "US DOLLAR", new BigDecimal("1.6182780")),
                        new ExchangeRate("ZAR", "RAND", new BigDecimal("16.7644057"))))));
    }

    @Test
    public void theParsedRatesContainsAllExchangeRatesForAllCompanies() throws IOException {
        final Optional<WorldPayExchangeRates> parsed = underTest.parse(getClass().getResourceAsStream(TEST_FILE));

        assertThat(parsed.isPresent(), is(true));
        assertThat(parsed.get().getCompanyExchangeRates(), hasItems(
                companyWith(707816, date(23, 8, 2013), date(24, 8, 2013),
                        new ExchangeRate("AED", "UAE DIRHAM", new BigDecimal("5.9435916")),
                        new ExchangeRate("AUD", "AUSTRALIAN DOLLAR", new BigDecimal("1.8023265")),
                        new ExchangeRate("BRL", "BRAZILIAN REAL", new BigDecimal("3.9677974")),
                        new ExchangeRate("CAD", "CANADIAN DOLLAR", new BigDecimal("1.6986592")),
                        new ExchangeRate("CHF", "SWISS FRANC", new BigDecimal("1.4967136")),
                        new ExchangeRate("CNY", "CHINESE YUAN RENMINB", new BigDecimal("9.9824950")),
                        new ExchangeRate("DKK", "DANISH KRONE", new BigDecimal("9.0467744")),
                        new ExchangeRate("EUR", "EURO", new BigDecimal("1.2126666")),
                        new ExchangeRate("GBP", "STERLING", new BigDecimal("1.0000000")),
                        new ExchangeRate("HKD", "HONG KONG DOLLAR", new BigDecimal("12.5542036")),
                        new ExchangeRate("JPY", "YEN", new BigDecimal("159.5350185")),
                        new ExchangeRate("KRW", "KOREAN WON", new BigDecimal("1815.0624951")),
                        new ExchangeRate("KWD", "KUWAITI DINAR", new BigDecimal("0.4608789")),
                        new ExchangeRate("NOK", "NORWEGIAN KRONE", new BigDecimal("9.8455153")),
                        new ExchangeRate("NZD", "NEW ZEALAND DOLLAR", new BigDecimal("2.0703110")),
                        new ExchangeRate("QAR", "QATARI RIAL", new BigDecimal("5.8934625")),
                        new ExchangeRate("RUB", "RUSSIAN ROUBLE", new BigDecimal("53.7611532")),
                        new ExchangeRate("SAR", "SAUDI RIYAL", new BigDecimal("6.0686972")),
                        new ExchangeRate("SEK", "SWEDISH KRONA", new BigDecimal("10.5562057")),
                        new ExchangeRate("SGD", "SINGAPORE DOLLAR", new BigDecimal("2.0787760")),
                        new ExchangeRate("USD", "US DOLLAR", new BigDecimal("1.6182780")),
                        new ExchangeRate("ZAR", "RAND", new BigDecimal("16.7644057"))),
                companyWith(827920, date(23, 8, 2013), date(24, 8, 2013),
                        new ExchangeRate("AUD", "AUSTRALIAN DOLLAR", new BigDecimal("1.7909526")),
                        new ExchangeRate("CAD", "CANADIAN DOLLAR", new BigDecimal("1.6879395")),
                        new ExchangeRate("EUR", "EURO", new BigDecimal("1.2050138")),
                        new ExchangeRate("GBP", "STERLING", new BigDecimal("1.0000000")),
                        new ExchangeRate("JPY", "YEN", new BigDecimal("158.5282441")),
                        new ExchangeRate("USD", "US DOLLAR", new BigDecimal("1.6080656"))),
                companyWith(832202, date(23, 8, 2013), date(24, 8, 2013),
                        new ExchangeRate("AUD", "AUSTRALIAN DOLLAR", new BigDecimal("1.8198249")),
                        new ExchangeRate("CAD", "CANADIAN DOLLAR", new BigDecimal("1.7151510")),
                        new ExchangeRate("EUR", "EURO", new BigDecimal("1.2244400")),
                        new ExchangeRate("GBP", "STERLING", new BigDecimal("1.0000000")),
                        new ExchangeRate("JPY", "YEN", new BigDecimal("161.0839022")),
                        new ExchangeRate("USD", "US DOLLAR", new BigDecimal("1.6339894"))),
                companyWith(839055, date(23, 8, 2013), date(24, 8, 2013),
                        new ExchangeRate("CHF", "SWISS FRANC", new BigDecimal("1.2758722")),
                        new ExchangeRate("EUR", "EURO", new BigDecimal("1.0000000")),
                        new ExchangeRate("GBP", "STERLING", new BigDecimal("0.8856477")),
                        new ExchangeRate("USD", "US DOLLAR", new BigDecimal("1.3795833")))));
    }

    @Test
    public void blankLinesAreIgnoredWhenParsing() throws IOException {
        final Optional<WorldPayExchangeRates> parsed = underTest.parse(new ByteArrayInputStream(BLANK_LINES.getBytes(Charsets.UTF_8)));

        assertThat(parsed.isPresent(), is(true));
        assertThat(parsed.get().getCompanyExchangeRates(), hasItems(
                companyWith(839055, date(23, 8, 2013), date(24, 8, 2013),
                        new ExchangeRate("CHF", "SWISS FRANC", new BigDecimal("1.2758722")),
                        new ExchangeRate("EUR", "EURO", new BigDecimal("1.0000000")),
                        new ExchangeRate("GBP", "STERLING", new BigDecimal("0.8856477")))));
    }

    private DateTime date(final int day,
                          final int month,
                          final int year) {
        return new DateTime(year, month, day, 0, 0, 0, 0);
    }

    private CompanyExchangeRates companyWith(final long companyId,
                                             final DateTime processingDate,
                                             final DateTime agreementDate,
                                             final ExchangeRate... exchangeRates) {
        return new CompanyExchangeRates(companyId, processingDate, agreementDate, newHashSet(exchangeRates));
    }

    static class ShallowCompanyExchangeRatesMatcher extends TypeSafeMatcher<CompanyExchangeRates> {
        private final CompanyExchangeRates expected;

        private ShallowCompanyExchangeRatesMatcher(final CompanyExchangeRates expected) {
            notNull(expected, "expected may not be null");

            this.expected = expected;
        }

        public static ShallowCompanyExchangeRatesMatcher equalToExcludingRates(final CompanyExchangeRates companyExchangeRates) {
            return new ShallowCompanyExchangeRatesMatcher(companyExchangeRates);
        }

        @Override
        protected boolean matchesSafely(final CompanyExchangeRates actual) {
            if (actual == expected) {
                return true;
            } else if (expected == null) {
                return false;
            }
            return new EqualsBuilder()
                    .append(expected.getCompanyNumber(), actual.getCompanyNumber())
                    .append(expected.getAgreementDate(), actual.getAgreementDate())
                    .append(expected.getProcessingDate(), actual.getProcessingDate())
                    .isEquals();
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText(" is equal to ").appendValue(expected);
        }
    }

}
