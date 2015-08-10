package com.yazino.bi.operations.model;

import com.yazino.bi.operations.service.InvitationFilter;

public class DashboardParameters extends PlayerSearchRequest {
    private Integer pageNumber;
    private InvitationFilter.Order order;

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(final Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public InvitationFilter.Order getOrder() {
        return order;
    }

    public void setOrder(final InvitationFilter.Order order) {
        this.order = order;
    }

}
