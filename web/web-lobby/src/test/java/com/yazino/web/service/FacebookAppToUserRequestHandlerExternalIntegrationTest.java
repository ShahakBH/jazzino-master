package com.yazino.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restfb.*;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchResponse;
import com.restfb.types.FacebookType;
import com.yazino.web.domain.facebook.FacebookClientFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import strata.server.lobby.api.facebook.*;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CANVAS;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.LOOSE;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FacebookAppToUserRequestHandlerExternalIntegrationTest {

    public static final String GAME_TYPE = "SLOTS";
    @Autowired
    private FacebookClientFactory clientFactory;

    @Autowired
    private FacebookConfiguration fbConfiguration;

    private FacebookClient client;
    private FacebookAppConfiguration appConfigForGameType;
    private WebRequestor webRequestor = new DefaultWebRequestor();

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String ACCESS_TOKEN_REQUEST_URI_FORMAT = "https://graph.facebook.com/oauth/access_token"
            + "?client_id=%s&client_secret=%s&grant_type=client_credentials";
    private FbTestUser testUser;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        appConfigForGameType = fbConfiguration.getAppConfigFor(GAME_TYPE, CANVAS, LOOSE);
        final WebRequestor.Response authTokenResponse = requestAccessToken(appConfigForGameType.getApplicationId(),
                appConfigForGameType.getSecretKey());
        final String accessToken = authTokenResponse.getBody().split("=")[1];
        client = clientFactory.getClient(accessToken);
        client = clientFactory.getClient(accessToken);

        final WebRequestor webRequestor = client.getWebRequestor();
        final WebRequestor.Response response = webRequestor.executeGet(
                "https://graph.facebook.com/"
                        + appConfigForGameType.getApplicationId()
                        + "/accounts/test-users?access_token="
                        + accessToken
                        + "&limit=500");
        if (response.getStatusCode() == 200) {
            final String fbUserJson = response.getBody();
            if (StringUtils.isNotBlank(fbUserJson)) {
                final List<FbTestUser> fbTestUsers = new DefaultJsonMapper().toJavaList(fbUserJson,
                        FbTestUser.class);
                for (FbTestUser fbTestUser : fbTestUsers) {
                    if (fbTestUser.getAccessToken() != null) {
                        testUser = fbTestUser;
                    }
                }
            }
        }
        if (testUser == null) {
            testUser = createAFacebookUserWithAppInstalled(true, "MrWendall");
        }
    }

    @After
    public void deleteTestUser() {
        // facebook currently (20/09/2012) throws an OAuth exception when deleting test users. The test user has in fact been deleted which you see by
        // query facebook for users linked to the app e.g.
        // https://graph.facebook.com/99070479631/accounts?type=test-users&access_token=99070479631|ImvUA6vNGzS3LGCSmaYUrduQ5gE&limit=500
        // where 99070479631 is the app id. After deleting the test guy, he's no longer present in the response list of test users.
        // they will eventually fix this.
        // for now just swallow the exception.
        try {
            if (testUser.shouldDeleteTestUser()) {
                client.deleteObject(testUser.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void shouldMapAppRequestToFacebookEngagementAppRequest() throws IOException {
        AppRequestResponse response = postAppRequestToUserAtFacebook(testUser);


        final BatchRequest batchRequest = new BatchRequest.BatchRequestBuilder(response.getRequest()).build();
        final List<BatchResponse> batchResponseList = client.executeBatch(batchRequest);

        final BatchResponse batchResponse = batchResponseList.get(0);
        assertEquals(new Integer(200), batchResponse.getCode());

        Pattern engagementPattern = Pattern.compile(FacebookAppToUserRequestHandler.ENGAGEMENT_PATTERN);
        final Matcher engagementMatcher = engagementPattern.matcher(batchResponse.getBody());

        assertTrue(engagementMatcher.find());
    }

    private AppRequestResponse postAppRequestToUserAtFacebook(final FbTestUser testUser) throws IOException {
        final FacebookDataContainer facebookDataContainer =
                new FacebookDataContainerBuilder()
                        .withType(FacebookAppToUserRequestType.Engagement)
                        .withTrackingRef("Arrested Development").build();


        // given an apprequest has been published
        return client.publish(testUser.getId() + "/apprequests",
                AppRequestResponse.class,
                Parameter.with("message", "Here, have a dollar"),
                Parameter.with("data", mapper.writeValueAsString(facebookDataContainer)));
    }


    private FbTestUser createAFacebookUserWithAppInstalled(Boolean installed, String name) {
        final FbTestUser testUser = client.publish(appConfigForGameType.getApplicationId() + "/accounts/test-users?",
                FbTestUser.class,
                Parameter.with("installed", installed),
                Parameter.with("name", name));
        testUser.setDeleteTestUser(true);
        return testUser;
    }

    private static class FbTestUser {
        @Facebook
        private String id;
        @Facebook("access_token")
        private String accessToken;
        @Facebook("login_url")
        private String loginUrl;

        private boolean deleteTestUser = false;

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

        public boolean shouldDeleteTestUser() {
            return deleteTestUser;
        }

        public void setDeleteTestUser(boolean deleteTestUser) {
            this.deleteTestUser = deleteTestUser;
        }
    }

    public static class AppRequestResponse extends FacebookType {
        private static final long serialVersionUID = 1L;

        @Facebook
        private String request;

        @Facebook
        private String to;

        public String getRequest() {
            return request;
        }

        public void setRequest(final String request) {
            this.request = request;
        }

        public String getTo() {
            return to;
        }

        public void setTo(final String to) {
            this.to = to;
        }
    }

    private WebRequestor.Response requestAccessToken(final String clientId,
                                                     final String clientSecret) {
        try {
            return webRequestor.executeGet(String.format(ACCESS_TOKEN_REQUEST_URI_FORMAT, clientId, clientSecret));
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return null;
    }

}
