package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.*;
import com.yazino.platform.tournament.TournamentVariationPayout;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TournamentPayoutCalculatorTest {

    private final MinimumPrizePoolPayoutStrategy minimumPrizePoolPayoutStrategy = mock(MinimumPrizePoolPayoutStrategy.class);
    private final EntryFeePrizePoolPayoutStrategy entryFeePrizePoolPayoutStrategy = mock(EntryFeePrizePoolPayoutStrategy.class);
    private final TournamentPayoutCalculator calculator = new TournamentPayoutCalculator();

    @Before
    public void setup() {
        calculator.setEntryFeePrizePoolPayoutStrategy(entryFeePrizePoolPayoutStrategy);
        calculator.setMinimumPrizePoolPayoutStrategy(minimumPrizePoolPayoutStrategy);
    }

    @Test
    public void invokes_all_strategies_and_returns_maximum_payout__minimum_is_low() {

        Tournament tournament = TournamentFactory.createTournament(new BigDecimal("40000"), new ArrayList<TournamentVariationPayout>(), new ArrayList<TournamentPlayer>());

        Map<TournamentPlayer, BigDecimal> LOW_DISTRIBUTION = fixedDistribution("1", tournament);
        Map<TournamentPlayer, BigDecimal> HIGH_DISTRIBUTION = fixedDistribution("2", tournament);

        when(minimumPrizePoolPayoutStrategy.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate().getPrizePool(), tournament.getTournamentVariationTemplate().getTournamentPayouts())).thenReturn(LOW_DISTRIBUTION);
        when(entryFeePrizePoolPayoutStrategy.calculatePayouts(tournament.tournamentPlayers(), tournament.getPot())).thenReturn(HIGH_DISTRIBUTION);

        Map<TournamentPlayer, BigDecimal> payouts = calculator.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate());

        assertEquals(HIGH_DISTRIBUTION, payouts);

    }

    @Test
    public void invokes_all_strategies_and_returns_maximum_payout__minimum_is_high() {

        Tournament tournament = TournamentFactory.createTournament(new BigDecimal("40000"), new ArrayList<TournamentVariationPayout>(), new ArrayList<TournamentPlayer>());

        Map<TournamentPlayer, BigDecimal> LOW_DISTRIBUTION = fixedDistribution("1", tournament);
        Map<TournamentPlayer, BigDecimal> HIGH_DISTRIBUTION = fixedDistribution("2", tournament);

        when(minimumPrizePoolPayoutStrategy.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate().getPrizePool(), tournament.getTournamentVariationTemplate().getTournamentPayouts())).thenReturn(HIGH_DISTRIBUTION);
        when(entryFeePrizePoolPayoutStrategy.calculatePayouts(tournament.tournamentPlayers(), tournament.getPot())).thenReturn(LOW_DISTRIBUTION);

        Map<TournamentPlayer, BigDecimal> payouts = calculator.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate());

        assertEquals(HIGH_DISTRIBUTION, payouts);

    }

    @Test
    public void invokes_all_strategies_and_returns_maximum_payout__high_spread_between_strategies() {

        Tournament tournament = TournamentFactory.createTournament(new BigDecimal("40000"), new ArrayList<TournamentVariationPayout>(), new ArrayList<TournamentPlayer>());

        Map<TournamentPlayer, BigDecimal> LOW_HIGH_DISTRIBUTION = alternatingDistribution("1", "2", tournament);
        Map<TournamentPlayer, BigDecimal> HIGH_LOW_DISTRIBUTION = alternatingDistribution("2", "1", tournament);

        when(minimumPrizePoolPayoutStrategy.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate().getPrizePool(), tournament.getTournamentVariationTemplate().getTournamentPayouts())).thenReturn(LOW_HIGH_DISTRIBUTION);
        when(entryFeePrizePoolPayoutStrategy.calculatePayouts(tournament.tournamentPlayers(), tournament.getPot())).thenReturn(HIGH_LOW_DISTRIBUTION);

        Map<TournamentPlayer, BigDecimal> payouts = calculator.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate());

        assertEquals(fixedDistribution("2", tournament), payouts);

    }

    @Test
    public void invokes_all_strategies_and_returns_maximum_payout__distributions_with_unequal_number_of_ranks() {

        Tournament tournament = TournamentFactory.createTournament(new BigDecimal("40000"), new ArrayList<TournamentVariationPayout>(), new ArrayList<TournamentPlayer>());

        Map<TournamentPlayer, BigDecimal> LOW_HIGH_DISTRIBUTION = fixedDistribution("1", tournament, tournament.tournamentPlayers().size() - 1);
        Map<TournamentPlayer, BigDecimal> HIGH_LOW_DISTRIBUTION = fixedDistribution("2", tournament);

        when(minimumPrizePoolPayoutStrategy.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate().getPrizePool(), tournament.getTournamentVariationTemplate().getTournamentPayouts())).thenReturn(LOW_HIGH_DISTRIBUTION);
        when(entryFeePrizePoolPayoutStrategy.calculatePayouts(tournament.tournamentPlayers(), tournament.getPot())).thenReturn(HIGH_LOW_DISTRIBUTION);

        Map<TournamentPlayer, BigDecimal> payouts = calculator.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate());

        assertEquals(fixedDistribution("2", tournament), payouts);

    }

    @Test
    public void sample_invocation() {

        Set<TournamentVariationPayout> minimumPayouts = new HashSet<TournamentVariationPayout>();
        minimumPayouts.add(new TournamentVariationPayout(1, new BigDecimal(".5").setScale(2)));
        minimumPayouts.add(new TournamentVariationPayout(2, new BigDecimal(".3").setScale(2)));
        minimumPayouts.add(new TournamentVariationPayout(3, new BigDecimal(".2").setScale(2)));

        int playerCount = 52;

        TournamentPlayers players = new TournamentPlayers();
        for (int rank = 1; rank <= playerCount; rank++) {
            TournamentPlayer player = new PlayerBuilder()
                    .withId(new BigDecimal(rank))
                    .withName("Player" + rank)
                    .withLeaderboardPosition(rank)
                    .build();
            players.add(player);
        }


        Tournament tournament = new TournamentBuilder()
                .withPlayers(players)
                .withMinimumPayouts(minimumPayouts)
                .withEntryFee(new BigDecimal("1000"))
                .withMinimumPrizePool(new BigDecimal("40000"))
                .build();

        tournament.setPot(tournament.getTournamentVariationTemplate().getEntryFee().multiply(new BigDecimal(Integer.toString(playerCount))));

        TournamentPayoutCalculator calculator = new TournamentPayoutCalculator();

        Map<TournamentPlayer, BigDecimal> payouts = calculator.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate());

        assertEquals(new BigDecimal("20000.00"), getPayoutForAnyPlayerOfRank(1, payouts));
        assertEquals(new BigDecimal("12000.00"), getPayoutForAnyPlayerOfRank(2, payouts));
        assertEquals(new BigDecimal("8000.00"), getPayoutForAnyPlayerOfRank(3, payouts));
    }

    @Test
    public void rounds_down_when_3rd_decimal_place_equal_to_5() {

        Set<TournamentVariationPayout> minimumPayouts = new HashSet<TournamentVariationPayout>();
        minimumPayouts.add(new TournamentVariationPayout(1, new BigDecimal(".5").setScale(2)));
        minimumPayouts.add(new TournamentVariationPayout(2, new BigDecimal(".3").setScale(2)));
        minimumPayouts.add(new TournamentVariationPayout(3, new BigDecimal(".2").setScale(2)));

        int playerCount = 8;
        TournamentBuilder tournamentBuilder = new TournamentBuilder()
                .withMinimumPayouts(minimumPayouts)
                .withEntryFee(new BigDecimal("1"))
                .withMinimumPrizePool(new BigDecimal("33"));

        TournamentPlayers players = new TournamentPlayers();
        for (int i = 0; i < playerCount; i++) {
            TournamentPlayer player = new PlayerBuilder()
                    .withId(new BigDecimal(Integer.toString(i)))
                    .withName("Player" + (i + 1))
                    .withLeaderboardPosition(i < 8 ? 1 : (i + 1))
                    .build();
            players.add(player);
        }
        tournamentBuilder.withPlayers(players);
        Tournament tournament = tournamentBuilder.build();
        tournament.setPot(tournament.getTournamentVariationTemplate().getEntryFee().multiply(new BigDecimal(Integer.toString(playerCount))));

        TournamentPayoutCalculator calculator = new TournamentPayoutCalculator();
        Map<TournamentPlayer, BigDecimal> payouts = calculator.calculatePayouts(tournament.tournamentPlayers(), tournament.getTournamentVariationTemplate());

        assertEquals(new BigDecimal("4.12"), getPayoutForPlayerNamed("Player1", payouts));
        assertEquals(new BigDecimal("4.12"), getPayoutForPlayerNamed("Player2", payouts));
        assertEquals(new BigDecimal("4.12"), getPayoutForPlayerNamed("Player3", payouts));
        assertEquals(new BigDecimal("4.12"), getPayoutForPlayerNamed("Player4", payouts));
        assertEquals(new BigDecimal("4.12"), getPayoutForPlayerNamed("Player5", payouts));
        assertEquals(new BigDecimal("4.12"), getPayoutForPlayerNamed("Player6", payouts));
        assertEquals(new BigDecimal("4.12"), getPayoutForPlayerNamed("Player7", payouts));
        assertEquals(new BigDecimal("4.12"), getPayoutForPlayerNamed("Player8", payouts));


    }

    @Test
    public void uses_template_prize_pool_when_minimum_payout_is_greater_than_entryfee_payout() throws Exception {
        // 3 players
        // prize pool is 40k
        // result should be 40k
    }

    @Test
    public void prize_pool_includes_minumum_payout_and_entry_fee_payout() throws Exception {
        // 4 players
        // prize pool is 40k
        // entry pool is 4k
        // result should be 40k + (x*4k)

    }

    @Test
    public void uses_entryfee_pool_when_minimum_payout_is_less_than_entryfee_payout() throws Exception {
        // 100 players (1st must pay greater than minimum)
        // prize pool is 40k
        // result should be 100k * (x)
    }

    private BigDecimal getPayoutForPlayerNamed(String name, Map<TournamentPlayer, BigDecimal> payouts) {
        for (TournamentPlayer candidate : payouts.keySet()) {
            if (name.equals(candidate.getName())) {
                return payouts.get(candidate);
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getPayoutForAnyPlayerOfRank(int rank, Map<TournamentPlayer, BigDecimal> payouts) {
        for (TournamentPlayer candidate : payouts.keySet()) {
            if (candidate.getLeaderboardPosition().equals(rank)) {
                return payouts.get(candidate);
            }
        }
        return BigDecimal.ZERO;
    }

    // TODO factory
    public static Set<TournamentPlayer> createPlayers(int quantity) {
        Set<TournamentPlayer> players = new HashSet<TournamentPlayer>();
        for (int i = 0; i < quantity; i++) {
            BigDecimal id = new BigDecimal(i);
            String name = "Player " + (i + 1);
            TournamentPlayer player = new TournamentPlayer(id, name);
            players.add(player);
        }
        return players;
    }

    private Map<TournamentPlayer, BigDecimal> fixedDistribution(String amountString, Tournament tournament) {
        return fixedDistribution(amountString, tournament.tournamentPlayers());
    }

    private Map<TournamentPlayer, BigDecimal> fixedDistribution(String amountString, Set<TournamentPlayer> players) {
        BigDecimal amount = new BigDecimal(amountString);
        Map<TournamentPlayer, BigDecimal> result = new HashMap<TournamentPlayer, BigDecimal>();
        for (TournamentPlayer player : players) {
            result.put(player, amount);
        }
        return result;
    }

    private Map<TournamentPlayer, BigDecimal> fixedDistribution(String amountString, Tournament tournament, int maxRankedPlayers) {
        return fixedDistribution(amountString, tournament.tournamentPlayers(), maxRankedPlayers);

    }

    private Map<TournamentPlayer, BigDecimal> fixedDistribution(String amountString, Set<TournamentPlayer> players, int maxRankedPlayers) {
        BigDecimal amount = new BigDecimal(amountString);
        int playersRanked = 0;
        Map<TournamentPlayer, BigDecimal> result = new HashMap<TournamentPlayer, BigDecimal>();
        for (TournamentPlayer player : players) {
            result.put(player, amount);
            playersRanked++;
            if (playersRanked == maxRankedPlayers) {
                break;
            }
        }
        return result;
    }

    private Map<TournamentPlayer, BigDecimal> alternatingDistribution(String lowAmountString, String highAmountString, Tournament tournament) {
        return alternatingDistribution(lowAmountString, highAmountString, tournament.tournamentPlayers());
    }

    private Map<TournamentPlayer, BigDecimal> alternatingDistribution(String lowAmountString, String highAmountString, Set<TournamentPlayer> players) {
        BigDecimal lowAmount = new BigDecimal(lowAmountString);
        BigDecimal highAmount = new BigDecimal(highAmountString);
        Map<TournamentPlayer, BigDecimal> result = new HashMap<TournamentPlayer, BigDecimal>();
        boolean low = true;
        for (TournamentPlayer player : players) {
            result.put(player, (low ? lowAmount : highAmount));
            low = !low;
        }
        return result;
    }
}
