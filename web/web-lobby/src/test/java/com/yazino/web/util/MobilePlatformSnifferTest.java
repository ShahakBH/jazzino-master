package com.yazino.web.util;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.util.List;

import static com.yazino.web.util.MobilePlatformSniffer.MobilePlatform;
import static org.apache.http.HttpHeaders.USER_AGENT;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

// These are not comprehensive tests, just something for a smidgen of confidence
public class MobilePlatformSnifferTest {

    private MobilePlatformSniffer underTest = new MobilePlatformSniffer();

    @SuppressWarnings("unchecked")
    @Test
    public void shouldIdentifyIosUserAgents() throws IOException {
        List<String> userAgentPrefixes = IOUtils.readLines(getClass().getResourceAsStream("ios-user-agents.txt"));
        for (String prefix : userAgentPrefixes) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(USER_AGENT, prefix + "/something");
            assertThat(underTest.inferPlatform(request), is(equalTo(MobilePlatform.IOS)));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldIdentifyAndroidUserAgents() throws IOException {
        List<String> userAgentPrefixes = IOUtils.readLines(getClass().getResourceAsStream("android-user-agents.txt"));
        for (String prefix : userAgentPrefixes) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(USER_AGENT, prefix + "/something");
            assertThat(underTest.inferPlatform(request), is(equalTo(MobilePlatform.ANDROID)));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAvoidFalsePositivesWhenIdentifyingOtherUserAgents() throws IOException {
        List<String> userAgentPrefixes = IOUtils.readLines(getClass().getResourceAsStream("other-user-agents.txt"));
        for (String prefix : userAgentPrefixes) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(USER_AGENT, prefix + "/something");
            assertThat(underTest.inferPlatform(request), is(not(equalTo(MobilePlatform.IOS))));
            assertThat(underTest.inferPlatform(request), is(not(equalTo(MobilePlatform.ANDROID))));
        }
    }
}
