package com.yazino.web.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public final class OpenGraphObjectIdParser {

    private OpenGraphObjectIdParser() {

    }

    public static OpenGraphObjectId parse(final String rawObject) {

        final Pattern simplePattern = Pattern.compile("([a-z]{2})_([a-z0-9]+)");
        final Pattern levelPattern = Pattern.compile("([a-z]{2})_([lev]{5})_([0-9]+)");

        final Matcher simplePatternMatcher = simplePattern.matcher(rawObject);
        final Matcher levelPatternMatcher = levelPattern.matcher(rawObject);


        if (levelPatternMatcher.matches()) {
            return new OpenGraphObjectId(levelPatternMatcher.group(1), levelPatternMatcher.group(2),
                    parseInt(levelPatternMatcher.group(3)));
        } else if (simplePatternMatcher.matches()) {
            return new OpenGraphObjectId(simplePatternMatcher.group(1), simplePatternMatcher.group(2));
        } else {
            throw new IllegalArgumentException("Unrecognised format: \"" + rawObject + "\".");
        }
    }

    public static OpenGraphObjectId parseGameType(final String rawObject) {

        final Pattern simplePattern = Pattern.compile("([A-Z]+)_([a-z0-9]+)");
        final Pattern doublePattern = Pattern.compile("([A-Z]+)_([A-Z]+)_([a-z0-9]+)");
        final Pattern levelPattern = Pattern.compile("([A-Z]+)_([lev]{5})_([0-9]+)");
        final Pattern doubleLevelPattern = Pattern.compile("([A-Z]+)_([A-Z]+)_([lev]{5})_([0-9]+)");

        final Matcher simplePatternMatcher = simplePattern.matcher(rawObject);
        final Matcher doublePatternMatcher = doublePattern.matcher(rawObject);
        final Matcher levelPatternMatcher = levelPattern.matcher(rawObject);
        final Matcher doubleLevelPatternMatcher = doubleLevelPattern.matcher(rawObject);


        if (levelPatternMatcher.matches()) {
            return new OpenGraphObjectId(levelPatternMatcher.group(1), levelPatternMatcher.group(2),
                    parseInt(levelPatternMatcher.group(3)));

        } else if (doubleLevelPatternMatcher.matches()) {
            return new OpenGraphObjectId(doubleLevelPatternMatcher.group(1) + "_" + doubleLevelPatternMatcher.group(2),
                    doubleLevelPatternMatcher.group(3), parseInt(doubleLevelPatternMatcher.group(4)));

        } else if (simplePatternMatcher.matches()) {
            return new OpenGraphObjectId(simplePatternMatcher.group(1), simplePatternMatcher.group(2));

        } else if (doublePatternMatcher.matches()) {
            return new OpenGraphObjectId(doublePatternMatcher.group(1) + "_" + doublePatternMatcher.group(2),
                    doublePatternMatcher.group(3));
        } else {
            throw new IllegalArgumentException("Unrecognised format: \"" + rawObject + "\".");
        }
    }


}
