package com.yazino.web.parature.service;

import com.yazino.platform.player.PlayerProfile;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ParatureSupportUserServiceTest {

    @Mock
    private HttpClient httpClient;

    private ParatureSupportUserService underTest;

    private String paratureUrl = "http://some/url";

    private PlayerProfile userProfile = new PlayerProfile();

    @Before
    public void setUp() throws IOException {
        userProfile.setEmailAddress("bill@bill.com");
        userProfile.setFirstName("First");
        userProfile.setLastName("Last");
        userProfile.setDisplayName("Display");

        underTest = new ParatureSupportUserService(httpClient, paratureUrl, "token");
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithoutAHttpClient() {
        new ParatureSupportUserService(null, paratureUrl, "token");
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithoutAParatureUrl() {
        new ParatureSupportUserService(httpClient, null, "token");
    }

    @Test(expected = NullPointerException.class)
    public void theServiceCannotBeCreatedWithoutAParatureToken() {
        new ParatureSupportUserService(httpClient, paratureUrl, null);
    }

    @Test
    public void createUserShouldReturnSuccess() throws IOException, SupportUserServiceException {
        PlayerProfile userProfile = new PlayerProfile();
        userProfile.setEmailAddress("bill@bill.com");
        userProfile.setFirstName("First");
        userProfile.setLastName("Last");
        userProfile.setDisplayName("Display");

        when(httpClient.execute(any(HttpPost.class))).thenReturn(aSuccessfulResponse());

        underTest.createSupportUser(BigDecimal.ONE, userProfile);

        verifyExpectedHttpPost();
    }

    @Test(expected = SupportUserServiceException.class)
    public void createUserShouldThrowExceptionWhenFailureResponseFromParature() throws IOException, SupportUserServiceException {
        when(httpClient.execute(any(HttpPost.class))).thenReturn(
                aResponseWithStatus(500, "ï»¿<?xml version=\"1.0\" encoding=\"utf-8\"?><Error code=\"400\" description=\"Bad Request\" message=\"Any other message\" />"));

        underTest.createSupportUser(BigDecimal.ONE, userProfile);
    }

    @Test
    public void createUserShouldNotThrowAnErrorWhenErrorResponseIsUserExists() throws IOException, SupportUserServiceException {
        PlayerProfile userProfile = new PlayerProfile();
        userProfile.setEmailAddress("bill@bill.com");
        userProfile.setFirstName("First");
        userProfile.setLastName("Last");
        userProfile.setDisplayName("Display");

        when(httpClient.execute(any(HttpPost.class))).thenReturn(aResponseWithStatus(500,
                "ï»¿<?xml version=\"1.0\" encoding=\"utf-8\"?><Error code=\"400\" description=\"Bad Request\" "
                        + "message=\"One or more fields are invalid; Invalid Field Validation Message : An existing Customer record has been found\" />"));

        underTest.createSupportUser(BigDecimal.ONE, userProfile);

        verifyExpectedHttpPost();
    }

    @Test(expected = SupportUserServiceException.class)
    public void createUserShouldThrowExceptionWhenResponseIsInvalidXML() throws IOException, SupportUserServiceException {
        when(httpClient.execute(any(HttpPost.class))).thenReturn(
                aResponseWithStatus(500, "ï»¿<bad><x><m><l>"));

        underTest.createSupportUser(BigDecimal.ONE, userProfile);
    }

    @Test
    public void createUserShouldWorkCorrectlyWhenResponseDoesNotTheUTFOrderHeader() throws IOException, SupportUserServiceException {
        when(httpClient.execute(any(HttpPost.class))).thenReturn(aResponseWithStatus(500, "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<Error code=\"400\" description=\"Bad Request\" "
                + "message=\"One or more fields are invalid; Invalid Field Validation Message : An existing Customer record has been found\" />"));

        underTest.createSupportUser(BigDecimal.ONE, userProfile);
    }

    @Test(expected = SupportUserServiceException.class)
    public void createUserShouldThrowExceptionWhenResponseIsNull() throws IOException, SupportUserServiceException {
        when(httpClient.execute(any(HttpPost.class))).thenReturn(
                aResponseWithStatus(500, null));

        underTest.createSupportUser(BigDecimal.ONE, userProfile);
    }

    private String stringContentOf(final HttpPost post) throws IOException {
        if (post.getEntity() != null) {
            return EntityUtils.toString(post.getEntity());
        }
        return null;
    }

    private void verifyExpectedHttpPost() throws IOException {
        final String expectedXML = "<Customer> <Email>bill@bill.com</Email>" +
                "<First_Name>First</First_Name> " +
                "<Last_Name>Last</Last_Name> \n" +
                "<User_Name>1</User_Name>\n" +
                "<Password> test1234 </Password>\n" +
                "<Password_Confirm> test1234 </Password_Confirm>\n" +
                "<Custom_Field id=\"49\">Display</Custom_Field>\n" +
                "<Sla><Sla id=\"2\"/></Sla>\n" +
                "<Status><Status id=\"2\" /></Status>\n" +
                "</Customer>";

        // HTTP Client is short of equals() methods, so we need to arse about
        final ArgumentCaptor<HttpPost> postCaptor = ArgumentCaptor.forClass(HttpPost.class);
        verify(httpClient).execute(postCaptor.capture());
        assertThat(postCaptor.getValue().getURI(), is(equalTo(expectedDestinationURI())));
        assertThat(stringContentOf(postCaptor.getValue()), is(equalTo(expectedXML)));
    }

    private BasicHttpResponse aSuccessfulResponse() throws UnsupportedEncodingException {
        final BasicHttpResponse successResponse = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("1.1", 1, 1), 201, "OK"));
        successResponse.setEntity(new StringEntity(""));
        return successResponse;
    }

    private BasicHttpResponse aResponseWithStatus(final int statusCode,
                                                  final String body) throws UnsupportedEncodingException {
        final BasicHttpResponse successResponse = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("1.1", 1, 1), statusCode, "OK"));
        if (body != null) {
            successResponse.setEntity(new StringEntity(body));
        }
        return successResponse;
    }

    private URI expectedDestinationURI() {
        try {
            return new URI(paratureUrl + "?_token_=token");
        } catch (URISyntaxException e) {
            fail("URI syntax is invalid");
            return null;
        }
    }
}
