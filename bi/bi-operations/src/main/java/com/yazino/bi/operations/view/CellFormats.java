package com.yazino.bi.operations.view;

import static jxl.format.Colour.*;
import static jxl.format.UnderlineStyle.NO_UNDERLINE;
import static jxl.write.NumberFormat.COMPLEX_FORMAT;
import static jxl.write.NumberFormat.CURRENCY_DOLLAR;
import static jxl.write.NumberFormat.CURRENCY_EURO_SUFFIX;
import static jxl.write.NumberFormat.CURRENCY_POUND;
import static jxl.write.WritableFont.BOLD;
import static jxl.write.WritableFont.NO_BOLD;
import static jxl.write.WritableFont.TAHOMA;
import jxl.biff.DisplayFormat;
import jxl.format.CellFormat;
import jxl.format.Colour;
import jxl.write.NumberFormat;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableFont.FontName;
import jxl.write.WriteException;

/**
 * Defines default Excel cell formats
 */
public enum CellFormats {
    /**
     * Default cell
     */
    DEFAULT_FORMAT(TAHOMA, 10, false),
    /**
     * Header cell
     */
    DEFAULT_HEADER_FORMAT(TAHOMA, 10, true, YELLOW, BLUE, null),
    /**
     * Integer cell
     */
    INTEGER_FORMAT(TAHOMA, 10, false, new NumberFormat("#,##0", COMPLEX_FORMAT)),
    /**
     * Double number format
     */
    DOUBLE_FORMAT(TAHOMA, 10, false, BLACK, null, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    /**
     * Percentage format
     */
    PERCENTAGE_FORMAT(TAHOMA, 10, false, BLACK, null, new NumberFormat("#,##0.00%", COMPLEX_FORMAT)),
    /**
     * DateTime format
     */
    DATE_TIME_FORMAT(TAHOMA, 10, false, BLACK, null, new jxl.write.DateFormat(
            DateFormat.DEFAULT)),
    /**
     * Dollar currency cell
     */
    DOLLAR_FORMAT(TAHOMA, 10, false, BLACK, null, new NumberFormat(CURRENCY_DOLLAR + "#,##0.00", COMPLEX_FORMAT)),
    /**
     * Pound currency cell
     */
    POUND_FORMAT(TAHOMA, 10, false, BLACK, null, new NumberFormat(CURRENCY_POUND + "#,##0.00", COMPLEX_FORMAT)),
    /**
     * Chip currency cell
     */
    CHIP_FORMAT(TAHOMA, 10, false, BLACK, null, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    /**
     * Euro currency cell
     */
    EURO_FORMAT(TAHOMA, 10, false, BLACK, null, new NumberFormat("#,##0.00" + CURRENCY_EURO_SUFFIX, COMPLEX_FORMAT)),
    /**
     * Totals cell
     */
    DEFAULT_TOTALS_FORMAT(TAHOMA, 10, true, BLACK, CORAL, null),
    /**
     * Totals cell containing a number
     */
    INTEGER_TOTALS_FORMAT(TAHOMA, 10, true, BLACK, CORAL, new NumberFormat("#,##0", COMPLEX_FORMAT)),
    /**
     * Totals cell containing a number
     */
    DOLLAR_TOTALS_FORMAT(TAHOMA, 10, true, BLACK, CORAL,
            new NumberFormat(CURRENCY_DOLLAR + "#,##0.00", COMPLEX_FORMAT)),
    /**
     * Totals cell containing a number
     */
    POUND_TOTALS_FORMAT(TAHOMA, 10, true, BLACK, CORAL, new NumberFormat(CURRENCY_POUND + "#,##0.00", COMPLEX_FORMAT)),
    /**
     * Totals cell containing a number
     */
    EURO_TOTALS_FORMAT(TAHOMA, 10, true, BLACK, CORAL, new NumberFormat("#,##0.00" + CURRENCY_EURO_SUFFIX,
            COMPLEX_FORMAT)),
    /**
     * Totals cell containing a number
     */
    DOUBLE_TOTALS_FORMAT(TAHOMA, 10, true, BLACK, CORAL, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    /**
     * Totals cell containing a percentage
     */
    PERCENTAGE_TOTALS_FORMAT(TAHOMA, 10, true, BLACK, CORAL, new NumberFormat("#,##0.00%", COMPLEX_FORMAT)),
    /**
     * Organic cell
     */
    DEFAULT_ORGANIC_FORMAT(TAHOMA, 10, false, BLACK, LAVENDER, null),
    /**
     * Totals cell containing a number
     */
    DOUBLE_ORGANIC_FORMAT(TAHOMA, 10, false, BLACK, LAVENDER, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    /**
     * Totals cell containing a number
     */
    POUNDS_ORGANIC_FORMAT(TAHOMA, 10, false, BLACK, LAVENDER, new NumberFormat(CURRENCY_POUND + "#,##0.00",
            COMPLEX_FORMAT)),
    /**
     * Totals cell containing a percentage
     */
    PERCENTAGE_ORGANIC_FORMAT(TAHOMA, 10, false, BLACK, LAVENDER, new NumberFormat("#,##0.00%", COMPLEX_FORMAT)),
    /**
     * CUSTOM cell
     */
    DEFAULT_CUSTOM_FORMAT(TAHOMA, 10, false, BLACK, AQUA, null),
    DOUBLE_CUSTOM_FORMAT(TAHOMA, 10, false, BLACK, AQUA, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    POUNDS_CUSTOM_FORMAT(TAHOMA, 10, false, BLACK, AQUA, new NumberFormat(CURRENCY_POUND + "#,##0.00", COMPLEX_FORMAT)),
    PERCENTAGE_CUSTOM_FORMAT(TAHOMA, 10, false, BLACK, AQUA, new NumberFormat("#,##0.00%", COMPLEX_FORMAT)),
    /**
     * IOS cell
     */
    DEFAULT_IOS_FORMAT(TAHOMA, 10, false, BLACK, TEAL, null),
    DOUBLE_IOS_FORMAT(TAHOMA, 10, false, BLACK, TEAL, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    POUNDS_IOS_FORMAT(TAHOMA, 10, false, BLACK, TEAL, new NumberFormat(CURRENCY_POUND + "#,##0.00", COMPLEX_FORMAT)),
    PERCENTAGE_IOS_FORMAT(TAHOMA, 10, false, BLACK, TEAL, new NumberFormat("#,##0.00%", COMPLEX_FORMAT)),
    /**
     * ANDROID cell
     */
    DEFAULT_ANDROID_FORMAT(TAHOMA, 10, false, BLACK, LIME, null),
    DOUBLE_ANDROID_FORMAT(TAHOMA, 10, false, BLACK, LIME, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    POUNDS_ANDROID_FORMAT(TAHOMA, 10, false, BLACK, LIME, new NumberFormat(CURRENCY_POUND + "#,##0.00", COMPLEX_FORMAT)),
    PERCENTAGE_ANDROID_FORMAT(TAHOMA, 10, false, BLACK, LIME, new NumberFormat("#,##0.00%", COMPLEX_FORMAT)),
    /**
     * Organic cell
     */
    DEFAULT_NONAD_FORMAT(TAHOMA, 10, false, BLACK, LIGHT_TURQUOISE, null),
    /**
     * Totals cell containing a number
     */
    DOUBLE_NONAD_FORMAT(TAHOMA, 10, false, BLACK, LIGHT_TURQUOISE, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    /**
     * Totals cell containing a number
     */
    POUNDS_NONAD_FORMAT(TAHOMA, 10, false, BLACK, LIGHT_TURQUOISE, new NumberFormat(CURRENCY_POUND + "#,##0.00",
            COMPLEX_FORMAT)),
    /**
     * Totals cell containing a percentage
     */
    PERCENTAGE_NONAD_FORMAT(TAHOMA, 10, false, BLACK, LIGHT_TURQUOISE, new NumberFormat("#,##0.00%", COMPLEX_FORMAT)),
    /**
     * Organic cell
     */
    DEFAULT_OG_FORMAT(TAHOMA, 10, false, BLACK, YELLOW, null),
    /**
     * Totals cell containing a number
     */
    DOUBLE_OG_FORMAT(TAHOMA, 10, false, BLACK, YELLOW, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    /**
     * Totals cell containing a number
     */
    POUNDS_OG_FORMAT(TAHOMA, 10, false, BLACK, YELLOW, new NumberFormat(CURRENCY_POUND + "#,##0.00",
            COMPLEX_FORMAT)),
    /**
     * Totals cell containing a percentage
     */
    PERCENTAGE_OG_FORMAT(TAHOMA, 10, false, BLACK, YELLOW, new NumberFormat("#,##0.00%", COMPLEX_FORMAT)),
    /**
     * Organic cell
     */
    DEFAULT_PROMOS_FORMAT(TAHOMA, 10, false, BLACK, LIGHT_GREEN, null),
    /**
     * Totals cell containing a number
     */
    DOUBLE_PROMOS_FORMAT(TAHOMA, 10, false, BLACK, LIGHT_GREEN, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    /**
     * Totals cell containing a number
     */
    POUNDS_PROMOS_FORMAT(TAHOMA, 10, false, BLACK, LIGHT_GREEN, new NumberFormat(CURRENCY_POUND + "#,##0.00",
            COMPLEX_FORMAT)),
    /**
     * Totals cell containing a percentage
     */
    PERCENTAGE_PROMOS_FORMAT(TAHOMA, 10, false, BLACK, LIGHT_GREEN, new NumberFormat("#,##0.00%", COMPLEX_FORMAT)),
    /**
     * Organic cell
     */
    DEFAULT_EMAILS_FORMAT(TAHOMA, 10, false, BLACK, LIGHT_BLUE, null),
    /**
     * Totals cell containing a number
     */
    DOUBLE_EMAILS_FORMAT(TAHOMA, 10, false, BLACK, LIGHT_BLUE, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    /**
     * Totals cell containing a number
     */
    POUNDS_EMAILS_FORMAT(TAHOMA, 10, false, BLACK, LIGHT_BLUE, new NumberFormat(CURRENCY_POUND + "#,##0.00",
            COMPLEX_FORMAT)),
    /**
     * Totals cell containing a percentage
     */
    PERCENTAGE_EMAILS_FORMAT(TAHOMA, 10, false, BLACK, LIGHT_BLUE, new NumberFormat("#,##0.00%", COMPLEX_FORMAT)),
    /**
     * Organic cell
     */
    DEFAULT_MAUDAU_FORMAT(TAHOMA, 10, false, BLACK, GREY_25_PERCENT, null),
    /**
     * Totals cell containing a number
     */
    DOUBLE_MAUDAU_FORMAT(TAHOMA, 10, false, BLACK, GREY_25_PERCENT, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    /**
     * Totals cell containing a number
     */
    POUNDS_MAUDAU_FORMAT(TAHOMA, 10, false, BLACK, GREY_25_PERCENT, new NumberFormat(CURRENCY_POUND + "#,##0.00",
            COMPLEX_FORMAT)),
    /**
     * Totals cell containing a percentage
     */
    PERCENTAGE_MAUDAU_FORMAT(TAHOMA, 10, false, BLACK, GREY_25_PERCENT, new NumberFormat("#,##0.00%", COMPLEX_FORMAT)),
    /**
     * Organic cell
     */
    DEFAULT_TWITTER_FORMAT(TAHOMA, 10, false, BLACK, ROSE, null),
    /**
     * Totals cell containing a number
     */
    DOUBLE_TWITTER_FORMAT(TAHOMA, 10, false, BLACK, ROSE, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    /**
     * Totals cell containing a number
     */
    POUNDS_TWITTER_FORMAT(TAHOMA, 10, false, BLACK, ROSE, new NumberFormat(CURRENCY_POUND + "#,##0.00",
            COMPLEX_FORMAT)),
    /**
     * Totals cell containing a percentage
     */
    PERCENTAGE_TWITTER_FORMAT(TAHOMA, 10, false, BLACK, ROSE, new NumberFormat("#,##0.00%", COMPLEX_FORMAT)),
    /**
     * Organic cell
     */
    DEFAULT_SUBTOTALS_FORMAT(TAHOMA, 10, true, BLACK, LIGHT_ORANGE, null),
    /**
     * Totals cell containing a number
     */
    DOUBLE_SUBTOTALS_FORMAT(TAHOMA, 10, true, BLACK, LIGHT_ORANGE, new NumberFormat("#,##0.00", COMPLEX_FORMAT)),
    /**
     * Totals cell containing a number
     */
    POUNDS_SUBTOTALS_FORMAT(TAHOMA, 10, true, BLACK, LIGHT_ORANGE, new NumberFormat(CURRENCY_POUND + "#,##0.00",
            COMPLEX_FORMAT)),
    /**
     * Totals cell containing a percentage
     */
    PERCENTAGE_SUBTOTALS_FORMAT(TAHOMA, 10, true, BLACK, LIGHT_ORANGE, new NumberFormat("#,##0.00%", COMPLEX_FORMAT));

    private final CellFormat format;

    /**
     * Creates a simple cell format
     *
     * @param font     Font type
     * @param fontSize Font size
     * @param isBold   If true, the cell will be bold
     */
    private CellFormats(final FontName font, final int fontSize, final boolean isBold) {
        this(font, fontSize, isBold, BLACK, null, null);
    }

    /**
     * Creates a simple cell format
     *
     * @param font             Font type
     * @param fontSize         Font size
     * @param isBold           If true, the cell will be bold
     * @param additionalFormat Additional format parameters
     */
    private CellFormats(final FontName font, final int fontSize, final boolean isBold,
                        final DisplayFormat additionalFormat) {
        this(font, fontSize, isBold, BLACK, null, additionalFormat);
    }

    /**
     * Creates a complex cell format
     *
     * @param font             Font type
     * @param fontSize         Font size
     * @param isBold           If true, the cell will be bold
     * @param c                Foreground colot
     * @param background       Background color
     * @param additionalFormat Additional format parameters
     */
    private CellFormats(final FontName font, final int fontSize, final boolean isBold, final Colour c,
                        final Colour background, final DisplayFormat additionalFormat) {
        final WritableFont fontFormat;
        if (isBold) {
            fontFormat = new WritableFont(font, fontSize, BOLD, false, NO_UNDERLINE, c);
        } else {
            fontFormat = new WritableFont(font, fontSize, NO_BOLD, false, NO_UNDERLINE, c);
        }

        final WritableCellFormat cellFormat;
        if (additionalFormat == null) {
            cellFormat = new WritableCellFormat(fontFormat);
        } else {
            cellFormat = new WritableCellFormat(fontFormat, additionalFormat);
        }
        if (background != null) {
            try {
                cellFormat.setBackground(background);
            } catch (final WriteException e) {
                // Never happens
            }
        }
        this.format = cellFormat;
    }

    public CellFormat getFormat() {
        return format;
    }

}
