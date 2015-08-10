package com.yazino.promotions.filter;

import com.google.common.collect.Collections2;
import helper.DailyAwardPromotionBuilder;
import org.junit.Test;
import strata.server.lobby.api.promotion.Promotion;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class NoRewardChipsFilterTest {
    @Test
    public void shouldFilterPromotionsWithNoMaximumAwards() {
        // GIVEN promotions with no reward chips
        Promotion dailyAwardWithReward = new DailyAwardPromotionBuilder().build();
        Promotion dailyAwardWithoutReward = new DailyAwardPromotionBuilder().withReward(null).build();
        List<Promotion> promotions = new ArrayList<Promotion>();
        promotions.add(dailyAwardWithoutReward);
        promotions.add(dailyAwardWithReward);

        // WHEN filtering
        promotions = newArrayList(Collections2.filter(promotions, new NoRewardChipsFilter()));

        // THEN promotions without max rewards should be removed
        assertThat(promotions, hasItem(dailyAwardWithReward));
        assertThat(promotions, not(hasItem(dailyAwardWithoutReward)));
    }
}
