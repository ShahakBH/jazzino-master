package com.yazino.platform.table;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.openspaces.remoting.RemoteResultReducer;
import org.openspaces.remoting.SpaceRemotingInvocation;
import org.openspaces.remoting.SpaceRemotingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Takes results from a broadcast and decides the best result to return to the caller.
 */
public class TableSearchReducer
        implements RemoteResultReducer<Collection<TableSearchResult>, Collection<TableSearchResult>> {
    private static final Logger LOG = LoggerFactory.getLogger(TableSearchReducer.class);

    private final Comparator<TableSearchResult> tableJoiningComparator = joiningComparator();
    private final Predicate<TableSearchResult> tableNotFullPredicate = tableNotFullPredicate();

    @Override
    public Collection<TableSearchResult> reduce(
            final SpaceRemotingResult<Collection<TableSearchResult>>[] spaceRemotingResults,
            final SpaceRemotingInvocation spaceRemotingInvocation) throws Exception {

        final Collection<TableSearchResult> sortedResults = Collections2.filter(
                sortTableSearchResults(spaceRemotingResults), tableNotFullPredicate);

        if (sortedResults.isEmpty()) {
            LOG.debug("No remoting results, nothing to reduce.");
            return Collections.emptyList();
        }

        // the first table is the one to use unless it's empty. If it is try and find the first non-empty one.
        final Iterator<TableSearchResult> tableSearchResultIterator = sortedResults.iterator();
        TableSearchResult table = tableSearchResultIterator.next();
        if (table.isEmpty()) {
            while (tableSearchResultIterator.hasNext()) {
                final TableSearchResult nextTable = tableSearchResultIterator.next();
                if (nextTable.isNotEmpty()) {
                    table = nextTable;
                    break;
                }
            }
        }

        LOG.debug("Reduced SearchResult to [{}]", table);

        return Arrays.asList(table);
    }

    private Set<TableSearchResult> sortTableSearchResults(
            final SpaceRemotingResult<Collection<TableSearchResult>>[] spaceRemotingResults) {
        final Set<TableSearchResult> sortedResults = new TreeSet<>(tableJoiningComparator);
        for (SpaceRemotingResult<Collection<TableSearchResult>> result : spaceRemotingResults) {
            if (result.getResult() != null) {
                sortedResults.addAll(result.getResult());
            }
        }
        return sortedResults;
    }

    /**
     * Comparator used to order tables as follows:
     * <pre>
     *      highest joining desirability
     *      highest number of free seats
     *      lowest table id
     * </pre>
     *
     * @return table joining comparator
     */
    private Comparator<TableSearchResult> joiningComparator() {
        return new Comparator<TableSearchResult>() {
            @Override
            public int compare(final TableSearchResult tableA, final TableSearchResult tableB) {
                final int desirabilityOrder = tableB.getJoiningDesirability() - tableA.getJoiningDesirability();
                if (desirabilityOrder != 0) {
                    return desirabilityOrder;
                }
                final int spareSeatsOrder = tableB.getSpareSeats() - tableA.getSpareSeats();
                if (spareSeatsOrder == 0) {
                    return tableA.getTableId().compareTo(tableB.getTableId());
                }
                return spareSeatsOrder;
            }
        };
    }

    private Predicate<TableSearchResult> tableNotFullPredicate() {
        return new Predicate<TableSearchResult>() {
            @Override
            public boolean apply(final TableSearchResult tableSearchResult) {
                return tableSearchResult != null && tableSearchResult.isNotFull();
            }
        };
    }
}
