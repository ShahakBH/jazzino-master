package com.yazino.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.restfb.FacebookClient;
import com.restfb.batch.BatchRequest;
import com.restfb.batch.BatchResponse;
import com.restfb.json.JsonObject;
import com.yazino.web.domain.facebook.FacebookAppToUserRequest;
import com.yazino.web.domain.facebook.FacebookClientFactory;
import com.yazino.web.domain.facebook.LegacyEngagementData;
import com.yazino.web.domain.facebook.LegacyFacebookAppRequestBuilder;
import com.yazino.web.util.FacebookAppRequestBuilder;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import strata.server.lobby.api.facebook.FacebookAppToUserRequestType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;


public class FacebookAppToUserRequestHandlerTest {

    private static final Logger LOG = LoggerFactory.getLogger(FacebookAppToUserRequestHandlerTest.class);

    private static final String REQUEST_ID1 = "399431233447704_100001186555918";
    private static final String TRACKING_DATA_1 = "Tracking Data 1";

    private static final String REQUEST_ID2 = "399431233447704_100001186555919";
    private static final String TRACKING_DATA_2 = "Tracking Data 2";

    private static final DateTime CREATED_TIME = new DateTime(2012, 7, 3, 15, 32, 15, 0, DateTimeZone.UTC);
    private static final String MESSAGE = "please work";
    private static final String LEAGACY_APP_REQUEST_ID = "758758_101";
    private static final String APP_REQUEST_TOKEN = "EENIEMEENIEMINIEMO";
    private static final String REQUEST_ID_STRING = "100003340303103";

    private final String ENGAGEMENT_REQUEST = "{\n" +
            "      \"id\": \"%s\", \n" +
            "      \"application\": {\n" +
            "        \"name\": \"Darren and Ralf Post Spam\", \n" +
            "        \"namespace\": \"facebooksuxballs\", \n" +
            "        \"id\": \"158626924248221\"\n" +
            "      }, \n" +
            "      \"to\": {\n" +
            "        \"name\": \"Don Dario\", \n" +
            "        \"id\": \"100003340303103\"\n" +
            "      }, \n" +
            "      \"data\": \"{\\\"actions\\\":null,\\\"type\\\":\\\"Engagement\\\",\\\"tracking\\\":{\\\"ref\\\":\\\"%s\\\"}}\", \n" +
            "      \"message\": \"please work\", \n" +
            "      \"created_time\": \"%s\"\n" +
            "    }";


    private final String LEGACY_APP_RESPONSE =
            "    {\n" +
                    "      \"id\": \"" + LEAGACY_APP_REQUEST_ID + "\", \n" +
                    "      \"application\": {\n" +
                    "        \"name\": \"Darren and Ralf Post Spam\", \n" +
                    "        \"namespace\": \"facebooksuxballs\", \n" +
                    "        \"id\": \"158626924248221\"\n" +
                    "      }, \n" +
                    "      \"to\": {\n" +
                    "        \"name\": \"Don Dario\", \n" +
                    "        \"id\": \"100003340303103\"\n" +
                    "      }, \n" +
                    "      \"data\": \"{\\\"Engagement\\\":{\\\"tracking\\\":\\\"RickyMartinIsATool\\\"}}\",\n" +
                    "      \"message\": \"" + MESSAGE + "\", \n" +
                    "      \"created_time\":\"" + CREATED_TIME + "\"\n" +
                    "    }\n" +
                    "  \"paging\": {\n" +
                    "    \"previous\": \"https://graph.facebook.com/100003340303103/apprequests?value=1&redirect=1&limit=50&since=1341481341\", \n" +
                    "    \"next\": \"https://graph.facebook.com/100003340303103/apprequests?value=1&redirect=1&limit=50&until=1341481341\"\n" +
                    "  }\n";

    private final String UNKNOWN_APP_RESPONSE =
            "    {\n" +
                    "      \"id\": \"" + LEAGACY_APP_REQUEST_ID + "\", \n" +
                    "      \"application\": {\n" +
                    "        \"name\": \"Darren and Ralf Post Spam\", \n" +
                    "        \"namespace\": \"facebooksuxballs\", \n" +
                    "        \"id\": \"158626924248221\"\n" +
                    "      }, \n" +
                    "      \"to\": {\n" +
                    "        \"name\": \"Don Dario\", \n" +
                    "        \"id\": \"100003340303103\"\n" +
                    "      }, \n" +
                    "      \"data\": \"false\",\n" +
                    "      \"message\": \"" + MESSAGE + "\", \n" +
                    "      \"created_time\":\"" + CREATED_TIME + "\"\n" +
                    "    }\n" +
                    "  \"paging\": {\n" +
                    "    \"previous\": \"https://graph.facebook.com/100003340303103/apprequests?value=1&redirect=1&limit=50&since=1341481341\", \n" +
                    "    \"next\": \"https://graph.facebook.com/100003340303103/apprequests?value=1&redirect=1&limit=50&until=1341481341\"\n" +
                    "  }\n";

    private final String REFER_FRIEND_APP_RESPONSE = "    {\n" +
            "      \"id\": \"%s\", \n" +
            "      \"application\": {\n" +
            "        \"name\": \"Darren and Ralf Post Spam\", \n" +
            "        \"namespace\": \"facebooksuxballs\", \n" +
            "        \"id\": \"158626924248221\"\n" +
            "      }, \n" +
            "      \"to\": {\n" +
            "        \"name\": \"Don Dario\", \n" +
            "        \"id\": \"100003340303103\"\n" +
            "      }, \n" +
            "      \"data\": \"234\",\n" +
            "      \"message\": \"" + MESSAGE + "\", \n" +
            "      \"created_time\":\"%s\"\n" +
            "    }\n" +
            "  \"paging\": {\n" +
            "    \"previous\": \"https://graph.facebook.com/100003340303103/apprequests?value=1&redirect=1&limit=50&since=1341481341\", \n" +
            "    \"next\": \"https://graph.facebook.com/100003340303103/apprequests?value=1&redirect=1&limit=50&until=1341481341\"\n" +
            "  }\n";

    private final String APP_REQUEST_RESPONSE_1_200 = String.format(ENGAGEMENT_REQUEST,
            REQUEST_ID1, TRACKING_DATA_1, CREATED_TIME);

    private final String APP_REQUEST_RESPONSE_2_200 = String.format(ENGAGEMENT_REQUEST,
            REQUEST_ID2,
            TRACKING_DATA_2,
            CREATED_TIME);

    @Mock
    private FacebookClient fbClient;

    @Mock
    private JsonObject appRequest;
    @Mock
    private FacebookAppToUserRequestProcessor fbEngagementAppRequestProcessor;
    @Mock
    private FacebookAppToUserRequestProcessor fbLegacyEngagementAppRequestService;
    @Mock
    private BatchResponse batchResponse;
    @Mock
    private FacebookClientFactory fbClientFactory;

    private FacebookAppToUserRequestHandler underTest;

    Map<FacebookAppToUserRequestType, FacebookAppToUserRequestProcessor> facebookAppToUserRequestProcessorMap = new HashMap<>();


    @Before
    public void setup() {
        initMocks(this);
        when(fbClientFactory.getClient(APP_REQUEST_TOKEN)).thenReturn(fbClient);
        facebookAppToUserRequestProcessorMap.put(FacebookAppToUserRequestType.Engagement, fbEngagementAppRequestProcessor);
        facebookAppToUserRequestProcessorMap.put(FacebookAppToUserRequestType.LegacyEngagement, fbLegacyEngagementAppRequestService);
        underTest = new FacebookAppToUserRequestHandler(fbClientFactory);
        underTest.setAppToUserRequestProcessorMap(facebookAppToUserRequestProcessorMap);
    }

    @Test
    public void testProcessAppRequestsShouldDoNothinOnNullRequestStringOrAccessToken() throws Exception {
        underTest.processAppRequests(null, "Access Token");
        underTest.processAppRequests("", "Access Token");
        underTest.processAppRequests("2112304923184", "");
        underTest.processAppRequests("2112304923184", null);
    }

    @Test
    public void testProcessAppRequestShouldSendAppRequestsToFacebookAppRequestService() throws IOException {
        final BatchRequest batchRequest = new BatchRequest.BatchRequestBuilder("100003340303103").build();

        List<BatchResponse> batchResponseList = new ArrayList<>();
        batchResponseList.add(new BatchResponse(200, null, APP_REQUEST_RESPONSE_1_200));

        when(fbClient.executeBatch(batchRequest)).thenReturn(batchResponseList);

        underTest.processAppRequests(REQUEST_ID_STRING, APP_REQUEST_TOKEN);

        FacebookAppToUserRequest request = new FacebookAppRequestBuilder()
                .withId(REQUEST_ID1)
                .withCreatedTime(CREATED_TIME)
                .withEngagementData(TRACKING_DATA_1, null)
                .withMessage(MESSAGE)
                .build();

        final ArgumentCaptor<List> capturedRequests = ArgumentCaptor.forClass(List.class);
        verify(fbEngagementAppRequestProcessor).processFacebookAppToUserRequest(capturedRequests.capture(), eq(fbClient));
        assertThat(capturedRequests.getValue().size(), is(equalTo(1)));
        Assert.assertThat((List<FacebookAppToUserRequest>) capturedRequests.getValue(), hasItem(anEqualRequestTo(request)));
    }

    @Test
    public void testProcessAppRequestShouldFilterListOfAppRequestsToFacebookAppRequestService() throws IOException {
        final BatchRequest batchRequest1 = new BatchRequest.BatchRequestBuilder("100003340303103").build();
        final BatchRequest batchRequest2 = new BatchRequest.BatchRequestBuilder("100003340303104").build();
        final BatchRequest[] batchRequestList = {batchRequest1, batchRequest2};

        List<BatchResponse> batchResponseList = new ArrayList<>();
        batchResponseList.add(new BatchResponse(200, null, APP_REQUEST_RESPONSE_1_200));
        batchResponseList.add(new BatchResponse(200, null, APP_REQUEST_RESPONSE_2_200));
        batchResponseList.add(new BatchResponse(200, null, LEGACY_APP_RESPONSE));
        batchResponseList.add(new BatchResponse(404, null, "Invalid App Request"));
        batchResponseList.add(new BatchResponse(200, null, UNKNOWN_APP_RESPONSE));
        batchResponseList.add(new BatchResponse(200, null, REFER_FRIEND_APP_RESPONSE));

        when(fbClient.executeBatch(batchRequestList)).thenReturn(batchResponseList);

        underTest.processAppRequests(REQUEST_ID_STRING + "%2C100003340303104", APP_REQUEST_TOKEN);

        FacebookAppToUserRequest request1 = new FacebookAppRequestBuilder()
                .withId(REQUEST_ID1)
                .withCreatedTime(CREATED_TIME)
                .withEngagementData(TRACKING_DATA_1, null)
                .withMessage(MESSAGE)
                .build();
        FacebookAppToUserRequest request2 = new FacebookAppRequestBuilder()
                .withId(REQUEST_ID2)
                .withCreatedTime(CREATED_TIME)
                .withEngagementData(TRACKING_DATA_2, null)
                .withMessage(MESSAGE)
                .build();
        final ArgumentCaptor<List> capturedRequests = ArgumentCaptor.forClass(List.class);
        verify(fbEngagementAppRequestProcessor).processFacebookAppToUserRequest(capturedRequests.capture(), eq(fbClient));
        assertThat(capturedRequests.getValue().size(), is(equalTo(2)));
        Assert.assertThat((List<FacebookAppToUserRequest>) capturedRequests.getValue(), hasItem(anEqualRequestTo(request1)));
        Assert.assertThat((List<FacebookAppToUserRequest>) capturedRequests.getValue(), hasItem(anEqualRequestTo(request2)));

        final HashMap<String, String> engagementData = new HashMap<>();
        engagementData.put(LegacyEngagementData.TRACKING_KEY, "RickyMartinIsATool");

        ObjectMapper mapper = objectMapper();
        String led = mapper.writeValueAsString(new LegacyEngagementData(engagementData));

        List<FacebookAppToUserRequest> legacyAppToUserRequests = new ArrayList<>();
        legacyAppToUserRequests.add(new FacebookAppRequestBuilder()
                .withId(LEAGACY_APP_REQUEST_ID)
                .withData(led)
                .withMessage(MESSAGE)
                .withCreatedTime(CREATED_TIME)
                .build());

        verify(fbLegacyEngagementAppRequestService).processFacebookAppToUserRequest(legacyAppToUserRequests, fbClient);
    }

    private ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JodaModule());
        return objectMapper;
    }

    @Test
    public void testDeserialiseOfLegacyObject() throws IOException {
        ObjectMapper om = objectMapper();

        FacebookAppToUserRequest legacyFacebookAppToUserRequest = null;
        try {
            legacyFacebookAppToUserRequest = om.readValue(LEGACY_APP_RESPONSE, FacebookAppToUserRequest.class);
        } catch (IOException e) {
            LOG.info("failed to serialise JSON", e);
        }

        assertEquals(new LegacyFacebookAppRequestBuilder()
                .withId(LEAGACY_APP_REQUEST_ID)
                .withMessage(MESSAGE)
                .withCreatedTime(CREATED_TIME)
                .withEngagementData("RickyMartinIsATool")
                .build(), legacyFacebookAppToUserRequest);
    }

    @Test
    public void testDeserializeOfFacebookAppRequest() throws IOException {
        ObjectMapper om = objectMapper();

        FacebookAppToUserRequest facebookAppToUserRequestActual = null;
        try {
            facebookAppToUserRequestActual = om.readValue(APP_REQUEST_RESPONSE_1_200, FacebookAppToUserRequest.class);

        } catch (IOException e) {
            LOG.info("failed to serialise JSON", e);
        }

        final FacebookAppToUserRequest facebookAppToUserRequestExpected = new FacebookAppRequestBuilder()
                .withId("399431233447704_100001186555918")
                .withCreatedTime(CREATED_TIME)
                .withMessage(MESSAGE)
                .withEngagementData(TRACKING_DATA_1, null)
                .build();
        assertThat(facebookAppToUserRequestActual, is(anEqualRequestTo(facebookAppToUserRequestExpected)));
    }

    @Test
    public void referFriendPatternShouldMatchReferAFriendOutput() {
        final Pattern pattern = Pattern.compile(FacebookAppToUserRequestHandler.REFER_FRIEND_PATTERN);
        final Matcher matcher = pattern.matcher(REFER_FRIEND_APP_RESPONSE);

        assertTrue(matcher.find());
    }


    @Test
    public void engagementPatternShouldMatchEngagementRequestOutput() {
        final Pattern pattern = Pattern.compile(FacebookAppToUserRequestHandler.ENGAGEMENT_PATTERN);
        final Matcher matcher = pattern.matcher(APP_REQUEST_RESPONSE_1_200);

        assertTrue(matcher.find());
    }

    @Test
    public void legacyEngagementPatternShouldMatchEngagementRequestOutput() {
        final Pattern pattern = Pattern.compile(FacebookAppToUserRequestHandler.LEGACY_ENGAGEMENT_PATTERN);
        final Matcher matcher = pattern.matcher(LEGACY_APP_RESPONSE);

        assertTrue(matcher.find());
    }

    private org.hamcrest.Matcher<FacebookAppToUserRequest> anEqualRequestTo(final FacebookAppToUserRequest expected) {
        return new TypeSafeMatcher<FacebookAppToUserRequest>() {
            private final ObjectMapper objectMapper = objectMapper();

            @Override
            protected boolean matchesSafely(final FacebookAppToUserRequest actual) {
                final boolean equal = new EqualsBuilder()
                        .append(actual.getId(), expected.getId())
                        .append(actual.getMessage(), expected.getMessage())
                        .append(actual.getCreatedTime(), expected.getCreatedTime()).isEquals();

                if (equal) {
                    return ObjectUtils.equals(deserialise(actual.getData()), deserialise(expected.getData()));
                }
                return equal;
            }

            private Object deserialise(final String text) {
                if (text == null) {
                    return null;
                }
                try {
                    return objectMapper.readTree(text);
                } catch (IOException e) {
                    throw new RuntimeException("Data cannot be deserialised: " + text, e);
                }
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("is equal to ").appendValue(expected);
            }
        };
    }

}
