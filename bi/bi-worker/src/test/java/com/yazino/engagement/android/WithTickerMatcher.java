package com.yazino.engagement.android;

import com.google.android.gcm.server.Message;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class WithTickerMatcher extends TypeSafeDiagnosingMatcher<Message> {

    private final String expectedTicker;

    public WithTickerMatcher(String expectedTicker) {
        this.expectedTicker = expectedTicker;
    }

    @Override
    protected boolean matchesSafely(Message message, Description mismatchDescription) {
        if (!expectedTicker.equals(message.getData().get("ticker"))) {
            mismatchDescription.appendText("with ticker ").appendValue(message.getData().get("ticker"));
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("with ticker ").appendValue(expectedTicker);
    }
}
