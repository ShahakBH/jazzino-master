package com.yazino.promotions;

public enum PromotionConfigKeyEnum {
    MAIN_IMAGE_KEY("main.image"),
    MAIN_IMAGE_LINK_KEY("main.image.link"),
    SECONDARY_IMAGE_KEY("secondary.image"),
    SECONDARY_IMAGE_LINK_KEY("secondary.image.link"),
    IOS_IMAGE_KEY("ios.image"),
    ANDROID_IMAGE_KEY("android.image"),
    REWARD_CHIPS_KEY("reward.chips"),
    MAX_REWARDS_KEY("max.rewards"),
    IN_GAME_NOTIFICATION_MSG_KEY("ingame.notification.msg"),
    IN_GAME_NOTIFICATION_HEADER_KEY("ingame.notification.header"),
    ROLLOVER_HEADER_KEY("rollover.header"),
    ROLLOVER_TEXT_KEY("rollover.text"),
    PAYMENT_METHODS("payment.methods"),
    ALL_PLAYERS("all.players"),
    TOPUP_AMOUNT_KEY("reward.chips"),
    GIFT_DESCRIPTION_KEY("gift.description"),
    GIFT_TITLE_KEY("gift.title"),
    GAME_TYPE("game.type");

    private final String description;

    private PromotionConfigKeyEnum(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
