package com.yazino.platform.community;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.Validate.notNull;

public class ProfanityFilter {
    private static final Logger LOG = LoggerFactory.getLogger(ProfanityFilter.class);

    private final ProfanityService profanityService;

    private Pattern pattern;

    @Autowired
    public ProfanityFilter(final ProfanityService profanityService) {
        notNull(profanityService, "profanityService may not be null");

        this.profanityService = profanityService;
    }

    public String filter(final String originalString) {
        notNull(originalString, "Original string is required");

        initPattern();

        final Matcher m = pattern.matcher(originalString);
        final StringBuilder filtered = new StringBuilder();
        int prevEnd = 0;
        while (m.find()) {
            if (m.start() > prevEnd) {
                filtered.append(originalString.substring(prevEnd, m.start()));
            }
            for (int i = 0; i < m.end() - m.start(); i++) {
                filtered.append("*");
            }
            prevEnd = m.end();
        }
        filtered.append(originalString.substring(prevEnd));
        return filtered.toString();
    }

    private void initPattern() {
        if (pattern != null && !StringUtils.isBlank(pattern.pattern())) {
            return;
        }
        final StringBuilder regex = new StringBuilder();
        for (String word : profanityService.findAllProhibitedWords()) {
            if (regex.length() > 0) {
                regex.append("|");
            }
            regex.append("\\b").append(word).append("\\b");
        }
        for (String word : profanityService.findAllProhibitedPartWords()) {
            if (regex.length() > 0) {
                regex.append("|");
            }
            regex.append(word);
        }

        LOG.info("Initialising profanity Filter with expression {}", regex);

        pattern = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
    }

}
