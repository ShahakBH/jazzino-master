package com.yazino.platform.reference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true)
public class JDBCCountryRepositoryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JDBCCountryRepository countryRepository;

    @Before
    public void setUpTestCountries() {
        jdbcTemplate.execute("DELETE FROM COUNTRY");
        jdbcTemplate.update("INSERT INTO COUNTRY (ISO_3166_1_CODE, NAME, CURRENCY_ISO_4217_CODE) VALUES "
                + "('GB','United Kingdom','GBP'),"
                + "('US','United States','USD'),"
                + "('DE','Germany','EUR')");
    }

    @Transactional
    @Test
    public void theRepositoryShouldReturnAnEmptySetWhenNoCountriesArePresent() {
        jdbcTemplate.update("DELETE FROM COUNTRY");

        final Set<Country> countries = countryRepository.getCountries();

        assertThat(countries, is(not(nullValue())));
        assertThat(countries.size(), is(equalTo(0)));
    }

    @SuppressWarnings("unchecked")
    @Transactional
    @Test
    public void theRepositoryShouldReturnTheSetOfAllCountries() {
        final Set<Country> countries = countryRepository.getCountries();

        assertThat(countries.size(), is(equalTo(3)));
        assertThat(countries, containsInAnyOrder(
                equalTo(new Country("GB", "United Kingdom", "GBP")),
                equalTo(new Country("US", "United States", "USD")),
                equalTo(new Country("DE", "Germany", "EUR"))));
    }

}
