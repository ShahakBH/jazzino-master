package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.tournament.TournamentVariationPayout;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class TournamentPayoutCalculator implements Serializable {
    private static final long serialVersionUID = -8520889809043342801L;

    private static final Logger LOG = LoggerFactory.getLogger(TournamentPayoutCalculator.class);

    private MinimumPrizePoolPayoutStrategy minimumPrizePoolPayoutStrategy = new MinimumPrizePoolPayoutStrategy();
    private EntryFeePrizePoolPayoutStrategy entryFeePrizePoolPayoutStrategy = new EntryFeePrizePoolPayoutStrategy();

    public Map<TournamentPlayer, BigDecimal> calculatePayouts(final Set<TournamentPlayer> players,
                                                              final TournamentVariationTemplate variationTemplate) {
        final BigDecimal minimumPrizePool = variationTemplate.getPrizePool();
        final List<TournamentVariationPayout> minimumVariationPayouts = variationTemplate.getTournamentPayouts();
        final BigDecimal entryFeePrizePool = calculateEntryFeePrizePool(players.size(), variationTemplate);
        final Map<TournamentPlayer, BigDecimal> minimumPayouts = calculateMinimumPrizePoolPayouts(
                players, minimumPrizePool, minimumVariationPayouts);
        final Map<TournamentPlayer, BigDecimal> entryFeePayouts = calculateEntryFeePrizePoolPayouts(
                players, entryFeePrizePool);
        final Map<TournamentPlayer, BigDecimal> highestPayouts = selectHighestPayouts(
                minimumPayouts, entryFeePayouts);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Highest Payouts [%s], Minimum Payouts [%s], EntryFee Payouts [%s]",
                    highestPayouts, minimumPayouts, entryFeePayouts));
        }
        return highestPayouts;
    }

    public List<BigDecimal> calculatePrizes(final int numberPlayers,
                                            final TournamentVariationTemplate tournamentVariationTemplate) {
        final BigDecimal minimumPrizePool = tournamentVariationTemplate.getPrizePool();
        final BigDecimal entryFeePrizePool = calculateEntryFeePrizePool(numberPlayers, tournamentVariationTemplate);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Number of players [%s], minimum prize pool [%s], entryFee prize pool [%s]",
                    numberPlayers, minimumPrizePool, entryFeePrizePool));
        }
        final List<BigDecimal> minimumPrizes = minimumPrizePoolPayoutStrategy.calculatePrizes(
                minimumPrizePool, tournamentVariationTemplate.getTournamentPayouts());
        final List<BigDecimal> entryFeePrizes = entryFeePrizePoolPayoutStrategy.calculatePrizes(
                numberPlayers, entryFeePrizePool);
        final List<BigDecimal> highestPrizes = selectHighestPrizes(minimumPrizes, entryFeePrizes);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Highest Prizes [%s], Minimum Prizes [%s], EntryFee Prizes [%s]",
                    highestPrizes, minimumPrizes, entryFeePrizes));
        }
        return highestPrizes;
    }

    public BigDecimal calculateUnpaidPrizePool(final int numberPlayers,
                                               final TournamentVariationTemplate tournamentVariationTemplate) {
        // todo this doesn't handle if there are no tournament variation payouts, doesn't use the pot, just returns 0
        final List<BigDecimal> prizes = calculatePrizes(numberPlayers, tournamentVariationTemplate);
        final BigDecimal sumOfPrizes = sum(prizes);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Unpaid Prize Pool [%s]", sumOfPrizes));
        }
        return sumOfPrizes;
    }

    private BigDecimal calculateEntryFeePrizePool(final int numberOfPlayers,
                                                  final TournamentVariationTemplate template) {
        BigDecimal total = BigDecimal.ZERO;
        final BigDecimal entryFee = template.getEntryFee();
        if (entryFee != null) {
            total = entryFee.multiply(BigDecimal.valueOf(numberOfPlayers));
        }
        return total;
    }

    private static BigDecimal sum(final List<BigDecimal> prizes) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal prize : prizes) {
            total = total.add(prize);
        }
        return total;
    }

    private Map<TournamentPlayer, BigDecimal> calculateMinimumPrizePoolPayouts(
            final Set<TournamentPlayer> players,
            final BigDecimal minimumPrizePool,
            final List<TournamentVariationPayout> minimumPayouts) {
        final List<BigDecimal> prizes = minimumPrizePoolPayoutStrategy.calculatePrizes(
                minimumPrizePool, minimumPayouts);
        return calculatePayoutsForPlayers(prizes, players);
    }

    private Map<TournamentPlayer, BigDecimal> calculateEntryFeePrizePoolPayouts(final Set<TournamentPlayer> players,
                                                                                final BigDecimal entryFeePrizePool) {
        final List<BigDecimal> prizes = entryFeePrizePoolPayoutStrategy.calculatePrizes(
                players.size(), entryFeePrizePool);
        return calculatePayoutsForPlayers(prizes, players);
    }

    private Map<TournamentPlayer, BigDecimal> calculatePayoutsForPlayers(final List<BigDecimal> prizes,
                                                                         final Set<TournamentPlayer> players) {
        final Map<TournamentPlayer, BigDecimal> payouts = new HashMap<TournamentPlayer, BigDecimal>(players.size());
        final Map<Integer, Set<TournamentPlayer>> playersByRank = groupPlayersByRank(players);
        for (Integer rank : playersByRank.keySet()) {
            final Set<TournamentPlayer> playersOfRank = playersByRank.get(rank);
            final int numberOfPlayersInRank = playersOfRank.size();
            final BigDecimal totalPrizeForRank = calculateTotalPrizeForRank(prizes, rank, numberOfPlayersInRank);
            final BigDecimal prizePerPlayer = totalPrizeForRank.divide(
                    BigDecimal.valueOf(numberOfPlayersInRank), 2, RoundingMode.DOWN);
            for (TournamentPlayer player : playersOfRank) {
                payouts.put(player, prizePerPlayer);
            }
        }
        return payouts;
    }

    private BigDecimal calculateTotalPrizeForRank(final List<BigDecimal> prizes,
                                                  final Integer rank,
                                                  final int numberOfPlayersWithRank) {
        if (numberOfPlayersWithRank < 1) {
            throw new IllegalArgumentException("numberOfPlayersWithRank must be greater than or equal to 1 but was: "
                    + numberOfPlayersWithRank);
        }
        BigDecimal totalPrize = BigDecimal.ZERO;
        final int lastRankIndex = Math.min(prizes.size(), rank + numberOfPlayersWithRank - 1);
        for (int i = rank; i <= lastRankIndex && i > 0; i++) {
            totalPrize = totalPrize.add(prizes.get(i - 1));
        }
        return totalPrize;
    }

    private Map<Integer, Set<TournamentPlayer>> groupPlayersByRank(final Set<TournamentPlayer> winners) {
        final Map<Integer, Set<TournamentPlayer>> rankedWinners = new HashMap<Integer, Set<TournamentPlayer>>();
        for (TournamentPlayer winner : winners) {
            final Integer rank = winner.getLeaderboardPosition();
            if (rank != null) {
                if (rankedWinners.get(rank) == null) {
                    rankedWinners.put(rank, new HashSet<TournamentPlayer>());
                }
                rankedWinners.get(rank).add(winner);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Ranked winners are: " + rankedWinners);
        }
        return rankedWinners;
    }

    private Map<TournamentPlayer, BigDecimal> selectHighestPayouts(
            final Map<TournamentPlayer, BigDecimal>... allPayouts) {
        final Map<TournamentPlayer, BigDecimal> highestPayouts = new HashMap<TournamentPlayer, BigDecimal>();
        for (Map<TournamentPlayer, BigDecimal> payouts : allPayouts) {
            for (TournamentPlayer player : payouts.keySet()) {
                BigDecimal currentHighest = highestPayouts.get(player);
                final BigDecimal candidate = payouts.get(player);
                if (currentHighest == null) {
                    currentHighest = candidate;
                } else if (candidate != null) {
                    currentHighest = currentHighest.max(candidate);
                }
                highestPayouts.put(player, currentHighest);
            }
        }
        return highestPayouts;
    }

    private List<BigDecimal> selectHighestPrizes(final List<BigDecimal> minimumPrizes,
                                                 final List<BigDecimal> entryFeePrizes) {
        final int longestList = Math.max(minimumPrizes.size(), entryFeePrizes.size());

        final List<BigDecimal> highestPrizes = new ArrayList<BigDecimal>(longestList);
        for (int i = 0; i < longestList; i++) {
            highestPrizes.add(null);
        }

        // todo refactor
        for (int i = 0; i < minimumPrizes.size(); i++) {
            final BigDecimal candidate = minimumPrizes.get(i);

            final BigDecimal currentHighest = highestPrizes.get(i);
            final BigDecimal newHighest;
            if (currentHighest == null) {
                newHighest = candidate;
            } else {
                newHighest = currentHighest.max(candidate);
            }
            highestPrizes.set(i, newHighest);
        }

        for (int i = 0; i < entryFeePrizes.size(); i++) {
            final BigDecimal candidate = entryFeePrizes.get(i);

            final BigDecimal currentHighest = highestPrizes.get(i);
            final BigDecimal newHighest;
            if (currentHighest == null) {
                newHighest = candidate;
            } else {
                newHighest = currentHighest.max(candidate);
            }
            highestPrizes.set(i, newHighest);
        }

        return highestPrizes;
    }

    void setMinimumPrizePoolPayoutStrategy(final MinimumPrizePoolPayoutStrategy minimumPrizePoolPayoutStrategy) {
        this.minimumPrizePoolPayoutStrategy = minimumPrizePoolPayoutStrategy;
    }

    void setEntryFeePrizePoolPayoutStrategy(final EntryFeePrizePoolPayoutStrategy entryFeePrizePoolPayoutStrategy) {
        this.entryFeePrizePoolPayoutStrategy = entryFeePrizePoolPayoutStrategy;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(minimumPrizePoolPayoutStrategy)
                .append(entryFeePrizePoolPayoutStrategy)
                .toString();
    }
}
