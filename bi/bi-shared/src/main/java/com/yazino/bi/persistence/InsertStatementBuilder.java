package com.yazino.bi.persistence;

import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Build batched insert statements, for drivers that don't support rewriting them.
 * <p/>
 * Note that this is tied to PostgreSQL 9.1 and above at present, due to the use of escape strings.
 */
public class InsertStatementBuilder {
    private static final ThreadLocal<SimpleDateFormat> TIMESTAMP_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        }
    };

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };

    private final StringBuilder stmt = new StringBuilder();

    private boolean firstValue = true;

    public InsertStatementBuilder(final String tableName, final String... fields) {
        notNull(tableName, "tableName may not be null");
        notEmpty(fields, "fields may not be null or empty");

        stmt.append("INSERT INTO ")
                .append(tableName)
                .append(" (");
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                stmt.append(",");
            }
            stmt.append(fields[i]);
        }
        stmt.append(") VALUES ");
    }

    public InsertStatementBuilder withValues(final String... values) {
        if (!firstValue) {
            stmt.append(",");
        } else {
            firstValue = false;
        }

        stmt.append("(");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                stmt.append(",");
            }
            stmt.append(values[i]);
        }
        stmt.append(")");
        return this;
    }

    public String toSql() {
        final String sqlStatement = stmt.toString();
        if (sqlStatement.endsWith("VALUES ")) {
            throw new IllegalStateException("No values have been set");
        }
        return sqlStatement;
    }

    public static String sqlBigDecimal(final BigDecimal bigDecimalValue) {
        if (bigDecimalValue != null) {
            return bigDecimalValue.toPlainString();
        }
        return "NULL";
    }

    public static String sqlString(final String stringValue) {
        if (stringValue != null) {
            return escapeString(replaceNullChar(stringValue));
        }
        return "NULL";
    }

    private static String replaceNullChar(final String stringValue) {
        return stringValue.replace("\u0000", "");
    }

    private static String escapeString(final String stringValue) {
        // this escaping code is taken from the MySQL Connector/J v5.1.25, with some MySQL oddities stripped, and
        // E added to trigger PostgreSQL 9.1+ escape string support

        final StringBuilder buf = new StringBuilder((int) (stringValue.length() * 1.1));
        buf.append('\'');

        boolean escapeUsed = false;

        for (int i = 0; i < stringValue.length(); ++i) {
            char c = stringValue.charAt(i);

            switch (c) {
                case 0: /* Must be escaped for 'mysql' */
                    escapeUsed = true;
                    buf.append('\\');
                    buf.append('0');

                    break;

                case '\n': /* Must be escaped for logs */
                    escapeUsed = true;
                    buf.append('\\');
                    buf.append('n');

                    break;

                case '\r':
                    escapeUsed = true;
                    buf.append('\\');
                    buf.append('r');

                    break;

                case '\\':
                    escapeUsed = true;
                    buf.append('\\');
                    buf.append('\\');

                    break;

                case '\'':
                    escapeUsed = true;
                    buf.append('\\');
                    buf.append('\'');

                    break;

                default:
                    buf.append(c);
            }
        }

        buf.append('\'');
        if (escapeUsed) {
            return buf.insert(0, 'E').toString();
        }
        return buf.toString();
    }

    public static String sqlBoolean(final Boolean booleanValue) {
        if (booleanValue != null) {
            return booleanValue.toString();
        }
        return "NULL";
    }

    public static String sqlLong(final Long longValue) {
        if (longValue != null) {
            return longValue.toString();
        }
        return "NULL";
    }

    public static String sqlInt(final Integer intValue) {
        if (intValue != null) {
            return intValue.toString();
        }
        return "NULL";
    }

    public static String sqlTimestamp(final Date timestampValue) {
        if (timestampValue != null) {
            return "'" + TIMESTAMP_FORMAT.get().format(timestampValue) + "'";
        }
        return "NULL";
    }

    public static String sqlTimestamp(final DateTime timestampValue) {
        if (timestampValue != null) {
            return "'" + TIMESTAMP_FORMAT.get().format(timestampValue.toDate()) + "'";
        }
        return "NULL";
    }

    public static String sqlDate(final Date dateValue) {
        if (dateValue != null) {
            return "'" + DATE_FORMAT.get().format(dateValue) + "'";
        }
        return "NULL";
    }

}
