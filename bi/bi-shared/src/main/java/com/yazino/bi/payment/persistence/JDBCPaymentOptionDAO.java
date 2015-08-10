package com.yazino.bi.payment.persistence;

import com.google.common.base.Optional;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.platform.Platform;
import com.yazino.platform.reference.Currency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class JDBCPaymentOptionDAO {

    private static final String SQL_SELECT_BY_ID_AND_PLATFORM
            = "SELECT * FROM PAYMENT_OPTION_CURRENT "
            + "WHERE PAYMENT_OPTION_ID = ? AND PLATFORM = ? ";

    private static final String SQL_SELECT_BY_CURRENCY_AND_PLATFORM
            = "SELECT * "
            + "FROM PAYMENT_OPTION_CURRENT "
            + "WHERE CURRENCY = ? AND PLATFORM = ? "
            + "ORDER BY PLATFORM,CURRENCY,LEVEL";

    private static final String SQL_SELECT_BY_PLATFORM
            = "SELECT * FROM PAYMENT_OPTION_CURRENT "
            + "WHERE PLATFORM = ? "
            + "ORDER BY PLATFORM,CURRENCY,LEVEL";

    private final RowMapper<PaymentOption> paymentOptionRowMapper = new PaymentOptionRowMapper();

    private final JdbcTemplate jdbcTemplate;

    JDBCPaymentOptionDAO() {
        // CGLib constructor
        jdbcTemplate = null;
    }

    @Autowired
    public JDBCPaymentOptionDAO(@Qualifier("marketingJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        notNull(jdbcTemplate, "jdbcTemplate may not be null");

        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PaymentOption> findByCurrencyAndPlatform(final Currency currency, final Platform platform) {
        notNull(currency, "currency may not be null");
        notNull(platform, "platform may not be null");

        final List<PaymentOption> paymentOptions = jdbcTemplate.query(SQL_SELECT_BY_CURRENCY_AND_PLATFORM,
                paymentOptionRowMapper, currency.getCode(), platform.toString());
        if (paymentOptions != null) {
            return paymentOptions;
        }
        return Collections.emptyList();
    }

    public Set<PaymentOption> findByPlatform(final Platform platform) {
        notNull(platform, "platform may not be null");

        final List<PaymentOption> paymentOptions = jdbcTemplate.query(SQL_SELECT_BY_PLATFORM, paymentOptionRowMapper, platform.toString());
        if (paymentOptions != null) {
            return new HashSet<>(paymentOptions);
        }
        return Collections.emptySet();
    }

    public Map<Currency, List<PaymentOption>> findByPlatformWithCurrencyKey(final Platform platform) {
        notNull(platform, "platform may not be null");

        final List<PaymentOption> paymentOptions = jdbcTemplate.query(SQL_SELECT_BY_PLATFORM, paymentOptionRowMapper, platform.toString());
        if (paymentOptions != null) {
            return asPaymentOptionsByCurrency(paymentOptions);
        }
        return Collections.emptyMap();
    }

    private Map<Currency, List<PaymentOption>> asPaymentOptionsByCurrency(final List<PaymentOption> paymentOptions) {
        final Map<Currency, List<PaymentOption>> paymentOptionsByCurrency = new HashMap<>();
        for (PaymentOption paymentOption : paymentOptions) {
            final Currency currency = Currency.valueOf(paymentOption.getCurrencyCode());

            List<PaymentOption> optionsForCurrency = paymentOptionsByCurrency.get(currency);
            if (optionsForCurrency == null) {
                optionsForCurrency = new ArrayList<>();
                paymentOptionsByCurrency.put(currency, optionsForCurrency);
            }

            optionsForCurrency.add(paymentOption);
        }
        return paymentOptionsByCurrency;
    }

    public Optional<PaymentOption> findByIdAndPlatform(final String id,
                                                       final Platform platform) {
        notNull(id, "id may not be null");
        notNull(platform, "platform may not be null");

        final List<PaymentOption> paymentOption = jdbcTemplate.query(SQL_SELECT_BY_ID_AND_PLATFORM,
                paymentOptionRowMapper, id, platform.toString());
        if (paymentOption == null || paymentOption.size() == 0) {
            return Optional.absent();
        }
        return Optional.fromNullable(paymentOption.get(0));
    }

    private class PaymentOptionRowMapper implements RowMapper<PaymentOption> {
        @Override
        public PaymentOption mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final PaymentOption paymentOption = new PaymentOption();
            paymentOption.setId(rs.getString("PAYMENT_OPTION_ID"));
            paymentOption.setLevel(rs.getInt("LEVEL"));
            paymentOption.setUpsellId(rs.getString("UPSELL_PAYMENT_OPTION_ID"));
            paymentOption.setCurrencyCode(rs.getString("CURRENCY"));
            paymentOption.setRealMoneyCurrency(rs.getString("CURRENCY"));
            paymentOption.setAmountRealMoneyPerPurchase(scale(rs.getBigDecimal("PRICE")));
            paymentOption.setNumChipsPerPurchase(rs.getBigDecimal("CHIPS"));
            paymentOption.setCurrencyLabel(rs.getString("CURRENCY_LABEL"));
            paymentOption.setTitle(rs.getString("HEADER"));
            paymentOption.setDescription(rs.getString("DESCRIPTION"));
            paymentOption.setUpsellTitle(rs.getString("UPSELL_HEADER"));
            paymentOption.setUpsellDescription(rs.getString("UPSELL_DESCRIPTION"));
            paymentOption.setUpsellNumChipsPerPurchase(rs.getBigDecimal("UPSELL_CHIPS"));
            paymentOption.setUpsellRealMoneyPerPurchase(scale(rs.getBigDecimal("UPSELL_PRICE")));
            paymentOption.setBaseCurrencyCode(rs.getString("BASE_CURRENCY"));
            paymentOption.setBaseCurrencyPrice(scale(rs.getBigDecimal("BASE_CURRENCY_PRICE")));
            paymentOption.setExchangeRate(rs.getBigDecimal("EXCHANGE_RATE"));
            return paymentOption;
        }

        private BigDecimal scale(final BigDecimal value) {
            if (value != null) {
                return value.setScale(2, RoundingMode.HALF_EVEN);
            }
            return null;
        }
    }
}
