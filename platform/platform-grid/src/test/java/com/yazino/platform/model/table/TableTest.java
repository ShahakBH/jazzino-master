package com.yazino.platform.model.table;

import com.yazino.platform.table.PlayerInformation;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.table.TableType;
import org.junit.Assert;
import org.junit.Test;
import com.yazino.game.api.time.SettableTimeSource;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TableTest {
    private static final com.yazino.game.api.GameType GAME_TYPE = new com.yazino.game.api.GameType("PRINTLN", "PrintLn", Collections.<String>emptySet());

    private Long gameId = 100L;
    private BigDecimal tableId = BigDecimal.valueOf(1000L);
    private Table table;

    private com.yazino.game.api.ScheduledEvent createEvent(long millis) {
        return new com.yazino.game.api.ScheduledEvent(millis, gameId, "EventClass", "TestEvent", new HashMap<String, String>(), false);
    }

    @Test
    public void testOnAdditionOfEventScheduledEventsMantainOrder() {
        List<com.yazino.game.api.ScheduledEvent> addedEventsInTest = new ArrayList<>();
        table = new Table(GAME_TYPE, null, null, true);
        table.setTableId(tableId);
        table.setTableStatus(TableStatus.open);
        SettableTimeSource settableTimeSource = new SettableTimeSource();

        com.yazino.game.api.ScheduledEvent firstEvent = createEvent(0);
        addedEventsInTest.add(firstEvent);
        table.addEvent(settableTimeSource, firstEvent);
        assertEquals(addedEventsInTest, table.getScheduledEvents());
        Long expectedFirstEventTimeStamp = firstEvent.getDelayInMillis() + settableTimeSource.getCurrentTimeStamp();
        assertEquals(expectedFirstEventTimeStamp, table.getNextEventTimestamp());

        settableTimeSource.addMillis(1000);
        com.yazino.game.api.ScheduledEvent secondEvent = createEvent(2000);
        table.addEvent(settableTimeSource, secondEvent);
        addedEventsInTest.add(secondEvent);
        assertEquals(addedEventsInTest, table.getScheduledEvents());
        assertEquals(expectedFirstEventTimeStamp, table.getNextEventTimestamp());
    }

    @Test
    public void testGetPendingEventsReturnsAndRemovesPendEvents() {
        com.yazino.game.api.ScheduledEvent scheduledEventA = createEvent(500L);
        com.yazino.game.api.ScheduledEvent scheduledEventB = createEvent(1000L);
        com.yazino.game.api.ScheduledEvent scheduledEventC = createEvent(1500L);
        SettableTimeSource settableTimeSource = new SettableTimeSource();
        table = new Table(GAME_TYPE, null,null, false);
        table.setTableId(tableId);
        table.addEvents(settableTimeSource, Arrays.asList(scheduledEventA, scheduledEventB, scheduledEventC));
        settableTimeSource.addMillis(1000L);
        List<com.yazino.game.api.ScheduledEvent> expectedPendingEvents = new ArrayList<>();
        expectedPendingEvents.add(scheduledEventA);
        expectedPendingEvents.add(scheduledEventB);
        assertEquals(expectedPendingEvents, table.getPendingEvents(settableTimeSource));
        List<com.yazino.game.api.ScheduledEvent> expectedRemainingEvents = new ArrayList<>();
        expectedRemainingEvents.add(scheduledEventC);
        assertEquals(expectedRemainingEvents, table.getScheduledEvents());
    }

    @Test
    public void testEventsWithSameTimeCoexistInTable() {
        final com.yazino.game.api.ScheduledEvent scheduledEventA = createEvent(200L);
        final com.yazino.game.api.ScheduledEvent scheduledEventB = createEvent(500L);
        final com.yazino.game.api.ScheduledEvent scheduledEventC = createEvent(500L);
        final com.yazino.game.api.ScheduledEvent scheduledEventD = createEvent(500L);
        final com.yazino.game.api.ScheduledEvent scheduledEventE = createEvent(500L);
        final com.yazino.game.api.ScheduledEvent scheduledEventF = createEvent(1000L);

        table = new Table(GAME_TYPE, null, null, false);
        table.setTableId(tableId);
        SettableTimeSource settableTimeSource = new SettableTimeSource();
        table.addEvents(settableTimeSource, Arrays.asList(scheduledEventA, scheduledEventB, scheduledEventC, scheduledEventD,
                scheduledEventE, scheduledEventF));

        assertEquals(6, table.getScheduledEvents().size());

        assertEquals(scheduledEventA, table.getScheduledEvents().get(0));
        assertEquals(500L, table.getScheduledEvents().get(1).getDelayInMillis());
        assertEquals(500L, table.getScheduledEvents().get(2).getDelayInMillis());
        assertEquals(500L, table.getScheduledEvents().get(3).getDelayInMillis());
        assertEquals(500L, table.getScheduledEvents().get(4).getDelayInMillis());
        assertEquals(scheduledEventF, table.getScheduledEvents().get(5));
    }

    @Test
    public void cleanUnwantedPlayers() {
        table = new Table();

        final HashMap<BigDecimal, Long> expected = new HashMap<>();
        final int numberOfPlayersAtStart = 5;
        final List<BigDecimal> retainedPlayerIds = Arrays.asList(BigDecimal.valueOf(1), BigDecimal.valueOf(4), BigDecimal.valueOf(10));
        Collection<com.yazino.game.api.PlayerAtTableInformation> players = new ArrayList<>();
        for (BigDecimal retainedPlayerId : retainedPlayerIds) {
            players.add(new com.yazino.game.api.PlayerAtTableInformation(new com.yazino.game.api.GamePlayer(retainedPlayerId, null, "foo"), Collections.<String, String>emptyMap()));
        }

        for (long i = 0L; i < numberOfPlayersAtStart; i++) {
            BigDecimal playerId = BigDecimal.valueOf(i);
            final PlayerInformation playerInformation = new PlayerInformation(playerId, "foo", playerId, BigDecimal.ZERO.subtract(playerId), BigDecimal.ZERO);
            playerInformation.setAcknowledgedIncrement(i * 10);
            table.addPlayerToTable(playerInformation);
            if (retainedPlayerIds.contains(playerId)) {
                expected.put(playerId, i * 10);
            }
        }
        final com.yazino.game.api.GameStatus gameStatus = mock(com.yazino.game.api.GameStatus.class);
        final com.yazino.game.api.GameRules gameRules = mock(com.yazino.game.api.GameRules.class);
        when(gameRules.getPlayerInformation(gameStatus)).thenReturn(players);
        table.setCurrentGame(gameStatus);

        table.removePlayersNoLongerAtTable(gameRules);

        final Map<BigDecimal, Long> retainedIncrements = new HashMap<>();
        for (PlayerInformation playerInformation : table.allPlayersAtTable()) {
            retainedIncrements.put(playerInformation.getPlayerId(), playerInformation.getAcknowledgedIncrement());
        }

        Assert.assertEquals(expected, retainedIncrements);
    }

    @Test
    public void updateFreeSeatsSetsValue() {
        final Client dummyClient = new Client("bob");
        dummyClient.setNumberOfSeats(5);

        table = new Table();
        table.setClient(dummyClient);

        table.updateFreeSeats(2);

        assertFalse(table.getFull());
        Assert.assertTrue(table.getHasPlayers());
    }

    @Test
    public void updateFreeSeatsHandlesExcessivePlayers() {
        final Client dummyClient = new Client("bob");
        dummyClient.setNumberOfSeats(1);

        table = new Table();
        table.setClient(dummyClient);

        table.updateFreeSeats(2);

        Assert.assertTrue(table.getFull());
    }

    @Test
    public void updateFreeSeatsHandlesNullPlayers() {
        final Client dummyClient = new Client("bob");
        dummyClient.setNumberOfSeats(5);

        table = new Table();
        table.setClient(dummyClient);

        table.updateFreeSeats(0);

        assertFalse(table.getFull());
    }

    @Test
    public void updateFreeSeatsHandlesNullClient() {
        table = new Table();
        table.updateFreeSeats(0);

        Assert.assertTrue(table.getFull());
    }

    @Test
    public void closeSetsTableStatusToClosingWhenPlayersArePresent() {
        final com.yazino.game.api.GamePlayer[] players = new com.yazino.game.api.GamePlayer[5];

        final Table table = createTable(players);
        table.close();

        assertEquals(TableStatus.closing, table.getTableStatus());
    }

    @Test
    public void equalsShouldNotMatchIfOwnerIdsDiffer() {

        Table t1 = new Table();
        t1.setOwnerId(new BigDecimal(123));

        Table t2 = new Table();
        t2.setOwnerId(new BigDecimal(321));

        assertFalse(t2.equals(t1));

        t1.setOwnerId(null);

        assertFalse(t2.equals(t1));
    }

    private Table createTable(final com.yazino.game.api.GamePlayer[] players) {
        final com.yazino.game.api.GameStatus gameStatus = mock(com.yazino.game.api.GameStatus.class);
        final com.yazino.game.api.GameRules gameRules = mock(com.yazino.game.api.GameRules.class);
        Collection<com.yazino.game.api.PlayerAtTableInformation> playerAtTableInformationCollection = new ArrayList<>();
        for (com.yazino.game.api.GamePlayer player : players) {
            playerAtTableInformationCollection.add(new com.yazino.game.api.PlayerAtTableInformation(player, Collections.<String, String>emptyMap()));
        }
        when(gameRules.getPlayerInformation(gameStatus)).thenReturn(playerAtTableInformationCollection);

        final Table table = new Table();
        table.setGameId(gameId);
        table.setTableStatus(TableStatus.open);
        table.setCurrentGame(gameStatus);

        return table;
    }

    @Test
    public void shouldResolveTypeForTournament() {
        Assert.assertEquals(TableType.PUBLIC, createSimpleTable(null, null).resolveType());
        assertEquals(TableType.PUBLIC, createSimpleTable(true, null).resolveType());
        assertEquals(TableType.TOURNAMENT, createSimpleTable(false, null).resolveType());
        assertEquals(TableType.PRIVATE, createSimpleTable(true, BigDecimal.TEN).resolveType());
        assertEquals(TableType.PRIVATE, createSimpleTable(false, BigDecimal.TEN).resolveType());
    }

    @Test
    public void joiningDesirabilityShouldBeMinIntWhenTableHasNoGameStatus() {
        final com.yazino.game.api.GameRules gameRules = mock(com.yazino.game.api.GameRules.class);
        table = new Table();
        Assert.assertThat(table.getJoiningDesirability(gameRules), is(Integer.MIN_VALUE));
    }

    @Test
    public void joinDesirabilityIsThatOfTheGameStatus() {
        final com.yazino.game.api.GameStatus gameStatus = mock(com.yazino.game.api.GameStatus.class);
        final com.yazino.game.api.GameRules gameRules = mock(com.yazino.game.api.GameRules.class);
        when(gameRules.getJoiningDesirability(gameStatus)).thenReturn(50);
        table = new Table();
        table.setCurrentGame(gameStatus);

        assertThat(table.getJoiningDesirability(gameRules), is(50));
    }

    private Table createSimpleTable(Boolean showInLobby, BigDecimal ownerId) {
        Table table = new Table();
        table.setOwnerId(ownerId);
        table.setShowInLobby(showInLobby);
        return table;
    }

}
