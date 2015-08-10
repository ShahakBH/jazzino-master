package strata.server.lobby.api.promotion;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PromotionFactoryTest {
    @Test
    public void shouldCreateDailyAwardPromotion() {
        final Promotion promotion = PromotionFactory.createPromotion(PromotionType.DAILY_AWARD);
        assertTrue(promotion instanceof DailyAwardPromotion);
    }
    @Test
    public void shouldCreateBuyChipsPromotionPromotion() {
        final Promotion promotion = PromotionFactory.createPromotion(PromotionType.BUY_CHIPS);
        assertTrue(promotion instanceof BuyChipsPromotion);
    }
}
