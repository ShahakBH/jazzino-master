package com.yazino.platform.table;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A simple pojo for returning the results of a table allocation search.
 */
public class TableSearchResult implements Serializable {

    private static final long serialVersionUID = 1994122903380097293L;

    public static final TableSearchResult NULL = new TableSearchResult(BigDecimal.valueOf(-1), -1, -1, -1);

    private BigDecimal tableId;
    private int spareSeats;
    private int maxSeats;
    // factor indicating how desirable it is to seat players at this table.
    private int joiningDesirability;

    public TableSearchResult(final BigDecimal tableId,
                             final int spareSeats,
                             final int maxSeats,
                             final int joiningDesirability) {
        this.tableId = tableId;
        this.spareSeats = spareSeats;
        this.maxSeats = maxSeats;
        this.joiningDesirability = joiningDesirability;
    }

    public TableSearchResult() {
    }

    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    public void setSpareSeats(final int spareSeats) {
        this.spareSeats = spareSeats;
    }

    public void setMaxSeats(final int maxSeats) {
        this.maxSeats = maxSeats;
    }

    public BigDecimal getTableId() {
        return tableId;
    }

    public int getSpareSeats() {
        return spareSeats;
    }

    public int getMaxSeats() {
        return maxSeats;
    }

    public int getJoiningDesirability() {
        return joiningDesirability;
    }

    public void setJoiningDesirability(final int joiningDesirability) {
        this.joiningDesirability = joiningDesirability;
    }

    public boolean isNotFull() {
        return spareSeats > 0;
    }

    public boolean isEmpty() {
        return spareSeats == maxSeats;
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TableSearchResult that = (TableSearchResult) o;

        return new EqualsBuilder()
                .append(this.spareSeats, that.spareSeats)
                .append(this.maxSeats, that.maxSeats)
                .append(this.joiningDesirability, that.joiningDesirability)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, that.tableId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(tableId))
                .append(spareSeats)
                .append(maxSeats)
                .append(joiningDesirability)
                .toHashCode();
    }

    @Override
    public String toString() {
        final ToStringBuilder builder = new ReflectionToStringBuilder(this);
        return builder.toString();
    }
}
