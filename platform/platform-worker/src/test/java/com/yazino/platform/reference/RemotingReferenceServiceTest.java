package com.yazino.platform.reference;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.NoSuchElementException;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RemotingReferenceServiceTest {
    private static final String DEFAULT_CURRENCY = "CAD";

    @Mock
    private JDBCCountryRepository countryRepository;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private static final Set<Country> COUNTRIES = newHashSet(
            new Country("GB", "United Kingdom", "GBP"),
            new Country("AU", "Australia", "AUD"),
            new Country("US", "United States", "USDx"));

    private RemotingReferenceService underTest;

    @Before
    public void setUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis());

        underTest = new RemotingReferenceService(countryRepository, yazinoConfiguration);

        when(countryRepository.getCountries()).thenReturn(COUNTRIES);
        when(yazinoConfiguration.getString("payments.currency.default")).thenReturn(DEFAULT_CURRENCY);
    }

    @After
    public void tearDown() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void getCountriesShouldReturnAllCountries() {
        assertThat(underTest.getCountries(), is(equalTo(COUNTRIES)));
    }

    @Test
    public void getCountriesShouldCacheTheResultsOfGetCountries() {
        underTest.setCacheTimeInMillis(2000);

        underTest.getCountries();
        underTest.getCountries();

        verify(countryRepository, times(1)).getCountries();
    }

    @Test
    public void getCountriesShouldUpdateTheCachedListOfCountriesAfterTheCacheExpires() {
        underTest.setCacheTimeInMillis(10);

        underTest.getCountries();
        sleepFor(13);
        underTest.getCountries();

        verify(countryRepository, times(2)).getCountries();
    }

    @Test
    public void getPreferredCurrencyShouldReturnTheDefaultCurrencyForANullCountry() {
        assertThat(underTest.getPreferredCurrency(null), is(equalTo(Currency.valueOf(DEFAULT_CURRENCY))));
    }

    @Test
    public void getPreferredCurrencyShouldReturnTheDefaultCurrencyForAnUnknownCountry() {
        assertThat(underTest.getPreferredCurrency("XXX"), is(equalTo(Currency.valueOf(DEFAULT_CURRENCY))));
    }

    @Test
    public void getPreferredCurrencyShouldReturnTheCorrectCurrencyForAKnownCountryByCode() {
        assertThat(underTest.getPreferredCurrency("AU"), is(equalTo(Currency.valueOf("AUD"))));
    }

    @Test
    public void getPreferredCurrencyShouldReturnTheCorrectCurrencyForAKnownCountryByName() {
        assertThat(underTest.getPreferredCurrency("United Kingdom"), is(equalTo(Currency.valueOf("GBP"))));
    }

    @Test
    public void getPreferredCurrencyShouldReturnTheDefaultCurrencyWhenTheCountryCurrencyIsMalformedCurrency() {
        assertThat(underTest.getPreferredCurrency("US"), is(equalTo(Currency.valueOf(DEFAULT_CURRENCY))));
    }

    @Test
    public void getPreferredCurrencyShouldReturnUSDWhenNoDefaultCurrencyIsSet() {
        reset(yazinoConfiguration);
        when(yazinoConfiguration.getString("payments.currency.default")).thenThrow(
                new NoSuchElementException("aTestException"));

        assertThat(underTest.getPreferredCurrency(null), is(equalTo(Currency.valueOf("USD"))));
    }

    private void sleepFor(final long time) {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(DateTimeUtils.currentTimeMillis() + time);
    }

}
