package com.yazino.engagement.facebook;

import com.restfb.WebRequestor;
import com.yazino.engagement.campaign.AccessTokenException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import java.io.IOException;
import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class FacebookAccessTokenServiceTest {

    public static final String CLIENT_ID = "261829473833589";
    public static final String CLIENT_SECRET = "04df1e150612db3ca138b90fc52426a2";
    public static final String UNKNOWN_GAME_TYPE = "unknownGameType";
    public static final String BLACKJACK = "Blackjack";
    public static final String BLACKJACK_ACCESS_TOKEN = "Blackjack_Access_Token";
    public static final String SLOTS_ACCESS_TOKEN = "Slots_Access_Token";
    public static final String BLACKJACK_SECRET = "blackjack_secret";
    public static final String BLACKJACK_CLIENT_ID = "black jack app id";
    public static final String FACEBOOK_ACCESS_TOKEN_URL = "https://graph.facebook.com/oauth/access_token?client_id=%s&client_secret="
                               + "%s&grant_type=client_credentials";
    private FacebookAccessTokenService underTest;

    @Mock
    private WebRequestor defaultWebRequestor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final ArrayList<FacebookAppConfiguration> applicationConfigs = new ArrayList<FacebookAppConfiguration>();
        FacebookAppConfiguration facebookAppConfiguration = new FacebookAppConfiguration();
        facebookAppConfiguration.setSecretKey(BLACKJACK_SECRET);
        facebookAppConfiguration.setApplicationId(BLACKJACK_CLIENT_ID);
        facebookAppConfiguration.setGameType(BLACKJACK);
        applicationConfigs.add(facebookAppConfiguration);

        FacebookConfiguration facebookConfiguration = new FacebookConfiguration();
        facebookConfiguration.setApplicationConfigs(applicationConfigs);

        underTest = new FacebookAccessTokenService(defaultWebRequestor, facebookConfiguration);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("NullableProblems")
    public void fetchApplicationAccessTokenWithNullClientIdShouldThrowException() throws AccessTokenException {
        underTest.fetchApplicationAccessToken(null, CLIENT_SECRET);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("NullableProblems")
    public void fetchApplicationAccessTokenWithNullClientSecretShouldThrowException() throws AccessTokenException {
        underTest.fetchApplicationAccessToken(CLIENT_ID, null);
    }

    @Test
    public void fetchApplicationAccessTokenWithClientCredentialsShouldReturnValidAuthToken() throws AccessTokenException, IOException {
        WebRequestor.Response mockResponse = new WebRequestor.Response(200, "access_token=" + SLOTS_ACCESS_TOKEN);

        when(defaultWebRequestor.executeGet(
                String.format(FACEBOOK_ACCESS_TOKEN_URL, CLIENT_ID, CLIENT_SECRET))).thenReturn(mockResponse);

        assertThat(underTest.fetchApplicationAccessToken(CLIENT_ID, CLIENT_SECRET), is(SLOTS_ACCESS_TOKEN));
    }

    @Test(expected = AccessTokenException.class)
    public void fetchApplicationAccessTokenWithClientCredentialsThrowsAccessExceptionOnIOException() throws AccessTokenException, IOException {
        doThrow(new IOException()).when(defaultWebRequestor).executeGet(any(String.class));

        underTest.fetchApplicationAccessToken(CLIENT_ID, CLIENT_SECRET);
    }

    @Test(expected = AccessTokenException.class)
    public void fetchApplicationAccessTokenWithClientCredentialsDealsWith400Error() throws IOException, AccessTokenException {
        WebRequestor.Response mockResponse = new WebRequestor.Response(400, "{\n" +
                "   \"error\": {\n" +
                "      \"message\": \"Error validating application. Invalid application ID.\",\n" +
                "      \"type\": \"OAuthException\",\n" +
                "      \"code\": 101\n" +
                "   }\n" +
                "}");

        when(defaultWebRequestor.executeGet(
                String.format(FACEBOOK_ACCESS_TOKEN_URL, "Bad Client Id", CLIENT_SECRET))).thenReturn(mockResponse);

        underTest.fetchApplicationAccessToken("Bad Client Id", CLIENT_SECRET);
    }

    @Test(expected = AccessTokenException.class)
    public void fetchApplicationAccessTokenForUnknownGameTypeShouldThrowException() throws AccessTokenException {
        underTest.fetchApplicationAccessToken(UNKNOWN_GAME_TYPE);
    }

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("NullableProblems")
    public void fetchApplicationAccessTokenWithNullGameTypeShouldThrowException() throws AccessTokenException {
        underTest.fetchApplicationAccessToken(null);
    }

    @Test
    public void fetchingAccessTokenForGameTypeShouldSendRequestToFacebook() throws AccessTokenException, IOException {
        WebRequestor.Response mockResponse = new WebRequestor.Response(200, "access_token=" + BLACKJACK_ACCESS_TOKEN);

        when(defaultWebRequestor.executeGet(
                String.format(FACEBOOK_ACCESS_TOKEN_URL, BLACKJACK_CLIENT_ID, BLACKJACK_SECRET))).thenReturn(mockResponse);


        assertThat(underTest.fetchApplicationAccessToken(BLACKJACK), is(BLACKJACK_ACCESS_TOKEN));

    }
}
