package com.yazino.web.payment.googlecheckout;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Partner;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

public class AndroidInAppOrderSecurityTest {
    // Do not change this data. The signature is valid for ORDER_JSON.
    public static final String ORDER_JSON = "{\"nonce\":1818223156688188385,\"orders\":[{\"notificationId\":\"8698862048128790553\",\"orderId\":\"12999763169054705758.1344173743070417\",\"packageName\":\"air.com.yazino.android.slots\",\"productId\":\"slots_usd3_buys_5k\",\"purchaseTime\":1355763527000,\"purchaseState\":0,\"purchaseToken\":\"deqxhfgftyrcgutlzjeuixjv\"}]}";
    public static final String LICENSE_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApoRY5GpNf3kddKwO9oRAuwjAoT70B6vb1qUsr7ojrbkoJ3arEmXyk3+0GinJR8o0EPE+sSagrTj8D/tyRl+NtcfVg7InFi+MVH+y5kLXhW/l820AHAJEkl42akJIfyY/QU6njirlWn6Y7RuTukZ5FXnVerCiGDoU127HgO+xeTfXIEUa0jN3PSQTluWGuduK8IqQE2TM03y0Mv/K9tavrAbtjYFQiQtrLRMIA9tSe82ry7hfhsNHKM/CE9ZAYk/KnpogGmH5MQf0fN2R2fyyjBPFFY72kHJ7NPA4a5h5/S/CO8QotZ6gWr5L8mGLs1cUB6No8EmzgG57BAs7pDgAYQIDAQAB";
    public static final String SIGNATURE = "BpTfDtW4xxe7Uvw55BvmCK8S4evoI+6+2PpqU1Sqj/JwPHpvVuSKfa05jMYQQeNy4iFmTehi5eAn9VVwNFqL2bimLCouS6FnT71GRP/BAmSRO3Z1e8SRS+pPV1om0CI/fqLGF2rBgKBBFi/n67HFffwxmGofrMEETRvB5L5IQTYBTgzWufy7ZwR7EIsgOnhi9Oi/0Y2acrKdm2OOSR849eIcPL8rdEfwrOMiMLF1ZYbdYnqacTBLX9m5cLgWZWqGvAB7TU+jzWaQD5faVD+51rsAuBI9B6A600SRPsZTFxmSzPlPSQizzAFwhU/JkpmKZeBqJ4xkBSNY7bqmvsKG+w==";
    // END do not change
    public static final String LICENSE_KEY_INVALID = "this is not a valid base64 encoded rsa public key";
    public static final String INVALID_SIGNATURE = "This signature is not that of ORDER_JSON";
    public static final String GAME_TYPE = "SLOTS";
    public static final String GAME_TYPE_FOR_INVALID_KEY = "TEXAS";
    private static final Partner partnerId = Partner.YAZINO;

    private AndroidInAppOrderSecurity underTest;

    @Before
    public void init() {
        final YazinoConfiguration yazinoConfiguration = mock(YazinoConfiguration.class);
        stub(yazinoConfiguration.getString("payment.googlecheckout.billing-key.YAZINO.SLOTS")).toReturn(LICENSE_KEY);
        stub(yazinoConfiguration.getString("payment.googlecheckout.billing-key.YAZINO.TEXAS")).toReturn(LICENSE_KEY_INVALID);
        underTest = new AndroidInAppOrderSecurity(yazinoConfiguration);
    }

    @Test(expected = NullPointerException.class)
    public void verifyShouldThrowNullPointerWhenGameTypeIsNull() {
        underTest.verify(null, ORDER_JSON, SIGNATURE, partnerId);
    }

    @Test(expected = NullPointerException.class)
    public void verifyShouldThrowNullPointerWhenOrderDataIsNull() {
        underTest.verify(GAME_TYPE, null, SIGNATURE, partnerId);
    }

    @Test(expected = NullPointerException.class)
    public void verifyShouldThrowNullPointerWhenSignatureIsNull() {
        underTest.verify(GAME_TYPE, ORDER_JSON, null, partnerId);
    }

    @Test
    public void verifyShouldReturnTrueWhenDataIsSignedCorrectly() {
        assertTrue(underTest.verify(GAME_TYPE, ORDER_JSON, SIGNATURE, Partner.YAZINO));
    }

    @Test
    public void verifyShouldReturnFalseWhenDataIsInCorrectlySigned() {
        assertFalse(underTest.verify(GAME_TYPE, ORDER_JSON, INVALID_SIGNATURE, partnerId));
    }

    @Test
    public void verifyShouldReturnFalseWhenLicenseKeyIsInvalid() {
        assertFalse(underTest.verify(GAME_TYPE_FOR_INVALID_KEY, ORDER_JSON, SIGNATURE, partnerId));
    }

    @Test
    public void nullPartnerShouldDefaultToYazino() {

        assertTrue(underTest.verify(GAME_TYPE, ORDER_JSON, SIGNATURE, null));

    }

}
