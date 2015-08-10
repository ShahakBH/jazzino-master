package com.yazino.bi.aggregator.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class AnalyzeInfo {

    private final String tableName;
    private final String keyColumns;

    public AnalyzeInfo(String tableName, String keyColumns) {
        this.tableName = tableName;
        this.keyColumns = keyColumns;
    }

    public String getTableName() {
        return tableName;
    }

    public String getKeyColumns() {
        return keyColumns;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.tableName).append(this.keyColumns).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AnalyzeInfo other = (AnalyzeInfo) obj;
        return new EqualsBuilder()
                .append(this.tableName, other.tableName)
                .append(this.keyColumns, other.keyColumns)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("tableName", tableName)
                .append("keyColumns", keyColumns)
                .toString();
    }
}
