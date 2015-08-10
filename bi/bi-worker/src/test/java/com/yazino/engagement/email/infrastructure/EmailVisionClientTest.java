package com.yazino.engagement.email.infrastructure;

import com.yazino.engagement.email.domain.*;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.web.client.RestOperations;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.yazino.engagement.email.infrastructure.EmailVisionClient.URL_PARAMS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EmailVisionClientTest {

    @Mock
    private RestOperations restOperations;

    private EmailVisionClient underTest;
    private final DateTime sendDate = new DateTime(2013, 6, 20, 10, 30, 0);
    private static final String baseUrl = "https://api.notificationmessaging.com/NMSREST";
    private static final String url = baseUrl + URL_PARAMS + "&dyn=DISPLAY_NAME:WhatIsMyName";

    private EmailConfig emailConfig;
    private EmailData emailData;

    @Before
    public void setUp() throws Exception {
        underTest = new EmailVisionClient(restOperations, baseUrl);
        emailConfig = new EmailConfig("ABCD", "z1x2c3", "email", SynchronizationType.NOTHING);
        emailData = new EmailData("a@b.com", sendDate, createDynamicKeysMap());

    }

    @Test
    public void sendEmailShouldReturnTrueIfTheResponseIsSuccess() {

        Map<String, ?> uriVariables = underTest.createUriVariables(emailConfig, emailData);
        EmailVisionZeroDayResponse expectedResponse = new EmailVisionZeroDayResponse();
        expectedResponse.setResponseStatus("success");

        when(restOperations.getForObject(url, EmailVisionZeroDayResponse.class, uriVariables)).thenReturn(expectedResponse);

        assertThat(underTest.sendEmail(emailConfig, emailData), is(true));
    }

    @Test
    public void sendEmailShouldReturnFalseIfTheResponseIsNull() {
        assertThat(underTest.sendEmail(emailConfig, emailData), is(false));
    }

    @Test
    public void createDynamicKeysUrlShouldReturnTheRightUrlForSingleKeyMap() {
        Map<String, String> dynamicKeysMap = createDynamicKeysMap();
        String actualUrl = underTest.createDynamicKeysUrl(dynamicKeysMap);
        String expectedUrl = "&dyn=DISPLAY_NAME:WhatIsMyName";
        assertThat(actualUrl, is(equalTo(expectedUrl)));
    }

    @Test
    public void createDynamicKeysUrlShouldReturnTheRightUrlForMultiKeyMap() {
        Map<String, String> dynamicKeysMap = createDynamicKeysMap();
        dynamicKeysMap.put("email", "a@example.com");
        String actualUrl = underTest.createDynamicKeysUrl(dynamicKeysMap);
        String expectedUrl = "&dyn=DISPLAY_NAME:WhatIsMyName|email:a@example.com";
        assertThat(actualUrl, is(equalTo(expectedUrl)));
    }

    @Test
    public void createDynamicKeysUrlShouldReturnEmptyStringIfDynamicKeysMapIsNull() {
        String actualUrl = underTest.createDynamicKeysUrl(null);
        assertThat(actualUrl, is(equalTo("")));
    }

    private Map<String, String> createDynamicKeysMap() {
        Map<String, String> dynamicKeys = new LinkedHashMap<String, String>();
        dynamicKeys.put("DISPLAY_NAME", "WhatIsMyName");
        return dynamicKeys;
    }

}
