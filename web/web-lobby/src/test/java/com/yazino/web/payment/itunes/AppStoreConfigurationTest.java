package com.yazino.web.payment.itunes;

import com.yazino.mobile.yaps.config.TypedMapBean;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class AppStoreConfigurationTest {

    private final AppStoreConfiguration mConfiguration = new AppStoreConfiguration();


    @Test
    public void shouldReturnEmptyStandardChipPackagesForNonExistingGame() throws Exception {
        Map<String, String> productMappings = AppStoreTestUtils.toMap("ABC", "FOO");
        Map<String, Map<String, String>> gameMappings = AppStoreTestUtils.toMap("TEST", productMappings);
        mConfiguration.setStandardPackageMappings(gameMappings);
        mConfiguration.afterPropertiesSet();

        assertTrue(mConfiguration.productIdentifierMappingsForGame("FOO").isEmpty());
    }

    @Test
    public void shouldReturnEmptyPromotionChipPackagesForNonExistingGame() throws Exception {
        Map<String, String> productMappings = AppStoreTestUtils.toMap("ABC", "FOO");
        Map<String, Map<String, String>> gameMappings = AppStoreTestUtils.toMap("TEST", productMappings);
        mConfiguration.setPromotionPackageMappings(gameMappings);
        mConfiguration.afterPropertiesSet();

        assertTrue(mConfiguration.productIdentifierMappingsForGame("FOO").isEmpty());
    }

    @Test
    public void shouldReturnCorrectStandardChipPackagesForGame() throws Exception {
        Map<String, String> fooMappings = AppStoreTestUtils.toMap("ABC", "FOO");
        Map<String, Map<String, String>> packageMappings = AppStoreTestUtils.toMap("TEST", fooMappings);
        mConfiguration.setStandardPackageMappings(packageMappings);
        mConfiguration.afterPropertiesSet();

        assertEquals(fooMappings, mConfiguration.productIdentifierMappingsForGame("TEST"));
    }

    @Test
    public void shouldReturnCorrectPromotionChipPackageForGame() throws Exception {
        Map<String, String> fooMappings = AppStoreTestUtils.toMap("ABC", "FOO");
        Map<String, Map<String, String>> packageMappings = AppStoreTestUtils.toMap("TEST", fooMappings);
        mConfiguration.setPromotionPackageMappings(packageMappings);
        mConfiguration.afterPropertiesSet();

        assertEquals(fooMappings, mConfiguration.productIdentifierMappingsForGame("TEST"));
    }

    @Test
    public void shouldMapStandardInternalIdentifierCorrectly() throws Exception {
        Map<String, String> fooMappings = AppStoreTestUtils.toMap("ABC", "FOO");
        Map<String, Map<String, String>> packageMappings = AppStoreTestUtils.toMap("TEST", fooMappings);
        mConfiguration.setStandardPackageMappings(packageMappings);
        mConfiguration.afterPropertiesSet();

        String actual = mConfiguration.findInternalIdentifier("TEST", "FOO");
        assertEquals("ABC", actual);
    }

    @Test
    public void shouldMapPromotionInternalIdentifierCorrectly() throws Exception {
        Map<String, String> fooMappings = AppStoreTestUtils.toMap("ABC", "FOO");
        Map<String, Map<String, String>> packageMappings = AppStoreTestUtils.toMap("TEST", fooMappings);
        mConfiguration.setPromotionPackageMappings(packageMappings);
        mConfiguration.afterPropertiesSet();

        String actual = mConfiguration.findInternalIdentifier("TEST", "FOO");
        assertEquals("ABC", actual);
    }

    @Test
    public void shouldMapStandardAppleIdentifierCorrectly() throws Exception {
        Map<String, String> fooMappings = AppStoreTestUtils.toMap("ABC", "FOO");
        Map<String, Map<String, String>> packageMappings = AppStoreTestUtils.toMap("TEST", fooMappings);
        mConfiguration.setStandardPackageMappings(packageMappings);
        mConfiguration.afterPropertiesSet();

        String actual = mConfiguration.findAppleIdentifier("TEST", "ABC");
        assertEquals("FOO", actual);
    }

    @Test
    public void shouldMapPromotionAppleIdentifierCorrectly() throws Exception {
        Map<String, String> fooMappings = AppStoreTestUtils.toMap("ABC", "FOO");
        Map<String, Map<String, String>> packageMappings = AppStoreTestUtils.toMap("TEST", fooMappings);
        mConfiguration.setPromotionPackageMappings(packageMappings);
        mConfiguration.afterPropertiesSet();

        String actual = mConfiguration.findAppleIdentifier("TEST", "ABC");
        assertEquals("FOO", actual);
    }

    @Test
    public void shouldReturnNullAppleIdentifierWhenNoMappingsForGame() throws Exception {
        mConfiguration.afterPropertiesSet();

        assertNull(mConfiguration.findAppleIdentifier("TEST","FOO"));
    }

    @Test
    public void shouldReturnNullInternalIdentifierWhenNoMappingsForGame() throws Exception {
        mConfiguration.afterPropertiesSet();
        assertNull(mConfiguration.findInternalIdentifier("TEST", "FOO"));
    }

    @Test
    public void shouldReturnNullWhenNoInternalIdentifierForAppleIdentifier() throws Exception {
        Map<String, String> fooMappings = AppStoreTestUtils.toMap("ABC", "FOO");
        Map<String, Map<String, String>> packageMappings = AppStoreTestUtils.toMap("TEST", fooMappings);
        mConfiguration.setStandardPackageMappings(packageMappings);
        mConfiguration.afterPropertiesSet();

        assertNull(mConfiguration.findInternalIdentifier("TEST", "BAR"));
    }

    @Test
    public void shouldReturnEmptyStandardProductsSetWhenNoneConfigured() throws Exception {
        mConfiguration.setStandardPackageMappings(Collections.<String, Map<String, String>>emptyMap());
        mConfiguration.afterPropertiesSet();
        assertTrue(mConfiguration.standardProductsForGame("FOO").isEmpty());
    }

    @Test
    public void shouldReturnEmptyPromotionProductsSetWhenNoneConfigured() throws Exception {
        mConfiguration.setPromotionPackageMappings(Collections.<String, Map<String, String>>emptyMap());
        mConfiguration.afterPropertiesSet();
        assertTrue(mConfiguration.promotionProductsForGame("FOO").isEmpty());
    }

    @Test
    public void shouldLookupCorrectGameTypeWhenNoMapping() throws Exception {
        mConfiguration.setGameBundleMappings(new TypedMapBean<String, String>(AppStoreTestUtils.toMap("com.yazino.WheelDeal", "SLOTS", "yazino.WheelDeal", "SLOTS")));
        assertEquals("BLACKJACK", mConfiguration.lookupGameType("BLACKJACK"));
    }

    @Test
    public void shouldLookupCorrectGameTypeWhenMapping() throws Exception {
        mConfiguration.setGameBundleMappings(new TypedMapBean<String, String>(AppStoreTestUtils.toMap("com.yazino.WheelDeal", "SLOTS", "yazino.Blackjack", "BJ")));
        assertEquals("SLOTS", mConfiguration.lookupGameType("com.yazino.WheelDeal"));
        assertEquals("BJ", mConfiguration.lookupGameType("yazino.Blackjack"));
    }

}
