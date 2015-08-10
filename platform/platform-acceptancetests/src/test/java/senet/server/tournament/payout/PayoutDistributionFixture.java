package senet.server.tournament.payout;

import fitnesse.fixtures.TableFixture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yazino.platform.processor.tournament.EntryFeePrizePoolPayoutStrategy;

import java.math.BigDecimal;

/**
 * Sanity tests the {@link com.yazino.platform.processor.tournament.EntryFeePrizePoolPayoutStrategy}class and the percentage values that it should return.
 */
public class PayoutDistributionFixture extends TableFixture {
    private static final Logger LOG = LoggerFactory.getLogger(PayoutDistributionFixture.class);

    private static final int NUMBER_PLAYER_COUNT_GROUPS = 21;

    private final EntryFeePrizePoolPayoutStrategy payoutDistribution = new EntryFeePrizePoolPayoutStrategy();

    @Override
    protected void doStaticTable(int rows) {

        for (int row = 2; row < rows; row++) {

            String[] finishingPositionRange = getText(row, 0).split("-");
            int lowerFinishingPosition = Integer.parseInt(finishingPositionRange[0]);
            int upperFinishingPosition = finishingPositionRange.length > 1 ? Integer.parseInt(finishingPositionRange[1]) : lowerFinishingPosition;

            for (int column = 1; column <= NUMBER_PLAYER_COUNT_GROUPS; column++) {
                String[] totalPlayers = getText(1, column).split("-");
                int lowerPlayerCount = Integer.parseInt(totalPlayers[0]);
                int upperPlayerCount = Integer.parseInt(totalPlayers[1]);

                BigDecimal expectedPercentage = BigDecimal.ZERO;
                if (!blank(row, column)) {
                    expectedPercentage = new BigDecimal(getText(row, column));
                }

                boolean valid = validatePercentage(lowerPlayerCount, upperPlayerCount, lowerFinishingPosition, upperFinishingPosition, expectedPercentage);
                if (valid) {
                    right(row, column);
                } else {
                    wrong(row, column);
                }
            }
        }
    }

    private boolean validatePercentage(int lowerPlayerCount, int upperPlayerCount, int lowerFinishingPosition, int upperFinishingPosition, BigDecimal expectedPercentage) {
        int middlePlayerCount = findMidPoint(lowerPlayerCount, upperPlayerCount);
        int middleFinishingPosition = findMidPoint(lowerFinishingPosition, upperFinishingPosition);

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Validating Percentage: finishingPosition [%s], lowerPlayerCount [%s], middlePlayerCount [%s], upperPlayerCount [%s], expectedPercentage [%s]", lowerFinishingPosition, lowerPlayerCount, middlePlayerCount, upperPlayerCount, expectedPercentage));
        }

        return expectedPercentage.equals(payoutDistribution.getPercentageOfPrizePoolForPosition(lowerFinishingPosition, lowerPlayerCount)) &&
                expectedPercentage.equals(payoutDistribution.getPercentageOfPrizePoolForPosition(lowerFinishingPosition, middlePlayerCount)) &&
                expectedPercentage.equals(payoutDistribution.getPercentageOfPrizePoolForPosition(lowerFinishingPosition, upperPlayerCount)) &&
                expectedPercentage.equals(payoutDistribution.getPercentageOfPrizePoolForPosition(middleFinishingPosition, lowerPlayerCount)) &&
                expectedPercentage.equals(payoutDistribution.getPercentageOfPrizePoolForPosition(middleFinishingPosition, middlePlayerCount)) &&
                expectedPercentage.equals(payoutDistribution.getPercentageOfPrizePoolForPosition(middleFinishingPosition, upperPlayerCount)) &&
                expectedPercentage.equals(payoutDistribution.getPercentageOfPrizePoolForPosition(upperFinishingPosition, lowerPlayerCount)) &&
                expectedPercentage.equals(payoutDistribution.getPercentageOfPrizePoolForPosition(upperFinishingPosition, middlePlayerCount)) &&
                expectedPercentage.equals(payoutDistribution.getPercentageOfPrizePoolForPosition(upperFinishingPosition, upperPlayerCount));
    }

    private int findMidPoint(int lowerBound, int upperBound) {
        return (lowerBound == upperBound ? lowerBound : lowerBound + (upperBound - lowerBound) / 2);
    }

}
