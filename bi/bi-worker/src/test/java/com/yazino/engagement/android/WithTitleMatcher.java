package com.yazino.engagement.android;

import com.google.android.gcm.server.Message;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class WithTitleMatcher extends TypeSafeDiagnosingMatcher<Message> {

    private final String expectedTitle;

    public WithTitleMatcher(String expectedTitle) {
        this.expectedTitle = expectedTitle;
    }

    @Override
    protected boolean matchesSafely(Message message, Description mismatchDescription) {
        if (!expectedTitle.equals(message.getData().get("title"))) {
            mismatchDescription.appendText("with title ").appendValue(message.getData().get("title"));
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("with title ").appendValue(expectedTitle);
    }
}
