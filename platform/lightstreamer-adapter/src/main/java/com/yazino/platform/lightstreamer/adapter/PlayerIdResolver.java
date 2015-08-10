package com.yazino.platform.lightstreamer.adapter;

import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerIdResolver {
    private static final Pattern PATTERN = Pattern.compile("\\d+");

    public BigDecimal resolve(final String item) {
        if (StringUtils.isBlank(item)) {
            throw new IllegalArgumentException("item is empty");
        }
        final Matcher matcher = PATTERN.matcher(item);
        if (matcher.find()) {
            return new BigDecimal(matcher.group());
        }
        throw new IllegalArgumentException(String.format("Item '%s' is invalid", item));
    }
}
