package com.yazino.web.security;

import org.junit.Test;
import org.springframework.util.PathMatcher;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class WhiteListDomainTest {

    private final Set<String> whiteListedUrls = newHashSet("/whitelisted");
    private final PathMatcher pathMatcher = mock(PathMatcher.class);

    private WhiteListDomain underTest = new WhiteListDomain(whiteListedUrls, pathMatcher);

    @Test
    public void shouldIncludeUrlWhenMatchedByAnyPathMatcher() {
        String pattern = "/public/**";
        String pattern2 = "/private/**";
        String url = "/public/resource";
        when(pathMatcher.match(pattern, url)).thenReturn(true);
        when(pathMatcher.match(pattern2, url)).thenReturn(false);
        underTest.addWhiteListedUrl(pattern);

        assertTrue(underTest.includesUrl(url));
    }

    @Test
    public void shouldNotIncludeUrlWhenNotMatchedByAnyPathMatcher() {
        String pattern = "/public/**";
        String pattern2 = "/private/**";
        String url = "/resource";
        when(pathMatcher.match(pattern, url)).thenReturn(false);
        when(pathMatcher.match(pattern2, url)).thenReturn(false);
        underTest.addWhiteListedUrl(pattern);

        assertFalse(underTest.includesUrl(url));
    }
}
