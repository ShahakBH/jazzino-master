package com.yazino.web.data;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.reference.ReferenceService;
import com.yazino.web.data.CurrencyRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CurrencyRepositoryTest {

    @Mock
    private ReferenceService referenceService;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private CurrencyRepository underTest;

    @Before
    public void setUp() {
        when(referenceService.getPreferredCurrency("GB")).thenReturn(Currency.valueOf("GBP"));
        when(yazinoConfiguration.getList("payments.currency.accepted", Collections.emptyList()))
                .thenReturn(asList((Object) "USD", "GBP", "AUD"));

        underTest = new CurrencyRepository(referenceService, yazinoConfiguration);
    }

    @Test(expected = IllegalStateException.class)
    public void preferredCurrencyThrowsIllegalStateExceptionWhenClassCreatedWithCGLibConstructor() {
        new CurrencyRepository().getPreferredCurrencyFor("aCountryCode");
    }

    @Test(expected = IllegalStateException.class)
    public void acceptedCurrenciesThrowsIllegalStateExceptionWhenClassCreatedWithCGLibConstructor() {
        new CurrencyRepository().getAcceptedCurrencies();
    }

    @Test
    public void preferredCurrencyDelegatesToTheReferenceService() {
        assertThat(underTest.getPreferredCurrencyFor("GB"), is(equalTo(Currency.valueOf("GBP"))));
    }

    @Test
    public void acceptedCurrenciesFetchesThePropertiesFromYazinoConfiguration() {
        assertThat(underTest.getAcceptedCurrencies(),
                is(equalTo((Set<Currency>) newHashSet(Currency.USD, Currency.GBP, Currency.AUD))));
    }

    @Test
    public void acceptedCurrenciesDefaultsToUSDWhenNoPropertyIsAvailable() {
        reset(yazinoConfiguration);
        when(yazinoConfiguration.getList("payments.currency.accepted", Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        assertThat(underTest.getAcceptedCurrencies(), is(equalTo((Set<Currency>) newHashSet(Currency.USD))));
    }

}
