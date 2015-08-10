package com.yazino.engagement.android;

import com.google.android.gcm.server.Message;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class WithMessageMatcher extends TypeSafeDiagnosingMatcher<Message> {

    private final String expectedMessage;

    public WithMessageMatcher(String expectedMessage) {
        this.expectedMessage = expectedMessage;
    }

    @Override
    protected boolean matchesSafely(Message message, Description mismatchDescription) {
        if (!expectedMessage.equals(message.getData().get("message"))) {
            mismatchDescription.appendText("with message ").appendValue(message.getData().get("message"));
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("with message ").appendValue(expectedMessage);
    }
}
