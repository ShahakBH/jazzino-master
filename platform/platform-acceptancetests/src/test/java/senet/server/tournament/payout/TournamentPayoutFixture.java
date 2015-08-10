package senet.server.tournament.payout;

import com.google.common.collect.Sets;
import com.yazino.platform.model.tournament.*;
import com.yazino.platform.tournament.TournamentException;
import com.yazino.platform.tournament.TournamentVariationPayout;
import fit.Fixture;
import fitlibrary.DoFixture;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class TournamentPayoutFixture extends DoFixture {

    private SetupPayoutsFixture payoutSetup = new SetupPayoutsFixture();
    private SetupRanksFixture rankSetup = new SetupRanksFixture();
    private BigDecimal entryFee;
    private BigDecimal minimumPrizePool;
    private Integer numberOfPlayers;

    public void entryFeeIs(String amount) {
        entryFee = new BigDecimal(amount);
    }

    public void minimumPrizePoolIs(String minimumPrizePool) {
        this.minimumPrizePool = new BigDecimal(minimumPrizePool);
    }

    public void numberOfPlayersIs(String numberOfPlayers) {
        this.numberOfPlayers = Integer.parseInt(numberOfPlayers);
    }

    protected TournamentBuilder prepareTournamentBuilder() {
        TournamentBuilder builder = new TournamentBuilder();
        builder.withEntryFee(entryFee);
        builder.withMinimumPrizePool(minimumPrizePool);
        if (minimumPrizePool == null) {
            builder.withMinimumPrizePool(payoutSetup.calculateTotalPrize());
        }
        builder.withMinimumPayouts(getMinimumPayouts());
        return builder;
    }


    protected void addPlayers(SetupRanksFixture rankSetup, Integer numberOfPlayers, TournamentBuilder tournamentBuilder) {
        TournamentPlayers players = new TournamentPlayers();
        addRankedPlayers(rankSetup, players);
        if (numberOfPlayers != null) {
            padRanks(players, rankSetup.getPlayers().size() + 1, numberOfPlayers);
        }
        tournamentBuilder.withPlayers(players);
    }

    private void addRankedPlayers(SetupRanksFixture rankSetup, TournamentPlayers players) {
        for (TournamentPlayer player : rankSetup.getPlayers()) {
			players.add(player);
        }
    }

    protected void padRanks(TournamentPlayers players, int firstRank, int lastRank) {
        for (int rank = firstRank; rank <= lastRank; rank++) {
			TournamentPlayer player = new PlayerBuilder()
                    .withId(new BigDecimal(1000 + rank))
                    .withName("AdditionalPlayer" + rank)
                    .withLeaderboardPosition(rank)
					.build();
			players.add(player);
        }
    }

    protected void autoConfigurePot(Tournament tournament) {
        tournament.setPot(tournament.getTournamentVariationTemplate().getEntryFee().multiply(new BigDecimal(Integer.toString(tournament.getPlayers().size()))));
    }


    public Set<TournamentVariationPayout> getMinimumPayouts() {
        return Sets.newHashSet(payoutSetup.getPayouts());
    }

    // HandlingTies, NotEnoughPlayers, PayoutRounding

    /**
     * 1 or both required
     * finalRankForTournament - required - named players assigned chips resulting in ranks
     * number of players - optional
     */
    public Fixture payouts() throws TournamentException {
        final List<TournamentPlayer> winners = rankSetup.getPlayers();
        int playerCount = rankSetup.getPlayers().size();
        TournamentBuilder tournamentBuilder = prepareTournamentBuilder();
        addPlayers(rankSetup, numberOfPlayers, tournamentBuilder);
        Tournament tournament = tournamentBuilder.build();
        autoConfigurePot(tournament);
        return new PayoutCheckerFixture(tournament);
    }

    // MinimumPayout
    public Fixture payouts2() {
        return new PayoutsFixture(this);
    }

    // NotEnoughPlayers, PayoutRounding
    public Fixture payouts3() {
        return new PayoutsFixture(this, PayoutsFixture.Columns.ACTUAL_PAYOUT);
    }

    public Fixture finalRankForTournament() {
        rankSetup = new SetupRanksFixture();
        return rankSetup;
    }

    public Fixture payoutForTournament() {
        return payoutSetup;
    }
}
