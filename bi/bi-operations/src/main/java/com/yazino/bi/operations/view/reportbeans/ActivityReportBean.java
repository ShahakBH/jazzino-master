package com.yazino.bi.operations.view.reportbeans;

import static com.yazino.bi.operations.view.ReportColumnFormat.*;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.yazino.bi.operations.view.CustomColumnFormat;
import com.yazino.bi.operations.view.FormatClassSource;
import com.yazino.bi.operations.view.ReportField;

/**
 * Bean holding the information about a report row
 */
public class ActivityReportBean {
    @ReportField(position = 0, header = "Report interval", columnWidth = 20, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "averages", format = TOTALS_STRING)
    // End of custom formats
    })
    private String reportInterval;

    @ReportField(position = 1, header = "Total users", format = INTEGER, columnWidth = 10, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "averages", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long users;

    @ReportField(position = 2, header = "Total players", format = INTEGER, columnWidth = 10, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "averages", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long players;

    @ReportField(position = 3, multipleColumnsSource = "gameTypes", format = INTEGER, columnWidth = 10, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "averages", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private List<Long> games;

    private static List<String> gameTypes;

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("reportInterval", reportInterval).append("users", users)
                .append("players", players).append("games", games).append("rowFormat", rowFormat)
                .append("rawDate", rawDate).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof ActivityReportBean)) {
            return false;
        }
        final ActivityReportBean castOther = (ActivityReportBean) other;
        return new EqualsBuilder().append(reportInterval, castOther.reportInterval)
                .append(users, castOther.users).append(players, castOther.players)
                .append(games, castOther.games).append(rowFormat, castOther.rowFormat)
                .append(rawDate, castOther.rawDate).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(reportInterval).append(users).append(players).append(games)
                .append(rowFormat).append(rawDate).toHashCode();
    }

    public List<Long> getGames() {
        return games;
    }

    public void setGames(final List<Long> games) {
        this.games = games;
    }

    public static List<String> getGameTypes() {
        return gameTypes;
    }

    public static void setGameTypes(final List<String> gameTypes) {
        ActivityReportBean.gameTypes = gameTypes;
    }

    @FormatClassSource
    private String rowFormat;

    private Long rawDate;

    public String getReportInterval() {
        return reportInterval;
    }

    public void setReportInterval(final String reportInterval) {
        this.reportInterval = reportInterval;
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

    public Long getRawDate() {
        return rawDate;
    }

    public void setRawDate(final Long rawDate) {
        this.rawDate = rawDate;
    }

    public String getRowFormat() {
        return rowFormat;
    }

    public void setRowFormat(final String rowFormat) {
        this.rowFormat = rowFormat;
    }

}
