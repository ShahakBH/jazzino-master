package com.yazino.engagement.facebook;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookOAuthException;
import com.yazino.engagement.campaign.AccessTokenException;
import com.yazino.engagement.campaign.AppRequestExternalReference;
import com.yazino.engagement.FacebookMessageType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import strata.server.lobby.api.facebook.FacebookAppToUserRequestType;
import strata.server.lobby.api.facebook.FacebookDataContainer;
import strata.server.lobby.api.facebook.FacebookDataContainerBuilder;

import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FacebookRequestSenderTest {

    public static final String ACCESS_TOKEN = "261829473833589|VhI1uM2579Cg6_nTMyvEvlugMB0";
    public static final String REQUEST_MESSAGE = "message";
    public static final String FACEBOOK_USER_ID = "534552376";
    public static final String APP_USER_REQUEST = FACEBOOK_USER_ID + "/apprequests";
    public static final String APP_USER_NOTIFICATION = FACEBOOK_USER_ID + "/notifications";
    public static final String GAME_TYPE = "SLOTS";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";

    public static final String TRACKING_DATA = "some tracking data";
    public static final String ENGAGEMENT_JSON_DATA = "{\"actions\":null,\"type\":\"Engagement\",\"tracking\":{\"ref\":\"" + TRACKING_DATA + "\"}}";
    public static final String EXTERNAL_REFERENCE = "415341436";

    @Mock
    private FacebookAccessTokenService facebookAccessTokenService;

    @Mock
    private FacebookClientFactory fbClientFactory;

    @Mock
    FacebookClient fbClient;

    private FacebookRequestSender underTest;

    @Before
    public void setup() throws AccessTokenException {
        MockitoAnnotations.initMocks(this);

        underTest = new FacebookRequestSender(facebookAccessTokenService, fbClientFactory);

        when(facebookAccessTokenService.fetchApplicationAccessToken(GAME_TYPE)).thenReturn(ACCESS_TOKEN);
        when(fbClientFactory.getClient(ACCESS_TOKEN)).thenReturn(fbClient);
    }

    @Test
    public void sendRequestShouldFetchAccessToken() throws IOException, AccessTokenException {
        FacebookAppRequestEnvelope facebookAppRequestEnvelope = new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, FACEBOOK_USER_ID,
                GAME_TYPE, REQUEST_MESSAGE, ENGAGEMENT_JSON_DATA, null);
        underTest.sendRequest(FacebookMessageType.APP_TO_USER_REQUEST, facebookAppRequestEnvelope);
        verify(facebookAccessTokenService).fetchApplicationAccessToken(GAME_TYPE);
    }

    @Test
    public void sendRequestShouldPostRequestToFacebook_appToUserRequest() throws IOException, AccessTokenException {
        FacebookAppRequestEnvelope facebookAppRequestEnvelope = new FacebookAppRequestEnvelope(
                TITLE, DESCRIPTION, FACEBOOK_USER_ID, GAME_TYPE, REQUEST_MESSAGE, TRACKING_DATA, null);

        underTest.sendRequest(FacebookMessageType.APP_TO_USER_REQUEST, facebookAppRequestEnvelope);

        verify(fbClient).publish(APP_USER_REQUEST, FacebookRequestSender.AppRequestResponse.class,
                Parameter.with("message", REQUEST_MESSAGE),
                Parameter.with("data", trackingDataAsJson()));
    }

    @Test
    public void sendRequestShouldPostRequestToFacebook_appToUserNotification() throws IOException, AccessTokenException {
        FacebookAppRequestEnvelope facebookAppRequestEnvelope = new FacebookAppRequestEnvelope(
                TITLE, DESCRIPTION, FACEBOOK_USER_ID, GAME_TYPE, REQUEST_MESSAGE, TRACKING_DATA, null);

        underTest.sendRequest(FacebookMessageType.APP_TO_USER_NOTIFICATION, facebookAppRequestEnvelope);

        verify(fbClient).publish(APP_USER_NOTIFICATION, FacebookRequestSender.AppNotificationResponse.class,
                Parameter.with("template", REQUEST_MESSAGE),
                Parameter.with("href", TRACKING_DATA));
    }

    private String trackingDataAsJson() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final FacebookDataContainer facebookDataContainer =
                new FacebookDataContainerBuilder()
                        .withType(FacebookAppToUserRequestType.Engagement)
                        .withTrackingRef(TRACKING_DATA).build();

        return mapper.writeValueAsString(facebookDataContainer);
    }

    @Test
    public void deleteRequestShouldFetchAccessToken() throws IOException, AccessTokenException {
        AppRequestExternalReference toDelete = new AppRequestExternalReference(FACEBOOK_USER_ID, GAME_TYPE,
                EXTERNAL_REFERENCE);

        underTest.deleteRequest(toDelete);

        verify(facebookAccessTokenService).fetchApplicationAccessToken(GAME_TYPE);
    }

    @Test
    public void deleteRequestShouldPostDeleteRequestToFacebook() throws IOException, AccessTokenException {
        AppRequestExternalReference toDelete = new AppRequestExternalReference(FACEBOOK_USER_ID, GAME_TYPE,
                EXTERNAL_REFERENCE);

        underTest.deleteRequest(toDelete);
        verify(fbClient).deleteObject(EXTERNAL_REFERENCE + "_" + FACEBOOK_USER_ID);
    }

    @Test
    public void deleteRequestShouldNotPropagateExceptions() throws IOException, AccessTokenException {
        when(fbClient.deleteObject(EXTERNAL_REFERENCE + "_" + FACEBOOK_USER_ID))
                .thenThrow(new FacebookOAuthException("anError", "aMessage", 123, 404));
        AppRequestExternalReference toDelete = new AppRequestExternalReference(FACEBOOK_USER_ID, GAME_TYPE,
                EXTERNAL_REFERENCE);

        underTest.deleteRequest(toDelete);

        verify(fbClient).deleteObject(EXTERNAL_REFERENCE + "_" + FACEBOOK_USER_ID);
    }
}
