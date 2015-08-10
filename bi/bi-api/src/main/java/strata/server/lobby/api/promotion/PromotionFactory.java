package strata.server.lobby.api.promotion;

public final class PromotionFactory {

    private PromotionFactory() {
    }

    public static Promotion createPromotion(final PromotionType type) {

        switch (type) {
            case DAILY_AWARD:
                return new DailyAwardPromotion();
            case BUY_CHIPS:
                return new BuyChipsPromotion();
            case GIFTING:
                return new GiftingPromotion();
            case PROGRESSIVE_DAY_1:
                return new ProgressiveDailyAwardPromotion(PromotionType.PROGRESSIVE_DAY_1);
            case PROGRESSIVE_DAY_2:
                return new ProgressiveDailyAwardPromotion(PromotionType.PROGRESSIVE_DAY_2);
            case PROGRESSIVE_DAY_3:
                return new ProgressiveDailyAwardPromotion(PromotionType.PROGRESSIVE_DAY_3);
            case PROGRESSIVE_DAY_4:
                return new ProgressiveDailyAwardPromotion(PromotionType.PROGRESSIVE_DAY_4);
            case PROGRESSIVE_DAY_5:
                return new ProgressiveDailyAwardPromotion(PromotionType.PROGRESSIVE_DAY_5);
            default:
                throw new IllegalArgumentException("Attempting to creating Promotion with unknown type argument: "
                        + type);

        }
    }
}
