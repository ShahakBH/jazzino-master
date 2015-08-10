package com.yazino.engagement.amazon;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpClientErrorException;

import static junit.framework.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class AmazonDeviceMessagingSenderIntegrationTest {

    // Dseeto app details can be found in the amazon developer console
//    public static final String CLIENT_ID = "amzn1.application-oa2-client.7929277dd4674b49a23c5cb740b08fd9";
//    public static final String CLIENT_SECRET = "9708d4d4d31884053b8ec5867166f33078f686aa7de7d1dbc3a0571ede2977f0";


    // apk flash wheeldeal flex keys if you deploy the apk built by flash with hacked androidmanifest
    public static final String CLIENT_SECRET = "a00c94c7c697f75ef6d9c033dee24f0a8356ee714f10d72b0fa01862e6c11d74";
    public static final String CLIENT_ID = "amzn1.application-oa2-client.60d223f5135c48daa6e76b85e60fa9c6";

    // you need to be able to get this off a device, db
//    public static final String REGISTRATION_ID = "amzn1.adm-registration.v2.Y29tLmFtYXpvbi5EZXZpY2VNZXNzYWdpbmcuUmVnaXN0cmF0aW9uSWRFbmNyeXB0aW9uS2V5ITEhazJGakEvVFhkaXp2NXN6RWEyMU1YV3laYzZjMzZCZTE3YytkUUdDVWxhVFMrTlFqNlZ6V3NqeE1HYk1FTkRpY0F0TU8rK1BBSEk5RXp6aXlxWlJrMEtFVTZVMUh0Q0l0U3FPMTAyazQwalNubHBCcExvUXNjZHNrYStQdTV2SzlUVm5yZU03ZUdmeFlqTTExOVIwc0htd3pXVEtUenJjcW9YdzliQWtFZU9HcHc2QXhhZXpSVk9qS3h2SDRZVnFkV0piWVp1b3hQZkluQUF1RE1zek95RG1mUDUzb3ljNWczU0VDUVlUQTNrc05wY0FONDZCS3pEMWtsa1orVzJOckhuVXA4Wjh5YmVMcXQyelFyOE9yMG4wdFFHcE5XTWxhYWwxdG9oYkw3Zk09IVZtY0d1ZGVGTjByVHIyMEJJdzlwcEE9PQ";
    public static final String REGISTRATION_ID = "amzn1.adm-registration.v2.Y29tLmFtYXpvbi5EZXZpY2VNZXNzYWdpbmcuUmVnaXN0cmF0aW9uSWRFbmNyeXB0aW9uS2V5ITEhaDlCTWdNYkZ5WHQ5bHNCeUNqcy9rMExubktYK1duT0trWXZrOExiWWdZa2VvNHYrT3VmdGhVejhLWTNFSDI5QThFSWUvUkZTbjhUU0lBbWVUYUlsc2dGRFF1SEs5SEU5RzIxTEd4VTZsL0ljQUhPbGp3NkdDcndOZmgyOUlmUWxFejkxUTNvRFJhSnRGbXVDRTdOSS9GajE0TXJ5bFRHUHd1SUx2Wi9CM2dqRjNpNjhONlpWR1M4UXF4T0NUMFdISlhxWTNwWmhCNjhjTFc3ZUxFenRpV3VRbVB6NXlkUEcrQXcrRXF2amhFY2djT0krbUtyZWo2QjlBS1dvLzJneVR3Wkp6QndoTU9uU0FaZDRENXVjRkg1RXF5ajVBMGdnL0M0bklmRm83ekE9IUtucStUWFJPM29WS09Oa00xRVR5eVE9PQ";

    @Autowired
    AmazonDeviceMessagingSender underTest;
    AmazonAccessTokenService amazonAccessTokenService;

    @Before
    public void setUp() throws Exception {
        amazonAccessTokenService = new AmazonAccessTokenService();
    }

    @Test
    @Ignore
    public void sendShouldPostMessageToAmazon() throws Exception {
        AmazonAccessToken authToken = amazonAccessTokenService.getAuthToken(CLIENT_ID, CLIENT_SECRET);

        String result = null;
        try {
            result = underTest.sendMessage(REGISTRATION_ID, authToken.getToken(), "title is title 4", "title is the ticker 4", "this is the Message amazon 4", 2l);
        } catch (HttpClientErrorException e) {
            String responseBodyAsString = e.getResponseBodyAsString();
            System.out.print(responseBodyAsString);
            e.printStackTrace();
        }

        assertNotNull(result);
    }

    @Test
    @Ignore
    public void twoMessagesShouldStack() throws Exception {
        AmazonAccessToken authToken = amazonAccessTokenService.getAuthToken(CLIENT_ID, CLIENT_SECRET);

        String result = null;
        try {
            result = underTest.sendMessage(REGISTRATION_ID, authToken.getToken(), "title is title", "title is the ticker", "this is the Message amazon", 1l);
            result = underTest.sendMessage(REGISTRATION_ID, authToken.getToken(), "title is title 2", "title is the ticker 2", "this is the Message amazon 2", 2l);
        } catch (HttpClientErrorException e) {
            String responseBodyAsString = e.getResponseBodyAsString();
            System.out.print(responseBodyAsString);
            e.printStackTrace();
        }

        assertNotNull(result);
    }
}

