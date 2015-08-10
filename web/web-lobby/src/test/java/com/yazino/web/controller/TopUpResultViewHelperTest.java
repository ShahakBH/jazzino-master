package com.yazino.web.controller;

import com.yazino.web.util.JsonHelper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import strata.server.lobby.api.promotion.MobileTopUpResult;
import strata.server.lobby.api.promotion.TopUpResult;
import strata.server.lobby.api.promotion.TopUpStatus;
import strata.server.lobby.api.promotion.WebTopUpResult;

import java.math.BigDecimal;
import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TopUpResultViewHelperTest {

    public static final BigDecimal TOTAL_TOP_UP_AMOUNT = BigDecimal.valueOf(34546);
    public static final String MOBILE_IMAGE_URL = "http://imageUrl.png";
    private TopUpResultViewHelper underTest;

    @Before
    public void init() {
        underTest = new TopUpResultViewHelper();
    }

    @Test
    public void serialiseAsJsonShouldFormatNullTopUpRequestAsEmptyJson() {
        assertThat(underTest.serialiseAsJson(null), is("{}"));
    }

    @Test
    public void shouldSerializeTopUpResultStatusOnly() {
        TopUpResult topUpResult = new TopUpResult(BigDecimal.TEN, TopUpStatus.ACKNOWLEDGED, new DateTime());

        String actualJson = underTest.serialiseAsJson(topUpResult);

        String expectedJson = "{\"status\":\"ACKNOWLEDGED\"}";
        assertThat(actualJson, is(expectedJson));
    }

    @Test
    public void shouldSerializeWeTopUpResult() {
        final DateTime lastTopUpDate = new DateTime();
        WebTopUpResult webTopUpResult = new WebTopUpResult(BigDecimal.TEN, TopUpStatus.CREDITED, lastTopUpDate);
        webTopUpResult.setPromotionValueList(asList(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO));
        webTopUpResult.setConsecutiveDaysPlayed(0);
        webTopUpResult.setMainImage("mainImage.jpg");
        webTopUpResult.setMainImageLink("http://main.image.link");
        webTopUpResult.setSecondaryImage("secondaryImage.jpg");
        webTopUpResult.setSecondaryImageLink("http://secondary/image.jpg");
        webTopUpResult.setTotalTopUpAmount(BigDecimal.valueOf(34546));

        String actualJson = underTest.serialiseAsJson(webTopUpResult);

        final Map<String, Object> expectedJson = new HashMap<>();
        final List<Map<String, String>> articles = new ArrayList<>();
        final Map<String, String> article1 = new HashMap<>();
        article1.put("imageUrl", "mainImage.jpg");
        article1.put("imageLink", "http://main.image.link");
        articles.add(article1);
        final HashMap<String, String> article2 = new HashMap<>();
        article2.put("imageUrl", "secondaryImage.jpg");
        article2.put("imageLink", "http://secondary/image.jpg");
        articles.add(article2);
        expectedJson.put("articles", articles);
        final Map<String, Object> progressive = new HashMap<>();
        progressive.put("amounts", asList(10, 1, 0));
        progressive.put("consecutiveDaysPlayed", 0);
        final Map<String, Object> topupDetails = new HashMap<>();
        topupDetails.put("totalAmount", 34546);
        topupDetails.put("date", lastTopUpDate.getMillis());
        topupDetails.put("progressive", progressive);
        expectedJson.put("topUpDetails", topupDetails);
        expectedJson.put("status", "CREDITED");
        assertThat(actualJson, is(new JsonHelper().serialize(expectedJson)));
    }

    @Test
    public void shouldSerializeIOSTopUpResult() {
        final DateTime lastTopUpDate = new DateTime();
        MobileTopUpResult topUpResult = new MobileTopUpResult(BigDecimal.TEN, TopUpStatus.CREDITED, lastTopUpDate);
        topUpResult.setTotalTopUpAmount(BigDecimal.valueOf(TOTAL_TOP_UP_AMOUNT.longValue()));
        topUpResult.setImageUrl(MOBILE_IMAGE_URL);

        String actualJson = underTest.serialiseAsJson(topUpResult);

        HashMap<String, Object> actualValues = (HashMap<String, Object>) new JsonHelper().deserialize(HashMap.class, actualJson);
        assertEquals(actualValues.get("imageUrl"), MOBILE_IMAGE_URL);
        assertEquals(actualValues.get("totalAmount"), TOTAL_TOP_UP_AMOUNT.intValue());
        assertEquals(actualValues.get("status"), TopUpStatus.CREDITED.name());
        assertEquals(actualValues.get("date"), lastTopUpDate.getMillis());
    }
}
