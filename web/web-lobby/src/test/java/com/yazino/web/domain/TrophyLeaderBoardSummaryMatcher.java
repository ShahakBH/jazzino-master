package com.yazino.web.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class TrophyLeaderBoardSummaryMatcher extends TypeSafeMatcher<TrophyLeaderBoardSummary> {
    private TrophyLeaderBoardSummary summary;

    TrophyLeaderBoardSummaryMatcher(TrophyLeaderBoardSummary summary) {
        this.summary = summary;
    }

    public static TrophyLeaderBoardSummaryMatcher is(TrophyLeaderBoardSummary summary) {
        return new TrophyLeaderBoardSummaryMatcher(summary);
    }

    @Override
    protected boolean matchesSafely(TrophyLeaderBoardSummary trophyLeaderBoardSummary) {
        return EqualsBuilder.reflectionEquals(summary, trophyLeaderBoardSummary, false);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(" summary ").appendValue(summary);
    }
}
