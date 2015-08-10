package com.yazino.web.payment.googlecheckout.v3;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Partner;
import com.yazino.web.payment.googlecheckout.AndroidInAppOrderSecurity;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

// Tests android in app billing v3.
public class AndroidInAppOrderSecurityV3Test {

    private AndroidInAppOrderSecurity underTest;

    @Before
    public void init() {
        final YazinoConfiguration yazinoConfiguration = mock(YazinoConfiguration.class);
        String yazinoSlotsLicenseKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmHDtE2lg7umCpnJGPU+BOJNcEuHY3Buj/WA7tQ/AN0iXVNGCx5sV1EnALUQbDjshtS64Szjf6jv085UvyqKgRfnR8cqWfp4swJHdrCIlinGWqPqt/ORycZQwDQUKkMKe5kCuja3HEULERIZkmU7KSVqWRx8LTPA1c5uvPU6ajSDu7Ezw14s1Pfc61bthfBwPdPB+mijWt7qv7la4VhNE+wCjpWNLxAMJjtqdJgvYXZRtDS9gwPAEPfMxFz94s/Ei/OpL10LKeJp9IYoYa0r8UJMpMoaJXoLbc3XHY8z23LQgJ3MyVurZwCSXc3zMFDHkLXQaih2PqbCEoa/jbNUmCQIDAQAB";
        String yazinoBlackjackLicenseKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmjSVjrpxiCKY/QH/EemQPWKEH5rqDZ3Vl8iA8qfGMFLbQ+igR2uytouSGQ949PQLYRizujGdiLbQkqxf51AmWTnGRniEx6y1TsnQh6jDB7OH7uBQt0qBJokAscd0qo+5i1ZyTObTNaQ8q/pWNxyaa1qQZt8aTLKWcXJ3WPXsgAjY49kii5qR8gpwGyhWmIAEMAwwh5fmRLQ8+h1nMMZ/L0rdyp7sB0MdnBmAv2o6bCcKtlUphxk+j5XAEPBr3Y2/RXxlcjhzLZh1l6hEERgA+RpFnRwNnptP6Y+EDzcjHFWL7sIqz/inKcgwMvPNVhNSO9CdSCLgzP4oJvtBCD0tpQIDAQAB";
        stub(yazinoConfiguration.getString("payment.googlecheckout.billing-key.YAZINO.SLOTS")).toReturn(yazinoSlotsLicenseKey);
        stub(yazinoConfiguration.getString("payment.googlecheckout.billing-key.YAZINO.BLACKJACK")).toReturn(yazinoBlackjackLicenseKey);
        underTest = new AndroidInAppOrderSecurity(yazinoConfiguration);
    }

    @Test
    public void slotsOrderVerification() {
        String orderData = "{\"orderId\":\"12999763169054705758.1363011803587520\",\"packageName\":\"air.com.yazino.android.extension.Example\",\"productId\":\"slots_usd3_buys_5k\",\"purchaseTime\":1374506402000,\"purchaseState\":0,\"developerPayload\":\"{\\\"purchaseId\\\":\\\"p1374506398141\\\"}\",\"purchaseToken\":\"ppygeqrrosfydwabjhsiolgw.AO-J1Oz8jJeIXp4fGdhz2yRxqHYevucBT4UlC5_urjKojEb17XegnNVrePPRYCzCV7TaS4WFeV4vSlyBnPY38SrITBJYJomekGEasYAUNV2NDimb0FvF1ucN9VOvCYxQwzPur4SS6MevxKXyu7chznEk5VyH20ckqA\"}";
        String signature = "CMTQdrMVKk0cBl6z3VnxSe1liQVhjJMYS91xb2eKEO6B7mz8NSKcenFkhDgqeZ5e9naae46yalLxbJGBNzIbjBQn2+3HQc+Jxed3szUImjJI8LDSmXSmDd41pyyVoJpAkYs/01m1Hlobsb93R4j5tCDmlWDUx8nv9j/R/VNZ/rckSsPIHiki26C44dSeSvPgOaT89hl1fdTYuWyQCd1XMpfg8uoeQBKFwovIhXsAdX+558oTqY8yXJSF35s6u6GwAztG+QGlWahAbpWUMNSynkxMHMy7jMNuHVTGFMVVJuRd2lDbWpRtJhaD5T26WpHUSZlkKWuUke18cWCKtb7V6g==";
        boolean verified = underTest.verify("SLOTS", orderData, signature, Partner.YAZINO);
        assertTrue(verified);
    }

    @Test
    public void blackjackOrderVerofication() {
        String orderData = "{\"orderId\":\"12999763169054705758.1302278199088375\",\"packageName\":\"air.com.yazino.android.blackjack\",\"productId\":\"blackjack_usd8_buys_30k_p100\",\"purchaseTime\":1401463270637,\"purchaseState\":0,\"developerPayload\":\"{\\\"purchaseId\\\":\\\"800a5309-8b1c-4bcc-a90a-4d2850bd2599\\\"}\",\"purchaseToken\":\"pbjhigbhfliomooloeghjbei.AO-J1Oy1SBid8MHC1ho8j-0OLZ50sNrHy0HiwEttKmBLQZxIoI4d5jbzX1vAComJHOmly4G-n4QjxNDq5W3HvtBl1GqZlhpHRxoE4XxbmZ3zAfczbaUFwDO_0piDRg6FbzJEg47eiui59Eh79Ig1HNhcI9N2rqA7TQ\"}";
        String signature = "DV7PZGMUJ3N+WxMoji3LxV/hJNxeCS5evkYXfIyia/KNgfLG/XGRLNAG+GKO3wwvc0Q+M0KvjkkHw4EQBBEni/ZT9fOTOe91bF8+fw0aRIf58bx9hOLovJf+bcIVgOeAgXgqSNqmiXWhGjLcu9iOIC4SMJsLLEEPkwewT/lnj2zlK9kjxbMUaeNF4UcAUE6807pSn5Ob6ENngkHBqq5/sqQp4GOr2Js07puLNPHMTPA+GXgJ1jgRF9hoBI7EOvj0NelOe7vvLXqEWHJxQVfyGZXxfMjXzHW9FDnsCvPYGGYAlB/RN6oQO97QvgGt+6oZFTkS+xnxR6zyq+pJzkrjAA==";
        boolean verified = underTest.verify("BLACKJACK", orderData, signature, Partner.YAZINO);
        assertTrue(verified);
    }
}
