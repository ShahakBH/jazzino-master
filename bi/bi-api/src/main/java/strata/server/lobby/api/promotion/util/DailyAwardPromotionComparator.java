package strata.server.lobby.api.promotion.util;

import strata.server.lobby.api.promotion.Promotion;

import java.util.Comparator;

import static strata.server.lobby.api.promotion.DailyAwardPromotion.REWARD_CHIPS_KEY;

/**
 * Comparator based on highest reward, highest priority then earliest start date
 */
public class DailyAwardPromotionComparator implements Comparator<Promotion> {
    private PromotionPriorityDateComparator priorityComparator = new PromotionPriorityDateComparator();

    @Override
    public int compare(final Promotion p1, final Promotion p2) {
        final int rewardChipsDiff = p2.getConfiguration().getConfigurationValueAsInteger(REWARD_CHIPS_KEY)
                - p1.getConfiguration().getConfigurationValueAsInteger(REWARD_CHIPS_KEY);
        if (rewardChipsDiff == 0) {
            return priorityComparator.compare(p1, p2);
        }
        return rewardChipsDiff;
    }
}
