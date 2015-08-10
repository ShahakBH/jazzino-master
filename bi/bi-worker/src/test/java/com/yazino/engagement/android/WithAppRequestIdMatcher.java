package com.yazino.engagement.android;

import com.google.android.gcm.server.Message;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

public class WithAppRequestIdMatcher extends TypeSafeDiagnosingMatcher<Message> {

    private final int expectedAppRequestId;

    public WithAppRequestIdMatcher(int expectedAppRequestId) {
        this.expectedAppRequestId = expectedAppRequestId;
    }

    @Override
    protected boolean matchesSafely(Message message, Description mismatchDescription) {
        String appRequestIdString = message.getData().get("appRequestId");
        Integer appRequestId = null;
        if (appRequestIdString != null) {
            appRequestId = Integer.parseInt(appRequestIdString);
        }
        if (expectedAppRequestId != appRequestId) {
            mismatchDescription.appendText("with app-request-id ").appendValue(appRequestId);
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("with app-request-id ").appendValue(expectedAppRequestId);
    }
}
