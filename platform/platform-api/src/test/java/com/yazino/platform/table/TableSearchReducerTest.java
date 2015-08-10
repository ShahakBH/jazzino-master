package com.yazino.platform.table;

import org.junit.Test;
import org.openspaces.remoting.SpaceRemotingInvocation;
import org.openspaces.remoting.SpaceRemotingResult;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TableSearchReducerTest {


    private final TableSearchReducer reducer = new TableSearchReducer();
    private final SpaceRemotingInvocation spaceRemotingInvocation = mock(SpaceRemotingInvocation.class);

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnEmptyCollectionWhenNoMatchingTablesInSpaces() throws Exception {
        SpaceRemotingResult<Collection<TableSearchResult>>[] emptyResults = new SpaceRemotingResult[0];
        Collection<TableSearchResult> reduced = reducer.reduce(emptyResults, spaceRemotingInvocation);
        assertTrue(reduced.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnEmptyCollectionWhenAllTablesAreFull() throws Exception {
        TableSearchResult resultA = createResult(BigDecimal.valueOf(1), 0, 10, 20000);
        TableSearchResult resultB = createResult(BigDecimal.valueOf(2), 0, 10, 20);
        SpaceRemotingResult<Collection<TableSearchResult>> spaceAResults = toRemotingResult(resultA);
        SpaceRemotingResult<Collection<TableSearchResult>> spaceBResults = toRemotingResult(resultB);

        Collection<TableSearchResult> reduced = reducer.reduce(toArray(spaceAResults, spaceBResults), spaceRemotingInvocation);

        assertTrue(reduced.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnNonEmptyNonFullTableWithHighestJoiningDesirabilityAndHighestSpareSeats() throws Exception {
        TableSearchResult resultA = createResult(BigDecimal.valueOf(1), 10, 10, 20000);
        TableSearchResult resultB = createResult(BigDecimal.valueOf(2), 10, 10, 30000);
        TableSearchResult resultC = createResult(BigDecimal.valueOf(3), 8, 10, 30);
        TableSearchResult resultD = createResult(BigDecimal.valueOf(4), 2, 10, 100);
        TableSearchResult resultE = createResult(BigDecimal.valueOf(5), 3, 10, 90);
        TableSearchResult resultF = createResult(BigDecimal.valueOf(6), 0, 10, 10000);  // full: should be ignored
        SpaceRemotingResult<Collection<TableSearchResult>> spaceAResults = toRemotingResult(resultA, resultB, resultF);
        SpaceRemotingResult<Collection<TableSearchResult>> spaceBResults = toRemotingResult(resultC, resultD, resultE);

        Collection<TableSearchResult> reduced = reducer.reduce(toArray(spaceAResults, spaceBResults), spaceRemotingInvocation);
        assertEquals(1, reduced.size());

        TableSearchResult result = reduced.iterator().next();
        assertEquals(resultD, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnNonEmptyTableWithHighestSpareSeatsWhenJoiningDesirablitiesAreEqual() throws Exception {
        TableSearchResult resultA = createResult(BigDecimal.valueOf(1), 10, 10, 0);
        TableSearchResult resultB = createResult(BigDecimal.valueOf(2), 7, 10, 0);
        TableSearchResult resultC = createResult(BigDecimal.valueOf(3), 8, 10, 0);
        TableSearchResult resultD = createResult(BigDecimal.valueOf(4), 2, 10, 0);
        TableSearchResult resultE = createResult(BigDecimal.valueOf(5), 0, 10, 1000);
        SpaceRemotingResult<Collection<TableSearchResult>> spaceAResults = toRemotingResult(resultA, resultB);
        SpaceRemotingResult<Collection<TableSearchResult>> spaceBResults = toRemotingResult(resultC, resultD, resultE);

        Collection<TableSearchResult> reduced = reducer.reduce(toArray(spaceAResults, spaceBResults), spaceRemotingInvocation);
        assertEquals(1, reduced.size());

        TableSearchResult result = reduced.iterator().next();
        assertEquals(resultC, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnNonEmptyTableWithLowestTableIdWhenJoinDesirbilityAndSpareSeatsAreEqual() throws Exception {
        TableSearchResult resultA = createResult(BigDecimal.valueOf(1), 5, 10, 100);
        TableSearchResult resultB = createResult(BigDecimal.valueOf(2), 5, 10, 100);
        TableSearchResult resultC = createResult(BigDecimal.valueOf(3), 5, 10, 100);
        TableSearchResult resultD = createResult(BigDecimal.valueOf(4), 5, 10, 100);
        TableSearchResult resultE = createResult(BigDecimal.valueOf(5), 0, 10, 1000);
        SpaceRemotingResult<Collection<TableSearchResult>> spaceAResults = toRemotingResult(resultA, resultB);
        SpaceRemotingResult<Collection<TableSearchResult>> spaceBResults = toRemotingResult(resultC, resultD, resultE);

        Collection<TableSearchResult> reduced = reducer.reduce(toArray(spaceAResults, spaceBResults), spaceRemotingInvocation);
        assertEquals(1, reduced.size());

        TableSearchResult result = reduced.iterator().next();
        assertEquals(resultA, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnEmptyTableWithHighestJoiningDesirabilityWhenAllOtherTablesAreFull() throws Exception {
        TableSearchResult resultA = createResult(BigDecimal.valueOf(1), 0, 10, 0);
        TableSearchResult resultB = createResult(BigDecimal.valueOf(2), 0, 10, 0);
        TableSearchResult resultC = createResult(BigDecimal.valueOf(3), 10, 10, 0);
        TableSearchResult resultD = createResult(BigDecimal.valueOf(4), 10, 10, 2);
        TableSearchResult resultE = createResult(BigDecimal.valueOf(5), 0, 10, 0);
        SpaceRemotingResult<Collection<TableSearchResult>> spaceAResults = toRemotingResult(resultA, resultB);
        SpaceRemotingResult<Collection<TableSearchResult>> spaceBResults = toRemotingResult(resultC, resultD, resultE);

        Collection<TableSearchResult> reduced = reducer.reduce(toArray(spaceAResults, spaceBResults), spaceRemotingInvocation);
        assertEquals(1, reduced.size());

        TableSearchResult result = reduced.iterator().next();
        assertEquals(resultD, result);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldHandleNullResultsFromSpaces() throws Exception {
        TableSearchResult resultA = createResult(BigDecimal.valueOf(1), 1, 10, 0);
        TableSearchResult resultB = createResult(BigDecimal.valueOf(2), 0, 10, 0);
        SpaceRemotingResult<Collection<TableSearchResult>> spaceAResults = toRemotingResult(resultA, resultB);
        SpaceRemotingResult<Collection<TableSearchResult>> spaceBResults = toRemotingResult((TableSearchResult[]) null);

        Collection<TableSearchResult> reduced = reducer.reduce(toArray(spaceAResults, spaceBResults), spaceRemotingInvocation);
        assertEquals(1, reduced.size());

        TableSearchResult result = reduced.iterator().next();
        assertEquals(resultA, result);
    }

    @SuppressWarnings("unchecked")
    private static SpaceRemotingResult<Collection<TableSearchResult>> toRemotingResult(TableSearchResult... searchResults) {
        SpaceRemotingResult<Collection<TableSearchResult>> remotingResult = mock(SpaceRemotingResult.class);
        if (searchResults != null) {
            when(remotingResult.getResult()).thenReturn(Arrays.asList(searchResults));
        } else {
            when(remotingResult.getResult()).thenReturn(null);
        }
        return remotingResult;
    }


    private static TableSearchResult createResult(BigDecimal tableId, int spareSeats, int maxSeats, int joiningDesirability) {
        final TableSearchResult result = new TableSearchResult();
        result.setTableId(tableId);
        result.setSpareSeats(spareSeats);
        result.setMaxSeats(maxSeats);
        result.setJoiningDesirability(joiningDesirability);
        return result;
    }

    private <T> T[] toArray(final T... values) {
        return values;
    }

}
