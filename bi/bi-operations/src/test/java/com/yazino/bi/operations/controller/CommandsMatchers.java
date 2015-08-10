package com.yazino.bi.operations.controller;

import static com.yazino.bi.operations.util.DateIntervalHelper.*;

import java.util.Date;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import com.yazino.bi.operations.model.CommandWithDateIntervals;

public final class CommandsMatchers {
    private CommandsMatchers() {
    }

    public static Matcher<CommandWithDateIntervals> containsDatesInterval(final Date start, final Date end) {
        return new ContainsDatesInterval(start, end);
    }

    private static final class ContainsDatesInterval extends BaseMatcher<CommandWithDateIntervals> {
        private final Date start;
        private final Date end;

        private ContainsDatesInterval(final Date start, final Date end) {
            super();
            this.start = getDateStart(start);
            this.end = getDateEnd(end);
        }

        @Override
        public boolean matches(final Object item) {
            if (!(item instanceof CommandWithDateIntervals)) {
                return false;
            }
            final CommandWithDateIntervals itemCast = (CommandWithDateIntervals) item;
            return start.equals(itemCast.getStart()) && end.equals(itemCast.getEnd());
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("Dates must contain ").appendText(start.toString()).appendText(" and ")
                    .appendText(end.toString());
        }

    }
}
