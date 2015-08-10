package com.yazino.platform.processor.tournament;

import org.apache.commons.lang3.Range;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link com.yazino.platform.processor.tournament.EntryFeePayoutDistributionParser} class.
 */
public class EntryFeePayoutDistributionParserTest {

    private final EntryFeePayoutDistributionParser parser = new EntryFeePayoutDistributionParser();

    @Test
    public void parserShouldParseSinglePositionsCorrectly() throws Exception {
        String tsv =
                "\t2-27\t28-36\n" +
                        "1\t50.00%\t45.00%\n" +
                        "2\t30.00%\t25.00%\n" +
                        "3\t20.00%\t18.00%";

        parser.setDistributionTsv(tsv);
        parser.parse();
        Range<Integer>[] positionRanges = parser.getPositionRanges();

        assertEquals(3, positionRanges.length);
        assertEquals(Range.is(1), positionRanges[0]);
        assertEquals(Range.is(2), positionRanges[1]);
        assertEquals(Range.is(3), positionRanges[2]);
    }

    @Test
    public void parserShouldParsePositionRangesCorrectly() throws Exception {
        String tsv =
                "\t2-50\t51-53\n" +
                        "1\t50.00%\t45.00%\n" +
                        "2\t30.00%\t25.00%\n" +
                        "10\t\t";
        parser.setDistributionTsv(tsv);

        parser.parse();
        Range<Integer>[] positionRanges = parser.getPositionRanges();

        assertEquals(3, positionRanges.length);
        assertEquals(Range.is(1), positionRanges[0]);
        assertEquals(Range.is(2), positionRanges[1]);
        assertEquals(Range.between(3, 10), positionRanges[2]);
    }

    @Test
    public void parserShouldParsePayoutsCorrectly() throws Exception {
        String tsv =
                "\t2-27\t28-36\n" +
                        "1\t50.00%\t45.00%\n" +
                        "2\t30.00%\t25.00%\n" +
                        "3\t20.00%\t18.00%\n" +
                        "4\t\t9.00%";

        parser.setDistributionTsv(tsv);
        parser.parse();

        List<EntryFeePrizePoolPayoutStrategy.PlayerRangePayout> payouts = parser.getPayouts();
        assertEquals(2, payouts.size());

        EntryFeePrizePoolPayoutStrategy.PlayerRangePayout first = payouts.get(0);
        assertPayoutColumn(first, 2, 27, "50.00", "30.00", "20.00");

        EntryFeePrizePoolPayoutStrategy.PlayerRangePayout second = payouts.get(1);
        assertPayoutColumn(second, 28, 36, "45.00", "25.00", "18.00", "9.00");

    }

    @Test
    public void parserShouldIgnoreEmptyValues() throws Exception {
        String tsv =
                "\t2-56\t57-100\n" +
                        "1\t55.00%\t45.00%\n" +
                        "2\t27.25%\t25.00%\n" +
                        "3\t20.89%\t18.00%\n" +
                        "4\t\t";

        parser.setDistributionTsv(tsv);
        parser.parse();

        List<EntryFeePrizePoolPayoutStrategy.PlayerRangePayout> payouts = parser.getPayouts();
        assertEquals(2, payouts.size());

        EntryFeePrizePoolPayoutStrategy.PlayerRangePayout first = payouts.get(0);
        assertPayoutColumn(first, 2, 56, "55.00", "27.25", "20.89");

        EntryFeePrizePoolPayoutStrategy.PlayerRangePayout second = payouts.get(1);
        assertPayoutColumn(second, 57, 100, "45.00", "25.00", "18.00");
    }

    private static void assertPayoutColumn(EntryFeePrizePoolPayoutStrategy.PlayerRangePayout playerRange, int rangeStart, int rangeEnd, String... payouts) {
        assertEquals(Range.between(rangeStart, rangeEnd), playerRange.getPlayerRange());
        for (int i = 0; i < payouts.length; i++) {
            String payout = payouts[i];
            assertEquals(new BigDecimal(payout), playerRange.getPayout(i));
        }
        assertEquals(BigDecimal.ZERO, playerRange.getPayout(payouts.length));
    }

}