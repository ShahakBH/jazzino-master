package com.yazino.platform.repository.table;

import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.client.ReadModifiers;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameType;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.TableSummary;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ReadMultipleWithTemplateIntegrationTest {
    private static final String GAME_ID = "AGAME";
    private static final String GAME_NAME = "A GAME";
    private static final String PARTNER_ID = "ONE";
    private static final String CLIENT_ID = "ONE";

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameRules gameRules;

    @Mock
    @Autowired
    private GigaSpace tableGigaSpace;

    private ReadMultipleWithTemplateTask underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        underTest = new ReadMultipleWithTemplateTask() {
            @Override
            Table createTemplate() {
                return new Table();
            }
        };

        ReflectionTestUtils.setField(underTest, "tableGigaSpace", tableGigaSpace);
        ReflectionTestUtils.setField(underTest, "gameRepository", gameRepository);

        when(gameRepository.getGameRules(GAME_ID)).thenReturn(gameRules);
    }

    @Test
    public void shouldReturnTransformedTablesOnExecute() throws Exception {
        Table expected = createTable();
        when(tableGigaSpace.readMultiple(new Table(), Integer.MAX_VALUE, ReadModifiers.DIRTY_READ))
                .thenReturn(new Table[]{expected});

        ArrayList<TableSummary> tableSummaries = underTest.execute();

        assertEquals(Arrays.asList(expected.summarise(gameRules)), tableSummaries);
    }

    @Test
    public void shouldReturnMergedResults() throws Exception {
        final Table aTable = createTable();
        List<AsyncResult<ArrayList<TableSummary>>> results = new ArrayList<AsyncResult<ArrayList<TableSummary>>>();
        final ArrayList<TableSummary> expected = new ArrayList<TableSummary>(Arrays.asList(aTable.summarise(gameRules)));
        results.add(new AsyncResult<ArrayList<TableSummary>>() {
            @Override
            public ArrayList<TableSummary> getResult() {
                return expected;
            }

            @Override
            public Exception getException() {
                return null;
            }
        });
        List<TableSummary> result = underTest.reduce(results);

        assertEquals(expected, result);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldThrowExceptionIfReturnedAsResult() throws Exception {
        final Table aTable = createTable();
        List<AsyncResult<ArrayList<TableSummary>>> results = new ArrayList<AsyncResult<ArrayList<TableSummary>>>();
        results.add(new AsyncResult<ArrayList<TableSummary>>() {
            @Override
            public ArrayList<TableSummary> getResult() {
                return new ArrayList<TableSummary>(Arrays.asList(aTable.summarise(gameRules)));
            }

            @Override
            public Exception getException() {
                return new UnsupportedOperationException();
            }
        });

        underTest.reduce(results);
    }

    private Table createTable() {
        Table table = new Table(createGameType(), BigDecimal.ONE, CLIENT_ID, true);
        table.setTableId(BigDecimal.ONE);
        return table;
    }

    private GameType createGameType() {
        return new GameType(GAME_ID, GAME_NAME, Collections.<String>emptySet());
    }

}
