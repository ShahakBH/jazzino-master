package strata.server.lobby.promotion.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import com.yazino.promotions.PromotionPlayerReward;

public class PromotionPlayerRewardIsEqual extends TypeSafeMatcher<PromotionPlayerReward> {
    private PromotionPlayerReward promotionPlayerReward;

    public PromotionPlayerRewardIsEqual(PromotionPlayerReward promotionPlayerReward) {
        this.promotionPlayerReward = promotionPlayerReward;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(promotionPlayerReward.toString());
    }

    @Override
    protected boolean matchesSafely(PromotionPlayerReward other) {
        return org.apache.commons.lang.ObjectUtils.equals(promotionPlayerReward.getPromoId(), other.getPromoId())
                && org.apache.commons.lang.ObjectUtils.equals(promotionPlayerReward.getPlayerId(), other.getPlayerId())
                && org.apache.commons.lang.ObjectUtils.equals(promotionPlayerReward.getRewardDate(), other.getRewardDate())
                && promotionPlayerReward.isControlGroup() == other.isControlGroup()
                && org.apache.commons.lang.ObjectUtils.equals(promotionPlayerReward.getDetails(), other.getDetails());
    }

    public static <T> TypeSafeMatcher<PromotionPlayerReward> equalTo(PromotionPlayerReward promotionPlayerReward) {
        return new PromotionPlayerRewardIsEqual(promotionPlayerReward);
    }
}

