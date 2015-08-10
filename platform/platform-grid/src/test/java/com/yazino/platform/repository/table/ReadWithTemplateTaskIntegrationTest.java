package com.yazino.platform.repository.table;

import com.gigaspaces.async.AsyncResult;
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
import com.yazino.game.api.GameRules;
import com.yazino.game.api.GameType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ReadWithTemplateTaskIntegrationTest {

    private static final String GAME_TYPE_ID = "AGAME";
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

    private ReadWithTemplateTask underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        underTest = new ReadWithTemplateTask() {
            @Override
            Table createTemplate() {
                return new Table();
            }
        };

        when(gameRepository.getGameRules(GAME_TYPE_ID)).thenReturn(gameRules);

        ReflectionTestUtils.setField(underTest, "tableGigaSpace", tableGigaSpace);
        ReflectionTestUtils.setField(underTest, "gameRepository", gameRepository);
    }

    @Test
    public void shouldReturnTransformedTableOnExecute() throws Exception {
        Table expected = createTable();
        when(tableGigaSpace.read(new Table())).thenReturn(expected);

        TableSummary tableSummary = underTest.execute();

        assertEquals(expected.summarise(gameRules), tableSummary);
    }

    @Test
    public void shouldReturnMergedResults() throws Exception {
        final Table aTable = createTable();
        List<AsyncResult<TableSummary>> taskResults = new ArrayList<AsyncResult<TableSummary>>();
        final TableSummary expected = aTable.summarise(gameRules);
        taskResults.add(new AsyncResult<TableSummary>() {
            @Override
            public TableSummary getResult() {
                return expected;
            }

            @Override
            public Exception getException() {
                return null;
            }
        });
        TableSummary result = underTest.reduce(taskResults);

        assertEquals(expected, result);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldThrowExceptionIfReturnedAsResult() throws Exception {
        final Table aTable = createTable();
        List<AsyncResult<TableSummary>> results = new ArrayList<AsyncResult<TableSummary>>();
        results.add(new AsyncResult<TableSummary>() {
            @Override
            public TableSummary getResult() {
                return aTable.summarise(gameRules);
            }

            @Override
            public Exception getException() {
                return new UnsupportedOperationException();
            }
        });

        underTest.reduce(results);
    }

    private Table createTable() {
        Table table = new Table(createGameType(), BigDecimal.ONE,CLIENT_ID, true);
        table.setTableId(BigDecimal.ONE);
        return table;
    }

    private GameType createGameType() {
        return new GameType(GAME_TYPE_ID, GAME_NAME, Collections.<String>emptySet());
    }

}
