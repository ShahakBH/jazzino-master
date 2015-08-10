package com.yazino.engagement.amazon;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.Assert.assertNotNull;

public class AmazonAccessTokenServiceTest {
    private static final Logger LOG = LoggerFactory.getLogger(AmazonAccessTokenServiceTest.class);
    // Darrens' client id and secrets
    public static final String CLIENT_ID = "amzn1.application-oa2-client.7929277dd4674b49a23c5cb740b08fd9";
    public static final String CLIENT_SECRET = "9708d4d4d31884053b8ec5867166f33078f686aa7de7d1dbc3a0571ede2977f0";
    AmazonAccessTokenService underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new AmazonAccessTokenService();
    }

    @Test
    public void getAuthTokenShouldReturnAuthToken() {
        AmazonAccessToken authToken = null;

        try {
            authToken = underTest.getAuthToken(CLIENT_ID, CLIENT_SECRET);
        } catch (Exception e) {
            e.printStackTrace();
        }

        LOG.debug(authToken.toString());
        assertNotNull(authToken);
    }
}
