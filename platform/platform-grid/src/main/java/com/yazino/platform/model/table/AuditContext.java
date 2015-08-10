package com.yazino.platform.model.table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Date;

public final class AuditContext implements Serializable {
    private static final long serialVersionUID = 5369839497542487352L;

    private final String label;
    private final Date auditDate;
    private final String hostname;

    public AuditContext(final String label,
                        final Date auditDate,
                        final String hostname) {
        this.label = label;
        this.auditDate = auditDate;
        this.hostname = hostname;
    }


    public String getLabel() {
        return label;
    }


    public Date getAuditDate() {
        return auditDate;
    }


    public String getHostname() {
        return hostname;
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
        final AuditContext rhs = (AuditContext) obj;
        return new EqualsBuilder()
                .append(label, rhs.label)
                .append(auditDate, rhs.auditDate)
                .append(hostname, rhs.hostname)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 19)
                .append(label)
                .append(auditDate)
                .append(hostname)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(label)
                .append(auditDate)
                .append(hostname)
                .toString();
    }
}
