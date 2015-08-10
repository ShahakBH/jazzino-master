package com.yazino.engagement.email.application;

import com.yazino.bi.aggregator.HostUtils;
import com.yazino.engagement.email.domain.EmailData;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.yazino.engagement.email.domain.EmailVisionRestParams.DISPLAY_NAME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ContextConfiguration
@DirtiesContext
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore("Removed until business make up their minds about paying Email Vision")
public class EmailVisionApiExternalIntegrationTest {

    @Autowired
    private EmailApi underTest;

    private String displayName;

    @Before
    public void setUp() throws Exception {
        displayName = "IntegrationTest" + HostUtils.getHostName() + new DateTime();
    }

    @Test(timeout = 30000)
    public void sendDayZeroEmailShouldReturnTrueIfSendingOfEmailSucceeds() {
        String emailAddress = "test@example.com";
        Map<String, String> dynamicKeys = createDynamicKeys();
        EmailData emailData = new EmailData(emailAddress, new DateTime(), dynamicKeys);

        assertThat(underTest.sendDayZeroEmail(emailData), is(true));
    }

    @Test(timeout = 30000)
    public void sendDayZeroEmailShouldReturnFalseIfSendingOfEmailFailsWithException() {
        Map<String, String> dynamicKeys = createDynamicKeys();
        EmailData emailData = new EmailData("IntegrationTestInvalidEmailAddress+++", new DateTime(), dynamicKeys);

        assertThat(underTest.sendDayZeroEmail(emailData), is(false));
    }

    private Map<String, String> createDynamicKeys() {
        Map<String, String> dynamicKeys = new LinkedHashMap<String, String>();
        dynamicKeys.put(DISPLAY_NAME.toString(), displayName);
        return dynamicKeys;
    }
}
