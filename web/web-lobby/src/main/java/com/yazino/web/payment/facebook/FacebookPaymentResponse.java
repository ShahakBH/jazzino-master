package com.yazino.web.payment.facebook;

import com.yazino.web.util.JsonHelper;

import java.math.BigDecimal;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;

@SuppressWarnings("UnusedDeclaration")
public class FacebookPaymentResponse {


    public String getMethod() {
        return method;
    }

    public Object[] getContent() {
        return new Object[]{content};
    }

    private final String method;

    private final Map<String, Object> content;

    public FacebookPaymentResponse(final String title,
                                   final String description,
                                   final Integer priceInCredits,
                                   final String method,
                                   final String imageUrl,
                                   final String productUrl,
                                   final String gameType,
                                   final String itemId,
                                   final Long promoId,
                                   final BigDecimal playerId,
                                   final String currencyCode,
                                   final BigDecimal amountPaidInCurrency) {
        this.content = newHashMap();
        content.put("title", title);
        content.put("description", description);
        content.put("price", priceInCredits);
        content.put("image_url", imageUrl);
        content.put("product_url", productUrl);
        String promoIdString = promoId == null ? "" : format(",\"promo_id\":\"%s\"", promoId);
        content.put("data", format("{\"player_id\":\"%s\",\"game_type\":\"%s\",\"item_id\":\"%s\"%s,\"currency_code\":\"%s\",\"amount_paid_in_currency\":\"%s\"}",
                        playerId.toPlainString(), gameType, itemId, promoIdString, currencyCode, amountPaidInCurrency.toPlainString()));
        this.method = method;
    }

    public String toJSON() {
        return new JsonHelper().serialize(this);
    }

}
