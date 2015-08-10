package com.yazino.engagement.android;

import com.google.android.gcm.server.Message;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class WithMessageObject extends TypeSafeDiagnosingMatcher<Message> {

    private Message expectedMessage;

    public WithMessageObject(Message expectedMessage) {

        this.expectedMessage = expectedMessage;
    }

    @Override
    protected boolean matchesSafely(Message item, Description mismatchDescription) {
        if (!expectedMessage.getTimeToLive().equals(item.getTimeToLive())) {
            mismatchDescription.appendText("expected ").appendText(expectedMessage.getTimeToLive().toString())
                    .appendText(" but got ").appendText(item.getTimeToLive().toString());
            return false;
        } else if (!expectedMessage.getCollapseKey().equals(item.getCollapseKey())) {
            mismatchDescription.appendText("expected ").appendText(expectedMessage.getCollapseKey())
                    .appendText(" but got ").appendText(item.getCollapseKey());
            return false;
        } else if (!expectedMessage.getData().equals(item.getData())) {
            mismatchDescription.appendText("expected ").appendText(expectedMessage.getData().toString())
                    .appendText(" but got ").appendText(item.getData().toString());
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("").appendText(expectedMessage.toString());
    }
}
