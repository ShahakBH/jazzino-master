package com.yazino.bi.payment.persistence;

import com.google.common.base.Optional;
import com.yazino.platform.Platform;
import com.yazino.platform.reference.Currency;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import com.yazino.bi.payment.PaymentOption;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
@DirtiesContext
public class JDBCPaymentOptionDAOIntegrationTest {
    private static final int OPTION_COUNT = 6;
    private static final String[] TITLES = new String[]{
            "You've got STARTER STYLE!", "You're a CLEVER COMPETITOR!", "You've got a LUCKY BREAK!",
            "You're a SAVVY STAR!", "You're a POWER PLAYER!", "You're a MILLIONAIRE MAVEN!"
    };
    private static final String[] UPSELL_TITLES = new String[]{
            "Be a CLEVER COMPETITOR", "Take a LUCKY BREAK", "Be a SAVVY STAR", "Be a POWER PLAYER",
            "Be a MILLIONAIRE MAVEN", "You're a MILLIONAIRE MAVEN!"
    };
    private static final Map<String, String> CURRENCY_LABELS = new HashMap<>();
    private static final String BASE_CURRENCY = "USD";

    static {
        CURRENCY_LABELS.put("AUD", "A$");
        CURRENCY_LABELS.put("CAD", "C$");
        CURRENCY_LABELS.put("USD", "$");
        CURRENCY_LABELS.put("EUR", "€");
        CURRENCY_LABELS.put("GBP", "£");
    }

    private static final int CURRENCY_COUNT = 5;

    private final Map<String, BigDecimal> exchangeRates = new HashMap<String, BigDecimal>();
    private final Map<String, BigDecimal> fxCommission = new HashMap<String, BigDecimal>();

    @Autowired
    private JDBCPaymentOptionDAO underTest;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate.query("SELECT * FROM PAYMENT_FX_CURRENT", new RowCallbackHandler() {
            @Override
            public void processRow(final ResultSet rs) throws SQLException {
                exchangeRates.put(rs.getString("CURRENCY"), rs.getBigDecimal("EXCHANGE_RATE"));
            }
        });
        jdbcTemplate.query("SELECT * FROM CURRENCY", new RowCallbackHandler() {
            @Override
            public void processRow(final ResultSet rs) throws SQLException {
                fxCommission.put(rs.getString("CURRENCY_CODE"), rs.getBigDecimal("COMMISSION_MULTIPLIER"));
            }
        });
    }

    @Test
    public void aPaymentOptionCanBeFoundByIdAndPlatform() {
        final Optional<PaymentOption> paymentOption = underTest.findByIdAndPlatform("optionAUD2", Platform.WEB);

        assertThat(paymentOption.isPresent(), is(true));
        assertThat(paymentOption.get(), is(equalTo(optionAUD2())));
    }

    @Test
    public void anAbsentPaymentOptionReturnsAbsent() {
        final Optional<PaymentOption> paymentOption = underTest.findByIdAndPlatform("nonExistentOption", Platform.WEB);

        assertThat(paymentOption.isPresent(), is(false));
    }

    @Test
    public void findingPaymentMethodsForAnAbsentCurrencyReturnsAnEmptyCollection() {
        final List<PaymentOption> paymentOptions = underTest.findByCurrencyAndPlatform(Currency.BGN, Platform.WEB);

        assertThat(paymentOptions, is(not(nullValue())));
    }

    @Test
    public void findingPaymentMethodsForACurrencyAndPlatformReturnsAllOptions() {
        final List<PaymentOption> paymentOptions = underTest.findByCurrencyAndPlatform(Currency.AUD, Platform.WEB);

        assertThat(paymentOptions, is(not(nullValue())));
        assertThat(paymentOptions.size(), is(equalTo(OPTION_COUNT)));
        assertThat(paymentOptions, contains(audOptions()));
    }

    @Test
    public void findingPaymentMethodsForAPlatformReturnsAllOptionsWhereOnlyASingleCurrencyIsSupported() {
        final Set<PaymentOption> paymentOptions = underTest.findByPlatform(Platform.IOS);

        assertThat(paymentOptions, is(not(nullValue())));
        assertThat(paymentOptions.size(), is(equalTo(OPTION_COUNT)));
        assertThat(paymentOptions, hasItems(iosOptions()));
    }

    @Test
    public void findingPaymentMethodsForAPlatformReturnsAllOptionsWhereMultipleCurrenciesAreSupported() {
        final Set<PaymentOption> paymentOptions = underTest.findByPlatform(Platform.WEB);

        assertThat(paymentOptions, is(not(nullValue())));
        assertThat(paymentOptions.size(), is(equalTo(OPTION_COUNT * CURRENCY_COUNT)));
        assertThat(paymentOptions, hasItems(merge(audOptions(), cadOptions(), eurOptions(), gbpOptions(), usdOptions())));
    }

    @Test
    public void findingPaymentMethodsForAPlatformWithCurrencyKeyReturnsAllOptionsKeyedByCurrency() {
        final Map<Currency, List<PaymentOption>> paymentOptions = underTest.findByPlatformWithCurrencyKey(Platform.WEB);

        assertThat(paymentOptions, is(not(nullValue())));
        assertThat(paymentOptions.size(), is(equalTo(CURRENCY_COUNT)));
        assertThat(paymentOptions.get(Currency.AUD), contains(audOptions()));
        assertThat(paymentOptions.get(Currency.CAD), contains(cadOptions()));
        assertThat(paymentOptions.get(Currency.EUR), contains(eurOptions()));
        assertThat(paymentOptions.get(Currency.GBP), contains(gbpOptions()));
        assertThat(paymentOptions.get(Currency.USD), contains(usdOptions()));
    }

    private PaymentOption[] merge(final PaymentOption[]... arrays) {
        int totalSize = 0;
        for (PaymentOption[] array : arrays) {
            totalSize += array.length;
        }
        int currentPosition = 0;
        final PaymentOption[] mergedArray = new PaymentOption[totalSize];
        for (PaymentOption[] array : arrays) {
            System.arraycopy(array, 0, mergedArray, currentPosition, array.length);
            currentPosition += array.length;
        }
        return mergedArray;
    }

    private PaymentOption[] audOptions() {
        return new PaymentOption[]{
                option(1, "optionAUD1", "optionAUD2", "AUD", 10000, 5, 21000, 10),
                optionAUD2(),
                option(3, "optionAUD3", "optionAUD4", "AUD", 50000, 20, 150000, 50),
                option(4, "optionAUD4", "optionAUD5", "AUD", 150000, 50, 400000, 100),
                option(5, "optionAUD5", "optionAUD6", "AUD", 400000, 100, 1000000, 150),
                option(6, "optionAUD6", "optionAUD6", "AUD", 1000000, 150, 1000000, 150)
        };
    }

    private PaymentOption[] usdOptions() {
        return new PaymentOption[]{
                option(1, "optionUSD1", "optionUSD2", "USD", 10000, 5, 21000, 10),
                option(2, "optionUSD2", "optionUSD3", "USD", 21000, 10, 50000, 20),
                option(3, "optionUSD3", "optionUSD4", "USD", 50000, 20, 150000, 50),
                option(4, "optionUSD4", "optionUSD5", "USD", 150000, 50, 400000, 100),
                option(5, "optionUSD5", "optionUSD6", "USD", 400000, 100, 1000000, 150),
                option(6, "optionUSD6", "optionUSD6", "USD", 1000000, 150, 1000000, 150)
        };
    }

    private PaymentOption[] cadOptions() {
        return new PaymentOption[]{
                option(1, "optionCAD1", "optionCAD2", "CAD", 10000, 5, 21000, 10),
                option(2, "optionCAD2", "optionCAD3", "CAD", 21000, 10, 50000, 20),
                option(3, "optionCAD3", "optionCAD4", "CAD", 50000, 20, 150000, 50),
                option(4, "optionCAD4", "optionCAD5", "CAD", 150000, 50, 400000, 100),
                option(5, "optionCAD5", "optionCAD6", "CAD", 400000, 100, 1000000, 150),
                option(6, "optionCAD6", "optionCAD6", "CAD", 1000000, 150, 1000000, 150)
        };
    }

    private PaymentOption[] eurOptions() {
        return new PaymentOption[]{
                option(1, "optionEUR1", "optionEUR2", "EUR", 10000, 3.5, 21000, 7),
                option(2, "optionEUR2", "optionEUR3", "EUR", 21000, 7, 50000, 14),
                option(3, "optionEUR3", "optionEUR4", "EUR", 50000, 14, 150000, 35),
                option(4, "optionEUR4", "optionEUR5", "EUR", 150000, 35, 400000, 70),
                option(5, "optionEUR5", "optionEUR6", "EUR", 400000, 70, 1000000, 105),
                option(6, "optionEUR6", "optionEUR6", "EUR", 1000000, 105, 1000000, 105)
        };
    }

    private PaymentOption[] gbpOptions() {
        return new PaymentOption[]{
                option(1, "optionGBP1", "optionGBP2", "GBP", 10000, 3, 21000, 6),
                option(2, "optionGBP2", "optionGBP3", "GBP", 21000, 6, 50000, 12),
                option(3, "optionGBP3", "optionGBP4", "GBP", 50000, 12, 150000, 30),
                option(4, "optionGBP4", "optionGBP5", "GBP", 150000, 30, 400000, 60),
                option(5, "optionGBP5", "optionGBP6", "GBP", 400000, 60, 1000000, 90),
                option(6, "optionGBP6", "optionGBP6", "GBP", 1000000, 90, 1000000, 90)
        };
    }

    private PaymentOption[] iosOptions() {
        return new PaymentOption[]{
                option(1, "IOS_USD3", "IOS_USD8", "USD", 5000, 3, 15000, 8),
                option(2, "IOS_USD8", "IOS_USD15", "USD", 15000, 8, 30000, 15),
                option(3, "IOS_USD15", "IOS_USD30", "USD", 30000, 15, 70000, 30),
                option(4, "IOS_USD30", "IOS_USD70", "USD", 70000, 30, 200000, 70),
                option(5, "IOS_USD70", "IOS_USD90", "USD", 200000, 70, 300000, 90),
                option(6, "IOS_USD90", "IOS_USD90", "USD", 300000, 90, 300000, 90)
        };
    }

    private PaymentOption optionAUD2() {
        return option(2, "optionAUD2", "optionAUD3", "AUD", 21000, 10, 50000, 20);
    }

    private PaymentOption option(final int optionNumber,
                                 final String id,
                                 final String upsellId,
                                 final String currency,
                                 final int chips,
                                 final double price,
                                 final int upsellChips,
                                 final int upsellPrice) {
        final PaymentOption expectedPaymentOption = new PaymentOption();
        expectedPaymentOption.setId(id);
        expectedPaymentOption.setUpsellId(upsellId);
        expectedPaymentOption.setLevel(optionNumber);
        expectedPaymentOption.setCurrencyCode(currency);
        expectedPaymentOption.setRealMoneyCurrency(currency);
        expectedPaymentOption.setCurrencyLabel(CURRENCY_LABELS.get(currency));
        expectedPaymentOption.setNumChipsPerPurchase(BigDecimal.valueOf(chips));
        expectedPaymentOption.setAmountRealMoneyPerPurchase(fx(currency, price));
        expectedPaymentOption.setUpsellNumChipsPerPurchase(BigDecimal.valueOf(upsellChips));
        expectedPaymentOption.setUpsellRealMoneyPerPurchase(fx(currency, upsellPrice));
        expectedPaymentOption.setTitle(TITLES[optionNumber - 1]);
        expectedPaymentOption.setDescription("$CHIPS$ CHIPS for $CURRENCY$$PRICE$");
        expectedPaymentOption.setUpsellTitle(UPSELL_TITLES[optionNumber - 1]);
        if (optionNumber == OPTION_COUNT) {
            expectedPaymentOption.setUpsellDescription("$CHIPS$ CHIPS for $CURRENCY$$PRICE$");
        } else {
            expectedPaymentOption.setUpsellDescription("Get $CHIPS$ CHIPS for $CURRENCY$$PRICE_DELTA$ more!");
        }
        expectedPaymentOption.setExchangeRate(exchangeRates.get(currency));
        if (expectedPaymentOption.getExchangeRate() != null) {
            expectedPaymentOption.setBaseCurrencyCode(BASE_CURRENCY);
            expectedPaymentOption.setBaseCurrencyPrice(BigDecimal.valueOf(price).setScale(2));
        }
        return expectedPaymentOption;
    }

    private BigDecimal fx(final String currency, final double value) {
        final BigDecimal valueBd = BigDecimal.valueOf(value);
        if (exchangeRates.containsKey(currency)) {
            final BigDecimal commission = defaultIfNull(fxCommission.get(currency), BigDecimal.ONE);
            return valueBd.multiply(exchangeRates.get(currency))
                    .multiply(commission)
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
        return valueBd.setScale(2);
    }

}
