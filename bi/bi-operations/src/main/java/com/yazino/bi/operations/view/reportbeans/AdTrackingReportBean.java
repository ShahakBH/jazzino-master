package com.yazino.bi.operations.view.reportbeans;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.yazino.bi.operations.view.CustomColumnFormat;
import com.yazino.bi.operations.view.FormatClassSource;
import com.yazino.bi.operations.view.ReportField;

import static com.yazino.bi.operations.view.ReportColumnFormat.*;

/**
 * Bean defining a row of an ad tracking report
 */
public class AdTrackingReportBean {
    @ReportField(position = 0, header = "Ad code", columnWidth = 30, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_STRING),
            @CustomColumnFormat(value = "organic", format = ORGANIC_STRING),
            @CustomColumnFormat(value = "nonAd", format = NONAD_STRING),
            @CustomColumnFormat(value = "opengraph", format = OG_STRING),
            @CustomColumnFormat(value = "promos", format = PROMOS_STRING),
            @CustomColumnFormat(value = "emails", format = EMAILS_STRING),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_STRING),
            @CustomColumnFormat(value = "twitter", format = TWITTER_STRING),
            @CustomColumnFormat(value = "custom", format = CUSTOM_STRING),
            @CustomColumnFormat(value = "ios", format = IOS_STRING),
            @CustomColumnFormat(value = "android", format = ANDROID_STRING),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_STRING)
            // End of custom formats
    })
    private String adCode;

    @ReportField(position = 1, header = "Clicks", format = INTEGER, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER),
            @CustomColumnFormat(value = "organic", format = ORGANIC_INTEGER),
            @CustomColumnFormat(value = "nonAd", format = NONAD_INTEGER),
            @CustomColumnFormat(value = "opengraph", format = OG_INTEGER),
            @CustomColumnFormat(value = "promos", format = PROMOS_INTEGER),
            @CustomColumnFormat(value = "emails", format = EMAILS_INTEGER),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_INTEGER),
            @CustomColumnFormat(value = "twitter", format = TWITTER_INTEGER),
            @CustomColumnFormat(value = "custom", format = CUSTOM_INTEGER),
            @CustomColumnFormat(value = "ios", format = IOS_INTEGER),
            @CustomColumnFormat(value = "android", format = ANDROID_INTEGER),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_INTEGER)
    // End of custom formats
    })
    private Long clicks;

    @ReportField(position = 2, header = "CTR", format = PERCENTAGE, columnWidth = 12, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_PERCENTAGE),
            @CustomColumnFormat(value = "organic", format = ORGANIC_PERCENTAGE),
            @CustomColumnFormat(value = "nonAd", format = NONAD_PERCENTAGE),
            @CustomColumnFormat(value = "opengraph", format = OG_PERCENTAGE),
            @CustomColumnFormat(value = "promos", format = PROMOS_PERCENTAGE),
            @CustomColumnFormat(value = "emails", format = EMAILS_PERCENTAGE),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_PERCENTAGE),
            @CustomColumnFormat(value = "twitter", format = TWITTER_PERCENTAGE),
            @CustomColumnFormat(value = "custom", format = CUSTOM_PERCENTAGE),
            @CustomColumnFormat(value = "ios", format = IOS_PERCENTAGE),
            @CustomColumnFormat(value = "android", format = ANDROID_PERCENTAGE),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_PERCENTAGE)
    // End of custom formats
    })
    private Double ctr;

    @ReportField(position = 3, header = "Regs", format = INTEGER, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER),
            @CustomColumnFormat(value = "organic", format = ORGANIC_INTEGER),
            @CustomColumnFormat(value = "nonAd", format = NONAD_INTEGER),
            @CustomColumnFormat(value = "opengraph", format = OG_INTEGER),
            @CustomColumnFormat(value = "promos", format = PROMOS_INTEGER),
            @CustomColumnFormat(value = "emails", format = EMAILS_INTEGER),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_INTEGER),
            @CustomColumnFormat(value = "twitter", format = TWITTER_INTEGER),
            @CustomColumnFormat(value = "custom", format = CUSTOM_INTEGER),
            @CustomColumnFormat(value = "ios", format = IOS_INTEGER),
            @CustomColumnFormat(value = "android", format = ANDROID_INTEGER),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_INTEGER)
    // End of custom formats
    })
    private Long registrations;

    @ReportField(position = 4, header = "Regs / click", format = PERCENTAGE, columnWidth = 12, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_PERCENTAGE),
            @CustomColumnFormat(value = "organic", format = ORGANIC_PERCENTAGE),
            @CustomColumnFormat(value = "nonAd", format = NONAD_PERCENTAGE),
            @CustomColumnFormat(value = "opengraph", format = OG_PERCENTAGE),
            @CustomColumnFormat(value = "promos", format = PROMOS_PERCENTAGE),
            @CustomColumnFormat(value = "emails", format = EMAILS_PERCENTAGE),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_PERCENTAGE),
            @CustomColumnFormat(value = "twitter", format = TWITTER_PERCENTAGE),
            @CustomColumnFormat(value = "custom", format = CUSTOM_PERCENTAGE),
            @CustomColumnFormat(value = "ios", format = IOS_PERCENTAGE),
            @CustomColumnFormat(value = "android", format = ANDROID_PERCENTAGE),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_PERCENTAGE)
    // End of custom formats
    })
    private Double regsPerClick;

    @ReportField(position = 5, header = "Purch.", format = INTEGER, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER),
            @CustomColumnFormat(value = "organic", format = ORGANIC_INTEGER),
            @CustomColumnFormat(value = "nonAd", format = NONAD_INTEGER),
            @CustomColumnFormat(value = "opengraph", format = OG_INTEGER),
            @CustomColumnFormat(value = "promos", format = PROMOS_INTEGER),
            @CustomColumnFormat(value = "emails", format = EMAILS_INTEGER),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_INTEGER),
            @CustomColumnFormat(value = "twitter", format = TWITTER_INTEGER),
            @CustomColumnFormat(value = "custom", format = CUSTOM_INTEGER),
            @CustomColumnFormat(value = "ios", format = IOS_INTEGER),
            @CustomColumnFormat(value = "android", format = ANDROID_INTEGER),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_INTEGER)
    // End of custom formats
    })
    private Long purchases;

    @ReportField(position = 6, header = "Unique buyers", format = INTEGER, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER),
            @CustomColumnFormat(value = "organic", format = ORGANIC_INTEGER),
            @CustomColumnFormat(value = "nonAd", format = NONAD_INTEGER),
            @CustomColumnFormat(value = "opengraph", format = OG_INTEGER),
            @CustomColumnFormat(value = "promos", format = PROMOS_INTEGER),
            @CustomColumnFormat(value = "emails", format = EMAILS_INTEGER),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_INTEGER),
            @CustomColumnFormat(value = "twitter", format = TWITTER_INTEGER),
            @CustomColumnFormat(value = "custom", format = CUSTOM_INTEGER),
            @CustomColumnFormat(value = "ios", format = IOS_INTEGER),
            @CustomColumnFormat(value = "android", format = ANDROID_INTEGER),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_INTEGER)
    // End of custom formats
    })
    private Long buyers;

    @ReportField(position = 7, header = "Cons. GBP", format = POUND, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_POUNDS),
            @CustomColumnFormat(value = "organic", format = ORGANIC_POUNDS),
            @CustomColumnFormat(value = "nonAd", format = NONAD_POUNDS),
            @CustomColumnFormat(value = "opengraph", format = OG_POUNDS),
            @CustomColumnFormat(value = "promos", format = PROMOS_POUNDS),
            @CustomColumnFormat(value = "emails", format = EMAILS_POUNDS),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_POUNDS),
            @CustomColumnFormat(value = "twitter", format = TWITTER_POUNDS),
            @CustomColumnFormat(value = "custom", format = CUSTOM_POUNDS),
            @CustomColumnFormat(value = "ios", format = IOS_POUNDS),
            @CustomColumnFormat(value = "android", format = ANDROID_POUNDS),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_POUNDS)
    // End of custom formats
    })
    private Double totals;

    @ReportField(position = 8, header = "Rev. / purch.", format = POUND, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_POUNDS),
            @CustomColumnFormat(value = "organic", format = ORGANIC_POUNDS),
            @CustomColumnFormat(value = "nonAd", format = NONAD_POUNDS),
            @CustomColumnFormat(value = "opengraph", format = OG_POUNDS),
            @CustomColumnFormat(value = "promos", format = PROMOS_POUNDS),
            @CustomColumnFormat(value = "emails", format = EMAILS_POUNDS),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_POUNDS),
            @CustomColumnFormat(value = "twitter", format = TWITTER_POUNDS),
            @CustomColumnFormat(value = "custom", format = CUSTOM_POUNDS),
            @CustomColumnFormat(value = "ios", format = IOS_POUNDS),
            @CustomColumnFormat(value = "android", format = ANDROID_POUNDS),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_POUNDS)
    // End of custom formats
    })
    private Double revenuePerPurchase;

    @ReportField(position = 9, header = "Purch. / buyer", format = DOUBLE, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_DOUBLE),
            @CustomColumnFormat(value = "organic", format = ORGANIC_DOUBLE),
            @CustomColumnFormat(value = "nonAd", format = NONAD_DOUBLE),
            @CustomColumnFormat(value = "opengraph", format = OG_DOUBLE),
            @CustomColumnFormat(value = "promos", format = PROMOS_DOUBLE),
            @CustomColumnFormat(value = "emails", format = EMAILS_DOUBLE),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_DOUBLE),
            @CustomColumnFormat(value = "twitter", format = TWITTER_DOUBLE),
            @CustomColumnFormat(value = "custom", format = CUSTOM_DOUBLE),
            @CustomColumnFormat(value = "ios", format = IOS_DOUBLE),
            @CustomColumnFormat(value = "android", format = ANDROID_DOUBLE),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_DOUBLE)
    // End of custom formats
    })
    private Double purchasesPerBuyer;

    @ReportField(position = 10, header = "Rev. / buyer", format = POUND, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_POUNDS),
            @CustomColumnFormat(value = "organic", format = ORGANIC_POUNDS),
            @CustomColumnFormat(value = "nonAd", format = NONAD_POUNDS),
            @CustomColumnFormat(value = "opengraph", format = OG_POUNDS),
            @CustomColumnFormat(value = "promos", format = PROMOS_POUNDS),
            @CustomColumnFormat(value = "emails", format = EMAILS_POUNDS),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_POUNDS),
            @CustomColumnFormat(value = "twitter", format = TWITTER_POUNDS),
            @CustomColumnFormat(value = "custom", format = CUSTOM_POUNDS),
            @CustomColumnFormat(value = "ios", format = IOS_POUNDS),
            @CustomColumnFormat(value = "android", format = ANDROID_POUNDS),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_POUNDS)
    // End of custom formats
    })
    private Double revenuePerBuyer;

    @ReportField(position = 11, header = "Purch. / reg.", format = PERCENTAGE, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_PERCENTAGE),
            @CustomColumnFormat(value = "organic", format = ORGANIC_PERCENTAGE),
            @CustomColumnFormat(value = "nonAd", format = NONAD_PERCENTAGE),
            @CustomColumnFormat(value = "opengraph", format = OG_PERCENTAGE),
            @CustomColumnFormat(value = "promos", format = PROMOS_PERCENTAGE),
            @CustomColumnFormat(value = "emails", format = EMAILS_PERCENTAGE),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_PERCENTAGE),
            @CustomColumnFormat(value = "twitter", format = TWITTER_PERCENTAGE),
            @CustomColumnFormat(value = "custom", format = CUSTOM_PERCENTAGE),
            @CustomColumnFormat(value = "ios", format = IOS_PERCENTAGE),
            @CustomColumnFormat(value = "android", format = ANDROID_PERCENTAGE),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_PERCENTAGE)
    // End of custom formats
    })
    private Double purchasesPerReg;

    @ReportField(position = 12, header = "Buyers / reg.", format = PERCENTAGE, columnWidth = 12, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_PERCENTAGE),
            @CustomColumnFormat(value = "organic", format = ORGANIC_PERCENTAGE),
            @CustomColumnFormat(value = "nonAd", format = NONAD_PERCENTAGE),
            @CustomColumnFormat(value = "opengraph", format = OG_PERCENTAGE),
            @CustomColumnFormat(value = "promos", format = PROMOS_PERCENTAGE),
            @CustomColumnFormat(value = "emails", format = EMAILS_PERCENTAGE),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_PERCENTAGE),
            @CustomColumnFormat(value = "twitter", format = TWITTER_PERCENTAGE),
            @CustomColumnFormat(value = "custom", format = CUSTOM_PERCENTAGE),
            @CustomColumnFormat(value = "ios", format = IOS_PERCENTAGE),
            @CustomColumnFormat(value = "android", format = ANDROID_PERCENTAGE),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_PERCENTAGE)
    // End of custom formats
    })
    private Double buyersPerReg;

    @ReportField(position = 13, header = "Days to 1st purch.", format = DOUBLE, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_DOUBLE),
            @CustomColumnFormat(value = "organic", format = ORGANIC_DOUBLE),
            @CustomColumnFormat(value = "nonAd", format = NONAD_DOUBLE),
            @CustomColumnFormat(value = "opengraph", format = OG_DOUBLE),
            @CustomColumnFormat(value = "promos", format = PROMOS_DOUBLE),
            @CustomColumnFormat(value = "emails", format = EMAILS_DOUBLE),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_DOUBLE),
            @CustomColumnFormat(value = "twitter", format = TWITTER_DOUBLE),
            @CustomColumnFormat(value = "custom", format = CUSTOM_DOUBLE),
            @CustomColumnFormat(value = "ios", format = IOS_DOUBLE),
            @CustomColumnFormat(value = "android", format = ANDROID_DOUBLE),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_DOUBLE)
    // End of custom formats
    })
    private Double daysToBuy;

    @ReportField(position = 14, header = "Invites", format = INTEGER, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER),
            @CustomColumnFormat(value = "organic", format = ORGANIC_INTEGER),
            @CustomColumnFormat(value = "nonAd", format = NONAD_INTEGER),
            @CustomColumnFormat(value = "opengraph", format = OG_INTEGER),
            @CustomColumnFormat(value = "promos", format = PROMOS_INTEGER),
            @CustomColumnFormat(value = "emails", format = EMAILS_INTEGER),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_INTEGER),
            @CustomColumnFormat(value = "twitter", format = TWITTER_INTEGER),
            @CustomColumnFormat(value = "custom", format = CUSTOM_INTEGER),
            @CustomColumnFormat(value = "ios", format = IOS_INTEGER),
            @CustomColumnFormat(value = "android", format = ANDROID_INTEGER),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_INTEGER)
    // End of custom formats
    })
    private Long invites;

    @ReportField(position = 15, header = "Inv. / reg.", format = DOUBLE, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_DOUBLE),
            @CustomColumnFormat(value = "organic", format = ORGANIC_DOUBLE),
            @CustomColumnFormat(value = "nonAd", format = NONAD_DOUBLE),
            @CustomColumnFormat(value = "opengraph", format = OG_DOUBLE),
            @CustomColumnFormat(value = "promos", format = PROMOS_DOUBLE),
            @CustomColumnFormat(value = "emails", format = EMAILS_DOUBLE),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_DOUBLE),
            @CustomColumnFormat(value = "twitter", format = TWITTER_DOUBLE),
            @CustomColumnFormat(value = "custom", format = CUSTOM_DOUBLE),
            @CustomColumnFormat(value = "ios", format = IOS_DOUBLE),
            @CustomColumnFormat(value = "android", format = ANDROID_DOUBLE),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_DOUBLE)
            // End of custom formats
    })
    private Double invitesPerReg;

    @ReportField(position = 16, header = "Inv. / buyer", format = DOUBLE, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_DOUBLE),
            @CustomColumnFormat(value = "organic", format = ORGANIC_DOUBLE),
            @CustomColumnFormat(value = "nonAd", format = NONAD_DOUBLE),
            @CustomColumnFormat(value = "opengraph", format = OG_DOUBLE),
            @CustomColumnFormat(value = "promos", format = PROMOS_DOUBLE),
            @CustomColumnFormat(value = "emails", format = EMAILS_DOUBLE),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_DOUBLE),
            @CustomColumnFormat(value = "twitter", format = TWITTER_DOUBLE),
            @CustomColumnFormat(value = "custom", format = CUSTOM_DOUBLE),
            @CustomColumnFormat(value = "ios", format = IOS_DOUBLE),
            @CustomColumnFormat(value = "android", format = ANDROID_DOUBLE),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_DOUBLE)
            // End of custom formats
    })
    private Double invitesPerBuyer;

    @ReportField(position = 17, header = "Spent", format = POUND, columnWidth = 10, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_POUNDS),
            @CustomColumnFormat(value = "organic", format = ORGANIC_POUNDS),
            @CustomColumnFormat(value = "nonAd", format = NONAD_POUNDS),
            @CustomColumnFormat(value = "opengraph", format = OG_POUNDS),
            @CustomColumnFormat(value = "promos", format = PROMOS_POUNDS),
            @CustomColumnFormat(value = "emails", format = EMAILS_POUNDS),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_POUNDS),
            @CustomColumnFormat(value = "twitter", format = TWITTER_POUNDS),
            @CustomColumnFormat(value = "custom", format = CUSTOM_POUNDS),
            @CustomColumnFormat(value = "ios", format = IOS_POUNDS),
            @CustomColumnFormat(value = "android", format = ANDROID_POUNDS),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_POUNDS)
            // End of custom formats
    })
    private Double spent;

    @ReportField(position = 18, header = "ROI", format = PERCENTAGE, columnWidth = 12, customFormats = {
            // Custom format for the totals line
            @CustomColumnFormat(value = "totals", format = TOTALS_PERCENTAGE),
            @CustomColumnFormat(value = "organic", format = ORGANIC_PERCENTAGE),
            @CustomColumnFormat(value = "nonAd", format = NONAD_PERCENTAGE),
            @CustomColumnFormat(value = "opengraph", format = OG_PERCENTAGE),
            @CustomColumnFormat(value = "promos", format = PROMOS_PERCENTAGE),
            @CustomColumnFormat(value = "emails", format = EMAILS_PERCENTAGE),
            @CustomColumnFormat(value = "maudau", format = MAUDAU_PERCENTAGE),
            @CustomColumnFormat(value = "twitter", format = TWITTER_PERCENTAGE),
            @CustomColumnFormat(value = "custom", format = CUSTOM_PERCENTAGE),
            @CustomColumnFormat(value = "ios", format = IOS_PERCENTAGE),
            @CustomColumnFormat(value = "android", format = ANDROID_PERCENTAGE),
            @CustomColumnFormat(value = "subtotals", format = SUBTOTALS_PERCENTAGE)
            // End of custom formats
    })
    private Double roi;

    private Long buyersInvites;

    private long nonEmptyBuyers = 0;

    private long totalsCount = 0;

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("clicks", clicks).append("ctr", ctr).append("adCode", adCode)
                .append("registrations", registrations).append("regsPerClick", regsPerClick)
                .append("purchases", purchases).append("buyers", buyers).append("totals", totals)
                .append("revenuePerPurchase", revenuePerPurchase)
                .append("purchasesPerBuyer", purchasesPerBuyer).append("revenuePerBuyer", revenuePerBuyer)
                .append("purchasesPerReg", purchasesPerReg).append("buyersPerReg", buyersPerReg)
                .append("daysToBuy", daysToBuy).append("invites", invites)
                .append("buyersInvites", buyersInvites).append("nonEmptyBuyers", nonEmptyBuyers)
                .append("totalsCount", totalsCount).append("invitesPerReg", invitesPerReg)
                .append("invitesPerBuyer", invitesPerBuyer).append("spent", spent).append("roi", roi)
                .append("rowFormat", rowFormat).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AdTrackingReportBean)) {
            return false;
        }
        final AdTrackingReportBean castOther = (AdTrackingReportBean) other;
        return new EqualsBuilder().append(clicks, castOther.clicks).append(ctr, castOther.ctr)
                .append(adCode, castOther.adCode).append(registrations, castOther.registrations)
                .append(regsPerClick, castOther.regsPerClick).append(purchases, castOther.purchases)
                .append(buyers, castOther.buyers).append(totals, castOther.totals)
                .append(revenuePerPurchase, castOther.revenuePerPurchase)
                .append(purchasesPerBuyer, castOther.purchasesPerBuyer)
                .append(revenuePerBuyer, castOther.revenuePerBuyer)
                .append(purchasesPerReg, castOther.purchasesPerReg)
                .append(buyersPerReg, castOther.buyersPerReg).append(daysToBuy, castOther.daysToBuy)
                .append(invites, castOther.invites).append(buyersInvites, castOther.buyersInvites)
                .append(nonEmptyBuyers, castOther.nonEmptyBuyers).append(totalsCount, castOther.totalsCount)
                .append(invitesPerReg, castOther.invitesPerReg)
                .append(invitesPerBuyer, castOther.invitesPerBuyer).append(spent, castOther.spent)
                .append(roi, castOther.roi).append(rowFormat, castOther.rowFormat).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(clicks).append(ctr).append(adCode).append(registrations)
                .append(regsPerClick).append(purchases).append(buyers).append(totals)
                .append(revenuePerPurchase).append(purchasesPerBuyer).append(revenuePerBuyer)
                .append(purchasesPerReg).append(buyersPerReg).append(daysToBuy).append(invites)
                .append(buyersInvites).append(nonEmptyBuyers).append(totalsCount).append(invitesPerReg)
                .append(invitesPerBuyer).append(spent).append(roi).append(rowFormat).toHashCode();
    }

    public long getTotalsCount() {
        return totalsCount;
    }

    public void setTotalsCount(final long totalsCount) {
        this.totalsCount = totalsCount;
    }

    @FormatClassSource
    private String rowFormat;

    /**
     * Initializes the fields to zero
     */
    public AdTrackingReportBean() {
        setRegistrations(0L);
        setBuyers(0L);
        setPurchases(0L);
        setInvites(0L);
        setTotals(0D);
        setBuyersInvites(0L);
        setClicks(0L);
        setCtr(0D);
        setSpent(0D);
    }

    public String getAdCode() {
        return adCode;
    }

    public void setAdCode(final String adCode) {
        this.adCode = adCode;
    }

    public Long getRegistrations() {
        return registrations;
    }

    public void setRegistrations(final Long registrations) {
        this.registrations = registrations;
    }

    public Long getBuyers() {
        return buyers;
    }

    public void setBuyers(final Long buyers) {
        this.buyers = buyers;
    }

    public Long getPurchases() {
        return purchases;
    }

    public void setPurchases(final Long purchases) {
        this.purchases = purchases;
    }

    public Long getInvites() {
        return invites;
    }

    public void setInvites(final Long invites) {
        this.invites = invites;
    }

    public Double getBuyersPerReg() {
        return buyersPerReg;
    }

    public void setBuyersPerReg(final Double buyersPerReg) {
        this.buyersPerReg = buyersPerReg;
    }

    public Double getInvitesPerReg() {
        return invitesPerReg;
    }

    public void setInvitesPerReg(final Double invitesPerReg) {
        this.invitesPerReg = invitesPerReg;
    }

    public Double getInvitesPerBuyer() {
        return invitesPerBuyer;
    }

    public void setInvitesPerBuyer(final Double invitesPerBuyer) {
        this.invitesPerBuyer = invitesPerBuyer;
    }

    public Double getPurchasesPerBuyer() {
        return purchasesPerBuyer;
    }

    public void setPurchasesPerBuyer(final Double purchasesPerBuyer) {
        this.purchasesPerBuyer = purchasesPerBuyer;
    }

    public Double getPurchasesPerReg() {
        return purchasesPerReg;
    }

    public void setPurchasesPerReg(final Double purchasesPerReg) {
        this.purchasesPerReg = purchasesPerReg;
    }

    public String getRowFormat() {
        return rowFormat;
    }

    public void setRowFormat(final String rowFormat) {
        this.rowFormat = rowFormat;
    }

    public Double getTotals() {
        return totals;
    }

    public void setTotals(final Double totals) {
        this.totals = totals;
    }

    public Double getRevenuePerPurchase() {
        return revenuePerPurchase;
    }

    public void setRevenuePerPurchase(final Double revenuePerPurchase) {
        this.revenuePerPurchase = revenuePerPurchase;
    }

    public Double getRevenuePerBuyer() {
        return revenuePerBuyer;
    }

    public void setRevenuePerBuyer(final Double revenuePerBuyer) {
        this.revenuePerBuyer = revenuePerBuyer;
    }

    public Long getBuyersInvites() {
        return buyersInvites;
    }

    public void setBuyersInvites(final Long buyersInvites) {
        this.buyersInvites = buyersInvites;
    }

    public Double getDaysToBuy() {
        return daysToBuy;
    }

    public void setDaysToBuy(final Double daysToBuy) {
        this.daysToBuy = daysToBuy;
    }

    public long getNonEmptyBuyers() {
        return nonEmptyBuyers;
    }

    public void setNonEmptyBuyers(final long nonEmptyBuyers) {
        this.nonEmptyBuyers = nonEmptyBuyers;
    }

    public Long getClicks() {
        return clicks;
    }

    public void setClicks(final Long clicks) {
        this.clicks = clicks;
    }

    public Double getCtr() {
        return ctr;
    }

    public void setCtr(final Double ctr) {
        this.ctr = ctr;
    }

    public Double getRegsPerClick() {
        return regsPerClick;
    }

    public void setRegsPerClick(final Double regsPerClick) {
        this.regsPerClick = regsPerClick;
    }

    public Double getSpent() {
        return spent;
    }

    public void setSpent(final Double spent) {
        this.spent = spent;
    }

    public Double getRoi() {
        return roi;
    }

    public void setRoi(final Double roi) {
        this.roi = roi;
    }
}
