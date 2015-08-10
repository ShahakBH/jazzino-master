package com.yazino.web.payment.googlecheckout;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yazino.web.payment.googlecheckout.AndroidBuyChipStoreConfig.ChipBundleKeys.DEFAULT_VALUE;
import static org.junit.Assert.assertEquals;

public class AndroidBuyChipStoreConfigTest {
    private AndroidBuyChipStoreConfig underTest;

    @Before
    public void init() {
        underTest = new AndroidBuyChipStoreConfig();
    }

    @Test
    public void addToChipBundleListShouldAddChipBundlesToList() {
        final HashMap<AndroidBuyChipStoreConfig.ChipBundleKeys, Object> chipBundle = new HashMap<AndroidBuyChipStoreConfig.ChipBundleKeys, Object>();
        chipBundle.put(DEFAULT_VALUE, new BigDecimal(5000));

        underTest.addToChipBundleList(chipBundle);

        final List<Map<AndroidBuyChipStoreConfig.ChipBundleKeys, Object>> expectedChipBundleList
                = new ArrayList<Map<AndroidBuyChipStoreConfig.ChipBundleKeys, Object>>();

        expectedChipBundleList.add(chipBundle);

        assertEquals(expectedChipBundleList, underTest.getChipBundleList());
    }
}
