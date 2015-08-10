package com.yazino.platform.model.tournament;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PayoutDistribution {

    private Map<Integer, RankInfo> table;

    public PayoutDistribution(final Set<TournamentPlayer> players) {
        buildPayoutTable(players);
    }

    private void buildPayoutTable(final Set<TournamentPlayer> players) {
        resetTable();
        countPlayersInEachRank(players);
    }

    private void resetTable() {
        table = new HashMap<Integer, RankInfo>();
    }

    private void countPlayersInEachRank(final Set<TournamentPlayer> players) {
        for (TournamentPlayer player : players) {
            final int rank = player.getLeaderboardPosition();
            final RankInfo entry = entryForRank(rank);
            entry.playerCount++;
        }
    }

    public void setChipsForRank(final int rank, final BigDecimal amount) {
        entryForRank(rank).totalChips = amount;
    }

    private RankInfo entryForRank(final int rank) {
        if (rank < 1) {
            throw new IllegalArgumentException();
        }
        RankInfo entry = table.get(rank);
        if (entry == null) {
            table.put(rank, new RankInfo());
            entry = table.get(rank);
        }
        return entry;
    }

    public BigDecimal getPayoutForRank(final int rank) {
        return entryForRank(rank).totalChips;
    }

    public BigDecimal getShareOfChipsPerPlayerForRank(final int rank) {
        final RankInfo rankInfo = entryForRank(rank);
        if (rankInfo.playerCount == 0) {
            return BigDecimal.ONE;
        }
        final BigDecimal totalChipsForRank = sumChipsForRanks(rank, rankInfo.playerCount);
        return totalChipsForRank.divide(new BigDecimal(Integer.toString(rankInfo.playerCount)), 2, RoundingMode.DOWN);
    }

    private BigDecimal sumChipsForRanks(final int startingRank, final int numberOfRanks) {
        BigDecimal total = BigDecimal.ZERO;
        final int lowestRank = Math.min(startingRank + numberOfRanks - 1, getLowestRank());
        for (int rank = startingRank; rank <= lowestRank; rank++) {
            total = total.add(entryForRank(rank).totalChips);
        }
        return total;
    }

    public int getLowestRank() {
        Integer lowestRank = 0;
        for (Integer rank : table.keySet()) {
            lowestRank = Math.max(lowestRank, rank);
        }
        return lowestRank;
    }

    private class RankInfo {
        private int playerCount = 0;
        private BigDecimal totalChips = BigDecimal.ZERO;
        private BigDecimal chipsPerPlayer = BigDecimal.ZERO;
        private BigDecimal totalCollatedChips = BigDecimal.ZERO;
    }
}
