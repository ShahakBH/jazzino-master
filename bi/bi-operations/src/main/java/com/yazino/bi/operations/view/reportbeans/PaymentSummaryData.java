package com.yazino.bi.operations.view.reportbeans;

import static com.yazino.bi.operations.view.ReportColumnFormat.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.yazino.bi.operations.view.CustomColumnFormat;
import com.yazino.bi.operations.view.ReportField;

public class PaymentSummaryData {

    private static final int WIDE_COLUMN = 10;
    private static final int NARROW_COLUMN = 8;

    @ReportField(position = 0, header = "Date", columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String purchaseDate;

    @ReportField(position = 1, header = "Method", columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String paymentMethod;

    @ReportField(position = 2, header = "Game", columnWidth = NARROW_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String game;

    @ReportField(position = 3, header = "Curr.", columnWidth = NARROW_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String currencyCode;

    @ReportField(position = 4, header = "Gross", format = DOUBLE, columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_DOUBLE)
    // End of custom formats
    })
    private Double amountOrigin;

    @ReportField(position = 5, header = "GBP amt.", format = POUND, columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_POUNDS)
    // End of custom formats
    })
    private Double amountGbp;

    @ReportField(position = 6, header = "Transactions", format = INTEGER, columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long transactions;

    @ReportField(position = 7, header = "Buyers", format = INTEGER, columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long buyers;

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("purchaseDate", purchaseDate)
                .append("paymentMethod", paymentMethod).append("game", game)
                .append("currencyCode", currencyCode).append("amountOrigin", amountOrigin)
                .append("amountGbp", amountGbp).append("transactions", transactions).append("buyers", buyers)
                .toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof PaymentSummaryData)) {
            return false;
        }
        final PaymentSummaryData castOther = (PaymentSummaryData) other;
        return new EqualsBuilder().append(purchaseDate, castOther.purchaseDate)
                .append(paymentMethod, castOther.paymentMethod).append(game, castOther.game)
                .append(currencyCode, castOther.currencyCode).append(amountOrigin, castOther.amountOrigin)
                .append(amountGbp, castOther.amountGbp).append(transactions, castOther.transactions)
                .append(buyers, castOther.buyers).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(purchaseDate).append(paymentMethod).append(game)
                .append(currencyCode).append(amountOrigin).append(amountGbp).append(transactions)
                .append(buyers).toHashCode();
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public String getGame() {
        return game;
    }

    public void setGame(final String game) {
        this.game = game;
    }

    public void setPurchaseDate(final String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Double getAmountGbp() {
        return amountGbp;
    }

    public void setAmountGbp(final Double amountGbp) {
        this.amountGbp = amountGbp;
    }

    public Double getAmountOrigin() {
        return amountOrigin;
    }

    public void setAmountOrigin(final Double amountOrigin) {
        this.amountOrigin = amountOrigin;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(final String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Long getTransactions() {
        return transactions;
    }

    public void setTransactions(final Long chips) {
        this.transactions = chips;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(final String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getBuyers() {
        return buyers;
    }

    public void setBuyers(final Long buyers) {
        this.buyers = buyers;
    }

}
