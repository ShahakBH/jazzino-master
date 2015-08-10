package com.yazino.web.service;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FacebookServiceImplHttpClientTest {
    private final String expectedAccessToken = "AAACbTRiH3sQBACIvtSrPo32hdvpN3M4WZBt9wY2xglARK8OzOn6hqPsu3wmB4VDDvq1HaKuMAnbvkeqKsB4WDqqgqukiuKpIZCVGnoquj5cQkZCCZCba";
    private final String code = "1234";
    private final String appSecret = "appSecret";
    private final String appId = "appId";
    private final String redirectUri = "http://my.redirect.uri";

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private HttpEntity httpEntity;

    @Mock
    private StatusLine statusLine;

    private FacebookServiceImplHttpClient underTest;

    @Before
    public void setUp() {
        underTest = new FacebookServiceImplHttpClient(httpClient);
    }


    @Test
    public void shouldExtractCorrectAccessTokenFromResponse() throws Exception {

        setUpHttpClientWithEntity();
        when(httpEntity.getContent()).thenReturn((new ClassPathResource("/com/yazino/web/service/accessTokenOk.txt")).getInputStream());

        final String actualAccessToken = underTest.getAccessTokenForGivenCode(code, appId, appSecret, redirectUri);
        assertEquals(expectedAccessToken, actualAccessToken);
    }

    @Test(expected = RuntimeException.class)
    public void shouldReturnNullIfError() throws Exception {

        setUpHttpClientWithEntity();
        when(httpEntity.getContent()).thenThrow(new IOException());

        underTest.getAccessTokenForGivenCode(code, appId, appSecret, redirectUri);
    }

    @Test
    public void shouldAbortOnError() throws Exception {
        setUpHttpClientWithEntity();
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream(("{\n"
                + "   \"error\": {\n"
                + "      \"type\": \"OAuthException\",\n"
                + "      \"message\": \"Error validating verification code.\"\n"
                + "   }\n"
                + "}").getBytes("UTF-8")));
        setUpHttpClientWithEntity(400);
        final HttpGet httpGet = spy(new HttpGet());

        try {
            underTest.getAccessTokenForHttpGet(httpGet);
            fail("Expected exception not thrown");

        } catch (FacebookOAuthException ex) {
            assertThat(ex.getFacebookType(), is(equalTo("OAuthException")));
            assertThat(ex.getFacebookMessage(), is(equalTo("Error validating verification code.")));
        }
    }

    @Test
    public void anErrorFromFacebookShouldBeParsedAndIncludedInTheException() throws Exception {
        setUpHttpClientWithEntity();
        when(httpEntity.getContent()).thenThrow(new IOException());
        setUpHttpClientWithEntity(400);
        HttpGet httpGet = spy(new HttpGet());
        try {
            underTest.getAccessTokenForHttpGet(httpGet);
            fail("Expected exception not thrown");
        } catch (Exception ex) {
            verify(httpGet).abort();
        }
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfCanNotParse() throws Exception {

        setUpHttpClientWithEntity();
        when(httpEntity.getContent()).thenReturn((new ClassPathResource("/com/yazino/web/service/oAuthError.json")).getInputStream());

        underTest.getAccessTokenForGivenCode(code, appId, appSecret, redirectUri);
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionIfStatusCodeNot200() throws Exception {
        setUpHttpClientWithEntity(400);
        underTest.getAccessTokenForGivenCode(code, appId, appSecret, redirectUri);
    }

    @Test
    public void shouldCallAbortIfThingsDidNotGoOK() throws Exception {
        setUpHttpClientWithEntity(400);
        HttpGet httpGet = spy(new HttpGet());
        try {
            underTest.getAccessTokenForHttpGet(httpGet);
        } catch (Exception ex) {
            verify(httpGet).abort();
        }
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerArgumentExceptionForNullCode() {
        underTest.getAccessTokenForGivenCode(null, appId, appSecret, redirectUri);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionForNullAppId() {
        underTest.getAccessTokenForGivenCode(code, null, appSecret, redirectUri);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionForNullAppSecret() {
        underTest.getAccessTokenForGivenCode(code, appId, null, redirectUri);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerExceptionForNullRedirectUri() {
        underTest.getAccessTokenForGivenCode(code, appId, appSecret, null);
    }

    @Test
    public void shouldDoAGetWithCorrectParams() throws Exception {
        final StringBuilder url = new StringBuilder("https://graph.facebook.com/oauth/access_token?");
        url.append("client_id=").append(appId);
        url.append("&client_secret=").append(appSecret);
        url.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, "UTF-8"));
        url.append("&code=").append(code);

        setUpHttpClientWithEntity(200);
        when(httpEntity.getContent()).thenReturn(
                (new ClassPathResource("/com/yazino/web/service/accessTokenOk.txt")).getInputStream());

        underTest.getAccessTokenForGivenCode(code, appId, appSecret, redirectUri);

        ArgumentCaptor<HttpGet> httpGetArgumentCaptor = ArgumentCaptor.forClass(HttpGet.class);
        verify(httpClient).execute(httpGetArgumentCaptor.capture());
        final HttpGet httpGet = httpGetArgumentCaptor.getValue();
        assertEquals(url.toString(), httpGet.getURI().toASCIIString());
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithANullHttpClient() throws Exception {
        new FacebookServiceImplHttpClient(null);
    }

    private void setUpHttpClientWithEntity(final int statusCode) throws IOException {
        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(statusCode);
    }

    private void setUpHttpClientWithEntity() throws IOException {
        setUpHttpClientWithEntity(200);
    }
}
