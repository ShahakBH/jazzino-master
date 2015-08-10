package strata.server.lobby.api.promotion;

import java.math.BigDecimal;

import static strata.server.lobby.api.promotion.PromotionType.DAILY_AWARD;

public class DailyAwardPromotion extends Promotion {
    private static final long serialVersionUID = -7562759569804785668L;

    public static final String MAIN_IMAGE_KEY = "main.image";
    public static final String MAIN_IMAGE_LINK_KEY = "main.image.link";
    public static final String SECONDARY_IMAGE_KEY = "secondary.image";
    public static final String SECONDARY_IMAGE_LINK_KEY = "secondary.image.link";
    public static final String IOS_IMAGE_KEY = "ios.image";
    public static final String ANDROID_IMAGE_KEY = "android.image";

    public static final String REWARD_CHIPS_KEY = "reward.chips";
    public static final String MAX_REWARDS_KEY = "max.rewards";

    public DailyAwardPromotion() {
        this(DAILY_AWARD);
    }

    /* allow ProgressiveDailyAwardPromotion sub-class to override promotion type */
    public DailyAwardPromotion(PromotionType promotionType) {
        super(promotionType);
    }

    public boolean isDefaultPromotion() {
        return DAILY_AWARD.getDefaultPromotionName().equals(getName());
    }

    public BigDecimal getTopUpAmount() {
        return BigDecimal.valueOf(getConfiguration().getConfigurationValueAsInteger(REWARD_CHIPS_KEY));
    }
}
