package com.yazino.web.payment.googlecheckout;

import com.yazino.web.payment.chipbundle.ChipBundle;
import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class AndroidProductsSpringContextTest {

    @Autowired
    private ChipBundleResolver underTest;

    @Test
    public void contextHasExpectedProducts() {
        Map<String, LinkedHashMap<String, ChipBundle>> expectedProducts = setUpExpectedProducts();

        for (String gameType : expectedProducts.keySet()) {
            assertThat("Actual number of Chip packages does not match the Expected Number of Chip Packages for " + gameType,
                       underTest.getProductIdsFor(gameType).size(),
                       is(expectedProducts.get(gameType).size()));
            LinkedHashMap<String, ChipBundle> expectedChipBundleMap = expectedProducts.get(gameType);
            for (String chipValueId : expectedChipBundleMap.keySet()) {
                final ChipBundle actualChipBundle = underTest.findChipBundleFor(gameType, chipValueId);
                assertThat(actualChipBundle, is(expectedChipBundleMap.get(chipValueId)));
            }
        }
    }

    private Map<String, LinkedHashMap<String, ChipBundle>> setUpExpectedProducts() {
        Map<String, LinkedHashMap<String, ChipBundle>> expectedProducts = new HashMap<String, LinkedHashMap<String, ChipBundle>>();
        Currency currency = Currency.getInstance("USD");
        // SLOTS and BLACKJACK have identical products bar product id prefix
        String[] gameTypes = {"SLOTS", "BLACKJACK"};
        for (String gameType : gameTypes) {
            LinkedHashMap<String, ChipBundle> slotsProducts = new LinkedHashMap<String, ChipBundle>();
            expectedProducts.put(gameType, slotsProducts);
            slotsProducts.put("5000",
                              new ChipBundle(gameType.toLowerCase() + "_usd3_buys_5k",
                                             BigDecimal.valueOf(5000),
                                             BigDecimal.valueOf(5000),
                                             BigDecimal.valueOf(2.99),
                                             currency));
            slotsProducts.put("5000-7500",
                              new ChipBundle(gameType.toLowerCase() + "_usd3_buys_7.5k_p50",
                                             BigDecimal.valueOf(7500),
                                             BigDecimal.valueOf(5000),
                                             BigDecimal.valueOf(2.99),
                                             currency));
            slotsProducts.put("5000-10000",
                              new ChipBundle(gameType.toLowerCase() + "_usd3_buys_10k_p100",
                                             BigDecimal.valueOf(10000),
                                             BigDecimal.valueOf(5000),
                                             BigDecimal.valueOf(2.99),
                                             currency));
            slotsProducts.put("5000-12500",
                              new ChipBundle(gameType.toLowerCase() + "_usd3_buys_12.5k_p150",
                                             BigDecimal.valueOf(12500),
                                             BigDecimal.valueOf(5000),
                                             BigDecimal.valueOf(2.99),
                                             currency));
            slotsProducts.put("5000-15000",
                              new ChipBundle(gameType.toLowerCase() + "_usd3_buys_15k_p200",
                                             BigDecimal.valueOf(15000),
                                             BigDecimal.valueOf(5000),
                                             BigDecimal.valueOf(2.99),
                                             currency));
            slotsProducts.put("15000",
                              new ChipBundle(gameType.toLowerCase() + "_usd8_buys_15k",
                                             BigDecimal.valueOf(15000),
                                             BigDecimal.valueOf(15000),
                                             BigDecimal.valueOf(7.99),
                                             currency));
            slotsProducts.put("15000-22500",
                              new ChipBundle(gameType.toLowerCase() + "_usd8_buys_22.5k_p50",
                                             BigDecimal.valueOf(22500),
                                             BigDecimal.valueOf(15000),
                                             BigDecimal.valueOf(7.99),
                                             currency));
            slotsProducts.put("15000-37500",
                              new ChipBundle(gameType.toLowerCase() + "_usd8_buys_37.5k_p150",
                                             BigDecimal.valueOf(37500),
                                             BigDecimal.valueOf(15000),
                                             BigDecimal.valueOf(7.99),
                                             currency));
            slotsProducts.put("15000-30000",
                              new ChipBundle(gameType.toLowerCase() + "_usd8_buys_30k_p100",
                                             BigDecimal.valueOf(30000),
                                             BigDecimal.valueOf(15000),
                                             BigDecimal.valueOf(7.99),
                                             currency));
            slotsProducts.put("15000-45000",
                              new ChipBundle(gameType.toLowerCase() + "_usd8_buys_45k_p200",
                                             BigDecimal.valueOf(45000),
                                             BigDecimal.valueOf(15000),
                                             BigDecimal.valueOf(7.99),
                                             currency));
            slotsProducts.put("30000",
                              new ChipBundle(gameType.toLowerCase() + "_usd15_buys_30k",
                                             BigDecimal.valueOf(30000),
                                             BigDecimal.valueOf(30000),
                                             BigDecimal.valueOf(14.99),
                                             currency));
            slotsProducts.put("30000-45000",
                              new ChipBundle(gameType.toLowerCase() + "_usd15_buys_45k_p50",
                                             BigDecimal.valueOf(45000),
                                             BigDecimal.valueOf(30000),
                                             BigDecimal.valueOf(14.99),
                                             currency));
            slotsProducts.put("30000-60000",
                              new ChipBundle(gameType.toLowerCase() + "_usd15_buys_60k_p100",
                                             BigDecimal.valueOf(60000),
                                             BigDecimal.valueOf(30000),
                                             BigDecimal.valueOf(14.99),
                                             currency));
            slotsProducts.put("30000-75000",
                              new ChipBundle(gameType.toLowerCase() + "_usd15_buys_75k_p150",
                                             BigDecimal.valueOf(75000),
                                             BigDecimal.valueOf(30000),
                                             BigDecimal.valueOf(14.99),
                                             currency));
            slotsProducts.put("30000-90000",
                              new ChipBundle(gameType.toLowerCase() + "_usd15_buys_90k_p200",
                                             BigDecimal.valueOf(90000),
                                             BigDecimal.valueOf(30000),
                                             BigDecimal.valueOf(14.99),
                                             currency));
            slotsProducts.put("70000",
                              new ChipBundle(gameType.toLowerCase() + "_usd30_buys_70k",
                                             BigDecimal.valueOf(70000),
                                             BigDecimal.valueOf(70000),
                                             BigDecimal.valueOf(29.99),
                                             currency));
            slotsProducts.put("70000-105000",
                              new ChipBundle(gameType.toLowerCase() + "_usd30_buys_105k_p50",
                                             BigDecimal.valueOf(105000),
                                             BigDecimal.valueOf(70000),
                                             BigDecimal.valueOf(29.99),
                                             currency));
            slotsProducts.put("70000-140000",
                              new ChipBundle(gameType.toLowerCase() + "_usd30_buys_140k_p100",
                                             BigDecimal.valueOf(140000),
                                             BigDecimal.valueOf(70000),
                                             BigDecimal.valueOf(29.99),
                                             currency));
            slotsProducts.put("70000-175000",
                              new ChipBundle(gameType.toLowerCase() + "_usd30_buys_175k_p150",
                                             BigDecimal.valueOf(175000),
                                             BigDecimal.valueOf(70000),
                                             BigDecimal.valueOf(29.99),
                                             currency));
            slotsProducts.put("70000-210000",
                              new ChipBundle(gameType.toLowerCase() + "_usd30_buys_210k_p200",
                                             BigDecimal.valueOf(210000),
                                             BigDecimal.valueOf(70000),
                                             BigDecimal.valueOf(29.99),
                                             currency));
            slotsProducts.put("200000",
                              new ChipBundle(gameType.toLowerCase() + "_usd70_buys_200k",
                                             BigDecimal.valueOf(200000),
                                             BigDecimal.valueOf(200000),
                                             BigDecimal.valueOf(69.99),
                                             currency));
            slotsProducts.put("200000-300000",
                              new ChipBundle(gameType.toLowerCase() + "_usd70_buys_300k_p50",
                                             BigDecimal.valueOf(300000),
                                             BigDecimal.valueOf(200000),
                                             BigDecimal.valueOf(69.99),
                                             currency));
            slotsProducts.put("200000-400000",
                              new ChipBundle(gameType.toLowerCase() + "_usd70_buys_400k_p100",
                                             BigDecimal.valueOf(400000),
                                             BigDecimal.valueOf(200000),
                                             BigDecimal.valueOf(69.99),
                                             currency));
            slotsProducts.put("200000-500000",
                              new ChipBundle(gameType.toLowerCase() + "_usd70_buys_500k_p150",
                                             BigDecimal.valueOf(500000),
                                             BigDecimal.valueOf(200000),
                                             BigDecimal.valueOf(69.99),
                                             currency));
            slotsProducts.put("200000-600000",
                              new ChipBundle(gameType.toLowerCase() + "_usd70_buys_600k_p200",
                                             BigDecimal.valueOf(600000),
                                             BigDecimal.valueOf(200000),
                                             BigDecimal.valueOf(69.99),
                                             currency));
            slotsProducts.put("300000",
                              new ChipBundle(gameType.toLowerCase() + "_usd90_buys_300k",
                                             BigDecimal.valueOf(300000),
                                             BigDecimal.valueOf(300000),
                                             BigDecimal.valueOf(89.99),
                                             currency));
            slotsProducts.put("300000-450000",
                              new ChipBundle(gameType.toLowerCase() + "_usd90_buys_450k_p50",
                                             BigDecimal.valueOf(450000),
                                             BigDecimal.valueOf(300000),
                                             BigDecimal.valueOf(89.99),
                                             currency));
            slotsProducts.put("300000-600000",
                              new ChipBundle(gameType.toLowerCase() + "_usd90_buys_600k_p100",
                                             BigDecimal.valueOf(600000),
                                             BigDecimal.valueOf(300000),
                                             BigDecimal.valueOf(89.99),
                                             currency));
            slotsProducts.put("300000-750000",
                              new ChipBundle(gameType.toLowerCase() + "_usd90_buys_750k_p150",
                                             BigDecimal.valueOf(750000),
                                             BigDecimal.valueOf(300000),
                                             BigDecimal.valueOf(89.99),
                                             currency));
            slotsProducts.put("300000-900000",
                              new ChipBundle(gameType.toLowerCase() + "_usd90_buys_900k_p200",
                                             BigDecimal.valueOf(900000),
                                             BigDecimal.valueOf(300000),
                                             BigDecimal.valueOf(89.99),
                                             currency));
        }
        LinkedHashMap<String, ChipBundle> texasProducts = new LinkedHashMap<String, ChipBundle>();
        expectedProducts.put("TEXAS_HOLDEM", texasProducts);
        texasProducts.put("5000",
                          new ChipBundle("texasholdem_usd3_buys_5k", BigDecimal.valueOf(5000), BigDecimal.valueOf(5000), BigDecimal.valueOf(3), currency));
        texasProducts.put("5000-7500",
                          new ChipBundle("texasholdem_usd3_buys_7.5k_p50",
                                         BigDecimal.valueOf(7500),
                                         BigDecimal.valueOf(5000),
                                         BigDecimal.valueOf(3),
                                         currency));
        texasProducts.put("5000-10000",
                          new ChipBundle("texasholdem_usd3_buys_10k_p100",
                                         BigDecimal.valueOf(10000),
                                         BigDecimal.valueOf(5000),
                                         BigDecimal.valueOf(3),
                                         currency));
        texasProducts.put("5000-12500",
                          new ChipBundle("texasholdem_usd3_buys_12.5k_p150",
                                         BigDecimal.valueOf(12500),
                                         BigDecimal.valueOf(5000),
                                         BigDecimal.valueOf(3),
                                         currency));
        texasProducts.put("5000-15000",
                          new ChipBundle("texasholdem_usd3_buys_15k_p200",
                                         BigDecimal.valueOf(15000),
                                         BigDecimal.valueOf(5000),
                                         BigDecimal.valueOf(3),
                                         currency));
        texasProducts.put("15000",
                          new ChipBundle("texasholdem_usd8_buys_15k", BigDecimal.valueOf(15000), BigDecimal.valueOf(15000), BigDecimal.valueOf(8), currency));
        texasProducts.put("15000-22500",
                          new ChipBundle("texasholdem_usd8_buys_22.5k_p50",
                                         BigDecimal.valueOf(22500),
                                         BigDecimal.valueOf(15000),
                                         BigDecimal.valueOf(8),
                                         currency));
        texasProducts.put("15000-30000",
                          new ChipBundle("texasholdem_usd8_buys_30k_p100",
                                         BigDecimal.valueOf(30000),
                                         BigDecimal.valueOf(15000),
                                         BigDecimal.valueOf(8),
                                         currency));
        texasProducts.put("15000-37500",
                          new ChipBundle("texasholdem_usd8_buys_37.5k_p150",
                                         BigDecimal.valueOf(37500),
                                         BigDecimal.valueOf(15000),
                                         BigDecimal.valueOf(8),
                                         currency));
        texasProducts.put("15000-45000",
                          new ChipBundle("texasholdem_usd8_buys_45k_p200",
                                         BigDecimal.valueOf(45000),
                                         BigDecimal.valueOf(15000),
                                         BigDecimal.valueOf(8),
                                         currency));
        texasProducts.put("30000",
                          new ChipBundle("texasholdem_usd15_buys_30k", BigDecimal.valueOf(30000), BigDecimal.valueOf(30000), BigDecimal.valueOf(15), currency));
        texasProducts.put("30000-45000",
                          new ChipBundle("texasholdem_usd15_buys_45k_p50",
                                         BigDecimal.valueOf(45000),
                                         BigDecimal.valueOf(30000),
                                         BigDecimal.valueOf(15),
                                         currency));
        texasProducts.put("30000-60000",
                          new ChipBundle("texasholdem_usd15_buys_60k_p100",
                                         BigDecimal.valueOf(60000),
                                         BigDecimal.valueOf(30000),
                                         BigDecimal.valueOf(15),
                                         currency));
        texasProducts.put("30000-75000",
                          new ChipBundle("texasholdem_usd15_buys_75k_p150",
                                         BigDecimal.valueOf(75000),
                                         BigDecimal.valueOf(30000),
                                         BigDecimal.valueOf(15),
                                         currency));
        texasProducts.put("30000-90000",
                          new ChipBundle("texasholdem_usd15_buys_90k_p200",
                                         BigDecimal.valueOf(90000),
                                         BigDecimal.valueOf(30000),
                                         BigDecimal.valueOf(15),
                                         currency));
        texasProducts.put("70000",
                          new ChipBundle("texasholdem_usd30_buys_70k", BigDecimal.valueOf(70000), BigDecimal.valueOf(70000), BigDecimal.valueOf(30), currency));
        texasProducts.put("70000-105000",
                          new ChipBundle("texasholdem_usd30_buys_105k_p50",
                                         BigDecimal.valueOf(105000),
                                         BigDecimal.valueOf(70000),
                                         BigDecimal.valueOf(30),
                                         currency));
        texasProducts.put("70000-140000",
                          new ChipBundle("texasholdem_usd30_buys_140k_p100",
                                         BigDecimal.valueOf(140000),
                                         BigDecimal.valueOf(70000),
                                         BigDecimal.valueOf(30),
                                         currency));
        texasProducts.put("70000-175000",
                          new ChipBundle("texasholdem_usd30_buys_175k_p150",
                                         BigDecimal.valueOf(175000),
                                         BigDecimal.valueOf(70000),
                                         BigDecimal.valueOf(30),
                                         currency));
        texasProducts.put("70000-210000",
                          new ChipBundle("texasholdem_usd30_buys_210k_p200",
                                         BigDecimal.valueOf(210000),
                                         BigDecimal.valueOf(70000),
                                         BigDecimal.valueOf(30),
                                         currency));
        texasProducts.put("200000",
                          new ChipBundle("texasholdem_usd70_buys_200k",
                                         BigDecimal.valueOf(200000),
                                         BigDecimal.valueOf(200000),
                                         BigDecimal.valueOf(70),
                                         currency));
        texasProducts.put("200000-300000",
                          new ChipBundle("texasholdem_usd70_buys_300k_p50",
                                         BigDecimal.valueOf(300000),
                                         BigDecimal.valueOf(200000),
                                         BigDecimal.valueOf(70),
                                         currency));
        texasProducts.put("200000-400000",
                          new ChipBundle("texasholdem_usd70_buys_400k_p100",
                                         BigDecimal.valueOf(400000),
                                         BigDecimal.valueOf(200000),
                                         BigDecimal.valueOf(70),
                                         currency));
        texasProducts.put("200000-500000",
                          new ChipBundle("texasholdem_usd70_buys_500k_p150",
                                         BigDecimal.valueOf(500000),
                                         BigDecimal.valueOf(200000),
                                         BigDecimal.valueOf(70),
                                         currency));
        texasProducts.put("200000-600000",
                          new ChipBundle("texasholdem_usd70_buys_600k_p200",
                                         BigDecimal.valueOf(600000),
                                         BigDecimal.valueOf(200000),
                                         BigDecimal.valueOf(70),
                                         currency));
        texasProducts.put("300000",
                          new ChipBundle("texasholdem_usd90_buys_300k",
                                         BigDecimal.valueOf(300000),
                                         BigDecimal.valueOf(300000),
                                         BigDecimal.valueOf(90),
                                         currency));
        texasProducts.put("300000-450000",
                          new ChipBundle("texasholdem_usd90_buys_450k_p50",
                                         BigDecimal.valueOf(450000),
                                         BigDecimal.valueOf(300000),
                                         BigDecimal.valueOf(90),
                                         currency));
        texasProducts.put("300000-600000",
                          new ChipBundle("texasholdem_usd90_buys_600k_p100",
                                         BigDecimal.valueOf(600000),
                                         BigDecimal.valueOf(300000),
                                         BigDecimal.valueOf(90),
                                         currency));
        texasProducts.put("300000-750000",
                          new ChipBundle("texasholdem_usd90_buys_750k_p150",
                                         BigDecimal.valueOf(750000),
                                         BigDecimal.valueOf(300000),
                                         BigDecimal.valueOf(90),
                                         currency));
        texasProducts.put("300000-900000",
                          new ChipBundle("texasholdem_usd90_buys_900k_p200",
                                         BigDecimal.valueOf(900000),
                                         BigDecimal.valueOf(300000),
                                         BigDecimal.valueOf(90),
                                         currency));

        return expectedProducts;
    }
}

