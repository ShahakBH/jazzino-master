package com.yazino.bi.operations.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class InvitationDashboard extends Dashboard {
    private final TableModel tabData;

    public InvitationDashboard(final TableModel tabData) {
        super(PlayerDashboard.INVITE);

        this.tabData = tabData;
    }

    public TableModel getTabData() {
        return tabData;
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
        final InvitationDashboard rhs = (InvitationDashboard) obj;
        return new EqualsBuilder()
                .append(tabData, rhs.tabData)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(tabData)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(tabData)
                .toString();
    }
}
