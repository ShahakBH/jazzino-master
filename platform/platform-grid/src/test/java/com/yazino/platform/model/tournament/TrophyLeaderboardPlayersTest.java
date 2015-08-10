package com.yazino.platform.model.tournament;

import com.yazino.platform.tournament.TrophyLeaderboardPlayer;
import com.yazino.platform.tournament.TrophyLeaderboardPlayers;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

public class TrophyLeaderboardPlayersTest {
    private TrophyLeaderboardPlayers unit;

    @Before
    public void setUp() throws Exception {
        unit = new TrophyLeaderboardPlayers();
    }

    @Test
    public void shouldAddPlayer() {
        final TrophyLeaderboardPlayer player = new TrophyLeaderboardPlayer(BigDecimal.valueOf(1), "player 1", "picture 1");
        unit.addPlayer(player);
        final List<TrophyLeaderboardPlayer> result = unit.getOrderedByPosition();
        assertEquals(1, result.size());
        assertEquals(player, result.get(0));
    }

    @Test
    public void shouldUpdateOrderBasedOnPosition() {
        final TrophyLeaderboardPlayer player4 = new TrophyLeaderboardPlayer(BigDecimal.valueOf(4), "player 4", "picture 4");
        player4.incrementPoints(3);
        final TrophyLeaderboardPlayer player1 = new TrophyLeaderboardPlayer(BigDecimal.valueOf(1), "player 1", "picture 1");
        player1.incrementPoints(10);
        final TrophyLeaderboardPlayer player3 = new TrophyLeaderboardPlayer(BigDecimal.valueOf(3), "player 3", "picture 3");
        player3.incrementPoints(5);
        final TrophyLeaderboardPlayer player2 = new TrophyLeaderboardPlayer(BigDecimal.valueOf(2), "player 2", "picture 2");
        player2.incrementPoints(5);
        unit.addPlayer(player4);
        unit.addPlayer(player3);
        unit.addPlayer(player2);
        unit.addPlayer(player1);
        unit.updatePlayersPositions();
        final List<TrophyLeaderboardPlayer> expected = new ArrayList<TrophyLeaderboardPlayer>(Arrays.asList(player1, player3, player2, player4));
        final List<TrophyLeaderboardPlayer> result = unit.getOrderedByPosition();
        assertEquals(expected, result);
    }

    @Test
    public void shouldFindPlayerById() {
        final TrophyLeaderboardPlayer player1 = new TrophyLeaderboardPlayer(BigDecimal.valueOf(1), "player 1", "picture 1");
        final TrophyLeaderboardPlayer player2 = new TrophyLeaderboardPlayer(BigDecimal.valueOf(2), "player 2", "picture 2");
        final TrophyLeaderboardPlayer player3 = new TrophyLeaderboardPlayer(BigDecimal.valueOf(3), "player 3", "picture 3");
        final TrophyLeaderboardPlayer player4 = new TrophyLeaderboardPlayer(BigDecimal.valueOf(4), "player 4", "picture 4");
        unit.addPlayer(player4);
        unit.addPlayer(player3);
        unit.addPlayer(player2);
        unit.addPlayer(player1);
        assertEquals(player3, unit.findPlayer(BigDecimal.valueOf(3)));
        assertNull(unit.findPlayer(BigDecimal.TEN));
    }

    @Test
    public void shouldGroupPlayersByPosition() {
        final TrophyLeaderboardPlayer player1 = new TrophyLeaderboardPlayer(1, BigDecimal.valueOf(1), "player 1", 10, "picture 1");
        final TrophyLeaderboardPlayer player2 = new TrophyLeaderboardPlayer(2, BigDecimal.valueOf(2), "player 2", 5, "picture 2");
        final TrophyLeaderboardPlayer player3 = new TrophyLeaderboardPlayer(2, BigDecimal.valueOf(3), "player 3", 5, "picture 3");
        final TrophyLeaderboardPlayer player4 = new TrophyLeaderboardPlayer(4, BigDecimal.valueOf(4), "player 4", 3, "picture 4");
        unit.addPlayer(player1);
        unit.addPlayer(player2);
        unit.addPlayer(player3);
        unit.addPlayer(player4);
        unit.updatePlayersPositions();
        assertEquals(playerSet(player1), unit.getPlayersOnPosition(1));
        assertEquals(playerSet(player2, player3), unit.getPlayersOnPosition(2));
        assertEquals(playerSet(player4), unit.getPlayersOnPosition(4));
        assertEquals(playerSet(), unit.getPlayersOnPosition(5));
        assertEquals(new HashSet<Integer>(Arrays.asList(1, 2, 4)), unit.getPositions());
    }

    @Test
    public void shouldClearPlayers() {
        final TrophyLeaderboardPlayer player1 = new TrophyLeaderboardPlayer(1, BigDecimal.valueOf(1), "player 1", 10, "picture 1");
        final TrophyLeaderboardPlayer player2 = new TrophyLeaderboardPlayer(2, BigDecimal.valueOf(2), "player 2", 5, "picture 2");
        final TrophyLeaderboardPlayer player3 = new TrophyLeaderboardPlayer(2, BigDecimal.valueOf(3), "player 3", 5, "picture 3");
        final TrophyLeaderboardPlayer player4 = new TrophyLeaderboardPlayer(4, BigDecimal.valueOf(4), "player 4", 3, "picture 4");
        unit.addPlayer(player1);
        unit.addPlayer(player2);
        unit.addPlayer(player3);
        unit.addPlayer(player4);
        unit.updatePlayersPositions();
        unit.clear();
        assertTrue(unit.getOrderedByPosition().isEmpty());
        assertTrue(unit.getPlayersOnPosition(1).isEmpty());
        assertTrue(unit.getPositions().isEmpty());
    }

    private HashSet<TrophyLeaderboardPlayer> playerSet(TrophyLeaderboardPlayer... players) {
        return new HashSet<TrophyLeaderboardPlayer>(Arrays.asList(players));
    }
}
