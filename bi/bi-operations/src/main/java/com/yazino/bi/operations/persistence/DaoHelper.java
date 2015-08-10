package com.yazino.bi.operations.persistence;

public final class DaoHelper {
    private DaoHelper() {
    }

    public static String weekLimitsRequest(final String field) {
        final StringBuilder request =
                new StringBuilder("concat(date_format(DATE_SUB(").append(field).append(", INTERVAL WEEKDAY(")
                        .append(field).append(") DAY),'%d/%m/%Y'),date_format(DATE_ADD(").append(field)
                        .append(", INTERVAL(6-WEEKDAY(").append(field).append(")) DAY),' to %d/%m/%Y'))");
        return request.toString();
    }

    public static String weekGroup(final String field) {
        final StringBuilder request = new StringBuilder("week(").append(field).append(",5)");
        return request.toString();
    }
}
