package strata.server.lobby.api.promotion;

import org.joda.time.DateTime;

public class GiftingPromotion extends Promotion {

    public static final String MAX_REWARDS_KEY = "max.rewards";
    public static final String GIFT_TITLE = "gift.title";
    public static final String GIFT_DESCRIPTION = "gift.description";
    public static final String GIFT_REWARD = "gift.reward";
    public static final String GAME_TYPE = "GAME_TYPE";


    public GiftingPromotion(DateTime startTime, DateTime endTime, Long reward, boolean allPlayers, final String name, final String giftTitle, final String giftDescription, final String gameType) {
        super(PromotionType.GIFTING);
        setAllPlayers(allPlayers);
        setStartDate(startTime);
        setEndDate(endTime);
        setControlGroupFunction(ControlGroupFunctionType.PLAYER_ID);
        setControlGroupPercentage(0);
        setName(name);
        addConfigurationItem(GIFT_REWARD, Long.toString(reward));
        addConfigurationItem(MAX_REWARDS_KEY, "1");
        addConfigurationItem(GIFT_TITLE, giftTitle);
        addConfigurationItem(GIFT_DESCRIPTION, giftDescription);
        addConfigurationItem(GAME_TYPE, gameType);
        setPlatforms(null);
    }

    public GiftingPromotion() {
        super(PromotionType.GIFTING);
    }


    public Long getReward() {
        return Long.parseLong(getConfiguration().getConfigurationValue(GIFT_REWARD));
    }

    public String getGiftTitle() {
        return getConfiguration().getConfigurationValue(GIFT_TITLE);
    }

    public String getGiftDescription() {
        return getConfiguration().getConfigurationValue(GIFT_DESCRIPTION);
    }

    public String getMaxReward() {
        return getConfiguration().getConfigurationValue(MAX_REWARDS_KEY);
    }

    public String getGameTypes() {
        return getConfiguration().getConfigurationValue(GAME_TYPE);
    }
}
