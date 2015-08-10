package com.yazino.platform.processor.tournament;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.yazino.platform.model.tournament.PayoutDistribution;
import com.yazino.platform.model.tournament.TournamentPlayer;
import org.apache.commons.lang3.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class EntryFeePrizePoolPayoutStrategy implements Serializable {
    private static final long serialVersionUID = -5789664830504600512L;

    private static final Logger LOG = LoggerFactory.getLogger(EntryFeePrizePoolPayoutStrategy.class);

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final Range<Integer>[] positionRanges;
    private final List<PlayerRangePayout> payouts;

    public EntryFeePrizePoolPayoutStrategy() {
        final EntryFeePayoutDistributionParser parser = new EntryFeePayoutDistributionParser();
        parser.parse();
        positionRanges = parser.getPositionRanges();
        payouts = parser.getPayouts();
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Payouts parsed, position ranges [%s], payouts [%s]",
                    Arrays.toString(positionRanges), payouts));
        }
    }

    public BigDecimal getPercentageOfPrizePoolForPosition(final int finishingPosition,
                                                          final int numberPlayers) {
        final PlayerRangePayout playerRangePayout = findPayoutForNumberPlayers(numberPlayers);
        if (playerRangePayout == null) {
            return BigDecimal.ZERO;
        }
        final int finishingPostionRangeIndex = findFinishingPositionRangeIndex(finishingPosition);
        return playerRangePayout.getPayout(finishingPostionRangeIndex);
    }

    public Map<TournamentPlayer, BigDecimal> calculatePayouts(final Set<TournamentPlayer> players,
                                                              final BigDecimal prizePool) {
        final PayoutDistribution payoutDistribution = buildPayoutDistribution(Sets.newHashSet(players), prizePool);
        // calculate per person shares
        final Map<TournamentPlayer, BigDecimal> calculatedPayouts = new HashMap<TournamentPlayer, BigDecimal>();
        for (TournamentPlayer player : players) {
            final int rank = player.getLeaderboardPosition();
            final BigDecimal shareForRank = payoutDistribution.getShareOfChipsPerPlayerForRank(rank);
            calculatedPayouts.put(player, shareForRank);
        }
        return calculatedPayouts;
    }

    public List<BigDecimal> calculatePrizes(final int numberOfPlayers,
                                            final BigDecimal prizePool) {
        // quick and dirty pre-Xmas implementation
        final List<BigDecimal> prizes = new ArrayList<BigDecimal>();
        if (numberOfPlayers < minimumNumberOfPlayersForStrategy() || prizePool.compareTo(BigDecimal.ZERO) == 0) {
            return prizes;
        }
        final int lastRank = findLastRankWithPrizeForTotalPlayers(numberOfPlayers);
        for (int rank = 1; rank <= lastRank; rank++) {
            final BigDecimal percentageOfPoolForPosition = getPercentageOfPrizePoolForPosition(rank, numberOfPlayers);
            final BigDecimal prize = prizePool.multiply(percentageOfPoolForPosition)
                    .divide(ONE_HUNDRED).setScale(2, RoundingMode.DOWN);
            prizes.add(prize);
        }
        return prizes;
    }

    private int findFinishingPositionRangeIndex(final int finishingPosition) {
        for (int i = 0; i < positionRanges.length; i++) {
            final Range<Integer> positionRange = positionRanges[i];
            if (positionRange.contains(finishingPosition)) {
                return i;
            }
        }
        return -1;
    }

    private PlayerRangePayout findPayoutForNumberPlayers(final int numberPlayers) {
        if (numberPlayers >= maximumNumberOfPlayersForStrategy()) {
            return payouts.get(payouts.size() - 1);
        }
        return Iterables.tryFind(payouts, new PlayerCountPredicate(numberPlayers)).orNull();
    }

    private int findLastRankWithPrizeForTotalPlayers(final int numberPlayers) {
        final PlayerRangePayout playerRangePayout = findPayoutForNumberPlayers(numberPlayers);
        final int positionRangeIndex = playerRangePayout.getLowestPositionRangeIndex();

        final Range<Integer> finishingRange = positionRanges[positionRangeIndex];
        return finishingRange.getMaximum();
    }

    private int minimumNumberOfPlayersForStrategy() {
        return payouts.get(0).getPlayerRange().getMinimum();
    }

    private int maximumNumberOfPlayersForStrategy() {
        return payouts.get(payouts.size() - 1).getPlayerRange().getMaximum();
    }

    private BigDecimal calculateShareOfPrizePool(final BigDecimal percentage,
                                                 final BigDecimal prizePool) {
        return prizePool.multiply(percentage).divide(ONE_HUNDRED);
    }

    private PayoutDistribution buildPayoutDistribution(final Set<TournamentPlayer> players,
                                                       final BigDecimal prizePool) {
        final PayoutDistribution distribution = new PayoutDistribution(players);
        distributePrizePoolToRanks(distribution, players.size(), prizePool);
        return distribution;
    }

    private void distributePrizePoolToRanks(final PayoutDistribution distribution,
                                            final int playerCount,
                                            final BigDecimal prizePool) {
        final int lowestRank = getLowestRank();
        int rank = 1;
        BigDecimal percentage;
        do {
            percentage = getPercentageOfPrizePoolForPosition(rank, playerCount);
            if (!percentage.equals(BigDecimal.ZERO)) {
                distribution.setChipsForRank(rank, calculateShareOfPrizePool(percentage, prizePool));
            }
            rank++;
        } while (rank <= lowestRank && !BigDecimal.ZERO.equals(percentage));
    }

    private int getLowestRank() {
        return positionRanges[positionRanges.length - 1].getMaximum();
    }

    static class PlayerRangePayout {
        private final Range<Integer> playerRange;
        private final List<BigDecimal> payouts = new ArrayList<BigDecimal>();

        PlayerRangePayout(final Range<Integer> playerRange) {
            this.playerRange = playerRange;
        }

        public BigDecimal getPayout(final int index) {
            if (index >= 0 && index < payouts.size()) {
                return payouts.get(index);
            }
            return BigDecimal.ZERO;
        }

        void addPayout(final String payout) {
            payouts.add(new BigDecimal(payout));
        }

        public boolean isCorrectColumn(final int players) {
            return playerRange.contains(players);
        }

        public int getLowestPositionRangeIndex() {
            return payouts.size() - 1;
        }

        Range<Integer> getPlayerRange() {
            return playerRange;
        }

    }

    private final class PlayerCountPredicate implements Predicate<PlayerRangePayout> {
        private final int numberPlayers;

        private PlayerCountPredicate(final int numberPlayers) {
            this.numberPlayers = numberPlayers;
        }

        @Override
        public boolean apply(final PlayerRangePayout playerRangePayout) {
            return playerRangePayout.isCorrectColumn(numberPlayers);
        }

    }

}


