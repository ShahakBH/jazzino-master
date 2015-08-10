package com.yazino.platform.invitation.persistence;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import static org.apache.commons.lang3.Validate.notNull;

class PreparedStatementFormatter {
    private static final String SQL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public String format(final String sql,
                         final Object... arguments) {
        notNull(sql, "sql may not be null");

        final StringTokenizer sqlTokeniser = new StringTokenizer(sql, "?", true);

        final StringBuilder formattedSql = new StringBuilder(sqlTokeniser.nextToken());
        int argumentIndex = 0;
        while (sqlTokeniser.hasMoreTokens()) {
            final String token = sqlTokeniser.nextToken();
            if ("?".equals(token)) {
                if (argumentIndex < arguments.length) {
                    appendArgument(formattedSql, arguments[argumentIndex]);
                }
                ++argumentIndex;
            } else {
                formattedSql.append(token);
            }
        }
        return formattedSql.toString();
    }

    private void appendArgument(final StringBuilder formattedSql, final Object argument) {
        if (argument instanceof CharSequence || argument instanceof Character) {
            formattedSql.append("'").append(escapeSql(stringOf(argument))).append("'");
        } else if (argument instanceof Date) {
            formattedSql.append("timestamp('").append(formatAsValidTimestamp((Date) argument)).append("')");
        } else if (argument instanceof DateTime) {
            formattedSql.append("timestamp('").append(formatAsValidTimestamp(
                    ((DateTime) argument).toDate())).append("')");
        } else {
            formattedSql.append(argument);
        }
    }

    private String formatAsValidTimestamp(final java.util.Date date) {
        final SimpleDateFormat format
                = new SimpleDateFormat(SQL_DATE_FORMAT); // use new instance as not thread-safe
        return format.format(date);
    }

    private String escapeSql(final String str) {
        if (str == null) {
            return null;
        }
        return StringUtils.replace(str, "'", "''");
    }

    private String stringOf(final Object argument) {
        if (argument == null) {
            return null;
        }
        return argument.toString();
    }

    public PreparedStatementFormatter() {
        super();
    }
}
