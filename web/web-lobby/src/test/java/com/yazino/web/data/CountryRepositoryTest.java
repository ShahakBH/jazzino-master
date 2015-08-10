package com.yazino.web.data;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.reference.Country;
import com.yazino.platform.reference.ReferenceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CountryRepositoryTest {

    @Mock
    private ReferenceService referenceService;

    private CountryRepository underTest;

    @Before
    public void setUp() {
        when(referenceService.getCountries()).thenReturn(newHashSet(
                new Country("GB", "United Kingdom", "GBP"),
                new Country("AU", "Australia", "AUD"),
                new Country("CA", "Canada", "CAD")));

        underTest = new CountryRepository(referenceService);
    }

    @Test
    public void boo() {
        System.err.println(new YazinoConfiguration().getString("payments.currency.accepted"));
    }

    @Test
    public void anEmptyMapIsReturnedWhenThereAreNoCountries() {
        reset(referenceService);

        assertThat(underTest.getCountries().size(), is(equalTo(0)));
    }

    @Test
    public void theCountriesAreSortedByName() {
        final Map<String, String> expectedMap = new LinkedHashMap<String, String>();
        expectedMap.put("AU", "Australia");
        expectedMap.put("CA", "Canada");
        expectedMap.put("GB", "United Kingdom");

        assertThat(underTest.getCountries(), is(equalTo(expectedMap)));
    }

}
