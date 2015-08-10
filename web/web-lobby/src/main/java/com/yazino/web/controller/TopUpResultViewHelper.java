package com.yazino.web.controller;


import com.yazino.web.util.JsonHelper;
import org.springframework.stereotype.Component;
import strata.server.lobby.api.promotion.MobileTopUpResult;
import strata.server.lobby.api.promotion.TopUpResult;
import strata.server.lobby.api.promotion.WebTopUpResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TopUpResultViewHelper {

    public String serialiseAsJson(TopUpResult topUpResult) {
        if (topUpResult == null) {
            return "{}";
        }
        Map<String, Object> fieldsToBeSerialised = new HashMap<>();

        fieldsToBeSerialised.put("status", topUpResult.getStatus());

        if (topUpResult instanceof WebTopUpResult) {
            addWebTopUpFields(fieldsToBeSerialised, (WebTopUpResult) topUpResult);
        }

        if (topUpResult instanceof MobileTopUpResult) {
            fieldsToBeSerialised.put("date", topUpResult.getLastTopUpDate().getMillis());
            fieldsToBeSerialised.put("imageUrl", ((MobileTopUpResult) topUpResult).getImageUrl());
            fieldsToBeSerialised.put("totalAmount", topUpResult.getTotalTopUpAmount());
        }

        return new JsonHelper().serialize(fieldsToBeSerialised);
    }

    private Map<String, Object> addWebTopUpFields(Map<String, Object> fieldsToBeSerialised,
                                                  WebTopUpResult webTopUpResult) {
        final Map<String, Object> topUpDetails = new HashMap<>();
        fieldsToBeSerialised.put("topUpDetails", topUpDetails);
        topUpDetails.put("totalAmount", webTopUpResult.getTotalTopUpAmount());
        topUpDetails.put("date", webTopUpResult.getLastTopUpDate().getMillis());

        final Map<String, Object> progressive = new HashMap<>();
        topUpDetails.put("progressive", progressive);
        progressive.put("amounts", webTopUpResult.getPromotionValueList());
        progressive.put("consecutiveDaysPlayed", webTopUpResult.getConsecutiveDaysPlayed());

        final List<Map<String, String>> articles = new ArrayList<>();
        fieldsToBeSerialised.put("articles", articles);

        articles.add(createArticleMap(webTopUpResult.getMainImage(), webTopUpResult.getMainImageLink()));
        articles.add(createArticleMap(webTopUpResult.getSecondaryImage(), webTopUpResult.getSecondaryImageLink()));

        return fieldsToBeSerialised;
    }

    private Map<String, String> createArticleMap(String image, String link) {
        final Map<String, String> article = new HashMap<>();
        article.put("imageUrl", image);
        article.put("imageLink", link);

        return article;
    }
}
