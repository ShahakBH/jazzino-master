package strata.server.lobby.api.promotion;

public enum PromotionType {
    GIFTING("Gifting", "Default Gifting"),
    DAILY_AWARD("Daily Award", "Default Daily Award"),
    BUY_CHIPS("Buy Chips", "Default Buy Chips"),
    PROGRESSIVE_DAY_1("Progressive Day 1", ""),
    PROGRESSIVE_DAY_2("Progressive Day 2", ""),
    PROGRESSIVE_DAY_3("Progressive Day 3", ""),
    PROGRESSIVE_DAY_4("Progressive Day 4", ""),
    PROGRESSIVE_DAY_5("Progressive Day 5", "");

    private final String displayName;
    private final String defaultPromotionName;

    private PromotionType(final String displayName, final String defaultPromotionName) {
        this.displayName = displayName;
        this.defaultPromotionName = defaultPromotionName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultPromotionName() {
        return defaultPromotionName;
    }

    public static PromotionType getPromotionTypeFromString(final String name) {
        for (PromotionType promotionType : PromotionType.values()) {
            if (promotionType.name().equals(name)) {
                return promotionType;
            }
        }
        return null;
    }

    public Boolean isProgressive() {
        return this.name().contains("PROGRESSIVE_DAY_");
    }
}
