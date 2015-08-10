package com.yazino.bi.operations.view;

import jxl.format.CellFormat;

import static com.yazino.bi.operations.view.CellFormats.*;

/**
 * Data format types for different report columns
 */
public enum ReportColumnFormat {
    /**
     * Default string field
     */
    STRING_DEFAULT,
    /**
     * Default integer field
     */
    INTEGER(INTEGER_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Dollar currency field
     */
    DOLLAR(DOLLAR_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Pound currency field
     */
    POUND(POUND_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Euro currency field
     */
    EURO(EURO_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Chips currency field
     */
    CHIP(CHIP_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double precision number field
     */
    DOUBLE(DOUBLE_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Percentage number field
     */
    PERCENTAGE(PERCENTAGE_FORMAT, NumberCellCreator.getInstance()),
    /**
     * DateTime field
     */
    DATE_TIME(DATE_TIME_FORMAT, DateTimeCellCreator.getInstance()),
    /**
     * String field for totals
     */
    TOTALS_STRING(DEFAULT_TOTALS_FORMAT, TextCellCreator.getInstance()),
    /**
     * INTEGER field for totals
     */
    TOTALS_INTEGER(INTEGER_TOTALS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Dollar field for totals
     */
    TOTALS_DOLLARS(DOLLAR_TOTALS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Pound field for totals
     */
    TOTALS_POUNDS(POUND_TOTALS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Euro field for totals
     */
    TOTALS_EUROS(EURO_TOTALS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for totals
     */
    TOTALS_DOUBLE(DOUBLE_TOTALS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Percentage field for totals
     */
    TOTALS_PERCENTAGE(PERCENTAGE_TOTALS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * String field for organic data
     */
    ORGANIC_STRING(DEFAULT_ORGANIC_FORMAT, TextCellCreator.getInstance()),
    /**
     * INTEGER field for organic data
     */
    ORGANIC_INTEGER(DEFAULT_ORGANIC_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    ORGANIC_DOUBLE(DOUBLE_ORGANIC_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    ORGANIC_POUNDS(POUNDS_ORGANIC_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Percentage field for organic data
     */
    ORGANIC_PERCENTAGE(PERCENTAGE_ORGANIC_FORMAT, NumberCellCreator.getInstance()),

    CUSTOM_STRING(DEFAULT_CUSTOM_FORMAT, TextCellCreator.getInstance()),
    CUSTOM_INTEGER(DEFAULT_CUSTOM_FORMAT, NumberCellCreator.getInstance()),
    CUSTOM_DOUBLE(DOUBLE_CUSTOM_FORMAT, NumberCellCreator.getInstance()),
    CUSTOM_POUNDS(POUNDS_CUSTOM_FORMAT, NumberCellCreator.getInstance()),
    CUSTOM_PERCENTAGE(PERCENTAGE_CUSTOM_FORMAT, NumberCellCreator.getInstance()),

    IOS_STRING(DEFAULT_IOS_FORMAT, TextCellCreator.getInstance()),
    IOS_INTEGER(DEFAULT_IOS_FORMAT, NumberCellCreator.getInstance()),
    IOS_DOUBLE(DOUBLE_IOS_FORMAT, NumberCellCreator.getInstance()),
    IOS_POUNDS(POUNDS_IOS_FORMAT, NumberCellCreator.getInstance()),
    IOS_PERCENTAGE(PERCENTAGE_IOS_FORMAT, NumberCellCreator.getInstance()),

    ANDROID_STRING(DEFAULT_ANDROID_FORMAT, TextCellCreator.getInstance()),
    ANDROID_INTEGER(DEFAULT_ANDROID_FORMAT, NumberCellCreator.getInstance()),
    ANDROID_DOUBLE(DOUBLE_ANDROID_FORMAT, NumberCellCreator.getInstance()),
    ANDROID_POUNDS(POUNDS_ANDROID_FORMAT, NumberCellCreator.getInstance()),
    ANDROID_PERCENTAGE(PERCENTAGE_ANDROID_FORMAT, NumberCellCreator.getInstance()),
    /**
     * String field for organic data
     */
    NONAD_STRING(DEFAULT_NONAD_FORMAT, TextCellCreator.getInstance()),
    /**
     * INTEGER field for organic data
     */
    NONAD_INTEGER(DEFAULT_NONAD_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    NONAD_DOUBLE(DOUBLE_NONAD_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    NONAD_POUNDS(POUNDS_NONAD_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Percentage field for organic data
     */
    NONAD_PERCENTAGE(PERCENTAGE_NONAD_FORMAT, NumberCellCreator.getInstance()),
    /**
     * String field for organic data
     */
    OG_STRING(DEFAULT_OG_FORMAT, TextCellCreator.getInstance()),
    /**
     * INTEGER field for organic data
     */
    OG_INTEGER(DEFAULT_OG_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    OG_DOUBLE(DOUBLE_OG_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    OG_POUNDS(POUNDS_OG_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Percentage field for organic data
     */
    OG_PERCENTAGE(PERCENTAGE_OG_FORMAT, NumberCellCreator.getInstance()),
    /**
     * String field for organic data
     */
    PROMOS_STRING(DEFAULT_PROMOS_FORMAT, TextCellCreator.getInstance()),
    /**
     * INTEGER field for organic data
     */
    PROMOS_INTEGER(DEFAULT_PROMOS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    PROMOS_DOUBLE(DOUBLE_PROMOS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    PROMOS_POUNDS(POUNDS_PROMOS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Percentage field for organic data
     */
    PROMOS_PERCENTAGE(PERCENTAGE_PROMOS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * String field for organic data
     */
    EMAILS_STRING(DEFAULT_EMAILS_FORMAT, TextCellCreator.getInstance()),
    /**
     * INTEGER field for organic data
     */
    EMAILS_INTEGER(DEFAULT_EMAILS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    EMAILS_DOUBLE(DOUBLE_EMAILS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    EMAILS_POUNDS(POUNDS_EMAILS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Percentage field for organic data
     */
    EMAILS_PERCENTAGE(PERCENTAGE_EMAILS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * String field for organic data
     */
    MAUDAU_STRING(DEFAULT_MAUDAU_FORMAT, TextCellCreator.getInstance()),
    /**
     * INTEGER field for organic data
     */
    MAUDAU_INTEGER(DEFAULT_MAUDAU_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    MAUDAU_DOUBLE(DOUBLE_MAUDAU_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    MAUDAU_POUNDS(POUNDS_MAUDAU_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Percentage field for organic data
     */
    MAUDAU_PERCENTAGE(PERCENTAGE_MAUDAU_FORMAT, NumberCellCreator.getInstance()),
    /**
     * String field for organic data
     */
    TWITTER_STRING(DEFAULT_TWITTER_FORMAT, TextCellCreator.getInstance()),
    /**
     * INTEGER field for organic data
     */
    TWITTER_INTEGER(DEFAULT_TWITTER_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    TWITTER_DOUBLE(DOUBLE_TWITTER_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    TWITTER_POUNDS(POUNDS_TWITTER_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Percentage field for organic data
     */
    TWITTER_PERCENTAGE(PERCENTAGE_TWITTER_FORMAT, NumberCellCreator.getInstance()),
    /**
     * String field for organic data
     */
    SUBTOTALS_STRING(DEFAULT_SUBTOTALS_FORMAT, TextCellCreator.getInstance()),
    /**
     * INTEGER field for organic data
     */
    SUBTOTALS_INTEGER(DEFAULT_SUBTOTALS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    SUBTOTALS_DOUBLE(DOUBLE_SUBTOTALS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Double field for organic data
     */
    SUBTOTALS_POUNDS(POUNDS_SUBTOTALS_FORMAT, NumberCellCreator.getInstance()),
    /**
     * Percentage field for organic data
     */
    SUBTOTALS_PERCENTAGE(PERCENTAGE_SUBTOTALS_FORMAT, NumberCellCreator.getInstance());

    private final CellFormat excelHeaderFormat;

    private final CellFormat excelCellFormat;

    private final ExcelCellCreator cellCreator;

    /**
     * Constructs the column format definition with default settings
     */
    private ReportColumnFormat() {
        this.excelHeaderFormat = DEFAULT_HEADER_FORMAT.getFormat();
        this.excelCellFormat = DEFAULT_FORMAT.getFormat();
        this.cellCreator = TextCellCreator.getInstance();
    }

    /**
     * Constructs the column format definition with default settings
     *
     * @param cellFormat  Cell format for the column
     * @param cellCreator Cell creator for the column
     */
    private ReportColumnFormat(final CellFormats cellFormat, final ExcelCellCreator cellCreator) {
        this.excelHeaderFormat = DEFAULT_HEADER_FORMAT.getFormat();
        this.excelCellFormat = cellFormat.getFormat();
        this.cellCreator = cellCreator;
    }

    public CellFormat getExcelHeaderFormat() {
        return excelHeaderFormat;
    }

    public CellFormat getExcelCellFormat() {
        return excelCellFormat;
    }

    public ExcelCellCreator getCellCreator() {
        return cellCreator;
    }

    public String getHtmlClassName() {
        return name().toLowerCase();
    }
}
