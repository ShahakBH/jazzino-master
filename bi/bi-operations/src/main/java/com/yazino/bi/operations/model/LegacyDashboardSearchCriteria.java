package com.yazino.bi.operations.model;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class LegacyDashboardSearchCriteria extends DashboardSearchCriteria {

    /**     * Sets the search criteria specific to payment dashboard
         *
         * @param dateSearchBy         Search parameter
         * @param dateFrom             Lower date limit
         * @param dateTo               Higher date limit (inclusive)
         * @param providerSearchBy     Search parameter for games
         * @param provider             Game type to select
         * @param referenceSearchBy    Search parameter for tables
         * @param reference            Table to look for
         * @param externalIdSearchBy   Search parameter for transaction types
         * @param externalId           Transaction type to look for
         * @param additionalParameters Additional params
         */
        public void setPaymentSearch(final String dateSearchBy, final Date dateFrom, final Date dateTo,
                                     final String providerSearchBy, final String provider, final String referenceSearchBy,
                                     final String reference, final String externalIdSearchBy, final String externalId,
                                     final Object... additionalParameters) {
            if (dateFrom == null) {
                setSearchParameters(additionalParameters);
                return;
            }

            final List<Object> paramsList = new ArrayList<Object>();

            String paymentSearchString = "";
            if (provider != null && !"".equals(provider)) {
                paymentSearchString = " AND " + providerSearchBy + " LIKE ?";
                paramsList.add(provider);
            }

            if (reference != null && !"".equals(reference)) {
                paymentSearchString += " AND " + referenceSearchBy + " LIKE ?";
                paramsList.add(reference);
            }

            if (externalId != null && !"".equals(externalId)) {
                paymentSearchString += " AND " + externalIdSearchBy + " LIKE ?";
                paramsList.add(externalId);
            }

            setSearchString(paymentSearchString);

            createDateSearchString(dateSearchBy, dateFrom, dateTo, paramsList, paymentSearchString);

            fillParamsList(paramsList, additionalParameters);
        }

}
