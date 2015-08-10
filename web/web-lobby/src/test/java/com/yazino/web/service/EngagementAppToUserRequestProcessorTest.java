package com.yazino.web.service;

import com.restfb.FacebookClient;
import com.restfb.json.JsonObject;
import com.yazino.web.domain.facebook.FacebookAppToUserRequest;
import com.yazino.web.session.ReferrerSessionCache;
import com.yazino.web.util.FacebookAppRequestBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class EngagementAppToUserRequestProcessorTest {
    EngagementAppToUserRequestProcessor underTest;

    @Mock
    private FacebookClient fbClient;
    @Mock
    private ReferrerSessionCache referrerSessionCache;

    @Before
    public void setup() {
        initMocks(this);
        final JsonObject appRequest = mock(JsonObject.class);
        underTest = new EngagementAppToUserRequestProcessor(referrerSessionCache);
    }

    @Test
    public void processFacebookAppRequestsShouldHandleEmptyList()   {
         underTest.processFacebookAppToUserRequest(new ArrayList<FacebookAppToUserRequest>(), fbClient);
         verifyNoMoreInteractions(fbClient);
    }

    @Test
    public void processFacebookAppRequestsShouldSetTheReferrerOfTheMostRecentRequestAndDeleteAllRequests() throws IOException {
        List<FacebookAppToUserRequest> facebookAppToUserRequests = new ArrayList<FacebookAppToUserRequest>();
        facebookAppToUserRequests.add(new FacebookAppRequestBuilder()
                .withId("123")
                .withCreatedTime(new DateTime(2012, 7, 3, 15, 32, 15, 0, DateTimeZone.UTC))
                .withEngagementData("trackingData1", null)
                .build()
        );

        facebookAppToUserRequests.add(new FacebookAppRequestBuilder()
                .withId("345")
                .withCreatedTime(new DateTime(2011, 10, 15, 15, 32, 15, 0, DateTimeZone.UTC))
                .withEngagementData("trackingData2", null)
                .build()
        );

        // null engagement data
        facebookAppToUserRequests.add(new FacebookAppRequestBuilder()
                .withId("456")
                .withCreatedTime(new DateTime(2013, 10, 15, 15, 32, 15, 0, DateTimeZone.UTC))
                .build()
        );



        underTest.processFacebookAppToUserRequest(facebookAppToUserRequests, fbClient);

        verify(referrerSessionCache).setReferrer("trackingData1");
        verify(fbClient).deleteObject("123");
        verify(fbClient).deleteObject("345");
    }


}
