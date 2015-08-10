package strata.server.lobby.api.promotion;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PromotionTest {

    final DateTime TOMORROW = (new DateTime()).plusDays(1);
    final DateTime NEXT_YEAR = (new DateTime()).plusYears(1);
    final DateTime TEN_MINUTES_AGO = (new DateTime()).minusMinutes(10);
    final DateTime FIVE_MINUTES_AGO = (new DateTime()).minusMinutes(5);

    @Test
    public void shouldReturnCorrectBooleansForFuturePromotion() {
        final Promotion promotion = PromotionFactory.createPromotion(PromotionType.DAILY_AWARD);
        promotion.setStartDate(TOMORROW);
        promotion.setEndDate(NEXT_YEAR);
        assertTrue(promotion.isInFuture());
        assertFalse(promotion.isExpired());
        assertFalse(promotion.isActive());
    }

    @Test
    public void shouldReturnCorrectBooleansForExpiredPromotion() {
        final Promotion promotion = PromotionFactory.createPromotion(PromotionType.BUY_CHIPS);
        promotion.setStartDate(TEN_MINUTES_AGO);
        promotion.setEndDate(FIVE_MINUTES_AGO);
        assertFalse(promotion.isInFuture());
        assertTrue(promotion.isExpired());
        assertFalse(promotion.isActive());
    }

    @Test
    public void shouldReturnCorrectBooleansCurrentPromotion() {
        final Promotion promotion = PromotionFactory.createPromotion(PromotionType.BUY_CHIPS);
        promotion.setStartDate(TEN_MINUTES_AGO);
        promotion.setEndDate(TOMORROW);
        assertFalse(promotion.isInFuture());
        assertFalse(promotion.isExpired());
        assertTrue(promotion.isActive());
    }
}
