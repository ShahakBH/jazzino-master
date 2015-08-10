package strata.server.lobby.api.promotion;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ProgressiveAwardEnumTest {

    @Test
    public void testGetProgressiveAwardEnumForConsecutiveDaysPlayed() throws Exception {
        assertEquals(ProgressiveAwardEnum.AWARD_1, ProgressiveAwardEnum.getProgressiveAwardEnumForConsecutiveDaysPlayed(0));
        assertEquals(ProgressiveAwardEnum.AWARD_2, ProgressiveAwardEnum.getProgressiveAwardEnumForConsecutiveDaysPlayed(1));
        assertEquals(ProgressiveAwardEnum.AWARD_3, ProgressiveAwardEnum.getProgressiveAwardEnumForConsecutiveDaysPlayed(2));
        assertEquals(ProgressiveAwardEnum.AWARD_4, ProgressiveAwardEnum.getProgressiveAwardEnumForConsecutiveDaysPlayed(3));
        assertEquals(ProgressiveAwardEnum.AWARD_5, ProgressiveAwardEnum.getProgressiveAwardEnumForConsecutiveDaysPlayed(4));
        assertEquals(ProgressiveAwardEnum.AWARD_1, ProgressiveAwardEnum.getProgressiveAwardEnumForConsecutiveDaysPlayed(506));
    }

    @Test
    public void testGetNextReturnsNextProgressiveAwardDay() throws Exception {
        assertEquals(ProgressiveAwardEnum.AWARD_2, ProgressiveAwardEnum.AWARD_1.getNext());
        assertEquals(ProgressiveAwardEnum.AWARD_3, ProgressiveAwardEnum.AWARD_2.getNext());
        assertEquals(ProgressiveAwardEnum.AWARD_4, ProgressiveAwardEnum.AWARD_3.getNext());
        assertEquals(ProgressiveAwardEnum.AWARD_5, ProgressiveAwardEnum.AWARD_4.getNext());
        assertEquals(ProgressiveAwardEnum.AWARD_5, ProgressiveAwardEnum.AWARD_5.getNext());
    }

    @Test
    public void testValueOfProgressivePromotionType() {
        assertThat(ProgressiveAwardEnum.AWARD_1, is(ProgressiveAwardEnum.valueOf(PromotionType.PROGRESSIVE_DAY_1)));
        assertThat(ProgressiveAwardEnum.AWARD_2, is(ProgressiveAwardEnum.valueOf(PromotionType.PROGRESSIVE_DAY_2)));
        assertThat(ProgressiveAwardEnum.AWARD_3, is(ProgressiveAwardEnum.valueOf(PromotionType.PROGRESSIVE_DAY_3)));
        assertThat(ProgressiveAwardEnum.AWARD_4, is(ProgressiveAwardEnum.valueOf(PromotionType.PROGRESSIVE_DAY_4)));
        assertThat(ProgressiveAwardEnum.AWARD_5, is(ProgressiveAwardEnum.valueOf(PromotionType.PROGRESSIVE_DAY_5)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValueOfProgressivePromotionTypeThrowsExceptionForNonProgressiveTypes() {
        ProgressiveAwardEnum.valueOf(PromotionType.BUY_CHIPS);
    }
}
