package com.yazino.bi.operations.model;

import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.yazino.bi.operations.util.DateIntervalHelper;

/**
 * Command object for the report format controller
 */
public class ReportDefinitionCommand extends CommandWithPlatformAndFormat {
    private String fromDate;
    private String toDate;
    private String startMonth;
    private String endMonth;
    private FilterChoices filterChoice;
    private FilterChoices filterChoice2;
    private ReportDetailsLevel detailsLevel;
    private String paymentMethod;
    private String txnStatus;
    private String currencyCode;
    private String paymentTransactionReportType;
    private String transactionId;
    private String pack;
    private String country;
    private String game;

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("fromDate", fromDate).append("toDate", toDate)
                .append("startMonth", startMonth).append("endMonth", endMonth)
                .append("filterChoice", filterChoice).append("filterChoice2", filterChoice2)
                .append("detailsLevel", detailsLevel).append("paymentMethod", paymentMethod)
                .append("txnStatus", txnStatus).append("currencyCode", currencyCode)
                .append("paymentTransactionReportType", paymentTransactionReportType)
                .append("transactionId", transactionId).append("pack", pack).append("country", country)
                .append("game", game).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ReportDefinitionCommand)) {
            return false;
        }
        final ReportDefinitionCommand castOther = (ReportDefinitionCommand) other;
        return new EqualsBuilder().append(fromDate, castOther.fromDate).append(toDate, castOther.toDate)
                .append(startMonth, castOther.startMonth).append(endMonth, castOther.endMonth)
                .append(filterChoice, castOther.filterChoice).append(filterChoice2, castOther.filterChoice2)
                .append(detailsLevel, castOther.detailsLevel).append(paymentMethod, castOther.paymentMethod)
                .append(txnStatus, castOther.txnStatus).append(currencyCode, castOther.currencyCode)
                .append(paymentTransactionReportType, castOther.paymentTransactionReportType)
                .append(transactionId, castOther.transactionId).append(pack, castOther.pack)
                .append(country, castOther.country).append(game, castOther.game).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(fromDate).append(toDate).append(startMonth).append(endMonth)
                .append(filterChoice).append(filterChoice2).append(detailsLevel).append(paymentMethod)
                .append(txnStatus).append(currencyCode).append(paymentTransactionReportType)
                .append(transactionId).append(pack).append(country).append(game).toHashCode();
    }

    public String getGame() {
        return game;
    }

    public void setGame(final String game) {
        this.game = game;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public String getPack() {
        return pack;
    }

    public void setPack(final String pack) {
        this.pack = pack;
    }

    public FilterChoices getFilterChoice2() {
        return filterChoice2;
    }

    public void setFilterChoice2(final FilterChoices filterChoice2) {
        this.filterChoice2 = filterChoice2;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(final String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public FilterChoices getFilterChoice() {
        return filterChoice;
    }

    public void setFilterChoice(final FilterChoices filterChoice) {
        this.filterChoice = filterChoice;
    }

    public String getStartMonth() {
        return startMonth;
    }

    public void setStartMonth(final String startMonth) {
        this.startMonth = startMonth;
    }

    public String getEndMonth() {
        return endMonth;
    }

    public void setEndMonth(final String endMonth) {
        this.endMonth = endMonth;
    }

    public ReportDetailsLevel getDetailsLevel() {
        return detailsLevel;
    }

    public void setDetailsLevel(final ReportDetailsLevel detailsLevel) {
        this.detailsLevel = detailsLevel;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(final String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(final String toDate) {
        this.toDate = toDate;
    }

    public String getTxnStatus() {
        return txnStatus;
    }

    public void setTxnStatus(final String txnStatus) {
        this.txnStatus = txnStatus;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(final String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(final String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPaymentTransactionReportType() {
        return paymentTransactionReportType;
    }

    public void setPaymentTransactionReportType(final String paymentTransactionReportType) {
        this.paymentTransactionReportType = paymentTransactionReportType;
    }

    /**
     * Determines the start date of the range selected by the command
     *
     * @return Start date
     */
    public Date getStartDate() {
        return DateIntervalHelper.getStartDate(startMonth, endMonth);
    }

    /**
     * Determines the end date of the range selected by the command
     *
     * @return End date
     */
    public Date getEndDate() {
        return DateIntervalHelper.getEndDate(startMonth, endMonth);
    }
}
