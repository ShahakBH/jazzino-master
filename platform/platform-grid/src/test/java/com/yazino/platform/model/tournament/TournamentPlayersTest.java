package com.yazino.platform.model.tournament;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class TournamentPlayersTest {
    TournamentPlayers underTest;

    @Before
    public void init() {
        underTest = new TournamentPlayers();
    }

    @Test
    public void getPlayerIdsReturndIdsOfPlayers() {
        BigDecimal player1 = BigDecimal.ONE;
        underTest.add(new TournamentPlayer(player1, "player ONE"));
        Set<BigDecimal> expected = new HashSet<BigDecimal>();
        expected.add(player1);
        Assert.assertEquals(expected, underTest.getPlayerIds());
    }

    @Test
    public void getPlayerIdsCanReturnEmptySet() {
        Set<BigDecimal> expected = new HashSet<BigDecimal>();
        Assert.assertEquals(expected, underTest.getPlayerIds());

    }

}
