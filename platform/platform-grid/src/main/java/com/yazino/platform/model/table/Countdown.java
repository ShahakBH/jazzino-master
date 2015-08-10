package com.yazino.platform.model.table;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

@SpaceClass
public class Countdown implements Serializable {

    private static final long serialVersionUID = 3223382614052040796L;
    private String id;
    private Long countdown;

    public Countdown() {
    }

    public Countdown(final String id, final Long countdown) {
        this.id = id;
        this.countdown = countdown;
    }

    @SpaceId
    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public Long getCountdown() {
        return countdown;
    }

    public void setCountdown(final Long countdown) {
        this.countdown = countdown;
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
        final Countdown rhs = (Countdown) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(countdown, rhs.countdown)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(id)
                .append(countdown)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).
                append("id", id).
                append("countdown", countdown).
                toString();
    }
}
