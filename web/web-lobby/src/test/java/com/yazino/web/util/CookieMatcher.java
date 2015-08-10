package com.yazino.web.util;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import javax.servlet.http.Cookie;

public class CookieMatcher extends TypeSafeMatcher<Cookie> {
    private final Cookie expected;

    public CookieMatcher(Cookie expected) {
        this.expected = expected;
    }

    @Override
    public boolean matchesSafely(Cookie actual) {
        return actual.getValue().equals(expected.getValue()) &&
                actual.getPath().equals(expected.getPath()) &&
                actual.getName().equals(expected.getName()) &&
                actual.getMaxAge() == expected.getMaxAge();
    }

    public void describeTo(Description description) {
        description.appendText("does not match");
    }

    @Factory
    public static <T> Matcher<Cookie> matchesCookie(Cookie expected) {
        return new CookieMatcher(expected);
    }

}

