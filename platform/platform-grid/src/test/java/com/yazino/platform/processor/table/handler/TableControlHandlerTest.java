package com.yazino.platform.processor.table.handler;

import com.yazino.game.api.GameStatus;
import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableControlMessage;
import com.yazino.platform.model.table.TableControlMessageType;
import com.yazino.platform.persistence.table.TableDAO;
import com.yazino.platform.repository.table.TableRepository;
import com.yazino.platform.table.PlayerInformation;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.test.PrintlnStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.PlayerAtTableInformation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class TableControlHandlerTest {
    private static final BigDecimal TABLE_ID = BigDecimal.TEN;

    @Mock
    private TableRepository tableRepository;
    @Mock
    private TableDAO tableDAO;
    @Mock
    private GameHost gameHost;

    private TableControlHandler controlHandler;

    @Before
    public void setupRepository() {
        MockitoAnnotations.initMocks(this);

        controlHandler = new TableControlHandler();
        controlHandler.setGameHost(gameHost);
        controlHandler.setTableDAO(tableDAO);
        controlHandler.setTableRepository(tableRepository);
    }

    @Test
    public void loadMessageLoadsTheTableFromTheRepositoryWithPendingStatusIfTheTableDoesNotExist() {
        when(tableDAO.findById(TABLE_ID)).thenReturn(table(TABLE_ID, null, "Test Table Name", 0));

        controlHandler.handle(new TableControlMessage(TABLE_ID, TableControlMessageType.LOAD));

        verify(tableRepository).save(table(TABLE_ID, null, "Test Table Name", 0));
    }

    @Test
    public void loadMessageIsIgnoredIfTheTableExists() {
        final Table table = new Table();
        table.setTableId(TABLE_ID);
        table.setTableName("renamed table");

        when(tableRepository.findById(TABLE_ID)).thenReturn(table);

        controlHandler.handle(new TableControlMessage(TABLE_ID, TableControlMessageType.LOAD));

        verifyZeroInteractions(tableDAO);
        verify(tableRepository).findById(TABLE_ID);
        verifyNoMoreInteractions(tableRepository);
    }

    @Test
    public void unloadDropsTableFromSpace() {
        final Table tableNew = new Table();
        tableNew.setTableId(TABLE_ID);
        tableNew.setGameId(0L);

        when(tableRepository.findById(TABLE_ID)).thenReturn(tableNew);

        controlHandler.handle(new TableControlMessage(TABLE_ID, TableControlMessageType.UNLOAD));

        verify(tableRepository).unload(TABLE_ID);
    }

    @Test
    public void closeSetsTableStatusToClosingIfOpen() {
        when(tableRepository.findById(TABLE_ID)).thenReturn(table(TABLE_ID, TableStatus.open, "Closing Table Test", 3));

        controlHandler.handle(new TableControlMessage(TABLE_ID, TableControlMessageType.CLOSE));

        verify(tableRepository).save(table(TABLE_ID, TableStatus.closing, "Closing Table Test", 3));
    }

    @Test
    public void shutdownNotifiesTheGameHostAndSetsTableStatusToClosedIfOpen() {
        when(tableRepository.findById(TABLE_ID)).thenReturn(table(TABLE_ID, TableStatus.open, "Closing Table Test", 3));

        controlHandler.handle(new TableControlMessage(TABLE_ID, TableControlMessageType.SHUTDOWN));

        verify(tableRepository).save(table(TABLE_ID, TableStatus.closed, "Closing Table Test", 3));
        verify(gameHost).shutdown(table(TABLE_ID, TableStatus.closed, "Closing Table Test", 3));
    }

    @Test
    public void resetLoadsResetsAndPersistsTable() {
        when(tableRepository.findById(TABLE_ID)).thenReturn(table(TABLE_ID, TableStatus.error, "Reset Table Test", 3));

        controlHandler.handle(new TableControlMessage(TABLE_ID, TableControlMessageType.RESET));

        final ArgumentCaptor<Table> tableCaptor = ArgumentCaptor.forClass(Table.class);
        verify(tableRepository).save(tableCaptor.capture());
        assertThat(tableCaptor.getValue().getCurrentGame(), is(nullValue()));
        assertThat(tableCaptor.getValue().getTableStatus(), is(equalTo(TableStatus.open)));
    }

    @Test
    public void reopenSetsTableStatusToOpenAndResetsGameStatusIfClosingOrClosed() {
        when(tableRepository.findById(TABLE_ID)).thenReturn(table(TABLE_ID, TableStatus.closed, "Closing Table Test", 3));

        controlHandler.handle(new TableControlMessage(TABLE_ID, TableControlMessageType.REOPEN));

        final ArgumentCaptor<Table> tableCaptor = ArgumentCaptor.forClass(Table.class);
        verify(tableRepository).save(tableCaptor.capture());
        assertThat(tableCaptor.getValue().getCurrentGame(), is(nullValue()));
        assertThat(tableCaptor.getValue().getTableStatus(), is(equalTo(TableStatus.open)));
    }

    @Test
    public void nonExistentTablesAreLoadedIfTheyAreNotYetPresent() {
        final Table table = new Table();
        table.setTableId(TABLE_ID);
        table.setTableName("renamed table");

        when(tableRepository.findById(TABLE_ID)).thenReturn(table);

        controlHandler.handle(new TableControlMessage(TABLE_ID, TableControlMessageType.REOPEN));

        final ArgumentCaptor<Table> tableCaptor = ArgumentCaptor.forClass(Table.class);
        verify(tableRepository).save(tableCaptor.capture());
        assertThat(tableCaptor.getValue(), is(equalTo(table)));
    }

    @Test(expected = RuntimeException.class)
    public void nonExistentTablesThrowARuntimeExceptionIfTheyCannotBeLoaded() {
        controlHandler.handle(new TableControlMessage(TABLE_ID, TableControlMessageType.REOPEN));
    }

    private Table table(final BigDecimal tableId,
                        final TableStatus status,
                        final String name,
                        final int numPlayers) {
        final Table table = new Table();
        table.setTableId(tableId);
        table.setTableStatus(status);
        table.setTableName(name);
        table.setGameTypeId("PRINTLN");
        table.setGameId(100L);
        table.setShowInLobby(true);

        final PrintlnStatus gameStatus = new PrintlnStatus();
        final Collection<PlayerAtTableInformation> playerAtTableInformationCollection = new ArrayList<PlayerAtTableInformation>();
        for (int i = 0; i < numPlayers; i++) {
            final BigDecimal id = BigDecimal.valueOf(i);
            playerAtTableInformationCollection.add(new PlayerAtTableInformation(new GamePlayer(id, id.add(BigDecimal.valueOf(100)), "name " + i), Collections.<String, String>emptyMap()));
            table.addPlayerToTable(new PlayerInformation(id, "player" + id, id, BigDecimal.TEN, BigDecimal.ZERO));
        }
        gameStatus.setPlayersAtTable(playerAtTableInformationCollection);
        table.setCurrentGame(new GameStatus(gameStatus));
        return table;
    }
}
