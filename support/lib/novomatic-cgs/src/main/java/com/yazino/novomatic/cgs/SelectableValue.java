package com.yazino.novomatic.cgs;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.List;
import java.util.Map;

public class SelectableValue {
    private final List<Long> allowed;
    private long value;

    public SelectableValue(List<Long> allowed, long value) {
        this.allowed = allowed;
        this.value = value;
    }

    public static SelectableValue fromMap(Map<String, Object> map) {
        return new SelectableValue((List<Long>) map.get("allowed"), (Long) map.get("value"));
    }

    public List<Long> getAllowed() {
        return allowed;
    }

    public long getValue() {
        return value;
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
        SelectableValue rhs = (SelectableValue) obj;
        return new EqualsBuilder()
                .append(this.allowed, rhs.allowed)
                .append(this.value, rhs.value)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(allowed)
                .append(value)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
