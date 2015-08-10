package com.yazino.promotions.filter;

import com.google.common.collect.Collections2;
import helper.BuyChipsPromotionTestBuilder;
import helper.DailyAwardPromotionBuilder;
import org.junit.Test;
import strata.server.lobby.api.promotion.Promotion;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class NoMaximumAwardsFilterTest {

    @Test
    public void shouldFilterPromotionsWithNoMaximumAwards() {
        // GIVEN promotions with and without max rewards
        Promotion dailyAwardWithMaxAwards = new DailyAwardPromotionBuilder().build();
        Promotion buyChipsWithMaxAwards = new BuyChipsPromotionTestBuilder().build();
        Promotion dailyAwardWithoutMaxAwards = new DailyAwardPromotionBuilder().withMaxRewards(null).build();
        Promotion buyChipsWithoutMaxAwards = new BuyChipsPromotionTestBuilder().withMaxRewards(null).build();
        List<Promotion> promotions = new ArrayList<Promotion>();
        promotions.add(dailyAwardWithMaxAwards);
        promotions.add(dailyAwardWithoutMaxAwards);
        promotions.add(buyChipsWithMaxAwards);
        promotions.add(buyChipsWithoutMaxAwards);

        // WHEN filtering
        promotions = newArrayList(Collections2.filter(promotions, new NoMaximumAwardsFilter()));

        // THEN promotions without max rewards should be removed
        assertThat(promotions, hasItems(dailyAwardWithMaxAwards, buyChipsWithMaxAwards));
        assertThat(promotions, not(hasItems(dailyAwardWithoutMaxAwards, buyChipsWithoutMaxAwards)));
    }
}
