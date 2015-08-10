package com.yazino.platform.bonus;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

@JsonSerialize
public class BonusStatus implements Serializable {
    private static final long serialVersionUID = -6274025842123624510L;

    private Long milliesToNextBonus;
    private Long chipsAvailable;

    public BonusStatus(final Long nextTopup, final Long chipsAvailable) {
        this.milliesToNextBonus = nextTopup;
        this.chipsAvailable = chipsAvailable;
    }

    public Long getMilliesToNextBonus() {
        return milliesToNextBonus;
    }

    public Long getChipsAvailable() {
        return chipsAvailable;
    }

    public void setMilliesToNextBonus(final Long milliesToNextBonus) {
        this.milliesToNextBonus = milliesToNextBonus;
    }

    public void setChipsAvailable(final Long chipsAvailable) {
        this.chipsAvailable = chipsAvailable;
    }

    BonusStatus() {
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
        BonusStatus rhs = (BonusStatus) obj;
        return new EqualsBuilder()
                .append(this.milliesToNextBonus, rhs.milliesToNextBonus)
                .append(this.chipsAvailable, rhs.chipsAvailable)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(milliesToNextBonus)
                .append(chipsAvailable)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("nextTopup", milliesToNextBonus)
                .append("chipsAvailable", chipsAvailable)
                .toString();
    }
}
