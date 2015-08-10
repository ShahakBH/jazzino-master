package com.yazino.web.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.PathMatcher;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.Validate.notNull;

public class WhiteListDomain implements Domain {

    private final PathMatcher pathMatcher;
    private final Set<String> whiteListedUrls;

    public WhiteListDomain(Set<String> whiteListedUrls,
                           @Qualifier("authenticationWhiteListPathMatcher") final PathMatcher pathMatcher) {
        checkNotNull(whiteListedUrls);
        checkNotNull(pathMatcher);
        this.whiteListedUrls = new CopyOnWriteArraySet<String>(whiteListedUrls);
        this.pathMatcher = pathMatcher;
    }

    public void addWhiteListedUrl(final String url) {
        notNull(url, "url may not be null");

        whiteListedUrls.add(url);
    }

    @Override
    public boolean includesUrl(String url) {
        for (final String whiteListedUrlPattern : whiteListedUrls) {
            if (pathMatcher.match(whiteListedUrlPattern, url)) {
                return true;
            }
        }
        return false;
    }
}
