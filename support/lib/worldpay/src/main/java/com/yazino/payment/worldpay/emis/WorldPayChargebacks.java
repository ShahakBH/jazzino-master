package com.yazino.payment.worldpay.emis;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class WorldPayChargebacks implements Serializable {
    private static final long serialVersionUID = 7339211989757830880L;

    private final List<Chargeback> chargebacks = new ArrayList<>();
    private final DateTime date;

    public WorldPayChargebacks(final List<Chargeback> chargebacks,
                               final DateTime date) {
        notNull(date, "date may not be null");

        this.date = date;
        if (chargebacks != null) {
            this.chargebacks.addAll(chargebacks);
        }
    }

    public List<Chargeback> getChargebacks() {
        return Collections.unmodifiableList(chargebacks);
    }

    public DateTime getDate() {
        return date;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        WorldPayChargebacks rhs = (WorldPayChargebacks) obj;
        return new EqualsBuilder()
                .append(this.chargebacks, rhs.chargebacks)
                .append(this.date, rhs.date)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(chargebacks)
                .append(date)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("chargebacks", chargebacks)
                .append("date", date)
                .toString();
    }
}
