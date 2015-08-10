package com.yazino.web.domain.facebook;

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SignedRequestTest {

    private static final String APP_SECRET = "5399b76e1b14b3e983934fc9486b854e";
    private static final String SIGNED_REQUEST
            = "HkjJAV_wwLXSERM5q7NE__7VqZDQOR6RBrgDuUlsZL0.eyJhbGdvcml0aG0iOiJITUFDLVNIQTI1NiIs"
            + "ImV4cGlyZXMiOjEzMDQ1ODk2MDAsImlzc3VlZF9hdCI6MTMwNDU4NTA0Nywib2F1dGhfdG9rZW4iOiI5"
            + "OTA3MDQ3OTYzMXwyLjBPeFFzaDNrbmpOZWZHZVVGMXpSMWdfXy4zNjAwLjEzMDQ1ODk2MDAuMS0xMDAw"
            + "MDA1NjE4NDA3NzJ8QTNjYUVaNnQ3UVRKRlFESEY3OGpFTGk1T3pVIiwidXNlciI6eyJjb3VudHJ5Ijoi"
            + "Z2IiLCJsb2NhbGUiOiJlbl9HQiIsImFnZSI6eyJtaW4iOjIxfX0sInVzZXJfaWQiOiIxMDAwMDA1NjE4"
            + "NDA3NzIifQ";
    private static final String SIGNED_REQUEST_WITH_INVALID_PAYLOAD
            = "HkjJAV_wwLXSERM5q7NE__7VqZDQOR6RBrgDuUlsZL0.eyJhbGdvcml0aG0iOiJITUFDLVNIQTI1NiIs"
            + "ImV4cGlyZXMiOjEzMDQ1ODk2MDAsImlzc3VlZF9hdCI6MTMwNDU4NTA0Nywib2F1dGhfdG9rZW4iOiI5"
            + "OTA3MDQ3OTYzMXwyLjBPeFFzaDNrbmpOZWZHZVVGMXpSMWdfXy4zNjAwLjEzMDQ1ODk2MDAuMS0xMDAw"
            + "MDA1NjE4NDA3NzJ8QTNjYUVaNnQ3UVRKRlFESEY3sfwFTGk1T3pVIiwidXNlciI6eyJjb3VudHJ5Ijoi"
            + "Z2IiLCJsb2NhbGUiOiJlbl9HQiIsImFnZSI6eyJtaW4iOjIxfX0sInVzZXJfaWQiOiIxMDAwMDA1NjE4"
            + "NDA3NzIifQ";
    private static final String SIGNED_REQUEST_WITH_INVALID_SIGNATURE
            = "HkjJAV_wwLXSERM5q7NE__7VqZDQOR6RBrgDuUlsZL0.eyJhbGdsdfl0aG0iOiJITUFDLVNIQTI1NiIs"
            + "ImV4cGlyZXMiOjEzMDQ1ODk2MDAsImlzc3VlZF9hdCI6MTMwNDU4NTA0Nywib2F1dGhfdG9rZW4iOiI5"
            + "OTA3MDQ3OTYzMXwyLjBPeFFzaDNrbmpOZWZHZVVGMXpSMWdfXy4zNjAwLjEzMDQ1ODk2MDAuMS0xMDAw"
            + "MDA1NjE4NDA3NzJ8QTNjYUVaNnQ3UVRKRlFESEY3OGpFTGk1T3pVIiwidXNlciI6eyJjb3VudHJ5Ijoi"
            + "Z2IiLCJsb2NhbGUiOiJlbl9HQiIsImFnZSI6eyJtaW4iOjIxfX0sInVzZXJfaWQiOiIxMDAwMDA1NjE4"
            + "NDA3NzIifQ";

    @SuppressWarnings({"unchecked"})
    @Test
    public void aValidRequestIsDecodedAndContainsAllParameters() {
        final SignedRequest request = new SignedRequest(SIGNED_REQUEST, APP_SECRET);

        assertThat((String) request.get("algorithm"), is(equalTo("HMAC-SHA256")));
        assertThat((Integer) request.get("expires"), is(equalTo(1304589600)));
        assertThat((Integer) request.get("issued_at"), is(equalTo(1304585047)));
        assertThat((String) request.get("user_id"), is(equalTo("100000561840772")));
        assertThat((String) request.get("oauth_token"), is(equalTo(
                "99070479631|2.0OxQsh3knjNefGeUF1zR1g__.3600.1304589600.1-100000561840772|A3caEZ6t7QTJFQDHF78jELi5OzU")));
        assertThat(request.get("user"), is(not(nullValue())));

        final Map<String, Object> user = (Map<String, Object>) request.get("user");
        assertThat((String) user.get("country"), is(equalTo("gb")));
        assertThat((String) user.get("locale"), is(equalTo("en_GB")));
        assertThat(user.get("age"), is(not(nullValue())));

        final Map<String, Object> age = (Map<String, Object>) user.get("age");
        assertThat((Integer) age.get("min"), is(equalTo(21)));
    }
@Test
public void deletemeShould(){
    new SignedRequest("KBg_TK65mgEToXeRA3hUsfKjgfyA3yx_V1dPP6XOnvE.eyJhbGdvcml0aG0iOiJITUFDLVNIQTI1NiIsImNyZWRpdHMiOnsiYnV5ZXIiOjEwMDAwMzE0MDMyMjI0OSwicmVjZWl2ZXIiOjEwMDAwMzE0MDMyMjI0OSwib3JkZXJfaWQiOjQ0MjYxNTQ1OTEzNjU2Nywib3JkZXJfaW5mbyI6IntcIml0ZW1faWRcIjpcIm9wdGlvblVTRDNcIixcImdhbWVfdHlwZVwiOlwiSElHSF9TVEFLRVNcIixcInByb21vX2lkXCI6XCJcIixcInBsYXllcl9pZFwiOlwiODQxMTc1M1wiLFwiY3VycmVuY3lfY29kZVwiOlwiR0JQXCIsXCJhbW91bnRfcGFpZF9pbl9jdXJyZW5jeVwiOlwiMTUuODFcIn0iLCJ0ZXN0X21vZGUiOjF9LCJleHBpcmVzIjoxMzU0MDM1NjAwLCJpc3N1ZWRfYXQiOjEzNTQwMjk0NTQsIm9hdXRoX3Rva2VuIjoiQUFBRGo0UkZhZUpZQkFQcHp3OW4xMFRFVVliaUZ1VUZaQUJaQmpvaWdYZXZSWkI5QmM5T1Zvb2x3Tk02NXBlMzB1WHZHeWgyYTZ2OTN2QThEcWI4VjZJeWg3Y2Q5WXhGdXhaQ1ZaQUt3R1ZaQWlGS1RqTUk2WkNXIiwidXNlciI6eyJjb3VudHJ5IjoiZ2IiLCJsb2NhbGUiOiJlbl9HQiIsImFnZSI6eyJtaW4iOjIxfX0sInVzZXJfaWQiOiIxMDAwMDMxNDAzMjIyNDkifQ","561a5570de20108e01be4106b14b0789");
}
    @Test
    public void anOAuthTokenCanBeRetrievedFromAValidRequest() {
        final SignedRequest request = new SignedRequest(SIGNED_REQUEST, APP_SECRET);

        assertThat(request.getOAuthToken(), is(equalTo(
                "99070479631|2.0OxQsh3knjNefGeUF1zR1g__.3600.1304589600.1-100000561840772|A3caEZ6t7QTJFQDHF78jELi5OzU")));
    }

    @Test
    public void signedSignatureValidationReturnsAnEmptyMapIfTheSignatureIsIncorrect() {
        final SignedRequest request = new SignedRequest(SIGNED_REQUEST_WITH_INVALID_SIGNATURE, APP_SECRET);

        assertThat(request.size(), is(equalTo(0)));
    }

    @Test
    public void signedSignatureValidationReturnsAnEmptyMapIfThePayloadIsIncorrect() {
        final SignedRequest request = new SignedRequest(SIGNED_REQUEST_WITH_INVALID_PAYLOAD, APP_SECRET);

        assertThat(request.size(), is(equalTo(0)));
    }

    @Test
    public void signedSignatureValidationReturnsAnEmptyMapIfTheAppSecretIsIncorrect() {
        final SignedRequest request = new SignedRequest(SIGNED_REQUEST, APP_SECRET + "wrong");

        assertThat(request.size(), is(equalTo(0)));
    }

    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void signedSignatureValidationThrowsAnExceptionForANullRequest() {
        final SignedRequest request = new SignedRequest(null, APP_SECRET);

        assertThat(request.size(), is(equalTo(0)));
    }


    @SuppressWarnings({"ConstantConditions"})
    @Test(expected = NullPointerException.class)
    public void signedSignatureValidationThrowsAnExceptionForANullSecret() {
        final SignedRequest request = new SignedRequest(SIGNED_REQUEST, null);

        assertThat(request.size(), is(equalTo(0)));
    }

    @Test
    public void signedSignatureValidationReturnsAnEmptyMapForABadlyFormattedRequest() {
        final SignedRequest request = new SignedRequest(SIGNED_REQUEST + ".anotherpart", APP_SECRET);

        assertThat(request.size(), is(equalTo(0)));
    }

}
