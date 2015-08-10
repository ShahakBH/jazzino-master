package com.yazino.platform.community;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class PlayerCreditConfiguration implements Serializable {
    private static final long serialVersionUID = 1869833970749878077L;

    private final BigDecimal initialAmount;
    private final BigDecimal referralAmount;
    private final BigDecimal guestConversionAmount;

    public PlayerCreditConfiguration(final BigDecimal initialAmount,
                                     final BigDecimal referralAmount,
                                     final BigDecimal guestConversionAmount) {
        notNull(initialAmount, "initialAmount may not be null");
        notNull(referralAmount, "referralAmount may not be null");
        notNull(guestConversionAmount, "guestConversionAmount may not be null");

        this.initialAmount = initialAmount;
        this.referralAmount = referralAmount;
        this.guestConversionAmount = guestConversionAmount;
    }


    public BigDecimal getInitialAmount() {
        return initialAmount;
    }


    public BigDecimal getReferralAmount() {
        return referralAmount;
    }

    public BigDecimal getGuestConversionAmount() {
        return guestConversionAmount;
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
        final PlayerCreditConfiguration rhs = (PlayerCreditConfiguration) obj;
        return new EqualsBuilder()
                .append(initialAmount, rhs.initialAmount)
                .append(referralAmount, rhs.referralAmount)
                .append(guestConversionAmount, rhs.guestConversionAmount)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(initialAmount)
                .append(referralAmount)
                .append(guestConversionAmount)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(initialAmount)
                .append(referralAmount)
                .append(guestConversionAmount)
                .toString();
    }
}
