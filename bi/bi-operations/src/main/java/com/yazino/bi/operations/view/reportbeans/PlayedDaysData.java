package com.yazino.bi.operations.view.reportbeans;

import static com.yazino.bi.operations.view.ReportColumnFormat.*;
import com.yazino.bi.operations.view.CustomColumnFormat;
import com.yazino.bi.operations.view.ReportField;

public class PlayedDaysData {
    private static final int WIDE_COLUMN = 10;
    private static final int NARROW_COLUMN = 8;

    @ReportField(position = 0, header = "Player id", format = INTEGER, columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long playerId;

    @ReportField(position = 1, header = "Game", columnWidth = NARROW_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_STRING)
    // End of custom formats
    })
    private String game;

    @ReportField(position = 2, header = "Count", format = INTEGER, columnWidth = WIDE_COLUMN, customFormats = {
    // Custom format for the totals line
    @CustomColumnFormat(value = "totals", format = TOTALS_INTEGER)
    // End of custom formats
    })
    private Long count;

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final Long playerId) {
        this.playerId = playerId;
    }

    public String getGame() {
        return game;
    }

    public void setGame(final String game) {
        this.game = game;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(final Long count) {
        this.count = count;
    }

}
