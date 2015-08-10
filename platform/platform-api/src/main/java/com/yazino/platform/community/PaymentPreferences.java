package com.yazino.platform.community;

import com.yazino.platform.reference.Currency;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class PaymentPreferences implements Serializable {

    private static final long serialVersionUID = -4185617112761897805L;

    public enum PaymentMethod {
        PAYPAL, CREDITCARD, ZONG, TRIALPAY, ITUNES, FACEBOOK, GOOGLE_CHECKOUT, AMAZON
    }

    private Currency currency;
    private PaymentMethod paymentMethod;
    private String paymentCountry;

    public PaymentPreferences(final Currency acceptedCurrency) {
        this.currency = acceptedCurrency;
    }

    public PaymentPreferences(final Currency acceptedCurrency,
                              final PaymentMethod paymentMethod) {
        this(acceptedCurrency);
        this.paymentMethod = paymentMethod;
    }

    public PaymentPreferences(final PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentPreferences(final Currency acceptedCurrency,
                              final PaymentMethod paymentMethod,
                              final String paymentCountry) {
        this(acceptedCurrency);
        this.paymentMethod = paymentMethod;
        this.paymentCountry = paymentCountry;
    }

    public PaymentPreferences() {
        this.currency = null;
        this.paymentMethod = null;
    }

    public Currency getCurrency() {
        return currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentCountry() {
        return paymentCountry;
    }

    public PaymentPreferences withPaymentMethod(final PaymentMethod newPaymentMethod) {
        return new PaymentPreferences(currency, newPaymentMethod, paymentCountry);
    }

    public PaymentPreferences withCurrency(final Currency acceptedCurrency) {
        return new PaymentPreferences(acceptedCurrency, paymentMethod, paymentCountry);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final PaymentPreferences rhs = (PaymentPreferences) obj;
        return new EqualsBuilder()
                .append(currency, rhs.currency)
                .append(paymentMethod, rhs.paymentMethod)
                .append(paymentCountry, rhs.paymentCountry)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(currency)
                .append(paymentMethod)
                .append(paymentCountry)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(currency)
                .append(paymentMethod)
                .append(paymentCountry)
                .toString();
    }

}
