package com.yazino.bi.operations.view.reportbeans;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.yazino.bi.operations.view.ReportColumnFormat;
import com.yazino.bi.operations.view.ReportField;

public class PaymentTransactionData {
    @ReportField(position = 0, header = "Date", columnWidth = 10)
    private String date;

    @ReportField(position = 1, header = "Payment ID", columnWidth = 20)
    private String internalId;

    @ReportField(position = 2, header = "Player ID", format = ReportColumnFormat.INTEGER, columnWidth = 12)
    private Long playerId;

    @ReportField(position = 3, header = "Player Name", columnWidth = 20)
    private String playerName;

    @ReportField(position = 4, header = "Status", columnWidth = 8)
    private String txnStatus;

    @ReportField(position = 5, header = "External ID", columnWidth = 20)
    private String externalId;

    @ReportField(position = 6, header = "Details", columnWidth = 20)
    private String details;

    @ReportField(position = 7, header = "Method", columnWidth = 20)
    private String cashier;

    @ReportField(position = 8, header = "Game Type", columnWidth = 10)
    private String gameType;

    @ReportField(position = 9, header = "Currency", columnWidth = 3)
    private String currencyCode;

    @ReportField(position = 10, header = "Gross", format = ReportColumnFormat.DOUBLE, columnWidth = 12)
    private Double amount;

    @ReportField(position = 11, header = "GBP Amount", format = ReportColumnFormat.POUND, columnWidth = 12)
    private Double gbpAmount;

    @ReportField(position = 12, header = "Amount Chips", format = ReportColumnFormat.INTEGER, columnWidth = 12)
    private Long amountChips;

    @ReportField(position = 13, header = "Registration Country", columnWidth = 5)
    private String regCountry;

    @ReportField(position = 14, header = "First Purchase Date", columnWidth = 10)
    private String firstPurchaseDate;

    @ReportField(position = 15, header = "Registration Date", columnWidth = 10)
    private String registrationDate;

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public String getFirstPurchaseDate() {
        return firstPurchaseDate;
    }

    public void setFirstPurchaseDate(final String firstPurchaseDate) {
        this.firstPurchaseDate = firstPurchaseDate;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(final String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getRegCountry() {
        return regCountry;
    }

    public void setRegCountry(final String regCountry) {
        this.regCountry = regCountry;
    }


    public String getCashier() {
        return cashier;
    }

    public void setCashier(final String cashier) {
        this.cashier = cashier;
    }

    public String getDate() {
        return date;
    }

    public void setDate(final String date) {
        this.date = date;
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(final String internalId) {
        this.internalId = internalId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final Long playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(final String playerName) {
        this.playerName = playerName;
    }

    public String getTxnStatus() {
        return txnStatus;
    }

    public void setTxnStatus(final String txnStatus) {
        this.txnStatus = txnStatus;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(final String details) {
        this.details = details;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(final String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(final Double amount) {
        this.amount = amount;
    }

    public Double getGbpAmount() {
        return gbpAmount;
    }

    public void setGbpAmount(final Double gbpAmount) {
        this.gbpAmount = gbpAmount;
    }

    public Long getAmountChips() {
        return amountChips;
    }

    public void setAmountChips(final Long amountChips) {
        this.amountChips = amountChips;
    }

    @Override
    public String toString() {
        return "PaymentTransactionData{" + "date='" + date + '\'' + ", internalId='" + internalId + '\''
                + ", playerId=" + playerId + ", playerName='" + playerName + '\'' + ", txnStatus='"
                + txnStatus + '\'' + ", externalId='" + externalId + '\'' + ", details='" + details + '\''
                + ", cashier='" + cashier + '\'' + ", currencyCode='" + currencyCode + '\'' + ", amount="
                + amount + ", gbpAmount=" + gbpAmount + '}';
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final PaymentTransactionData rhs = (PaymentTransactionData) obj;
        return new EqualsBuilder()
                .append(date, rhs.date)
                .append(internalId, rhs.internalId)
                .append(playerId, rhs.playerId)
                .append(playerName, rhs.playerName)
                .append(txnStatus, rhs.txnStatus)
                .append(externalId, rhs.externalId)
                .append(details, rhs.details)
                .append(cashier, rhs.cashier)
                .append(currencyCode, rhs.currencyCode)
                .append(amount, rhs.amount)
                .append(gbpAmount, rhs.gbpAmount)
                .append(amountChips, rhs.amountChips)
                .append(regCountry, rhs.regCountry)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(date)
                .append(internalId)
                .append(playerId)
                .append(playerName)
                .append(txnStatus)
                .append(externalId)
                .append(details)
                .append(cashier)
                .append(currencyCode)
                .append(amount)
                .append(gbpAmount)
                .append(amountChips)
                .append(regCountry)
                .toHashCode();
    }
}
