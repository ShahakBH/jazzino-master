package com.yazino.bi.operations.view.reportbeans;

import static com.yazino.bi.operations.view.ReportColumnFormat.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.yazino.bi.operations.view.CustomColumnFormat;
import com.yazino.bi.operations.view.ReportField;

/**
 * Bean representing one line of the payment history report
 */
public class PaymentHistoryData {

    private static final int WIDE_COLUMN = 10;
    private static final int NARROW_COLUMN = 8;

    @ReportField(position = 0, header = "Player id", format = INTEGER, columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long playerId;

    @ReportField(position = 1, header = "Purch.date", columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String purchaseDate;

    @ReportField(position = 2, header = "Amt. GBP", format = POUND, columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_POUNDS)
    // End of custom formats
    })
    private Double amountGbp;

    @ReportField(position = 3, header = "Amt. orig.", format = DOUBLE, columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_DOUBLE)
    // End of custom formats
    })
    private Double amountOrigin;

    @ReportField(position = 4, header = "Curr.", columnWidth = NARROW_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String currencyCode;

    @ReportField(position = 5, header = "Chips", format = INTEGER, columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long chips;

    @ReportField(position = 6, header = "Method", columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String paymentMethod;

    @ReportField(position = 7, header = "Country", columnWidth = NARROW_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String country;

    @ReportField(position = 8, header = "Reg.date", columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String registrationDate;

    @ReportField(position = 9, header = "DOB", columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String dateOfBirth;

    @ReportField(position = 10, header = "Gend.", columnWidth = NARROW_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String gender;

    @ReportField(position = 11, header = "E-Mail", columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String emailAddress;

    @ReportField(position = 11, header = "Game", columnWidth = NARROW_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String game;

    @ReportField(position = 12, header = "1st. purch.", columnWidth = NARROW_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String firstPurchase;

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("playerId", playerId).append("purchaseDate", purchaseDate)
                .append("amountGbp", amountGbp).append("amountOrigin", amountOrigin)
                .append("currencyCode", currencyCode).append("chips", chips)
                .append("paymentMethod", paymentMethod).append("country", country)
                .append("registrationDate", registrationDate).append("dateOfBirth", dateOfBirth)
                .append("gender", gender).append("emailAddress", emailAddress).append("game", game)
                .append("firstPurchase", firstPurchase).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof PaymentHistoryData)) {
            return false;
        }
        final PaymentHistoryData castOther = (PaymentHistoryData) other;
        return new EqualsBuilder().append(playerId, castOther.playerId)
                .append(purchaseDate, castOther.purchaseDate).append(amountGbp, castOther.amountGbp)
                .append(amountOrigin, castOther.amountOrigin).append(currencyCode, castOther.currencyCode)
                .append(chips, castOther.chips).append(paymentMethod, castOther.paymentMethod)
                .append(country, castOther.country).append(registrationDate, castOther.registrationDate)
                .append(dateOfBirth, castOther.dateOfBirth).append(gender, castOther.gender)
                .append(emailAddress, castOther.emailAddress).append(game, castOther.game)
                .append(firstPurchase, castOther.firstPurchase).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(playerId).append(purchaseDate).append(amountGbp)
                .append(amountOrigin).append(currencyCode).append(chips).append(paymentMethod)
                .append(country).append(registrationDate).append(dateOfBirth).append(gender)
                .append(emailAddress).append(game).append(firstPurchase).toHashCode();
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final Long playerId) {
        this.playerId = playerId;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public String getFirstPurchase() {
        return firstPurchase;
    }

    public void setFirstPurchase(final String firstPurchase) {
        this.firstPurchase = firstPurchase;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
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

    public Long getChips() {
        return chips;
    }

    public void setChips(final Long chips) {
        this.chips = chips;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(final String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(final String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(final String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(final String gender) {
        this.gender = gender;
    }
}
