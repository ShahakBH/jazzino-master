package com.yazino.platform.processor.tournament;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.tournament.TournamentVariationPayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class MinimumPrizePoolPayoutStrategy implements Serializable {

    private static final long serialVersionUID = -1284003339328362951L;
    private static final Logger LOG = LoggerFactory.getLogger(MinimumPrizePoolPayoutStrategy.class);

    public static final RankComparator RANK_COMPARATOR = new RankComparator();

    public List<BigDecimal> calculatePrizes(final BigDecimal prizePot,
                                            final List<TournamentVariationPayout> payouts) {
        // assume payouts specifying contiguous ranks 1 - payouts.size(). This needs cleaning up
        final List<BigDecimal> result = new ArrayList<BigDecimal>(payouts.size());
        for (TournamentVariationPayout payout1 : payouts) {
            result.add(null);
        }
        for (TournamentVariationPayout payout : payouts) {
            final BigDecimal payoutAmount = calculatePrizeForPayout(prizePot, payout);
            result.set(payout.getRank() - 1, payoutAmount);
        }
        return result;
    }

    // TODO revisit payout name to indicate that it is a percentage?
    private BigDecimal calculatePrizeForPayout(final BigDecimal originalPrizePot,
                                               final TournamentVariationPayout payout) {
        BigDecimal prizePot = originalPrizePot;
        if (prizePot == null) {
            prizePot = BigDecimal.ZERO;
        }
        BigDecimal prize = payout.getPayout();
        if (prize == null) {
            prize = BigDecimal.ZERO;
        }
        final BigDecimal payoutAmount = prizePot.multiply(prize).setScale(2, RoundingMode.DOWN);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("prizePot=[%s] prize=[%s] payoutAmount=[%s]", prizePot, prize, payoutAmount));
        }
        return payoutAmount;
    }


    private Map<TournamentPlayer, BigDecimal> calculatePayouts(final BigDecimal totalPrize,
                                                               final List<TournamentVariationPayout> minimumPayouts,
                                                               final Set<TournamentPlayer> winners) {

        final List<TournamentVariationPayout> sortedMinimumPayouts = sortMinimumPayoutsByRank(minimumPayouts);

        final Map<TournamentPlayer, BigDecimal> result = new HashMap<TournamentPlayer, BigDecimal>();
        final Map<Integer, Set<TournamentPlayer>> rankWinners = rankWinners(winners);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Total prize for this tournament is " + totalPrize + " payouts=" + sortedMinimumPayouts.size());
            LOG.debug("Winners for each rank= " + rankWinners);
        }

        for (TournamentVariationPayout payout : sortedMinimumPayouts) {
            final Set<TournamentPlayer> winnersAtRank = rankWinners.get(payout.getRank());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Winners for rank " + payout.getRank() + " = " + winnersAtRank);
            }
            if (winnersAtRank != null) {
                final int numberOfWinners = winnersAtRank.size();
                final BigDecimal totalPayout = getTotalPayout(sortedMinimumPayouts, payout.getRank(), numberOfWinners);
                final BigDecimal individualPayout = totalPrize.multiply(totalPayout)
                        .divide(BigDecimal.valueOf(numberOfWinners), 2, RoundingMode.DOWN);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Paying " + payout);
                    LOG.debug("Number of winners= " + payout);
                    LOG.debug("Total payout percentage= " + totalPayout);
                    LOG.debug("Individual payout= " + individualPayout);
                }
                for (TournamentPlayer player : winnersAtRank) {
                    result.put(player, individualPayout);
                }
            }
        }
        return result;
    }

    private List<TournamentVariationPayout> sortMinimumPayoutsByRank(
            final List<TournamentVariationPayout> minimumPayouts) {
        final List<TournamentVariationPayout> payoutCopy = new ArrayList<TournamentVariationPayout>(minimumPayouts);
        Collections.sort(payoutCopy, RANK_COMPARATOR);
        return payoutCopy;
    }

    public Map<TournamentPlayer, BigDecimal> calculatePayouts(final Set<TournamentPlayer> players,
                                                              final BigDecimal minimumPrizePool,
                                                              final List<TournamentVariationPayout> minimumPayouts) {
        return calculatePayouts(Lists.newArrayList(players), minimumPrizePool, minimumPayouts);
    }

    private Map<TournamentPlayer, BigDecimal> calculatePayouts(final List<TournamentPlayer> players,
                                                               final BigDecimal minimumPrizePool,
                                                               final List<TournamentVariationPayout> minimumPayouts) {
        return calculatePayouts(minimumPrizePool, minimumPayouts, Sets.newHashSet(players));
    }

    private Map<Integer, Set<TournamentPlayer>> rankWinners(final Set<TournamentPlayer> winners) {
        final Map<Integer, Set<TournamentPlayer>> rankedWinners = new HashMap<Integer, Set<TournamentPlayer>>();
        for (TournamentPlayer winner : winners) {
            final Integer rank = winner.getLeaderboardPosition();
            if (!rankedWinners.containsKey(rank)) {
                rankedWinners.put(rank, new HashSet<TournamentPlayer>());
            }
            rankedWinners.get(rank).add(winner);
        }
        return rankedWinners;
    }

    private BigDecimal getTotalPayout(final List<TournamentVariationPayout> payouts,
                                      final int currentRank,
                                      final int numberOfWinners) {
        BigDecimal result = BigDecimal.ZERO;
        final int initialRankIndex = currentRank - 1;
        final int expectedRankFinal = initialRankIndex + numberOfWinners;
        final int endRankIndex;
        if (expectedRankFinal < payouts.size()) {
            endRankIndex = expectedRankFinal;
        } else {
            endRankIndex = payouts.size();
        }
        for (int i = initialRankIndex; i < endRankIndex; i++) {
            result = result.add(payouts.get(i).getPayout());
        }
        return result;
    }

    public static class RankComparator implements Comparator<TournamentVariationPayout> {

        @Override
        public int compare(final TournamentVariationPayout instance1,
                           final TournamentVariationPayout instance2) {
            return new Integer(instance1.getRank()).compareTo(instance2.getRank());
        }
    }
}
