package com.yazino.web.domain.facebook;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FacebookLocaleParser {

    private static final Set<String> COUNTRIES = new HashSet<String>(Arrays.asList(Locale.getISOCountries()));
    static final String DEFAULT = "US";

    public String parseCountry(final String locale) {
        if (StringUtils.isBlank(locale)) {
            return DEFAULT;
        }

        final String[] tokens = locale.split("_");
        if (tokens.length < 2) {
            return DEFAULT;
        }

        final String potentialCountry = tokens[1];
        if (!COUNTRIES.contains(potentialCountry)) {
            return DEFAULT;
        }
        return potentialCountry;

    }
}
