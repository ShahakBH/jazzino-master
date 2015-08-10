package com.yazino.bi.operations.view.reportbeans;

import static com.yazino.bi.operations.view.ReportColumnFormat.*;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.yazino.bi.operations.view.CustomColumnFormat;
import com.yazino.bi.operations.view.FormatClassSource;
import com.yazino.bi.operations.view.IgnoredFieldsDefinition;
import com.yazino.bi.operations.view.ReportColumnFormat;
import com.yazino.bi.operations.view.ReportField;

/**
 * Data used to fill the payment report
 */
public class PaymentReportData {
    @ReportField(position = 0, header = "Selection", columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String selection;

    @ReportField(position = 1, header = "Revenue", format = POUND, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_POUNDS)
    // End of custom formats
    })
    private Double amount;

    @ReportField(position = 2, header = "No. purchases", format = INTEGER, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long purchases;

    @ReportField(position = 3, header = "Pc. purchases", format = PERCENTAGE, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_PERCENTAGE)
    // End of custom formats
    })
    private Double pcPurchases;

    @ReportField(position = 4, header = "% successful purchases", format = PERCENTAGE, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_PERCENTAGE)
    // End of custom formats
    })
    private Double pcSuccessfulPurchases;

    private Long allPurchaseTransactions;

    @ReportField(position = 5, header = "Purch./buyer", format = DOUBLE, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_DOUBLE)
    // End of custom formats
    })
    private Double purchasesPerBuyer;

    @ReportField(position = 6, header = "Pc. revenue", format = PERCENTAGE, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_PERCENTAGE)
    // End of custom formats
    })
    private Double pcRevenue;

    @ReportField(position = 7, header = "AR / Purchase", format = POUND, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_POUNDS)
    // End of custom formats
    })
    private Double arpp;

    @ReportField(position = 8, header = "AR/buyer", format = POUND, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_POUNDS)
    // End of custom formats
    })
    private Double arpb;

    @ReportField(position = 9, header = "AR/player", format = POUND, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_POUNDS)
    // End of custom formats
    })
    private Double arppl;

    @ReportField(position = 10, header = "Unique buyers", format = INTEGER, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long buyers;

    @ReportField(position = 11, header = "% players purchased", format = PERCENTAGE, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_PERCENTAGE)
    // End of custom formats
    })
    private Double pcPurchased;

    @ReportField(position = 12, header = "Rev (GBP)", format = POUND, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_POUNDS)
    // End of custom formats
    })
    private Double revGbp;

    @ReportField(position = 13, header = "Rev (USD)", format = DOLLAR, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_DOLLARS)
    // End of custom formats
    })
    private Double revUsd;

    @ReportField(position = 14, header = "Rev (EUR)", format = EURO, columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = ReportColumnFormat.TOTALS_EUROS)
    // End of custom formats
    })
    private Double revEur;

    @IgnoredFieldsDefinition
    private static Set<String> ignoredFields = new HashSet<String>();

    private Long players;

    @FormatClassSource
    private String rowFormat;

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("selection", selection).append("amount", amount)
                .append("purchases", purchases).append("pcPurchases", pcPurchases)
                .append("pcSuccessfulPurchases", pcSuccessfulPurchases)
                .append("allPurchaseTransactions", allPurchaseTransactions)
                .append("purchasesPerBuyer", purchasesPerBuyer).append("pcRevenue", pcRevenue)
                .append("arpp", arpp).append("arpb", arpb).append("arppl", arppl).append("buyers", buyers)
                .append("pcPurchased", pcPurchased).append("revGbp", revGbp).append("revUsd", revUsd)
                .append("revEur", revEur).append("players", players).append("rowFormat", rowFormat)
                .toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof PaymentReportData)) {
            return false;
        }
        final PaymentReportData castOther = (PaymentReportData) other;
        return new EqualsBuilder().append(selection, castOther.selection).append(amount, castOther.amount)
                .append(purchases, castOther.purchases).append(pcPurchases, castOther.pcPurchases)
                .append(pcSuccessfulPurchases, castOther.pcSuccessfulPurchases)
                .append(allPurchaseTransactions, castOther.allPurchaseTransactions)
                .append(purchasesPerBuyer, castOther.purchasesPerBuyer)
                .append(pcRevenue, castOther.pcRevenue).append(arpp, castOther.arpp)
                .append(arpb, castOther.arpb).append(arppl, castOther.arppl).append(buyers, castOther.buyers)
                .append(pcPurchased, castOther.pcPurchased).append(revGbp, castOther.revGbp)
                .append(revUsd, castOther.revUsd).append(revEur, castOther.revEur)
                .append(players, castOther.players).append(rowFormat, castOther.rowFormat).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(selection).append(amount).append(purchases).append(pcPurchases)
                .append(pcSuccessfulPurchases).append(allPurchaseTransactions).append(purchasesPerBuyer)
                .append(pcRevenue).append(arpp).append(arpb).append(arppl).append(buyers).append(pcPurchased)
                .append(revGbp).append(revUsd).append(revEur).append(players).append(rowFormat).toHashCode();
    }

    public PaymentReportData() {
        this.amount = 0D;
    }

    public Double getPurchasesPerBuyer() {
        return purchasesPerBuyer;
    }

    public void setPurchasesPerBuyer(final Double purchasesPerBuyer) {
        this.purchasesPerBuyer = purchasesPerBuyer;
    }

    public Long getAllPurchaseTransactions() {
        return allPurchaseTransactions;
    }

    public void setAllPurchaseTransactions(final Long allPurchaseTransactions) {
        this.allPurchaseTransactions = allPurchaseTransactions;
    }

    public static Set<String> getIgnoredFields() {
        return ignoredFields;
    }

    public static void setIgnoredFields(final Set<String> ignoredFields) {
        PaymentReportData.ignoredFields = ignoredFields;
    }

    public Double getPcPurchases() {
        return pcPurchases;
    }

    public void setPcPurchases(final Double pcPurchases) {
        this.pcPurchases = pcPurchases;
    }

    public Double getPcRevenue() {
        return pcRevenue;
    }

    public void setPcRevenue(final Double pcRevenue) {
        this.pcRevenue = pcRevenue;
    }

    public Double getArpb() {
        return arpb;
    }

    public void setArpb(final Double arpb) {
        this.arpb = arpb;
    }

    public String getRowFormat() {
        return rowFormat;
    }

    public void setRowFormat(final String rowFormat) {
        this.rowFormat = rowFormat;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(final String selection) {
        this.selection = selection;
    }

    public Long getPurchases() {
        return purchases;
    }

    public void setPurchases(final Long purchases) {
        this.purchases = purchases;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(final Double amount) {
        this.amount = amount;
    }

    public Long getBuyers() {
        return buyers;
    }

    public void setBuyers(final Long buyers) {
        this.buyers = buyers;
    }

    public Long getPlayers() {
        return players;
    }

    public void setPlayers(final Long players) {
        this.players = players;
    }

    public Double getArpp() {
        return arpp;
    }

    public void setArpp(final Double arpp) {
        this.arpp = arpp;
    }

    public Double getPcPurchased() {
        return pcPurchased;
    }

    public void setPcPurchased(final Double pcPurchased) {
        this.pcPurchased = pcPurchased;
    }

    public Double getArppl() {
        return arppl;
    }

    public void setArppl(final Double arppl) {
        this.arppl = arppl;
    }

    public Double getPcSuccessfulPurchases() {
        return pcSuccessfulPurchases;
    }

    public void setPcSuccessfulPurchases(final Double pcSuccessfulPurchases) {
        this.pcSuccessfulPurchases = pcSuccessfulPurchases;
    }

    public Double getRevGbp() {
        return revGbp;
    }

    public void setRevGbp(final Double revGbp) {
        this.revGbp = revGbp;
    }

    public Double getRevUsd() {
        return revUsd;
    }

    public void setRevUsd(final Double revUsd) {
        this.revUsd = revUsd;
    }

    public Double getRevEur() {
        return revEur;
    }

    public void setRevEur(final Double revEur) {
        this.revEur = revEur;
    }

}
