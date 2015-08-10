package com.yazino.bi.operations.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class Dashboard {
    private final PlayerDashboard name;
    private final List<Map<String, Object>> results;
    private final Map<String, String> fields;
    private final Map<String, String> fieldTypes;

    public Dashboard(final PlayerDashboard name) {
        notNull(name, "name may not be null");

        this.name = name;
        results = Collections.emptyList();
        fields = Collections.emptyMap();
        fieldTypes = Collections.emptyMap();
    }

    public Dashboard(final PlayerDashboard name,
                     final List<Map<String, Object>> results,
                     final Map<String, String> fields,
                     final Map<String, String> fieldTypes) {
        notNull(name, "name may not be null");
        notNull(results, "results may not be null");
        notNull(fields, "fields may not be null");
        notNull(fieldTypes, "fieldTypes may not be null");

        this.name = name;
        this.results = results;
        this.fields = fields;
        this.fieldTypes = fieldTypes;
    }

    public PlayerDashboard getName() {
        return name;
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public Map<String, String> getFieldTypes() {
        return fieldTypes;
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
        final Dashboard rhs = (Dashboard) obj;
        return new EqualsBuilder()
                .append(name, rhs.name)
                .append(results, rhs.results)
                .append(fields, rhs.fields)
                .append(fieldTypes, rhs.fieldTypes)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(name)
                .append(results)
                .append(fields)
                .append(fieldTypes)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(name)
                .append(results)
                .append(fields)
                .append(fieldTypes)
                .toString();
    }
}
