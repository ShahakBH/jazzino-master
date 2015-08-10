package com.yazino.bi.payment.worldpay;

import com.google.common.base.Optional;
import com.yazino.bi.payment.persistence.JDBCPaymentFXDAO;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.payment.worldpay.fx.CompanyExchangeRates;
import com.yazino.payment.worldpay.fx.ExchangeRate;
import com.yazino.payment.worldpay.fx.WorldPayExchangeRates;
import com.yazino.payment.worldpay.fx.WorldPayExchangeRatesParser;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Currency;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WorldPayForexUpdaterTest {

    @Mock
    private WorldPayFileServer fileServer;
    @Mock
    private WorldPayExchangeRatesParser parser;
    @Mock
    private JDBCPaymentFXDAO paymentFXDao;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private WorldPayForexUpdater underTest;

    @Before
    public void setUp() throws IOException {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2013, 1, 1, 9, 0, 0, 0).getMillis());

        when(yazinoConfiguration.getBoolean("payment.worldpay.fx.update.active", true)).thenReturn(true);
        when(yazinoConfiguration.getList("payment.worldpay.fx.update.companies", emptyList())).thenReturn(asList((Object) "2", "3"));
        when(yazinoConfiguration.getList("payment.worldpay.fx.update.companies.2", emptyList())).thenReturn(asList((Object) "AUD"));
        when(yazinoConfiguration.getList("payment.worldpay.fx.update.companies.3", emptyList())).thenReturn(asList((Object) "CAD"));
        when(yazinoConfiguration.getList("payment.worldpay.fx.update.companies.4", emptyList())).thenReturn(asList((Object) "CAD"));
        when(parser.parse(any(InputStream.class))).thenReturn(Optional.fromNullable(worldPayExchangeRates()));

        underTest = new WorldPayForexUpdater(fileServer, parser, paymentFXDao, yazinoConfiguration);
    }

    @After
    public void resetJodaTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void whenUpdatesAreInactiveThenNoActionsTakePlace() {
        reset(yazinoConfiguration);

        underTest.updateExchangeRates();

        verifyZeroInteractions(fileServer, parser, paymentFXDao);
    }

    @Test
    public void fetchedFilesAreWrittenToATemporaryFile() throws IOException {
        when(fileServer.fetchTo(anyString(), anyString())).thenReturn(true);

        underTest.updateExchangeRates();

        final ArgumentCaptor<String> destFile = ArgumentCaptor.forClass(String.class);
        verify(fileServer).fetchTo(anyString(), destFile.capture());
        File tmpFile = File.createTempFile("wpfut", ".tmp");
        assertThat(destFile.getValue(), containsString(tmpFile.getParent()));
    }

    @Test
    public void anExceptionWhenFetchingFilesIsNotPropagated() throws WorldPayFileServerException {
        when(fileServer.fetchTo(anyString(), anyString())).thenThrow(new RuntimeException("aTestException"));

        underTest.updateExchangeRates();

        verifyZeroInteractions(parser, paymentFXDao);
    }

    @Test
    public void aFileContainingNoRatesIsIgnored() throws IOException {
        when(parser.parse(any(InputStream.class))).thenReturn(Optional.<WorldPayExchangeRates>absent());

        underTest.updateExchangeRates();

        verifyZeroInteractions(paymentFXDao);
    }

    @Test
    public void configuredCompaniesHaveTheConfiguredCurrenciesWrittenToTheDAO() throws WorldPayFileServerException {
        underTest.updateExchangeRates();

        verify(paymentFXDao).save(exchangeRateFor("AUD", "USD", "1.1009155"));
        verify(paymentFXDao).save(exchangeRateFor("CAD", "USD", "1.0734649"));
        verifyNoMoreInteractions(paymentFXDao);
    }

    @Test
    public void anInvalidCompanyIdDoesNotBreakParsingForOtherCompanies() throws WorldPayFileServerException {
        when(yazinoConfiguration.getList("payment.worldpay.fx.update.companies", emptyList())).thenReturn(asList((Object) "2C", "3"));

        underTest.updateExchangeRates();

        verify(paymentFXDao).save(exchangeRateFor("CAD", "USD", "1.0734649"));
        verifyNoMoreInteractions(paymentFXDao);
    }

    @Test
    public void aMissingConfiguredCurrencyForACompanyIsIgnored() throws WorldPayFileServerException {
        when(yazinoConfiguration.getList("payment.worldpay.fx.update.companies.2", emptyList())).thenReturn(asList((Object) "NZD"));

        underTest.updateExchangeRates();

        verify(paymentFXDao).save(exchangeRateFor("CAD", "USD", "1.0734649"));
        verifyNoMoreInteractions(paymentFXDao);
    }

    @Test
    public void aConfiguredCompanyWithNoBaseCurrencyIsNotProcessed() throws WorldPayFileServerException {
        when(yazinoConfiguration.getList("payment.worldpay.fx.update.companies", emptyList())).thenReturn(asList((Object) "4"));

        underTest.updateExchangeRates();

        verifyNoMoreInteractions(paymentFXDao);
    }

    @Test
    public void anAbsentConfiguredCompanyIsNotProcessed() throws WorldPayFileServerException {
        when(yazinoConfiguration.getList("payment.worldpay.fx.update.companies", emptyList())).thenReturn(asList((Object) "10"));

        underTest.updateExchangeRates();

        verifyNoMoreInteractions(paymentFXDao);
    }

    @Test
    public void aConfiguredCompanyWithNoConfiguredCurrenciesIsNotProcessed() throws WorldPayFileServerException {
        when(yazinoConfiguration.getList("payment.worldpay.fx.update.companies", emptyList())).thenReturn(asList((Object) "5"));

        underTest.updateExchangeRates();

        verifyNoMoreInteractions(paymentFXDao);
    }

    private com.yazino.bi.payment.ExchangeRate exchangeRateFor(final String currency,
                                                               final String baseCurrency,
                                                               final String rate) {
        return new com.yazino.bi.payment.ExchangeRate(Currency.getInstance(currency), Currency.getInstance(baseCurrency),
                new BigDecimal(rate), new DateTime().plusDays(2));
    }

    private WorldPayExchangeRates worldPayExchangeRates() {
        return new WorldPayExchangeRates(newHashSet(
                new CompanyExchangeRates(1, new DateTime(), new DateTime().plusDays(2), newHashSet(
                        new ExchangeRate("GBP", "Pound", new BigDecimal("1.00000"))
                )),
                new CompanyExchangeRates(2, new DateTime(), new DateTime().plusDays(2), newHashSet(
                        new ExchangeRate("USD", "Buck", new BigDecimal("1.00000")),
                        new ExchangeRate("AUD", "Aussie", new BigDecimal("1.1009155")),
                        new ExchangeRate("GBP", "Pound", new BigDecimal("0.6462969"))
                )),
                new CompanyExchangeRates(3, new DateTime(), new DateTime().plusDays(2), newHashSet(
                        new ExchangeRate("USD", "Buck", new BigDecimal("1.00000")),
                        new ExchangeRate("CAD", "Loonie", new BigDecimal("1.0734649")),
                        new ExchangeRate("GBP", "Pound", new BigDecimal("0.6462969"))
                )),
                new CompanyExchangeRates(4, new DateTime(), new DateTime().plusDays(2), newHashSet(
                        new ExchangeRate("CAD", "Loonie", new BigDecimal("1.0734649")),
                        new ExchangeRate("GBP", "Pound", new BigDecimal("0.6462969"))
                )),
                new CompanyExchangeRates(5, new DateTime(), new DateTime().plusDays(2), newHashSet(
                        new ExchangeRate("USD", "Buck", new BigDecimal("1.00000")),
                        new ExchangeRate("CAD", "Loonie", new BigDecimal("1.0734649"))
                ))
        )
        );
    }

}
