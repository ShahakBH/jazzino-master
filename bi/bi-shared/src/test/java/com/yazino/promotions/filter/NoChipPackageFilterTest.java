package com.yazino.promotions.filter;

import com.google.common.collect.Collections2;
import com.yazino.platform.Platform;
import helper.BuyChipsPromotionTestBuilder;
import org.junit.Test;
import strata.server.lobby.api.promotion.Promotion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class NoChipPackageFilterTest {
    @Test
    public void shouldFilterPromotionsWithOverriddenCheckPackages() {
        // GIVEN promotions with and without max rewards
        Promotion buyChipsWithChipPackage = new BuyChipsPromotionTestBuilder()
                .build();
        Promotion buyChipsWithoutChipPackage = new BuyChipsPromotionTestBuilder()
                .withDefaultChipsForPlatformAndPackage(new BigDecimal(BuyChipsPromotionTestBuilder.CHIP_DEFAULT_PACKAGE_VALUE), Platform.WEB)
                .build();
        List<Promotion> promotions = new ArrayList<Promotion>();
        promotions.add(buyChipsWithChipPackage);
        promotions.add(buyChipsWithoutChipPackage);

        // WHEN filtering
        promotions = newArrayList(Collections2.filter(promotions, new NoChipPackageFilter()));

        // THEN promotions without max rewards should be removed
        assertThat(promotions, hasItem(buyChipsWithChipPackage));
        assertThat(promotions, not(hasItem(buyChipsWithoutChipPackage)));
    }
}
