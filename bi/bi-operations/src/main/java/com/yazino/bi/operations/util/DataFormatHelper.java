package com.yazino.bi.operations.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.yazino.bi.operations.view.DateFormat;

/**
 * Formatting tools for the Spring MVC front-end
 */
public final class DataFormatHelper {
    private static final DataFormatHelper INSTANCE = new DataFormatHelper();

    private static final DecimalFormat PERCENTAGE = new DecimalFormat("#,##0.000%",
            DecimalFormatSymbols.getInstance(Locale.FRANCE));
    private static final DecimalFormat INTEGER = new DecimalFormat("#,##0",
            DecimalFormatSymbols.getInstance(Locale.FRANCE));
    private static final DecimalFormat POUNDS = new DecimalFormat("&pound\';\'#,##0.00",
            DecimalFormatSymbols.getInstance(Locale.FRANCE));
    private static final DecimalFormat DOLLARS = new DecimalFormat("$#,##0.00",
            DecimalFormatSymbols.getInstance(Locale.FRANCE));
    private static final DecimalFormat CHIPS = new DecimalFormat("#,##0.00",
            DecimalFormatSymbols.getInstance(Locale.FRANCE));
    private static final DecimalFormat EUROS = new DecimalFormat("#,##0.00&euro\';\'",
            DecimalFormatSymbols.getInstance(Locale.FRANCE));
    private static final DecimalFormat DOUBLE = new DecimalFormat("#,##0.00",
            DecimalFormatSymbols.getInstance(Locale.FRANCE));
    private static final DecimalFormat PERCENTS = new DecimalFormat("#,##0.00%",
            DecimalFormatSymbols.getInstance(Locale.FRANCE));
    private static final DateTimeFormatter DATE_TIME = DateTimeFormat.forPattern(DateFormat.DEFAULT);
    private static final double ONE_HUNDRED = 100D;

    /**
     * Singleton private construction
     */
    private DataFormatHelper() {
    }

    public static DataFormatHelper getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the list of headers based on the list of rows to display
     *
     * @param rows Rows list, as returned from the controller
     * @return List of column headers to display
     */
    public static List<String> headers(final List<Object> rows) {
        final List<String> headers = new ArrayList<String>();
        if (rows.size() == 0) {
            return headers;
        }
        final ReportColumnsExtractorHelper extractor = ReportColumnsExtractorHelper.forClass(rows.get(0).getClass());
        for (final ReportFieldInformation fieldInfo : extractor.getFieldsList()) {
            headers.add(fieldInfo.getFieldHeader());
        }
        return headers;
    }

    /**
     * Returns the displayable field information for a particular row
     *
     * @param row Row to display the information for
     * @return Full field view info
     */
    public static List<ReportFieldInformation> fieldInfos(final Object row) {
        final List<ReportFieldInformation> retval = new ArrayList<ReportFieldInformation>();
        final ReportColumnsExtractorHelper extractor = ReportColumnsExtractorHelper.forClass(row.getClass());
        for (final ReportFieldInformation fieldInformation : extractor.getFields(row).values()) {
            final Object fieldValue = fieldInformation.getFieldValue();
            if (fieldValue == null) {
                fieldInformation.setFieldValueFormatted("");
            } else {
                switch (fieldInformation.getFormat()) {
                    case INTEGER:
                    case TOTALS_INTEGER:
                    case ORGANIC_INTEGER:
                    case NONAD_INTEGER:
                    case SUBTOTALS_INTEGER:
                    case OG_INTEGER:
                    case PROMOS_INTEGER:
                    case EMAILS_INTEGER:
                    case MAUDAU_INTEGER:
                    case TWITTER_INTEGER:
                    case CUSTOM_INTEGER:
                    case IOS_INTEGER:
                    case ANDROID_INTEGER:
                        fieldInformation.setFieldValueFormatted(INTEGER.format(fieldValue));
                        break;
                    case CHIP:
                        fieldInformation.setFieldValueFormatted(CHIPS.format(fieldValue));
                        break;
                    case DOLLAR:
                    case TOTALS_DOLLARS:
                        fieldInformation.setFieldValueFormatted(DOLLARS.format(fieldValue));
                        break;
                    case POUND:
                    case TOTALS_POUNDS:
                    case ORGANIC_POUNDS:
                    case NONAD_POUNDS:
                    case SUBTOTALS_POUNDS:
                    case OG_POUNDS:
                    case PROMOS_POUNDS:
                    case EMAILS_POUNDS:
                    case MAUDAU_POUNDS:
                    case TWITTER_POUNDS:
                    case CUSTOM_POUNDS:
                    case IOS_POUNDS:
                    case ANDROID_POUNDS:
                        fieldInformation.setFieldValueFormatted(POUNDS.format(fieldValue));
                        break;
                    case EURO:
                    case TOTALS_EUROS:
                        fieldInformation.setFieldValueFormatted(EUROS.format(fieldValue));
                        break;
                    case DOUBLE:
                    case TOTALS_DOUBLE:
                    case ORGANIC_DOUBLE:
                    case NONAD_DOUBLE:
                    case SUBTOTALS_DOUBLE:
                    case OG_DOUBLE:
                    case PROMOS_DOUBLE:
                    case EMAILS_DOUBLE:
                    case MAUDAU_DOUBLE:
                    case TWITTER_DOUBLE:
                    case CUSTOM_DOUBLE:
                    case IOS_DOUBLE:
                    case ANDROID_DOUBLE:
                        fieldInformation.setFieldValueFormatted(DOUBLE.format(fieldValue));
                        break;
                    case PERCENTAGE:
                    case TOTALS_PERCENTAGE:
                    case ORGANIC_PERCENTAGE:
                    case NONAD_PERCENTAGE:
                    case SUBTOTALS_PERCENTAGE:
                    case OG_PERCENTAGE:
                    case PROMOS_PERCENTAGE:
                    case EMAILS_PERCENTAGE:
                    case MAUDAU_PERCENTAGE:
                    case TWITTER_PERCENTAGE:
                    case CUSTOM_PERCENTAGE:
                    case IOS_PERCENTAGE:
                    case ANDROID_PERCENTAGE:
                        fieldInformation.setFieldValueFormatted(PERCENTS.format(fieldValue));
                        break;
                    case DATE_TIME:
                        fieldInformation.setFieldValueFormatted(DATE_TIME.print((DateTime) fieldValue));
                        break;
                    default:
                        fieldInformation.setFieldValueFormatted(fieldValue.toString());
                }
            }
            retval.add(fieldInformation);
        }
        return retval;
    }

    /**
     * Formats the number as a percentage
     *
     * @param pc Decimal number representing the percentage
     * @return Formatted string
     */
    public static String formatPercentage(final double pc) {
        return PERCENTAGE.format(pc);
    }

    /**
     * Formats the number as a percentage
     *
     * @param num Integer number
     * @return Formatted string
     */
    public static String formatInteger(final long num) {
        return INTEGER.format(num);
    }

    /**
     * Formats the number as an amount in pounds
     *
     * @param num Double number
     * @return Formatted string
     */
    public static String formatPounds(final double num) {
        return POUNDS.format(Math.round(num * ONE_HUNDRED) / ONE_HUNDRED);
    }

    /**
     * Formats the number as an amount in dollars
     *
     * @param num Double number
     * @return Formatted string
     */
    public static String formatDollars(final double num) {
        return DOLLARS.format(Math.round(num * ONE_HUNDRED) / ONE_HUNDRED);
    }

    /**
     * Formats the number as an amount in euros
     *
     * @param num Double number
     * @return Formatted string
     */
    public static String formatEuros(final double num) {
        return EUROS.format(Math.round(num * ONE_HUNDRED) / ONE_HUNDRED);
    }

    /**
     * Formats the double number
     *
     * @param num Double number
     * @return Formatted string
     */
    public static String formatDouble(final double num) {
        return DOUBLE.format(Math.round(num * ONE_HUNDRED) / ONE_HUNDRED);
    }

    /**
     * Returns zero if the number object is null
     *
     * @param value Source value
     * @return Copy of the value or zero if the value is null
     */
    public static double zeroForNull(final Double value) {
        if (value == null) {
            return 0D;
        } else {
            return value;
        }
    }

    /**
     * Returns zero if the number object is null
     *
     * @param value Source value
     * @return Copy of the value or zero if the value is null
     */
    public static long zeroForNull(final Long value) {
        if (value == null) {
            return 0L;
        } else {
            return value;
        }
    }

    /**
     * Extracts a string from the object if the object is not null
     *
     * @param object Object to represent as a string
     * @return String value of the object if not null, empty string otherwise
     */
    public static String notNull(final Object object) {
        if (object == null) {
            return "";
        } else {
            return object.toString();
        }
    }

    /**
     * Extracts a date string from the object if the object is not null
     *
     * @param object Object to represent as a string
     * @return String date value of the object if nut null, empty string otherwise
     */
    public static String dateNotNull(final Object object) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (object == null) {
            return "";
        } else {
            return dateFormat.format((Date) object);
        }
    }
}
