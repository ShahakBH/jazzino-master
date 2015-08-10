package com.yazino.bi.payment.persistence;

import com.yazino.bi.payment.ExchangeRate;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class JDBCPaymentFXDAOIntegrationTest {
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("1.59740").setScale(7);

    @Autowired
    @Qualifier("marketingJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JDBCPaymentFXDAO underTest;

    @Test(expected = NullPointerException.class)
    public void aNullExchangeRateThrowsANullPointerException() {
        underTest.save(null);
    }

    @Test
    public void anExchangeRateCanBeSaved() {
        underTest.save(anExchangeRate());

        final Map<String, Object> fx = jdbcTemplate.queryForMap(
                "SELECT * FROM PAYMENT_FX WHERE CURRENCY='GBP' AND BASE_CURRENCY='USD' AND SETTLEMENT_DATE='2013-01-01'");
        assertThat((BigDecimal) fx.get("EXCHANGE_RATE"), is(equalTo(EXCHANGE_RATE)));
    }

    @Test
    public void anExchangeRateCanBeUpdated() {
        underTest.save(anExchangeRate());

        final BigDecimal newExchangeRate = new BigDecimal("1.59743").setScale(7);
        underTest.save(new ExchangeRate(Currency.getInstance("GBP"), Currency.getInstance("USD"),
                newExchangeRate, new DateTime(2013, 1, 1, 0, 0, 0, 0)));

        final Map<String, Object> fx = jdbcTemplate.queryForMap(
                "SELECT * FROM PAYMENT_FX WHERE CURRENCY='GBP' AND BASE_CURRENCY='USD' AND SETTLEMENT_DATE='2013-01-01'");
        assertThat((BigDecimal) fx.get("EXCHANGE_RATE"), is(equalTo(newExchangeRate)));
    }

    private ExchangeRate anExchangeRate() {
        return new ExchangeRate(Currency.getInstance("GBP"), Currency.getInstance("USD"),
                EXCHANGE_RATE, new DateTime(2013, 1, 1, 0, 0, 0, 0));
    }

}
