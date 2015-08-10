package strata.server.operations.promotion.model;

import com.yazino.platform.reference.Currency;

import java.math.BigDecimal;

public class PackagePrice {
    private Currency currency;
    private String currencyLabel;
    private BigDecimal price;

    public PackagePrice(final Currency currency,
                        final BigDecimal price,
                        final String currencyLabel) {
        this.currency = currency;
        this.currencyLabel = currencyLabel;
        this.price = price;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public String getCurrencyLabel() {
        return currencyLabel;
    }

    public void setCurrencyLabel(final String currencyLabel) {
        this.currencyLabel = currencyLabel;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(final BigDecimal price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "PackagePrice{"
                + "currency=" + currency
                + ", currencyLabel='" + currencyLabel + '\''
                + ", price=" + price
                + '}';
    }
}
