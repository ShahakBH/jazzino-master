package com.yazino.bi.operations.service;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.LocalDate;

public class InvitationFilter {

    public enum Order {
        ASC, DESC;

        public static Order forName(final String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private String orderBy;
    private Order order;
    private LocalDate from;
    private LocalDate to;
    private Integer pageSize;
    private Integer pageNumber;

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(final String orderBy) {
        this.orderBy = orderBy;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(final Order order) {
        this.order = order;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(final LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(final LocalDate to) {
        this.to = to;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(final Integer pageNumber) {
        this.pageNumber = pageNumber;
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
        final InvitationFilter rhs = (InvitationFilter) obj;
        return new EqualsBuilder()
                .append(orderBy, rhs.orderBy)
                .append(order, rhs.order)
                .append(from, rhs.from)
                .append(to, rhs.to)
                .append(pageSize, rhs.pageSize)
                .append(pageNumber, rhs.pageNumber)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(orderBy)
                .append(order)
                .append(from)
                .append(to)
                .append(pageSize)
                .append(pageNumber)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(orderBy)
                .append(order)
                .append(from)
                .append(to)
                .append(pageSize)
                .append(pageNumber)
                .toString();
    }
}
