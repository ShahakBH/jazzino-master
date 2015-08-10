package com.yazino.platform.model.community;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class SystemMessage implements Serializable {
    private static final long serialVersionUID = 5359769509624850166L;

    private BigDecimal id;
    private String message;
    private Date validFrom;
    private Date validTo;

    public SystemMessage() {
    }

    public SystemMessage(final BigDecimal id,
                         final String message,
                         final Date validFrom,
                         final Date validTo) {
        notNull(id, "ID may not be null");
        notBlank(message, "Message may not be blank");
        notNull(validFrom, "Valid From may not be null");
        notNull(validTo, "Valid To may not be null");

        this.id = id;
        this.message = message;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    @SpaceRouting
    @SpaceId
    public BigDecimal getId() {
        return id;
    }

    public void setId(final BigDecimal id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    @SpaceIndex
    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(final Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(final Date validTo) {
        this.validTo = validTo;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final SystemMessage rhs = (SystemMessage) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(message, rhs.message)
                .append(validFrom, rhs.validFrom)
                .append(validTo, rhs.validTo)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 41)
                .append(id)
                .append(message)
                .append(validFrom)
                .append(validTo)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    private static class DescendingValidFromComparator implements Comparator<SystemMessage>, Serializable {
        private static final long serialVersionUID = -4161656058975328710L;

        @Override
        public int compare(final SystemMessage o1, final SystemMessage o2) {
            if (ObjectUtils.equals(o1, o2) || ObjectUtils.equals(o1.getValidFrom(), o2.getValidFrom())) {
                return 0;
            }

            return o2.getValidFrom().compareTo(o1.getValidFrom());
        }
    }
}
