package strata.server.lobby.api.promotion.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.helper.DailyAwardPromotionBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DailyAwardPromotionComparatorTest {
    @Before
    public void init() {
        DateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
    }

    @After
    public void resetDateTime() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldOrderByHighestRewardThenHighestPriorityThenEarliestStartDate() {
        DateTime now = new DateTime();
        Promotion noPriorityStart1DayAgo = new DailyAwardPromotionBuilder().withPriority(null)
                .withStartDate(now.minusDays(1)).getPromotion();
        Promotion noPriorityStart1DayAgoWithHigherReward = new DailyAwardPromotionBuilder().withPriority(null)
                .withStartDate(now.minusDays(1))
                .withReward(Integer.valueOf(DailyAwardPromotionBuilder.REWARD_CHIPS_VALUE) + 1)
                .getPromotion();
        Promotion noPriorityStart2DaysAgo = new DailyAwardPromotionBuilder().withPriority(null)
                .withStartDate(now.minusDays(2)).getPromotion();
        Promotion priority1Start1HourAgo = new DailyAwardPromotionBuilder().withPriority(1)
                .withStartDate(now.minusHours(1)).getPromotion();
        Promotion priority1Start2HoursAgo = new DailyAwardPromotionBuilder().withPriority(1)
                .withStartDate(now.minusHours(2)).getPromotion();
        final List<Promotion> promotions = Arrays.asList(noPriorityStart1DayAgo, priority1Start1HourAgo, noPriorityStart2DaysAgo,
                priority1Start2HoursAgo, noPriorityStart1DayAgoWithHigherReward);

        Collections.sort(promotions, new DailyAwardPromotionComparator());

        assertThat(promotions, is(Arrays.asList(noPriorityStart1DayAgoWithHigherReward, priority1Start2HoursAgo, priority1Start1HourAgo,
                noPriorityStart2DaysAgo, noPriorityStart1DayAgo)));
    }
}
