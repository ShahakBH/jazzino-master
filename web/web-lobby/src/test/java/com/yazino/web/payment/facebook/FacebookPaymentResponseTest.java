package com.yazino.web.payment.facebook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;

import static java.math.BigDecimal.valueOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class FacebookPaymentResponseTest {

    private static final String HIGH_STAKES = "HIGH_STAKES";

    @Test
    public void jsonserializeShouldCreateJsonForPaymentResponse() throws IOException {
        final FacebookPaymentResponse facebookPaymentResponse = new FacebookPaymentResponse("title", "desc", 123, "method", "imageUrl", "productUrl",
                HIGH_STAKES,
                "optionUSD2",
                123l,
                new BigDecimal("321"),
                "USD",
                valueOf(10));
        final String json="{" +
                "\"method\":\"method\"," +
                "\"content\":[" +
                "{" +
                "\"title\":\"title\"," +
                "\"price\":123," +
                "\"image_url\":\"imageUrl\"," +
                "\"description\":\"desc\"," +
                "\"data\":\"{" +
                "\\\"player_id\\\":\\\"321\\\"," +
                "\\\"game_type\\\":\\\"HIGH_STAKES\\\"," +
                "\\\"item_id\\\":\\\"optionUSD2\\\"," +
                "\\\"promo_id\\\":\\\"123\\\"," +
                "\\\"currency_code\\\":\\\"USD\\\"," +
                "\\\"amount_paid_in_currency\\\":\\\"10\\\"" +
                "}\"," +
                "\"product_url\":\"productUrl\"" +
                "}" +
                "]" +
                "}";
        System.out.println(facebookPaymentResponse.toJSON());
        assertThat(deserialise(facebookPaymentResponse.toJSON()), is(equalTo(deserialise(json))));
    }

    private JsonNode deserialise(final String jsonAsText) throws IOException {
        return new ObjectMapper().readTree(jsonAsText);
    }

}
