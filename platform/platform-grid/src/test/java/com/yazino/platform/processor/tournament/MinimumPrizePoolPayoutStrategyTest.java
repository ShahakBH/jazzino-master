package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentFactory;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.tournament.TournamentVariationPayout;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MinimumPrizePoolPayoutStrategyTest {

    private final MinimumPrizePoolPayoutStrategy underTest = new MinimumPrizePoolPayoutStrategy();

    // TODO review
    @Test
    public void shouldCalculatePrizes() {
        final List<BigDecimal> actual = underTest.calculatePrizes(BigDecimal.valueOf(100), payouts(0.5, 0.3, 0.2));
        Assert.assertEquals(asList(50, 30, 20), actual);
    }

    @Test
    public void shouldCalculatePayoutForTopPlayers() {
        final List<TournamentPlayer> players = playersWithRank(1, 2, 3);
        Tournament tournament = TournamentFactory.createTournament(BigDecimal.valueOf(100), payouts(0.5, 0.3, 0.2), players);
        final Map<TournamentPlayer, BigDecimal> actual = underTest.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate().getPrizePool(), tournament.getTournamentVariationTemplate().getTournamentPayouts());
        Assert.assertEquals(v(50), actual.get(players.get(0)));
        Assert.assertEquals(v(30), actual.get(players.get(1)));
        Assert.assertEquals(v(20), actual.get(players.get(2)));
    }

    @Test
    public void shouldNotBeAffectedByOrderOfPayouts() {
        final List<TournamentPlayer> players = playersWithRank(1, 2, 3);
        List<TournamentVariationPayout> payouts = payouts(0.5, 0.3, 0.2);
        List<TournamentVariationPayout> payoutsReordered = new ArrayList<TournamentVariationPayout>();
        payoutsReordered.add(payouts.get(0));
        payoutsReordered.add(payouts.get(2));
        payoutsReordered.add(payouts.get(1));
        Tournament tournament = TournamentFactory.createTournament(BigDecimal.valueOf(100), payoutsReordered, players);
        final Map<TournamentPlayer, BigDecimal> actual = underTest.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate().getPrizePool(), tournament.getTournamentVariationTemplate().getTournamentPayouts());
        Assert.assertEquals(v(50), actual.get(players.get(0)));
        Assert.assertEquals(v(30), actual.get(players.get(1)));
        Assert.assertEquals(v(20), actual.get(players.get(2)));
    }

    @Test
    public void shouldCalculatePayoutForPlayersNotAtTheTop() {
        final List<TournamentPlayer> players = playersWithRank(1, 2, 3);
        Tournament tournament = TournamentFactory.createTournament(BigDecimal.valueOf(100), payouts(0.6, 0.4), players);
        final Map<TournamentPlayer, BigDecimal> actual = underTest.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate().getPrizePool(), tournament.getTournamentVariationTemplate().getTournamentPayouts());
        Assert.assertEquals(v(60), actual.get(players.get(0)));
        Assert.assertEquals(v(40), actual.get(players.get(1)));
        Assert.assertEquals(null, actual.get(players.get(2)));
    }

    @Test
    public void shouldCalculatePayoutForPlayersSplittingPrize() {
        final List<TournamentPlayer> players = playersWithRank(1, 1, 3);
        Tournament tournament = TournamentFactory.createTournament(BigDecimal.valueOf(100), payouts(0.6, 0.4), players);
        final Map<TournamentPlayer, BigDecimal> actual = underTest.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate().getPrizePool(), tournament.getTournamentVariationTemplate().getTournamentPayouts());
        Assert.assertEquals(v(50), actual.get(players.get(0)));
        Assert.assertEquals(v(50), actual.get(players.get(1)));
        Assert.assertEquals(null, actual.get(players.get(2)));
    }

    // TODO is this sufficiently testing rounding?  what about 33.67, 33.5, etc.?
    @Test
    public void shouldCalculatePayoutForPlayersRoundingPrize() {
        final List<TournamentPlayer> players = playersWithRank(1, 1, 1);
        Tournament tournament = TournamentFactory.createTournament(BigDecimal.valueOf(100), payouts(0.5, 0.3, 0.2), players);
        final Map<TournamentPlayer, BigDecimal> actual = underTest.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate().getPrizePool(), tournament.getTournamentVariationTemplate().getTournamentPayouts());
        Assert.assertEquals(v(33.33), actual.get(players.get(0)));
        Assert.assertEquals(v(33.33), actual.get(players.get(1)));
        Assert.assertEquals(v(33.33), actual.get(players.get(2)));
    }

    private BigDecimal v(double value) {
        return BigDecimal.valueOf(value).setScale(2);
    }

    private List<TournamentPlayer> playersWithRank(int... rankValues) {
        final List<TournamentPlayer> players = new ArrayList<TournamentPlayer>();
        int id = 0;
        for (int rank : rankValues) {
            final TournamentPlayer player = new TournamentPlayer(BigDecimal.valueOf(id++), "player " + id);
            player.setLeaderboardPosition(rank);
            players.add(player);
        }
        return players;
    }

    // TODO revert to private
    public static List<TournamentVariationPayout> payouts(double... payoutValues) {
        final List<TournamentVariationPayout> payouts = new ArrayList<TournamentVariationPayout>();
        int rank = 1;
        for (double payout : payoutValues) {
            payouts.add(new TournamentVariationPayout(rank++, BigDecimal.valueOf(payout)));
        }
        return payouts;
    }

    private List<BigDecimal> asList(double... values) {
        final List<BigDecimal> result = new ArrayList<BigDecimal>();
        for (double value : values) {
            result.add(new BigDecimal(value).setScale(2));
        }
        return result;
    }
}
