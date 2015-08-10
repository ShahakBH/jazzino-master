package com.yazino.engagement.android;

import com.google.android.gcm.server.Message;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class WithTimeToLiveMatcher extends TypeSafeDiagnosingMatcher<Message> {

    private final int expectedTimeToLive;

    public WithTimeToLiveMatcher(int expectedTimeToLive) {
        this.expectedTimeToLive = expectedTimeToLive;
    }

    @Override
    protected boolean matchesSafely(Message message, Description mismatchDescription) {
        if (expectedTimeToLive != message.getTimeToLive()) {
            mismatchDescription.appendText("with time-to-live ").appendValue(message.getTimeToLive());
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("with time-to-live ").appendValue(expectedTimeToLive);
    }
}
