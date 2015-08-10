package com.yazino.platform.processor.table.handler;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableRequestType;
import com.yazino.platform.model.table.TestAlterGameRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.yazino.game.api.GameStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class TestAlterGameHandlerTest {
    private static final BigDecimal TABLE_ID = BigDecimal.valueOf(4534);
    private static final long DEFAULT_INCREMENT = 7L;
    @Mock
    private GameHost gameHost;

    private TestAlterGameHandler underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new TestAlterGameHandler();
    }

    @Test
    public void handlerShouldAcceptTestAlterGameRequests() {
        assertThat(underTest.accepts(TableRequestType.TEST_ALTER_GAME), is(equalTo(true)));
    }

    @Test
    public void handlerShouldUpdateGameStatusForTable() {
        final GameStatus newGame = mock(GameStatus.class);
        final TestAlterGameRequest request = new TestAlterGameRequest(TABLE_ID, newGame);
        final Table table = aTable();

        underTest.execute(request, gameHost, table);

        assertThat(table.getCurrentGame(), is(sameInstance(newGame)));
    }

    @Test
    public void handlerShouldIncreaseIncrement() {
        final GameStatus newGame = mock(GameStatus.class);
        final TestAlterGameRequest request = new TestAlterGameRequest(TABLE_ID, newGame);
        final Table table = aTable();

        underTest.execute(request, gameHost, table);

        assertThat(table.incrementDefaultedToOne(), is(equalTo(DEFAULT_INCREMENT + 1)));
    }

    @Test
    public void handlerShouldReturnAnEmptyListOfDocuments() {
        final TestAlterGameRequest request = new TestAlterGameRequest(TABLE_ID, mock(GameStatus.class));
        final Table table = aTable();

        final List<HostDocument> hostDocuments = underTest.execute(request, gameHost, table);

        assertThat(hostDocuments, is(not(nullValue())));
        assertThat(hostDocuments.size(), is(equalTo(0)));
    }

    private Table aTable() {
        final Table table = new Table();
        table.setTableId(TABLE_ID);
        table.setIncrement(DEFAULT_INCREMENT);
        return table;
    }

}
