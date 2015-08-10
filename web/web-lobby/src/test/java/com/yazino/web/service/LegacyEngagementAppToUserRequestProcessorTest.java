package com.yazino.web.service;

import com.restfb.FacebookClient;
import com.yazino.web.domain.facebook.FacebookAppToUserRequest;
import com.yazino.web.domain.facebook.LegacyFacebookAppRequestBuilder;
import com.yazino.web.session.ReferrerSessionCache;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

public class LegacyEngagementAppToUserRequestProcessorTest {
    FacebookAppToUserRequestProcessor underTest;

    @Mock
    private FacebookClient fbClient;
    @Mock
    private ReferrerSessionCache referrerSessionCache;

    @Before
    public void setup() {
        initMocks(this);
        underTest = new LegacyEngagementAppToUserRequestProcessor(referrerSessionCache);
    }


    @Test
    public void processFacebookAppToUserRequestsShouldHandleEmptyList()   {
         underTest.processFacebookAppToUserRequest(new ArrayList<FacebookAppToUserRequest>(), fbClient);
         verifyNoMoreInteractions(fbClient);
    }

    @Test
    public void processFacebookAppToUserRequestShouldSetTrackingData() throws IOException {
        List<FacebookAppToUserRequest> legacyFacebookAppToUserRequestList = new ArrayList<FacebookAppToUserRequest>();

        legacyFacebookAppToUserRequestList.add(new LegacyFacebookAppRequestBuilder()
                .withId("123")
                .withCreatedTime(new DateTime(2012, 7, 3, 15, 32, 15, 0, DateTimeZone.UTC))
                .withEngagementData("trackingData1")
                .build()
        );

        legacyFacebookAppToUserRequestList.add(new LegacyFacebookAppRequestBuilder()
                .withId("345")
                .withCreatedTime(new DateTime(2011, 10, 15, 15, 32, 15, 0, DateTimeZone.UTC))
                .withEngagementData("trackingData2")
                .build()
        );

        // null engagement data
        legacyFacebookAppToUserRequestList.add(new LegacyFacebookAppRequestBuilder()
                .withId("456")
                .withCreatedTime(new DateTime(2013, 10, 15, 15, 32, 15, 0, DateTimeZone.UTC))
                .build()
        );

        underTest.processFacebookAppToUserRequest(legacyFacebookAppToUserRequestList, fbClient);
        verify(referrerSessionCache).setReferrer("trackingData1");
        verify(fbClient).deleteObject("123");
        verify(fbClient).deleteObject("345");
        verify(fbClient).deleteObject("456");
    }



}
