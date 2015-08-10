package com.yazino.bi.operations.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Object holding the player's search information
 */
public class PlayerSearchRequest {
    private String query;
    private PlayerDashboard dashboardToDisplay;
    private String sortOrder;
    private String tableDetail;
    private Date selectionDate;
    private Integer firstRecord;
    private Integer pageSize;
    private String searchBy;
    private String searchValue;
    private Long adjustment;
    private Boolean blockPlayer;

    private Date fromDate;
    private Date toDate;
    private String table;
    private String gameType;
    private String transactionType;

    private Set<String> statementConsolidation;

    private String provider;
    private String reference;
    private String externalTransactionId;

    private final SimpleDateFormat dateFormat;
    private String search;
    private boolean pagedRequest;

    /**
     * Constructor setting the default values
     */
    public PlayerSearchRequest() {
        final Calendar from = Calendar.getInstance();
        from.add(Calendar.DAY_OF_MONTH, -1);
        from.set(Calendar.HOUR_OF_DAY, 0);
        from.set(Calendar.MINUTE, 0);
        from.set(Calendar.MILLISECOND, 0);
        fromDate = from.getTime();
        toDate = new Date();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        statementConsolidation = new HashSet<String>();
        sortOrder = "";
        pageSize = 20;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    public PlayerDashboard getDashboardToDisplay() {
        if (dashboardToDisplay == null) {
            return PlayerDashboard.STATEMENT;
        } else {
            return dashboardToDisplay;
        }
    }

    public void setDashboardToDisplay(final PlayerDashboard dashboardToDisplay) {
        this.dashboardToDisplay = dashboardToDisplay;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void normalise() {
        if (sortOrder == null) {
            this.sortOrder = "";
        }
    }

    public void setSortOrder(final String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Date getSelectionDate() {
        return selectionDate;
    }

    public void setSelectionDate(final Date date) {
        this.selectionDate = date;
    }

    public String getTableDetail() {
        return tableDetail;
    }

    public void setTableDetail(final String tableDetail) {
        this.tableDetail = tableDetail;
    }

    public Integer getFirstRecord() {
        if (firstRecord == null) {
            return 0;
        } else {
            return firstRecord;
        }
    }

    public void setFirstRecord(final Integer firstRecord) {
        this.firstRecord = firstRecord;
    }

    public Integer getPageSize() {
        if (pageSize == null) {
            return 10;
        } else {
            return pageSize;
        }
    }

    public void setPageSize(final Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getSearchBy() {
        return searchBy;
    }

    public void setSearchBy(final String searchBy) {
        this.searchBy = searchBy;
    }

    public String getSearchValue() {
        return searchValue;
    }

    public void setSearchValue(final String searchValue) {
        this.searchValue = searchValue;
    }

    public Long getAdjustment() {
        return adjustment;
    }

    public void setAdjustment(final Long adjustment) {
        this.adjustment = adjustment;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(final Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(final Date toDate) {
        this.toDate = toDate;
    }

    protected SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public String getTable() {
        return table;
    }

    public void setTable(final String table) {
        this.table = table;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public Set<String> getStatementConsolidation() {
        return statementConsolidation;
    }

    public boolean shouldConsolidateStatement() {
        return getStatementConsolidation() != null && statementConsolidation.contains("consolidate");
    }

    public void setStatementConsolidation(final Set<String> statementConsolidation) {
        this.statementConsolidation = statementConsolidation;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(final String transactionType) {
        this.transactionType = transactionType;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(final String provider) {
        this.provider = provider;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(final String reference) {
        this.reference = reference;
    }

    public String getExternalTransactionId() {
        return externalTransactionId;
    }

    public void setExternalTransactionId(final String externalTransactionId) {
        this.externalTransactionId = externalTransactionId;
    }

    public Boolean getBlockPlayer() {
        return blockPlayer;
    }

    public void setBlockPlayer(final Boolean blockPlayer) {
        this.blockPlayer = blockPlayer;
    }

    public void setSearch(final String search) {
        this.search = search;
    }

    public String getSearch() {
        return search;
    }

    public void setPagedRequest(final boolean pagedRequest) {
        this.pagedRequest = pagedRequest;
    }

    public boolean isPagedRequest() {
        return pagedRequest;
    }
}
