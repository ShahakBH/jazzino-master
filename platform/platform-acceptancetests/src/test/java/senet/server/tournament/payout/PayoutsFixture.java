package senet.server.tournament.payout;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.model.tournament.TournamentPlayers;
import com.yazino.platform.processor.tournament.EntryFeePrizePoolPayoutStrategy;
import com.yazino.platform.processor.tournament.MinimumPrizePoolPayoutStrategy;
import com.yazino.platform.tournament.TournamentVariationPayout;
import fitnesse.fixtures.TableFixture;
import com.yazino.platform.model.tournament.TournamentBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class PayoutsFixture extends TableFixture {

    public enum Columns { ENTRY_FEE_PRIZE_POOL_PAYOUT, MINIMUM_PRIZE_POOL_PAYOUT, ACTUAL_PAYOUT }

    private MinimumPrizePoolPayoutStrategy minimumPrizePoolPayoutStrategy = new MinimumPrizePoolPayoutStrategy();
    private EntryFeePrizePoolPayoutStrategy entryFeePrizePoolPayoutStrategy = new EntryFeePrizePoolPayoutStrategy();
    private TournamentPayoutFixture context;
    private List<Columns> columns = new ArrayList<Columns>();

    public PayoutsFixture(TournamentPayoutFixture context) {
        this(context, Columns.ENTRY_FEE_PRIZE_POOL_PAYOUT, Columns.MINIMUM_PRIZE_POOL_PAYOUT, Columns.ACTUAL_PAYOUT);
    }

    public PayoutsFixture(TournamentPayoutFixture context, Columns... columns) {
        this(context, asList(columns));
    }

    public PayoutsFixture(TournamentPayoutFixture context, List<Columns> columns) {
        this.context = context;
        this.columns.addAll(columns);
    }

    @Override
    protected void doStaticTable(int rowCount) {

        int playerCount = extractPlayerCount();

        TournamentBuilder tournamentBuilder = context.prepareTournamentBuilder();
        TournamentPlayers players = new TournamentPlayers();
        context.padRanks(players, 1, playerCount);
        tournamentBuilder.withPlayers(players);
        Tournament tournament = tournamentBuilder.build();
        tournament.setPot(tournament.getTournamentVariationTemplate().getEntryFee().multiply(new BigDecimal(Integer.toString(playerCount))));

        Map<TournamentPlayer, BigDecimal> payouts = tournament.getPayoutCalculator().calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate());
        for(int row = 2; row < rowCount; row++) {
            int rank = extractRankForRow(row);

            if (columns.contains(Columns.MINIMUM_PRIZE_POOL_PAYOUT)) {
                verifyMinimumPrizePoolPayout(row, tournament, rank);
            }
            if (columns.contains(PayoutsFixture.Columns.ENTRY_FEE_PRIZE_POOL_PAYOUT)) {
                verifyEntryFeePrizePoolPayout(row, tournament, rank);
            }
            if (columns.contains(PayoutsFixture.Columns.ACTUAL_PAYOUT)) {
                verifyActualPayout(row, rank, payouts);
            }
        }
    }

    private void verifyActualPayout(int row, int rank, Map<TournamentPlayer, BigDecimal> payouts) {
        BigDecimal expectedPayout = extractExpectedPayoutForRow(row);
        BigDecimal actualPayout = findPayoutForRank(rank, payouts);    // distribution.getPayoutForRank(rank);
        if (equivalent(actualPayout, expectedPayout)) {
            right(row, columnIndex(PayoutsFixture.Columns.ACTUAL_PAYOUT));
        } else {
            wrong(row, columnIndex(PayoutsFixture.Columns.ACTUAL_PAYOUT), actualPayout.toString());
        }
    }

    private void verifyEntryFeePrizePoolPayout(int row, Tournament tournament, int rank) {
        BigDecimal expectedPayout = extractEntryFeePrizePoolPayoutForRow(row);
        BigDecimal actualPayout = findPayoutForRank(rank, entryFeePrizePoolPayoutStrategy.calculatePayouts(tournament.tournamentPlayers(), tournament.getPot()));
        if (equivalent(actualPayout, expectedPayout)) {
            right(row, columnIndex(Columns.ENTRY_FEE_PRIZE_POOL_PAYOUT));
        } else {
            wrong(row, columnIndex(Columns.ENTRY_FEE_PRIZE_POOL_PAYOUT), actualPayout.toString());
        }
    }

    private void verifyMinimumPrizePoolPayout(int row, Tournament tournament, int rank) {
        BigDecimal expectedPayout = extractExpectedMinimumPrizePoolPayoutForRow(row);
        BigDecimal minimumPrizePool = tournament.getTournamentVariationTemplate().getPrizePool();
        List<TournamentVariationPayout> minimumPayouts = tournament.getTournamentVariationTemplate().getTournamentPayouts();
        BigDecimal actualPayout = findPayoutForRank(rank, minimumPrizePoolPayoutStrategy.calculatePayouts(tournament.tournamentPlayers(), minimumPrizePool, minimumPayouts));
        if (equivalent(actualPayout, expectedPayout)) {
            right(row, columnIndex(Columns.MINIMUM_PRIZE_POOL_PAYOUT));
        } else {
            wrong(row, columnIndex(Columns.MINIMUM_PRIZE_POOL_PAYOUT), actualPayout.toString());
        }
    }

    private int columnIndex(Columns column) {
        int index = columns.indexOf(column);
        if (index == -1) {
            throw new IllegalArgumentException("Column not enabled: " + column);
        }
        return index + 1 /* skip Rank column in position 1 */;
    }

    private boolean equivalent(BigDecimal value1, BigDecimal value2) {
        return value1.setScale(2).equals(value2.setScale(2));
    }

    private BigDecimal findPayoutForRank(int rank, Map<TournamentPlayer, BigDecimal> payouts) {
        for(TournamentPlayer player : payouts.keySet()) {
            if (player.getLeaderboardPosition() == rank) {
                return payouts.get(player);
            }
        }
//        throw new RuntimeException("No players have rank: " + rank);
        return BigDecimal.ZERO;
    }

    private int extractRankForRow(int row) {
        return Integer.parseInt(getText(row, 0));
    }

    private BigDecimal extractExpectedPayoutForRow(int row) {
        return new BigDecimal(getText(row, columnIndex(Columns.ACTUAL_PAYOUT)));
    }

    private BigDecimal extractExpectedMinimumPrizePoolPayoutForRow(int row) {
        return new BigDecimal(getText(row, columnIndex(Columns.MINIMUM_PRIZE_POOL_PAYOUT)));
    }

    private BigDecimal extractEntryFeePrizePoolPayoutForRow(int row) {
        return new BigDecimal(getText(row, columnIndex(Columns.ENTRY_FEE_PRIZE_POOL_PAYOUT)));
    }

    private int extractPlayerCount() {
        return Integer.parseInt(getText(0, 1));
    }
}
