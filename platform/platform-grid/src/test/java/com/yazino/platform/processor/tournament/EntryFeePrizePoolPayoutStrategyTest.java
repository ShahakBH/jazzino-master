package com.yazino.platform.processor.tournament;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EntryFeePrizePoolPayoutStrategyTest {

    private final EntryFeePrizePoolPayoutStrategy strategy = new EntryFeePrizePoolPayoutStrategy();

    @Test
    public void shouldParseActualDistributionTableCorrectly() throws Exception {
        assertEquals(BigDecimal.ZERO, strategy.getPercentageOfPrizePoolForPosition(10, 75));
        assertEquals(BigDecimal.ZERO, strategy.getPercentageOfPrizePoolForPosition(11, 75));
        assertEquals(BigDecimal.ZERO, strategy.getPercentageOfPrizePoolForPosition(12, 75));
        assertEquals(new BigDecimal("1.75"), strategy.getPercentageOfPrizePoolForPosition(10, 110));
        assertEquals(new BigDecimal("1.75"), strategy.getPercentageOfPrizePoolForPosition(11, 110));
        assertEquals(new BigDecimal("1.75"), strategy.getPercentageOfPrizePoolForPosition(12, 110));
    }

    @Test
    public void shouldReturnEmptyListOfPrizesWhenNumberOfPlayersIsZero() throws Exception {
        List<BigDecimal> prizes = strategy.calculatePrizes(0, new BigDecimal(40000));
        assertEquals(0, prizes.size());
    }

    @Test
    public void shouldReturnEmptyListOfPrizesWhenNumberOfPlayersIsLessThanMinimumForEntryFeeStrategy() throws Exception {
        List<BigDecimal> prizes = strategy.calculatePrizes(1, new BigDecimal(40000));
        assertEquals(0, prizes.size());
    }

    @Test
    public void shouldReturnEmptyListOfPrizesWhenPrizePotIsZero() throws Exception {
        List<BigDecimal> prizes = strategy.calculatePrizes(10, BigDecimal.ZERO);
        assertEquals(0, prizes.size());
    }

    @Test
    public void shouldUseLastPlayerRangeWhenMorePlayersThanInTable() throws Exception {
        List<BigDecimal> prizes = strategy.calculatePrizes(5000, new BigDecimal(1000000));
        assertEquals(189, prizes.size());
        assertEquals(new BigDecimal("206000.00"), prizes.get(0));
        assertEquals(new BigDecimal("1100.00"), prizes.get(188));
    }


}
