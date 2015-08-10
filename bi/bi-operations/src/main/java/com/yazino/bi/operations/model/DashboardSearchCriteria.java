package com.yazino.bi.operations.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Internal search criteria for the SQL requests
 */
public class DashboardSearchCriteria {
    private Object[] searchParameters;
    private String searchString;
    private String havingString;

    /**
     * Initializing the criteria to default values
     */
    public DashboardSearchCriteria() {
        setSearchString("");
        setHavingString("");
        setSearchParameters(new Object[]{});
    }

    /**
     * Sets the search criteria specific to games dashboard
     *
     * @param dateSearchBy         Search parameter
     * @param dateFrom             Lower date limit
     * @param dateTo               Higher date limit (inclusive)
     * @param gameSearchBy         Search parameter for games
     * @param gameType             Game type to select
     * @param tableSearchBy        Search parameter for tables
     * @param tableId              Table to look for
     * @param ttSearchBy           Search parameter for transaction types
     * @param transactionType      Transaction type to look for
     * @param additionalParameters Additional params
     */
    public void setGameSearch(final String dateSearchBy,
                              final Date dateFrom,
                              final Date dateTo,
                              final String gameSearchBy,
                              final String gameType,
                              final String tableSearchBy,
                              final BigDecimal tableId,
                              final String ttSearchBy,
                              final String transactionType,
                              final Object... additionalParameters) {
        if (dateFrom == null) {
            setSearchParameters(additionalParameters);
            return;
        }

        final List<Object> paramsList = new ArrayList<>();

        String gameSearchString = "";
        if (gameType != null && !"".equals(gameType)) {
            gameSearchString = " AND " + gameSearchBy + " = ?";
            paramsList.add(gameType);
        }

        if (tableId != null) {
            gameSearchString += " AND " + tableSearchBy + " = ?";
            paramsList.add(tableId);
        }

        if (transactionType != null && !"".equals(transactionType)) {
            gameSearchString += " AND " + ttSearchBy + " LIKE ?";
            paramsList.add(transactionType);
        }

        setSearchString(gameSearchString);

        createDateSearchString(dateSearchBy, dateFrom, dateTo, paramsList, gameSearchString);

        fillParamsList(paramsList, additionalParameters);
    }

    /**
     * Sets the search criteria by dates
     *
     * @param dateSearchBy         Search parameter
     * @param dateFrom             Lower date limit
     * @param dateTo               Higher date limit (inclusive)
     * @param additionalParameters Additional params
     */
    public void setDatesSearch(final String dateSearchBy, final Date dateFrom, final Date dateTo,
                               final Object... additionalParameters) {
        final List<Object> paramsList = new ArrayList<>();

        createDateSearchString(dateSearchBy, dateFrom, dateTo, paramsList, searchString);

        fillParamsList(paramsList, additionalParameters);
    }

    /**
     * Creates a query string for date search
     *
     * @param dateSearchBy      Search parameter
     * @param dateFrom          Lower date limit
     * @param dateTo            Higher date limit
     * @param paramsList        Parameters list to fill
     * @param basicSearchString Basic search string
     */
    protected void createDateSearchString(final String dateSearchBy,
                                          final Date dateFrom,
                                          final Date dateTo,
                                          final List<Object> paramsList,
                                          final String basicSearchString) {
        paramsList.add(new Timestamp(dateFrom.getTime()));

        final String dateSearchString;
        if (dateTo == null) {
            dateSearchString = dateSearchBy + " = ?";
        } else {
            dateSearchString = dateSearchBy + " > ? AND " + dateSearchBy + " < ?";
            paramsList.add(new Timestamp(dateTo.getTime()));
        }

        if (dateSearchBy.contains("MAX(")) {
            setHavingString(" HAVING " + dateSearchString);
        } else {
            setSearchString(basicSearchString + " AND " + dateSearchString);
        }
    }

    /**
     * Merges two parameter lists and sets them to the actual search parameters
     *
     * @param paramsList           Parameters list filled dynamically
     * @param additionalParameters Additional params
     */
    protected void fillParamsList(final List<Object> paramsList, final Object[] additionalParameters) {
        final int listSize = paramsList.size();
        final int additionalLength = additionalParameters.length;
        final Object[] params = new Object[listSize + additionalLength];
        for (int i = 0; i < additionalLength; i++) {
            params[i] = additionalParameters[i];
        }
        for (int i = 0; i < listSize; i++) {
            params[i + additionalLength] = paramsList.get(i);
        }
        setSearchParameters(params);
    }

    /**
     * Constructs the search criteria from the request parameters
     *
     * @param searchBy             Search criteria
     * @param searchValue          Lower limit (or unique condition) for the search
     *                             If it contains a "--" string, this string is treated as limits separator
     *                             It it contains a "%" string, it is considered as a string search pattern
     * @param additionalParameters Additional parameters for the requests
     */
    public DashboardSearchCriteria(final String searchBy, final String searchValue,
                                   final Object... additionalParameters) {
        if (searchBy == null || searchValue == null || "".equals(searchValue)) {
            setSearchString("");
            setHavingString("");
            setSearchParameters(additionalParameters);
            return;
        }

        String searchFrom = null, searchTo = null;

        final int limitPos = searchValue.indexOf("--");
        if (limitPos > 0) {
            searchFrom = searchValue.substring(0, limitPos);
            searchTo = searchValue.substring(limitPos + 2);
        } else {
            searchFrom = searchValue;
            searchTo = null;
        }

        final String dashBoardSearchString;
        final Object[] dashBoardSearchParameters;
        if (searchTo != null) {
            dashBoardSearchString = searchBy + " BETWEEN ? AND ?";
            dashBoardSearchParameters = new Object[additionalParameters.length + 2];
            dashBoardSearchParameters[dashBoardSearchParameters.length - 2] = searchFrom;
            dashBoardSearchParameters[dashBoardSearchParameters.length - 1] = searchTo;
        } else {
            if (searchFrom.contains("%")) {
                dashBoardSearchString = searchBy + " LIKE ?";
            } else {
                dashBoardSearchString = searchBy + " = ?";
            }
            dashBoardSearchParameters = new Object[additionalParameters.length + 1];
            dashBoardSearchParameters[dashBoardSearchParameters.length - 1] = searchFrom;
        }
        for (int i = 0; i < additionalParameters.length; i++) {
            dashBoardSearchParameters[i] = additionalParameters[i];
        }
        setSearchParameters(dashBoardSearchParameters);
        if (searchBy.contains("SUM(") || searchBy.contains("MAX(")) {
            setHavingString(" HAVING " + dashBoardSearchString);
            setSearchString("");
        } else if (!"".equals(searchBy)) {
            setHavingString("");
            setSearchString(" AND " + dashBoardSearchString);
        } else {
            setHavingString("");
            setSearchString("");
        }
    }

    public Object[] getSearchParameters() {
        return searchParameters;
    }

    public void setSearchParameters(final Object[] searchParameters) {
        this.searchParameters = searchParameters;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(final String searchString) {
        this.searchString = searchString;
    }

    public String getHavingString() {
        return havingString;
    }

    public void setHavingString(final String havingString) {
        this.havingString = havingString;
    }

}
