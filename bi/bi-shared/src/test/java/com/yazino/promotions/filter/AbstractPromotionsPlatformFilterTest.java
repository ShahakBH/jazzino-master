package com.yazino.promotions.filter;

import com.yazino.platform.Platform;
import helper.BuyChipsPromotionTestBuilder;
import strata.server.lobby.api.promotion.Promotion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AbstractPromotionsPlatformFilterTest {

    Promotion buyChipsPromotionForWeb =
            new BuyChipsPromotionTestBuilder().withPlatforms(Arrays.asList(Platform.WEB)).build();
    Promotion buyChipsPromotionForIos =
            new BuyChipsPromotionTestBuilder().withPlatforms(Arrays.asList(Platform.IOS)).build();
    Promotion buyChipsPromotionForWebAndIos =
            new BuyChipsPromotionTestBuilder().withPlatforms(Arrays.asList(Platform.WEB, Platform.IOS)).build();

    protected List<Promotion> createPromotionList() {
        List<Promotion> promotions = new ArrayList<Promotion>();
        promotions.add(buyChipsPromotionForWeb);
        promotions.add(buyChipsPromotionForIos);
        promotions.add(buyChipsPromotionForWebAndIos);
        return promotions;
    }
}
