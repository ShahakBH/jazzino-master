package com.yazino.web.payment.radium;

import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RadiumValidationServiceTest {

    RadiumValidationService radiumValidationService = null;

//    We will send the following parameters to your Postback URL every time a user completes an offer or payment:
//• appId is Application ID of your application on RadiumOne Social
//• userId is the user ID value you've passed to us on iFrame call
//• amount is the transaction amount of your virtual currency
//• hash is MD5 hash you can use to verify the postback, calculated as md5([USER_ID]+":"+[APP_ID]+":"+[SECRET_KEY]), where "+" is string concatenation and [SECRET_KEY] is the secret key of your application
//• trackId is the tracking value you passed to us on iFrame call
//Note: Your reply must be a single character 1 on success or 0 otherwise

    @Before
    public void before() {
        final HashMap<String, String> appsAndKeys = Maps.newHashMap();
        appsAndKeys.put("6194", "648a64c002d541d38c6b51f3086bec09");
        appsAndKeys.put("6205", "1d278db9a1774ea2b3de9725473a5577");
        appsAndKeys.put("6131", "ec6fc2ff25594ad19eac9960f153d655");
        radiumValidationService = new RadiumValidationService(appsAndKeys, "80.70.60.0/25,88.211.55.18/31,10.9.8.0/24");
    }

    @Test
    public void shouldValidateSignature() throws IOException {
        assertTrue(radiumValidationService.validate("123456", "6131", "6b1cec9f52d1e82a282100f0a782e44a"));
        assertTrue(radiumValidationService.validate("123", "6194", "50a93eac4a7797d0b0f1e38ca323af86"));
        assertTrue(radiumValidationService.validate("456", "6205", "89652a635df46663e29dd891215d08ca"));

    }

    @Test
    public void shouldNotValidateInvalidUserId() throws IOException {
        //userId
        assertFalse(radiumValidationService.validate("1234567", "6131", "6b1cec9f52d1e82a282100f0a782e44a"));
        //appId
        assertFalse(radiumValidationService.validate("123456", "6132", "6b1cec9f52d1e82a282100f0a782e44a"));
        //hash
        assertFalse(radiumValidationService.validate("123456", "6131", "6b1cec9f52d1e82a282100f0a782e44b"));
    }

    @Test
    public void ipAddressForCallbackRequestShouldBeOnWhitelist() {
        Assert.assertThat(radiumValidationService.validateIp("1.2.3.4"), is(false));
        Assert.assertThat(radiumValidationService.validateIp("80.70.60.7"), is(true));
        Assert.assertThat(radiumValidationService.validateIp("80.70.60.8"), is(true));
        Assert.assertThat(radiumValidationService.validateIp("80.70.60.9"), is(true));
        Assert.assertThat(radiumValidationService.validateIp("88.211.55.17"), is(false));
        Assert.assertThat(radiumValidationService.validateIp("88.211.55.18"), is(true));
        Assert.assertThat(radiumValidationService.validateIp("88.211.55.19"), is(true));
        Assert.assertThat(radiumValidationService.validateIp("88.211.55.20"), is(false));

    }

    @Test
    public void tenNineEightShouldBeWhitelistedForTesting() {
        Assert.assertThat(radiumValidationService.validateIp("10.9.8.81"), is(true));
        Assert.assertThat(radiumValidationService.validateIp("10.9.8.162"), is(true));
    }

    @Test
    public void radiumsServersShouldBeAccessible() {
        final HashMap<String,String> appsAndKeys = Maps.newHashMap();
        radiumValidationService = new RadiumValidationService(appsAndKeys, "72.5.64.0/25,88.211.55.18/31,10.9.8.0/25");
        Assert.assertThat(radiumValidationService.validateIp("72.5.64.1"), is(true));
    }
}