package com.yazino.web.chipbundle;

import com.yazino.web.payment.chipbundle.ChipBundleResolver;
import com.yazino.web.payment.chipbundle.ChipBundle;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ChipBundleResolverTest {

    private static final Map<String, LinkedHashMap<String, ChipBundle>> KNOWN_PRODUCTS;

    public static final String GAME_TYPE = "TEXAS_HOLDEM";
    public static final String[] GOOGLE_PRODUCT_IDS = {"product1", "product2", "promotion1"};
    public static final String[] CHIP_VALUE_IDS = {"5000", "10000", "5000-10000"};
    public static final BigDecimal[] CHIPS = {BigDecimal.valueOf(5000), BigDecimal.valueOf(10000), BigDecimal.valueOf(10000)};
    public static final BigDecimal[] DEFAULT_CHIPS = {BigDecimal.valueOf(5000), BigDecimal.valueOf(10000), BigDecimal.valueOf(5000)};
    public static final BigDecimal[] PACKAGE_PRICES = {BigDecimal.valueOf(5), BigDecimal.valueOf(10), BigDecimal.valueOf(5)};
    public static final Currency CURRENCY = Currency.getInstance("USD");

    static {
        KNOWN_PRODUCTS = new HashMap<String, LinkedHashMap<String, ChipBundle>>();
        LinkedHashMap<String, ChipBundle> pokerProducts = new LinkedHashMap<String, ChipBundle>();
        for (int i = 0; i < GOOGLE_PRODUCT_IDS.length; i++) {
            pokerProducts.put(CHIP_VALUE_IDS[i], new ChipBundle(GOOGLE_PRODUCT_IDS[i], CHIPS[i], DEFAULT_CHIPS[i], PACKAGE_PRICES[i], CURRENCY));
            KNOWN_PRODUCTS.put(GAME_TYPE, pokerProducts);
        }
    }

    private ChipBundleResolver underTest;

    @Before
    public void setup() {
        underTest = new ChipBundleResolver();
        underTest.setChipBundles(KNOWN_PRODUCTS);
    }

    @SuppressWarnings("NullableProblems")
    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWhenRequestingProductIdsWithNullGameType() {
        underTest.getProductIdsFor(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenRequestingProductIdsWithEmptyGameType() {
        underTest.getProductIdsFor("");
    }

    @Test
    public void shouldReturnEmptyListWhenNoProductsFoundForGameType() {
        final List<String> productIds = underTest.getProductIdsFor("GAME_WITHOUT_PRODUCTS");

        assertTrue(productIds.isEmpty());
    }

    @Test
    public void shouldReturnProductIdsForKnownGameType() {
        final List<String> productIds = underTest.getProductIdsFor(GAME_TYPE);

        assertThat(productIds.size(), is(GOOGLE_PRODUCT_IDS.length));
        assertThat(productIds, hasItems(GOOGLE_PRODUCT_IDS));
    }

    @Test
    public void shouldFindChipBundleForGivenGameTypeAndProductId() {
        final ChipBundle chipBundle = underTest.findChipBundleForProductId(GAME_TYPE, GOOGLE_PRODUCT_IDS[1]);
        assertThat(chipBundle, is(KNOWN_PRODUCTS.get(GAME_TYPE).get(CHIP_VALUE_IDS[1])));
    }

    @Test
    public void shouldReturnNullWhenFindingChipBundleForUnknownProductId() {
        assertNull(underTest.findChipBundleForProductId(GAME_TYPE, "UNKNOWN_PRODUCT_ID"));
    }

    @Test
    public void shouldReturnNullWhenFindingChipBundleWithNullChipValueId() {
        assertNull(underTest.findChipBundleFor(GAME_TYPE, null));
    }

    @Test
    public void shouldReturnNullWhenFindingProductIdsWithUnknownChipValue() {
        assertNull(underTest.findChipBundleFor(GAME_TYPE, "4593532925273"));
    }

    @Test
    public void shouldReturnChipBundleWhenFindingWithChipValueId() {
        final ChipBundle chipBundle = underTest.findChipBundleFor(GAME_TYPE, CHIP_VALUE_IDS[0]);
        assertThat(chipBundle, is(KNOWN_PRODUCTS.get(GAME_TYPE).get(CHIP_VALUE_IDS[0])));
    }
}
