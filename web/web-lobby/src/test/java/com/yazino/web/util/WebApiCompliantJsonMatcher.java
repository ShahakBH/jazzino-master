package com.yazino.web.util;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Verifies that response has encoding "UTF-8", content type "application/json" and has json content equal to given object.
 * @param <T>
 */
public class WebApiCompliantJsonMatcher<T> extends TypeSafeDiagnosingMatcher<MockHttpServletResponse> {

    public static WebApiCompliantJsonMatcher isWebApiCompliantResponseWithJsonEqualTo(Object object) {
        return new WebApiCompliantJsonMatcher(object);
    }

    private T expectedObject;

    WebApiCompliantJsonMatcher(T expectedObject) {
        this.expectedObject = expectedObject;
    }

    @Override
    protected boolean matchesSafely(MockHttpServletResponse response, Description mismatchDescription) {
        if (!"UTF-8".equals(response.getCharacterEncoding())) {
            mismatchDescription.appendText("character encoding was: ").appendText(response.getCharacterEncoding());
            return false;
        }
        if (!"application/json".equals(response.getContentType())) {
            mismatchDescription.appendText("content type was: ").appendText(response.getContentType());
            return false;
        }
        try {
            Object actualObject = new JsonHelper().deserialize(expectedObject.getClass(), response.getContentAsString());
            if (!EqualsBuilder.reflectionEquals(expectedObject, actualObject, false, null, "stackTrace")) {
                mismatchDescription.appendText("content was : ").appendText(ToStringBuilder.reflectionToString(actualObject));
                return false;
            }
        } catch (Exception e) {
            mismatchDescription.appendText("Failed to deserialise response content.");
            return false;
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("response with character encoding: UTF-8, content type: UTF-8 and content equal to").appendValue(expectedObject);
    }
}
