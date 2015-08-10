package com.yazino.platform.model.tournament;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.PlayerAtTableInformation;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class PlayerGroupTest {

    private final AtomicLong idSource = new AtomicLong(1);
    private final AtomicLong accountIdSource = new AtomicLong(101);

    private PlayerGroup unit;

    @Before
    public void setUp() {
        unit = new PlayerGroup();
    }

    @Test
    public void itemsAddedRetainOrder() {
        unit.add(createPlayer(1));
        unit.add(createPlayer(2));
        unit.add(createPlayer(3));

        int index = 1;
        for (TournamentPlayer tournamentPlayer : unit) {
            Assert.assertEquals("Player at index " + index + " does not match", index, tournamentPlayer.getPlayerId().intValue());
            ++index;
        }
    }

    @Test
    public void itemsAddedUsingAListRetainOrder() {
        unit.addAll(Arrays.asList(createPlayer(1), createPlayer(2), createPlayer(3)));

        int index = 1;
        for (TournamentPlayer tournamentPlayer : unit) {
            Assert.assertEquals("Player at index " + index + " does not match", index, tournamentPlayer.getPlayerId().intValue());
            ++index;
        }
    }

    @Test
    public void itemsCanBeExtractedAsAList() {
        final List<TournamentPlayer> players = Arrays.asList(createPlayer(1), createPlayer(2), createPlayer(3));
        unit.addAll(players);

        Assert.assertEquals(players, unit.asList());
    }

    @Test
    public void accountIdsCanBeExtractedAsAList() {
        unit.addAll(Arrays.asList(createPlayer(1), createPlayer(2), createPlayer(3)));

        final Map<BigDecimal, BigDecimal> expectedAccountIds = new HashMap<BigDecimal, BigDecimal>();
        expectedAccountIds.put(BigDecimal.valueOf(1), BigDecimal.valueOf(101));
        expectedAccountIds.put(BigDecimal.valueOf(2), BigDecimal.valueOf(102));
        expectedAccountIds.put(BigDecimal.valueOf(3), BigDecimal.valueOf(103));

        Assert.assertEquals(expectedAccountIds, unit.asAccountIdList());
    }

    @Test
    public void playersCanBeExtractedAsAnInformationCollection() {
        unit.addAll(Arrays.asList(createPlayer(1), createPlayer(2), createPlayer(3)));

        final Collection<PlayerAtTableInformation> expectedPlayers = Arrays.asList(
                new PlayerAtTableInformation(new GamePlayer(BigDecimal.valueOf(1), null, "player 1"), createPropertyMap(1)),
                new PlayerAtTableInformation(new GamePlayer(BigDecimal.valueOf(2), null, "player 2"), createPropertyMap(2)),
                new PlayerAtTableInformation(new GamePlayer(BigDecimal.valueOf(3), null, "player 3"), createPropertyMap(3)));

        Assert.assertEquals(expectedPlayers, unit.asPlayerInformationCollection());
    }

    private Map<String, String> createPropertyMap(final int i) {
        final HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("property" + i, "value" + i);
        return properties;
    }

    private TournamentPlayer createPlayer(final int i) {
        final TournamentPlayer player = new TournamentPlayer(
                BigDecimal.valueOf(idSource.getAndIncrement()),
                "player " + i,
                BigDecimal.valueOf(accountIdSource.getAndIncrement()),
                TournamentPlayerStatus.ADDITION_PENDING);
        player.setProperties(createPropertyMap(i));
        return player;
    }
}
