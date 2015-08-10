package com.yazino.engagement.android;

import com.google.android.gcm.server.Message;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class WithMessageTypeMatcher extends TypeSafeDiagnosingMatcher<Message> {

    private final String expectedMessageType;

    public WithMessageTypeMatcher(String expectedMessageType) {
        this.expectedMessageType = expectedMessageType;
    }

    @Override
    protected boolean matchesSafely(Message message, Description mismatchDescription) {
        if (!expectedMessageType.equals(message.getData().get("type"))) {
            mismatchDescription.appendText("with type ").appendValue(message.getData().get("type"));
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("with type ").appendValue(expectedMessageType);
    }
}
