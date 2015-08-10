package com.yazino.engagement.android;

import com.google.android.gcm.server.Message;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class WithCollapseKeyMatcher extends TypeSafeDiagnosingMatcher<Message> {

    private final String expectedCollapseKey;

    public WithCollapseKeyMatcher(String expectedCollapseKey) {
        this.expectedCollapseKey = expectedCollapseKey;
    }

    @Override
    protected boolean matchesSafely(Message message, Description mismatchDescription) {
        String collapseKey = message.getCollapseKey();
        if (!expectedCollapseKey.equals(collapseKey)) {
            mismatchDescription.appendText("with collapse-key ").appendValue(collapseKey);
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("with collapse-key ").appendValue(expectedCollapseKey);
    }
}
