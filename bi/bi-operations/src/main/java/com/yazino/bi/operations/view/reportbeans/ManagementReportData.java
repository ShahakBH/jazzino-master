package com.yazino.bi.operations.view.reportbeans;

import static com.yazino.bi.operations.view.ReportColumnFormat.*;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.yazino.bi.operations.view.CustomColumnFormat;
import com.yazino.bi.operations.view.FormatClassSource;
import com.yazino.bi.operations.view.ReportField;

public class ManagementReportData {
    private static final int WIDE_COLUMN = 10;
    private static final int NARROW_COLUMN = 8;

    @ReportField(position = 0, header = "Date", columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String selection;

    @ReportField(position = 1, header = "Regs", format = INTEGER, columnWidth = NARROW_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long registrations;

    @ReportField(position = 2, header = "Users", format = INTEGER, columnWidth = NARROW_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long users;

    @ReportField(position = 3, header = "Players", format = INTEGER, columnWidth = NARROW_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long players;

    @ReportField(position = 4, header = "Revenue", format = POUND, columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_POUNDS)
    // End of custom formats
    })
    private Double revenue;

    @ReportField(position = 5, header = "No. purchases", format = INTEGER, columnWidth = NARROW_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long purchases;

    @FormatClassSource
    private String rowFormat;

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("selection", selection)
                .append("registrations", registrations).append("users", users).append("players", players)
                .append("revenue", revenue).append("purchases", purchases).toString();
    }

    public ManagementReportData() {
        this.registrations = 0L;
        this.players = 0L;
        this.users = 0L;
        this.revenue = 0D;
        this.purchases = 0L;
    }

    public String getSelection() {
        return selection;
    }

    public void setSelection(final String selection) {
        this.selection = selection;
    }

    public Long getRegistrations() {
        return registrations;
    }

    public void setRegistrations(final Long registrations) {
        this.registrations = registrations;
    }

    public Long getUsers() {
        return users;
    }

    public void setUsers(final Long users) {
        this.users = users;
    }

    public Long getPlayers() {
        return players;
    }

    public void setPlayers(final Long players) {
        this.players = players;
    }

    public Double getRevenue() {
        return revenue;
    }

    public void setRevenue(final Double revenue) {
        this.revenue = revenue;
    }

    public Long getPurchases() {
        return purchases;
    }

    public void setPurchases(final Long purchases) {
        this.purchases = purchases;
    }

    public String getRowFormat() {
        return rowFormat;
    }

    public void setRowFormat(final String rowFormat) {
        this.rowFormat = rowFormat;
    }
}
