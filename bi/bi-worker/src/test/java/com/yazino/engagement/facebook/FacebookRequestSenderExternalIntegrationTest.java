package com.yazino.engagement.facebook;

import com.restfb.*;
import com.yazino.engagement.FacebookMessageType;
import com.yazino.engagement.campaign.AccessTokenException;
import com.yazino.engagement.campaign.AppRequestExternalReference;
import com.yazino.engagement.campaign.dao.EngagementCampaignDao;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CANVAS;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.LOOSE;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
public class FacebookRequestSenderExternalIntegrationTest {

    public static final String GAME_TYPE = "SLOTS";
    public static final int MAX_MESSAGE_LENGTH_ALLOWED_BY_FACEBOOK = 180;
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";

    @Autowired
    private FacebookRequestSender facebookRequestSender;
    @Autowired
    private FacebookClientFactory clientFactory;
    @Autowired
    private FacebookAccessTokenService accessTokenService;
    @Autowired
    private FacebookConfiguration facebookConfiguration;

    @Autowired
    @Qualifier("fbDeleteRequestQueuePublishingService")
    private QueuePublishingService<FacebookDeleteAppRequestMessage> queuePublishingService;

    @Mock
    private EngagementCampaignDao campaignDao;

    private FacebookAppConfiguration appConfiguration;

    private static FbTestUser userWithAppInstalled;
    private static FbTestUser userWithAppNotInstalled;
    private static FacebookClient client;
    private static List<FbTestUser> fbTestUsers;

    @Before
    public void setup() throws AccessTokenException, IOException {
        MockitoAnnotations.initMocks(this);

        // there is an issue with deleting test users, as yet unresolved. Seems to be on the facebook side. In an attempt
        // to get this test passing (deleting the user was an issue), the test attempts to use existing test accounts if
        // possible
        if (fbTestUsers == null) {
            final String slotsAppAccessToken = accessTokenService.fetchApplicationAccessToken(GAME_TYPE);
            client = clientFactory.getClient(slotsAppAccessToken);

            appConfiguration = facebookConfiguration.getAppConfigFor(
                    GAME_TYPE, CANVAS, LOOSE);
            final WebRequestor webRequestor = client.getWebRequestor();
            final WebRequestor.Response response = webRequestor.executeGet(
                    "https://graph.facebook.com/"
                            + appConfiguration.getApplicationId()
                            + "/accounts/test-users?access_token="
                            + slotsAppAccessToken
                            + "&limit=500");
            if (response.getStatusCode() == 200) {
                fbTestUsers = new DefaultJsonMapper().toJavaList(response.getBody(),
                        FbTestUser.class);
                userWithAppNotInstalled = findTestUser(fbTestUsers, false);
                userWithAppInstalled = findTestUser(fbTestUsers, true);
            }
        }
    }

    @AfterClass
    public static void deleteTestUsers() {
        // delete the users if we created them
        deleteTestUser(userWithAppInstalled);
        deleteTestUser(userWithAppNotInstalled);
    }

    private static void deleteTestUser(FbTestUser testUser) {
        if (testUser.shouldDeleteAfterTest()) {
            try {
                // facebook currently throwing a wobbly when deleting the user, no one knows why
                // as deleting users is by product of teh tests, just fail silently, well ish
                client.deleteObject(testUser.getId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // find the first user in list
    private FbTestUser findTestUser(final List<FbTestUser> fbTestUsers, final boolean requireInstalled) {
        for (FbTestUser fbTestUser : fbTestUsers) {
            if (fbTestUser.getAccessToken() == null && !requireInstalled) {
                return fbTestUser;
            }
            if (fbTestUser.getAccessToken() != null && requireInstalled) {
                return fbTestUser;
            }
        }
        return null;
    }

    @Test
    public void facebookAppRequestRemovalShouldDeleteExpiredRequests() throws AccessTokenException {
        if (userWithAppInstalled == null) {
            userWithAppInstalled = createAFacebookUserWithAppInstalled(true, "userWhoHasNotAcceptedTos");
        }
        List<String> existingRequests = client.executeQuery(
                "SELECT request_id FROM apprequest WHERE recipient_uid =" + userWithAppInstalled.getId() + " AND app_id = " + appConfiguration
                        .getApplicationId(), String.class);


        //given app request exists and has been sent
        FacebookAppRequestEnvelope envelopeFacebook = new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, userWithAppInstalled.getId(), GAME_TYPE,
                "hello arthur", "track this", null);
        final FacebookResponse response = facebookRequestSender.sendRequest(FacebookMessageType.APP_TO_USER_REQUEST, envelopeFacebook);
        assertThat(response.getStatus(), is(FacebookAppToUserRequestStatus.SENT));

        // when removal service is run
        when(campaignDao.fetchCampaignsToExpire()).thenReturn(Arrays.asList(1));
        final AppRequestExternalReference appRequestExternalReference = new AppRequestExternalReference(
                userWithAppInstalled.getId(), GAME_TYPE, response.getRequestId());
        when(campaignDao.updateCampaignStatusToExpiring(1)).thenReturn(true);
        when(campaignDao.fetchAppRequestExternalReferences(1)).thenReturn(Arrays.asList(appRequestExternalReference));


        FacebookAppRequestRemovalService facebookAppRequestRemovalService = new FacebookAppRequestRemovalService(
                queuePublishingService, campaignDao);
        facebookAppRequestRemovalService.removeExpiredAppRequestsFromFacebook();

        int maxAttempts = 25;
        int count = 0;
        List<String> requests = null;
        while (count++ < maxAttempts) {
            try {
                Thread.sleep(150);
                requests = client.executeQuery(
                        "SELECT request_id FROM apprequest WHERE recipient_uid =" + userWithAppInstalled.getId() + " AND app_id = " + appConfiguration
                                .getApplicationId(), String.class);
                if (requests.size() == 0) {
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Assert.assertEquals(existingRequests.size(), requests.size());
        Assert.assertTrue(existingRequests.containsAll(requests));
    }

    @Test
    public void sendRequestShouldFailIfUserHasNotAcceptedTOS() throws AccessTokenException {
        if (userWithAppNotInstalled == null) {
            userWithAppNotInstalled = createAFacebookUserWithAppInstalled(false, "userWhoHasNotAcceptedTos");
        }
        FacebookAppRequestEnvelope envelopeFacebook = new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, userWithAppNotInstalled.getId(), GAME_TYPE,
                "hello arthur", "track this", null);
        final FacebookResponse response = facebookRequestSender.sendRequest(FacebookMessageType.APP_TO_USER_REQUEST, envelopeFacebook);
        assertThat(response.getStatus(), is(FacebookAppToUserRequestStatus.FAILED));
    }

    @Test
    public void sendRequestShouldFailUserIdIsInvalid() {
        FacebookAppRequestEnvelope envelopeFacebook = new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, "6327ty", GAME_TYPE,
                "hello arthur", "track this", null);
        final FacebookResponse response = facebookRequestSender.sendRequest(FacebookMessageType.APP_TO_USER_REQUEST, envelopeFacebook);
        assertThat(response.getStatus(), is(FacebookAppToUserRequestStatus.FAILED));
    }

    @Test
    public void sendNotificationShouldSendNotification() throws AccessTokenException {
        if (userWithAppInstalled == null) {
            userWithAppInstalled = createAFacebookUserWithAppInstalled(true, "userWhoHasInstalledApp");
        }
        FacebookAppRequestEnvelope envelopeFacebook = new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, userWithAppInstalled.getId(), GAME_TYPE,
                "sample notification message", "sample tracking ref", null);
        final FacebookResponse response = facebookRequestSender.sendRequest(FacebookMessageType.APP_TO_USER_NOTIFICATION, envelopeFacebook);
        assertThat(response.getStatus(), is(FacebookAppToUserRequestStatus.SENT));
    }

    @Test
    public void sendNotificationShouldFailIfUserHasNotInstalledApp() throws AccessTokenException {
        if (userWithAppNotInstalled == null) {
            userWithAppNotInstalled = createAFacebookUserWithAppInstalled(false, "userWhoHasNotAcceptedTos");
        }
        FacebookAppRequestEnvelope envelopeFacebook = new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, userWithAppNotInstalled.getId(), GAME_TYPE,
                "hello arthur", "track this", null);
        final FacebookResponse response = facebookRequestSender.sendRequest(FacebookMessageType.APP_TO_USER_NOTIFICATION, envelopeFacebook);
        assertThat(response.getStatus(), is(FacebookAppToUserRequestStatus.FAILED));
    }

    @Test
    public void sendNotificationShouldThrowIllegalArgumentExceptionIfMessageLongerThanAllowedByFacebook() throws AccessTokenException {
        if (userWithAppInstalled == null) {
            userWithAppInstalled = createAFacebookUserWithAppInstalled(true, "userWhoHasInstalledApp");
        }

        String overlyLongMessage = StringUtils.repeat('a', MAX_MESSAGE_LENGTH_ALLOWED_BY_FACEBOOK + 1);
        FacebookAppRequestEnvelope envelopeFacebook = new FacebookAppRequestEnvelope(TITLE, DESCRIPTION, userWithAppInstalled.getId(), GAME_TYPE,
                overlyLongMessage, "sample tracking ref", null);
        final FacebookResponse response = facebookRequestSender.sendRequest(FacebookMessageType.APP_TO_USER_NOTIFICATION, envelopeFacebook);
        assertThat(response.getStatus(), is(FacebookAppToUserRequestStatus.FAILED));
    }


    private FbTestUser createAFacebookUserWithAppInstalled(Boolean installed,
                                                           String name) throws AccessTokenException {
        final FbTestUser user = client.publish(
                appConfiguration.getApplicationId() + "/accounts/test-users?", FbTestUser.class,
                Parameter.with("installed", installed),
                Parameter.with("name", name));
        user.setDeleteAfterTest(true);
        return user;
    }

    private static class FbTestUser {
        @Facebook
        private String id;
        @Facebook("access_token")
        private String accessToken;
        @Facebook("login_url")
        private String loginUrl;

        // if we created this user, then delete it
        private boolean deleteAfterTest = false;

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(final String accessToken) {
            this.accessToken = accessToken;
        }

        public String getLoginUrl() {
            return loginUrl;
        }

        public void setLoginUrl(final String loginUrl) {
            this.loginUrl = loginUrl;
        }

        public boolean shouldDeleteAfterTest() {
            return deleteAfterTest;
        }

        public void setDeleteAfterTest(boolean deleteAfterTest) {
            this.deleteAfterTest = deleteAfterTest;
        }
    }

}
