package senet.server.tournament.payout;

import com.yazino.platform.model.tournament.*;
import com.yazino.platform.processor.tournament.EntryFeePrizePoolPayoutStrategy;
import fitnesse.fixtures.TableFixture;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Tests the calculations within the {@link com.yazino.platform.processor.tournament.EntryFeePrizePoolPayoutStrategy} class.
 */
public class PayoutDistributionCalculatorFixture extends TableFixture {
    private final EntryFeePrizePoolPayoutStrategy calculator = new EntryFeePrizePoolPayoutStrategy();

    @Override
    protected void doStaticTable(int rows) {
        int nextPlayerId = 0;

        int totalPlayers = Integer.parseInt(getText(0, 1));
        BigDecimal prizePool = new BigDecimal(getText(1, 1));

        TournamentBuilder tournamentBuilder = new TournamentBuilder()
                .withPot(prizePool);

        TournamentPlayers players = new TournamentPlayers();

        int lowestRank = 0;
        for (int row = 3; row < rows; row++) {
            int rank = Integer.valueOf(getText(row, 0));
            int playersAtPosition = Integer.valueOf(getText(row, 1));
            for (int i = 0; i < playersAtPosition; i++) {
                TournamentPlayer player = new PlayerBuilder()
                        .withName("Player" + (nextPlayerId))
                        .withId(new BigDecimal(nextPlayerId++))
                        .withLeaderboardPosition(rank)
                        .build();
                players.add(player);
            }
            lowestRank = Math.max(lowestRank, rank);
        }
        for (int rank = lowestRank + 1; rank <= totalPlayers; rank++) {
            TournamentPlayer player = new PlayerBuilder()
                    .withId(new BigDecimal(nextPlayerId++))
                    .withName("AdditionalPlayer" + rank)
                    .withLeaderboardPosition(rank)
                    .build();
            players.add(player);
        }

        tournamentBuilder.withPlayers(players);
        Tournament tournament = tournamentBuilder.build();

        Map<TournamentPlayer, BigDecimal> payouts = calculator.calculatePayouts(tournament.tournamentPlayers(), tournament.getPot());

        for (int row = 3; row < rows; row++) {
            int rank = Integer.valueOf(getText(row, 0));
            BigDecimal expectedPayout = new BigDecimal(getText(row, 2));
            BigDecimal actualPayout = findPayoutForAnyPlayerOfRank(payouts, rank);

            if (equivalent(expectedPayout, actualPayout)) {
                right(row, 2);
            } else {
                wrong(row, 2, actualPayout.toString());
            }
        }
    }

    private BigDecimal findPayoutForAnyPlayerOfRank(Map<TournamentPlayer, BigDecimal> payouts, int rank) {
        for (TournamentPlayer candidate : payouts.keySet()) {
            if (candidate.getLeaderboardPosition() == rank) {
                return payouts.get(candidate);
            }
        }
        return BigDecimal.ZERO;
    }

    private boolean equivalent(BigDecimal value1, BigDecimal value2) {
        return value1.setScale(2).equals(value2.setScale(2));
    }

}
