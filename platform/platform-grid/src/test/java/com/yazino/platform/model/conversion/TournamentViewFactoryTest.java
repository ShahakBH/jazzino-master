package com.yazino.platform.model.conversion;

import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.model.tournament.TournamentPlayerStatus;
import com.yazino.platform.model.tournament.TournamentPlayers;
import com.yazino.platform.processor.tournament.TournamentPayoutCalculator;
import com.yazino.platform.tournament.*;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TournamentViewFactoryTest {

    public static final BigDecimal ENTRY_FEE = BigDecimal.valueOf(1000);
    private TournamentViewFactory unit;
    private Tournament tournament;
    private List<TournamentVariationPayout> payouts = new ArrayList<TournamentVariationPayout>();
    private List<TournamentVariationRound> rounds = new ArrayList<TournamentVariationRound>();
    private TournamentPayoutCalculator payoutCalculator;
    private TournamentPlayers players = new TournamentPlayers();
    private TimeSource timeSource = new SettableTimeSource(0);

    @Before
    public void setUp() throws Exception {
        payoutCalculator = mock(TournamentPayoutCalculator.class);
        tournament = new Tournament();
        tournament.setTournamentId(BigDecimal.ONE);
        tournament.setName("A Tournament");
        tournament.setStartTimeStamp(new DateTime(0));
        tournament.setTournamentStatus(TournamentStatus.RUNNING);
        tournament.setCurrentRoundIndex(0);
        ReflectionTestUtils.setField(tournament, "tournamentPayoutCalculator", payoutCalculator);
        ReflectionTestUtils.setField(tournament, "players", players);
        for (int i = 0; i < 2; i++) {
            rounds.add(new TournamentVariationRound(1, 0, 10, BigDecimal.ONE, "client", BigDecimal.valueOf(1), "round " + i));
        }
        tournament.setTournamentVariationTemplate(new TournamentVariationTemplate(BigDecimal.ONE, TournamentType.PRESET, "t1",
                ENTRY_FEE, BigDecimal.valueOf(2000), BigDecimal.ZERO, BigDecimal.valueOf(2222), 3, 1000, "TEXAS_HOLDEM", 10000, "allocator", payouts, rounds));
        unit = new TournamentViewFactory(timeSource);
    }

    @Test
    public void shouldCopyBasicInformationFromTournament() {
        TournamentView view = unit.create(tournament);
        assertEquals(view.getOverview().getName(), tournament.getName());
        assertEquals(view.getOverview().getTournamentId(), tournament.getTournamentId());
        assertEquals(ENTRY_FEE, view.getOverview().getEntryFee());
    }

    @Test
    public void shouldCopyRoundInformationFromTournament() {
        TournamentView view = unit.create(tournament);
        List<TournamentRoundView> result = view.getRounds();
        assertEquals(rounds.size(), result.size());
        for (int i = 0; i < rounds.size(); i++) {
            TournamentVariationRound round = rounds.get(i);
            TournamentRoundView roundView = result.get(i);
            assertEquals(round.getMinimumBalance(), roundView.getMinStake());
        }
    }

    @Test
    public void shouldTransformStatus() {
        assertEquals(TournamentViewDetails.Status.RUNNING, unit.create(tournament).getOverview().getStatus());
    }

    @Test
    public void shouldCreateRanksForTournamentBeforeStart() {
        TournamentPlayer tournamentPlayer = new TournamentPlayer(BigDecimal.ONE, "player 1", BigDecimal.ZERO, TournamentPlayerStatus.ACTIVE, BigDecimal.valueOf(11));
        players.add(tournamentPlayer);
        List<BigDecimal> payouts = new ArrayList<BigDecimal>();
        for (int i = 0; i < 3; i++) {
            payouts.add(BigDecimal.valueOf(1001 + i));
        }
        when(payoutCalculator.calculatePrizes(eq(1), anyTemplate())).thenReturn(payouts);
        TournamentView view = unit.create(tournament);

        assertEquals(1, view.getPlayers().size());

        TournamentRankView playerRank = new TournamentRankView.Builder()
                .playerName(tournamentPlayer.getName())
                .playerId(tournamentPlayer.getPlayerId())
                .tableId(tournamentPlayer.getTableId())
                .status(TournamentRankView.Status.ACTIVE)
                .build();

        assertEquals(playerRank, view.getPlayers().get(tournamentPlayer.getPlayerId()));

        List<TournamentRankView> ranks = view.getRanks();
        assertEquals(3, ranks.size());
        for (int i = 0; i < 3; i++) {
            TournamentRankView expected = new TournamentRankView.Builder()
                    .rank(i + 1)
                    .prize(BigDecimal.valueOf(1001 + i))
                    .build();
            TournamentRankView actual = ranks.get(i);
            assertEquals(expected, actual);
        }
    }

    @Test
    public void shouldCreateRanksForTournamentAfterStartWithFewPlayers() {
        TournamentPlayer tournamentPlayer = new TournamentPlayer(BigDecimal.valueOf(1), "player 1", BigDecimal.ZERO, TournamentPlayerStatus.ACTIVE, BigDecimal.valueOf(11));
        tournamentPlayer.setLeaderboardPosition(1);
        players.add(tournamentPlayer);
        TournamentPlayer tournamentPlayer2 = new TournamentPlayer(BigDecimal.valueOf(2), "player 2", BigDecimal.ZERO, TournamentPlayerStatus.ACTIVE, BigDecimal.valueOf(11));
        tournamentPlayer2.setLeaderboardPosition(1);
        players.add(tournamentPlayer2);
        TournamentPlayer tournamentPlayer3 = new TournamentPlayer(BigDecimal.valueOf(3), "player 3", BigDecimal.ZERO, TournamentPlayerStatus.ACTIVE, BigDecimal.valueOf(11));
        tournamentPlayer3.setLeaderboardPosition(3);
        players.add(tournamentPlayer3);
        TournamentPlayer tournamentPlayer4 = new TournamentPlayer(BigDecimal.valueOf(4), "player 4", BigDecimal.ZERO, TournamentPlayerStatus.ACTIVE, BigDecimal.valueOf(11));
        players.add(tournamentPlayer4);

        Map<TournamentPlayer, BigDecimal> leaderboard = new HashMap<TournamentPlayer, BigDecimal>();
        leaderboard.put(tournamentPlayer, BigDecimal.valueOf(300));
        leaderboard.put(tournamentPlayer2, BigDecimal.valueOf(300));
        leaderboard.put(tournamentPlayer3, BigDecimal.valueOf(200));
        Set<TournamentPlayer> allPlayers = new HashSet<TournamentPlayer>(players.getByMaxLeaderboard(Integer.MAX_VALUE));
        List<BigDecimal> payouts = new ArrayList<BigDecimal>();

        for (int i = 0; i < 5; i++) {
            payouts.add(BigDecimal.valueOf(1001 + i));
        }

        when(payoutCalculator.calculatePayouts(eq(allPlayers), anyTemplate())).thenReturn(leaderboard);
        when(payoutCalculator.calculatePrizes(anyInt(), anyTemplate())).thenReturn(payouts);

        List<TournamentRankView> ranks = unit.create(tournament).getRanks();

        assertEquals(5, ranks.size());
        TournamentRankView playerRank1 = new TournamentRankView.Builder()
                .rank(1)
                .playerName(tournamentPlayer.getName())
                .playerId(tournamentPlayer.getPlayerId())
                .tableId(tournamentPlayer.getTableId())
                .prize(BigDecimal.valueOf(300))
                .status(TournamentRankView.Status.ACTIVE)
                .build();

        assertEquals(playerRank1, ranks.get(0));

        TournamentRankView playerRank = new TournamentRankView.Builder()
                .rank(1)
                .playerName(tournamentPlayer2.getName())
                .playerId(tournamentPlayer2.getPlayerId())
                .tableId(tournamentPlayer2.getTableId())
                .prize(BigDecimal.valueOf(300))
                .status(TournamentRankView.Status.ACTIVE)
                .build();

        assertEquals(playerRank, ranks.get(1));

        for (int i = 3; i < 5; i++) {
            TournamentRankView expected = new TournamentRankView.Builder()
                    .rank(i + 1)
                    .prize(BigDecimal.valueOf(1001 + i))
                    .build();
            TournamentRankView actual = ranks.get(i);
            assertEquals(expected, actual);
        }
    }

    private TournamentVariationTemplate anyTemplate() {
        return any(TournamentVariationTemplate.class);
    }

    @Test
    public void shouldCreateRanksForTournamentAfterSettled() {
        TournamentPlayer tournamentPlayer = new TournamentPlayer(BigDecimal.ONE, "player 1", BigDecimal.ZERO, TournamentPlayerStatus.ACTIVE, BigDecimal.valueOf(11));
        tournamentPlayer.setLeaderboardPosition(1);
        tournamentPlayer.setSettledPrize(BigDecimal.valueOf(230));
        players.add(tournamentPlayer);

        Set<TournamentPlayer> allPlayers = new HashSet<TournamentPlayer>(players.getByMaxLeaderboard(Integer.MAX_VALUE));

        when(payoutCalculator.calculatePayouts(eq(allPlayers), anyTemplate())).thenReturn(Collections.<TournamentPlayer, BigDecimal>emptyMap());
        when(payoutCalculator.calculatePrizes(anyInt(), anyTemplate())).thenReturn(Collections.<BigDecimal>emptyList());

        List<TournamentRankView> ranks = unit.create(tournament).getRanks();

        assertEquals(1, ranks.size());
        TournamentRankView playerRank = new TournamentRankView.Builder()
                .rank(1)
                .playerName(tournamentPlayer.getName())
                .playerId(tournamentPlayer.getPlayerId())
                .tableId(tournamentPlayer.getTableId())
                .prize(BigDecimal.valueOf(230))
                .status(TournamentRankView.Status.ACTIVE)
                .build();

        assertEquals(playerRank, ranks.get(0));
    }
}
