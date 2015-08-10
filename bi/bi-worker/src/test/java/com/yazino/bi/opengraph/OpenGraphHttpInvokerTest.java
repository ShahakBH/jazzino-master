package com.yazino.bi.opengraph;

import com.yazino.platform.opengraph.OpenGraphAction;
import com.yazino.platform.opengraph.OpenGraphObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class OpenGraphHttpInvokerTest {


    private static final String ACCESS_TOKEN = "some-access-token";
    private static final String APP_1_NAMESPACE = "testwheeldeal1";
    private static final String APP_2_NAMESPACE = "testwheeldeal2";
    public static final String ACTION_1_NAME = "spin1";
    public static final String ACTION_2_NAME = "spin2";
    public static final String OBJECT_1_TYPE = "wheel1";
    public static final String OBJECT_2_TYPE = "wheel2";
    public static final String OBJECT_1_NAME = "wheel1";
    public static final String OBJECT_2_NAME = "wheel2";
    private static final String  FB_REF_PREFIX ="fb_og_";
    public static final OpenGraphObject OBJECT_1 = new OpenGraphObject(OBJECT_1_TYPE, OBJECT_1_NAME);
    public static final OpenGraphObject OBJECT_2 = new OpenGraphObject(OBJECT_2_TYPE, OBJECT_2_NAME);
    public static final String OPEN_GRAPH_HOST = "http://www.example.com";
    private final OpenGraphAction ACTION_1 = new OpenGraphAction(ACTION_1_NAME, OBJECT_1);
    private final OpenGraphAction ACTION_2 = new OpenGraphAction(ACTION_2_NAME, OBJECT_2);


    private OpenGraphHttpInvoker underTest;
    private HttpClient httpClient;
    private HttpEntity entity;

    @Before
    public void setUp() throws IOException {
        httpClient = mock(HttpClient.class);
        underTest = new OpenGraphHttpInvoker(httpClient, OPEN_GRAPH_HOST);

        BasicHttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 0), 200, "OK"));
        entity = mock(HttpEntity.class);
        response.setEntity(entity);
        final InputStream inputStream = spy(new ByteArrayInputStream("{}".getBytes()));
        when(entity.getContent()).thenReturn(inputStream);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(response);
    }

    // TODO add test for close on fail (requires refactoring)
    @Test
    public void publishAction_shouldCloseInputStream() throws IOException {
        underTest.publishAction(ACCESS_TOKEN, ACTION_1, APP_1_NAMESPACE);
        verify(entity.getContent()).close();
    }

    @Test
    public void publishAction_shouldSpecifyCorrectAction() throws IOException {
        underTest.publishAction(ACCESS_TOKEN, ACTION_1, APP_1_NAMESPACE);
        verify(httpClient).execute(argThat(is(openGraphUriWithActionName(ACTION_1_NAME))));

        underTest.publishAction(ACCESS_TOKEN, ACTION_2, APP_1_NAMESPACE);
        verify(httpClient).execute(argThat(is(openGraphUriWithActionName(ACTION_2_NAME))));
    }

    @Test
    public void publishAction_shouldSpecifyCorrectObjectName() throws IOException {
        underTest.publishAction(ACCESS_TOKEN, ACTION_1, APP_1_NAMESPACE);
        verify(httpClient).execute(argThat(is(openGraphUriWithObjectName(OBJECT_1_TYPE))));


        underTest.publishAction(ACCESS_TOKEN, ACTION_2, APP_1_NAMESPACE);
        verify(httpClient).execute(argThat(is(openGraphUriWithObjectName(OBJECT_2_TYPE))));
    }

    @Test
    public void publishAction_shouldSpecifyObjectNameAsRef() throws IOException {
        underTest.publishAction(ACCESS_TOKEN, ACTION_1, APP_1_NAMESPACE);
        verify(httpClient).execute(argThat(is(openGraphUriWithRef(FB_REF_PREFIX+OBJECT_1_TYPE))));

        underTest.publishAction(ACCESS_TOKEN, ACTION_2, APP_1_NAMESPACE);
        verify(httpClient).execute(argThat(is(openGraphUriWithRef(FB_REF_PREFIX+OBJECT_2_TYPE))));
    }

    @Test
    public void publishAction_shouldSpecifyCorrectObjectUrl() throws IOException {
        underTest.publishAction(ACCESS_TOKEN, ACTION_1, APP_1_NAMESPACE);
        verify(httpClient).execute(argThat(is(openGraphUriWithObjectUrl(OBJECT_1_TYPE, String.format(OpenGraphHttpInvoker.OBJECT_URL_FORMAT, OPEN_GRAPH_HOST, OBJECT_1_TYPE, OBJECT_1_NAME)))));

        underTest.publishAction(ACCESS_TOKEN, ACTION_2, APP_1_NAMESPACE);
        verify(httpClient).execute(argThat(is(openGraphUriWithObjectUrl(OBJECT_2_TYPE, String.format(OpenGraphHttpInvoker.OBJECT_URL_FORMAT, OPEN_GRAPH_HOST, OBJECT_2_TYPE, OBJECT_2_NAME)))));
    }

    @Test
    public void publishAction_shouldSpecifyCorrectAppNamespace() throws IOException {
        underTest.publishAction(ACCESS_TOKEN, ACTION_1, APP_1_NAMESPACE);
        verify(httpClient).execute(argThat(is(openGraphUriWithAppNamespace(APP_1_NAMESPACE))));

        underTest.publishAction(ACCESS_TOKEN, ACTION_2, APP_2_NAMESPACE);
        verify(httpClient).execute(argThat(is(openGraphUriWithAppNamespace(APP_2_NAMESPACE))));
    }

    private Matcher<HttpUriRequest> openGraphUriWithObjectUrl(String expectedObjectName,
                                                              String expectedObjectUrl) {
        return new OpenGraphUriWithObjectUrl(expectedObjectName, expectedObjectUrl);
    }

    private Matcher<HttpUriRequest> openGraphUriWithAppNamespace(String expectedAppNamespace) {
        return new OpenGraphUriWithAppNamespace(expectedAppNamespace);
    }

    private Matcher<HttpUriRequest> openGraphUriWithActionName(String expectedName) {
        return new OpenGraphUriWithActionNameMatcher(expectedName);
    }

    private Matcher<HttpUriRequest> openGraphUriWithObjectName(String expectedName) {
        return new OpenGraphUriWithObjectNameMatcher(expectedName);
    }

    private Matcher<HttpUriRequest> openGraphUriWithRef(String expectedRef) {
        return new OpenGraphUriWithRef(expectedRef);
    }

    public static class D extends TypeSafeDiagnosingMatcher<HttpUriRequest> {

        private String expectedAccessToken;

        public D(String expectedAccessToken) {
            this.expectedAccessToken = expectedAccessToken;
        }

        @Override
        protected boolean matchesSafely(HttpUriRequest item, Description mismatchDescription) {
            String accessToken = (String) item.getParams().getParameter("accessToken");
            return expectedAccessToken.equals(accessToken);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("HTTP POST with accessToken parameter '" + expectedAccessToken + "'.");
        }
    }

    public static class OpenGraphUriWithAppNamespace extends TypeSafeDiagnosingMatcher<HttpUriRequest> {

        private String expectedAppNamespace;

        public OpenGraphUriWithAppNamespace(String expectedAppNamespace) {
            this.expectedAppNamespace = expectedAppNamespace;
        }

        @Override
        protected boolean matchesSafely(HttpUriRequest item, Description mismatchDescription) {
            String appNamespace = item.getURI().getPath().replaceAll("/me/", "").split(":")[0];
            return expectedAppNamespace.equals(appNamespace);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("HTTP POST with appNamespace '" + expectedAppNamespace + "'.");
        }
    }

    public static class OpenGraphUriWithActionNameMatcher extends TypeSafeDiagnosingMatcher<HttpUriRequest> {

        private String expectedActionName;

        public OpenGraphUriWithActionNameMatcher(String expectedActionName) {
            this.expectedActionName = expectedActionName;
        }

        @Override
        protected boolean matchesSafely(HttpUriRequest item, Description mismatchDescription) {
            String[] split = item.getURI().getPath().replaceAll("/me/", "").split(":");
            return (split.length == 2 && expectedActionName.equals(split[1]));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("HTTP POST with action '" + expectedActionName + "'.");
        }
    }

    public static class OpenGraphUriWithObjectNameMatcher extends TypeSafeDiagnosingMatcher<HttpUriRequest> {

        private String expectedObjectName;

        public OpenGraphUriWithObjectNameMatcher(String expectedObjectName) {
            this.expectedObjectName = expectedObjectName;
        }

        @Override
        protected boolean matchesSafely(HttpUriRequest item, Description mismatchDescription) {
            if (!(item instanceof HttpPost)) {
                return false;
            }
            String content = readContent((HttpPost) item);
            return (content + "=").contains(expectedObjectName);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("HTTP POST with object name '" + expectedObjectName + "'.");
        }
    }


    public static class OpenGraphUriWithObjectUrl extends TypeSafeDiagnosingMatcher<HttpUriRequest> {

        private String expectedObjectName;
        private String expectedObjectUrl;

        public OpenGraphUriWithObjectUrl(String expectedObjectName, String expectedObjectUrl) {
            this.expectedObjectName = expectedObjectName;
            this.expectedObjectUrl = expectedObjectUrl;
        }

        @Override
        protected boolean matchesSafely(HttpUriRequest item, Description mismatchDescription) {
            if (!(item instanceof HttpPost)) {
                return false;
            }
            String content = readContent((HttpPost) item);
            java.util.regex.Matcher matcher;
            try {
                matcher = Pattern.compile("[^?].*?(.*&)?" + expectedObjectName + "=" + URLEncoder.encode(expectedObjectUrl, "UTF-8") + ".*").matcher(content);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Unable to encode URL.", e);
            }
            return matcher.matches();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("HTTP POST with object named '" + expectedObjectName + "'.")
                    .appendText(" and with object URL ").appendValue(expectedObjectUrl);
        }
    }

    public static class I extends TypeSafeDiagnosingMatcher<HttpUriRequest> {

        private String expectedAccessToken;

        public I(String expectedAccessToken) {
            this.expectedAccessToken = expectedAccessToken;
        }

        @Override
        protected boolean matchesSafely(HttpUriRequest item, Description mismatchDescription) {
            return item.getMethod().equals("GET") && item.getURI().getPath().startsWith("/me/permissions");
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("HTTP POST with accessToken parameter '" + expectedAccessToken + "'.");
        }
    }

    public static class J extends TypeSafeDiagnosingMatcher<HttpUriRequest> {

        @Override
        protected boolean matchesSafely(HttpUriRequest item, Description mismatchDescription) {
            Pattern publishAction = Pattern.compile("/me/[^:]+:[^:]+.*");
            java.util.regex.Matcher m = publishAction.matcher(item.getURI().getPath());
            return m.matches();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("HTTP POST to publish action.");
        }
    }

    public static class OpenGraphUriWithRef extends TypeSafeDiagnosingMatcher<HttpUriRequest> {

        private String expectedRef;

        public OpenGraphUriWithRef(String expectedRef) {
            this.expectedRef = expectedRef;
        }

        @Override
        protected boolean matchesSafely(HttpUriRequest item, Description mismatchDescription) {
            if (!(item instanceof HttpPost)) {
                return false;
            }
            String content = readContent((HttpPost) item);
            java.util.regex.Matcher ref = Pattern.compile("[^?].*?.*ref=([^&]*).*").matcher(content);
            return (ref.matches() && ref.group(1).equals(expectedRef));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("HTTP POST with ref '" + expectedRef + "'.");
        }
    }

    private static String readContent(HttpPost item) {
        UrlEncodedFormEntity formEntity = (UrlEncodedFormEntity) item.getEntity();
        try {
            return IOUtils.toString(formEntity.getContent());

        } catch (IOException e) {
            throw new RuntimeException("Unable to read content.", e);
        }
    }


}
