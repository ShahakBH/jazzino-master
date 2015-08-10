package com.yazino.mobile.ws.spring;

import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * This class is used by the {@link PatternMatchingPropertyConfigurer} to allow a map to be declared based on
 * patterns in property keys.
 * The pattern should have one group which will be used as the key for the corresponding value.
 */
public class PatternMatchingPropertyMap extends HashMap<String, String> {

    private static final long serialVersionUID = 1856705482634819544L;

    private final Pattern mPattern;

    public PatternMatchingPropertyMap(final Pattern pattern) {
        Validate.notNull(pattern);
        mPattern = pattern;
    }

    public Pattern getPattern() {
        return mPattern;
    }
}
