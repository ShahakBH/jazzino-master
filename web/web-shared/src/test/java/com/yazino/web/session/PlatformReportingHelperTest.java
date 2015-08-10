package com.yazino.web.session;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static com.yazino.web.session.PlatformReportingHelper.REQUEST_URL;
import static com.yazino.web.session.PlatformReportingHelper.getRequestUrl;

@RunWith(MockitoJUnitRunner.class)
public class PlatformReportingHelperTest {
    @Mock
    private HttpServletRequest request;

    @Test
    public void shouldUseRequestsStartPageAttributeIfExists() {
        given(request.getRequestURL()).willReturn(new StringBuffer("http://url"));
        given(request.getAttribute(REQUEST_URL)).willReturn("http://www.yazino.com/public/registration/testable/url");
        final String requestUrl = getRequestUrl(request);
        assertThat(requestUrl, is("http://www.yazino.com/public/registration/testable/url"));
    }
}
