package com.yazino.web.payment.itunes;

import com.yazino.platform.community.PaymentPreferences;
import org.apache.commons.lang3.Validate;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class AppStoreTestUtils {

    public static <K,V> Map<K, V> toMap(K key, V value) {
        Map<K, V> map = new HashMap<K, V>(1);
        map.put(key, value);
        return map;
    }

    public static PaymentOption toOption(int cost, int standardChips, int promotionChips) {
        PaymentOption option = new PaymentOption();
        option.setId(String.format("IOS_USD%d", cost));
        option.setNumChipsPerPurchase(BigDecimal.valueOf(standardChips));
        option.setAmountRealMoneyPerPurchase(BigDecimal.valueOf(cost));
        option.setRealMoneyCurrency("USD");
        option.setCurrencyLabel("$");
        if (promotionChips > 0 && promotionChips != standardChips) {
            option.addPromotionPaymentOption(new PromotionPaymentOption(PaymentPreferences.PaymentMethod.ITUNES, 12L, BigDecimal.valueOf(promotionChips), "TestRolloverHeader", "TestRolloverText" ));
        }
        return option;
    }

    public static Map<String, String> toMap(String... pairs) {
        Validate.isTrue(pairs.length % 2 == 0);
        Map<String, String> map = new HashMap<String, String>();
        for (int i=0; i<pairs.length; i+=2) {
            String key = pairs[i];
            String value = pairs[i+1];
            Validate.isTrue(!map.containsKey(key));
            map.put(key, value);
        }
        return map;
    }


}
