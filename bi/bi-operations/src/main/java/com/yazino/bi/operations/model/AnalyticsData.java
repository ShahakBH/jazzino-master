package com.yazino.bi.operations.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class AnalyticsData {
    private final String selection;
    private final String type;
    private Long value;

    public void setValue(final Long value) {
        this.value = value;
    }

    private transient String toString;

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AnalyticsData)) {
            return false;
        }
        final AnalyticsData castOther = (AnalyticsData) other;
        return new EqualsBuilder().append(selection, castOther.selection).append(type, castOther.type)
                .append(value, castOther.value).isEquals();
    }

    private transient int hashCode;

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = new HashCodeBuilder().append(selection).append(type).append(value).toHashCode();
        }
        return hashCode;
    }

    @Override
    public String toString() {
        if (toString == null) {
            toString =
                    new ToStringBuilder(this).append("selection", selection).append("type", type)
                            .append("value", value).toString();
        }
        return toString;
    }

    public String getSelection() {
        return selection;
    }

    public Long getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public AnalyticsData(final String selection, final String type, final Long value) {
        super();
        this.selection = selection;
        this.value = value;
        this.type = type;
    }

}
