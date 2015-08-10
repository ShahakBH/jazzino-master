package com.yazino.bi.operations.controller;

import com.yazino.bi.operations.model.CommandWithDateIntervals;
import com.yazino.bi.operations.model.CommandWithPlatformAndFormat;

import java.util.Date;

import static com.yazino.bi.operations.util.DateIntervalHelper.getDateEnd;
import static com.yazino.bi.operations.util.DateIntervalHelper.getDateStart;

public class CommandWithPlatformFormatAndDates extends CommandWithPlatformAndFormat implements
        CommandWithDateIntervals {
    private Date start;
    private Date end;

    @Override
    public Date getStart() {
        if (start == null) {
            return null;
        } else {
            return getDateStart(start);
        }
    }

    public void setStart(final Date start) {
        this.start = start;
    }

    @Override
    public Date getEnd() {
        if (end == null) {
            return null;
        } else {
            return getDateEnd(end);
        }
    }

    public void setEnd(final Date end) {
        this.end = end;
    }
}
