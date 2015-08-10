package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.ErrorHostDocument;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.PlayerInformation;
import org.junit.Before;
import org.junit.Test;
import com.yazino.game.api.Command;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.GameRules;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TournamentPlayerValidatorTest {

    private final DestinationFactory destinationFactory = mock(DestinationFactory.class);
    private final TournamentPlayerValidator processor = new TournamentPlayerValidator(destinationFactory);

    private final GamePlayer player = new GamePlayer(BigDecimal.ONE, null, "TESTPLAYER");
    private final Table table = new Table();
    private final List<HostDocument> documents = new ArrayList<>();

    private GameRules gameRules = mock(GameRules.class);

    @Before
    public void setup() {
        Destination destination = mock(Destination.class);
        when(destinationFactory.player(any(BigDecimal.class))).thenReturn(destination);
    }

    @Test
    public void shouldReturnTrueWhenNoCommandOwner() {
        Command command = new Command(null, BigDecimal.ONE, 5L, "TEST", Command.CommandType.Game.name());
        boolean shouldContinue = processor.preProcess(gameRules, command, BigDecimal.ZERO, new Table(), "TEST", documents);
        assertTrue(shouldContinue);
    }

    @Test
    public void shouldReturnTrueWhenPrivateTableAndPlayerIsNotAtTheTable() {
        Command command = new Command(player, BigDecimal.ONE, 5L, "TEST", Command.CommandType.Game.name());
        table.setOwnerId(BigDecimal.valueOf(5));
        boolean shouldContinue = processor.preProcess(gameRules, command, BigDecimal.ZERO, table, "TEST", documents);
        assertTrue(shouldContinue);
    }

    @Test
    public void shouldReturnTrueWhenPrivateTableAndPlayerIsAtTheTable() {
        Command command = new Command(player, BigDecimal.ONE, 5L, "TEST", Command.CommandType.Game.name());
        table.addPlayerToTable(new PlayerInformation(player.getId()));
        table.setOwnerId(BigDecimal.valueOf(5));
        boolean shouldContinue = processor.preProcess(gameRules, command, BigDecimal.ZERO, table, "TEST", documents);
        assertTrue(shouldContinue);
    }

    @Test
    public void shouldReturnTrueWhenPublicTableAndPlayerIsNotAtTheTable() {
        Command command = new Command(player, BigDecimal.ONE, 5L, "TEST", Command.CommandType.Game.name());
        table.setShowInLobby(true);
        boolean shouldContinue = processor.preProcess(gameRules, command, BigDecimal.ZERO, table, "TEST", documents);
        assertTrue(shouldContinue);
    }

    @Test
    public void shouldReturnTrueWhenPublicTableAndPlayerIsAtTheTable() {
        Command command = new Command(player, BigDecimal.ONE, 5L, "TEST", Command.CommandType.Game.name());
        table.setShowInLobby(true);
        table.addPlayerToTable(new PlayerInformation(player.getId()));
        boolean shouldContinue = processor.preProcess(gameRules, command, BigDecimal.ZERO, table, "TEST", documents);
        assertTrue(shouldContinue);
    }

    @Test
    public void shouldReturnTrueWhenTournamenTableAndPlayerIsAtTheTable() {
        Command command = new Command(player, BigDecimal.ONE, 5L, "TEST", Command.CommandType.Game.name());
        table.setShowInLobby(false);
        table.addPlayerToTable(new PlayerInformation(player.getId()));
        boolean shouldContinue = processor.preProcess(gameRules, command, BigDecimal.ZERO, table, "TEST", documents);
        assertTrue(shouldContinue);
    }

    @Test
    public void shouldReturnFalseWhenTournamentTableAndPlayerIsNotAtTheTable() {
        Command command = new Command(player, BigDecimal.ONE, 5L, "TEST", Command.CommandType.Game.name());
        table.setTableId(BigDecimal.ONE);
        table.setGameId(1L);
        table.setShowInLobby(false);
        boolean shouldContinue = processor.preProcess(gameRules, command, BigDecimal.ZERO, table, "TEST", documents);
        assertFalse(shouldContinue);
        assertEquals(1, documents.size());
        assertEquals(ErrorHostDocument.class, documents.get(0).getClass());
    }

}
