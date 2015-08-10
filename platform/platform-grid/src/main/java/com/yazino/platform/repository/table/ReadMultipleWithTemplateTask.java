package com.yazino.platform.repository.table;

import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.client.ReadModifiers;
import com.google.common.base.Function;
import com.yazino.game.api.GameRules;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.TableSummary;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.DistributedTask;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.Validate.notNull;

@AutowireTask
public abstract class ReadMultipleWithTemplateTask
        implements DistributedTask<ArrayList<TableSummary>, ArrayList<TableSummary>> {
    private static final long serialVersionUID = -7274607404548598912L;

    @Resource(name = "gigaSpace")
    private transient GigaSpace tableGigaSpace;

    @Resource(name = "gameRepository")
    private transient GameRepository gameRepository;

    @Override
    public ArrayList<TableSummary> execute() throws Exception {
        final Table[] tables = tableGigaSpace.readMultiple(
                createTemplate(), Integer.MAX_VALUE, ReadModifiers.DIRTY_READ);
        if (tables == null || tables.length == 0) {
            return newArrayList();
        }
        return transform(Arrays.asList(tables), new TableSummaryTransformer());
    }

    @Override
    public ArrayList<TableSummary> reduce(final List<AsyncResult<ArrayList<TableSummary>>> results) throws Exception {
        final ArrayList<TableSummary> merged = new ArrayList<>();
        for (AsyncResult<ArrayList<TableSummary>> result : results) {
            if (result.getException() != null) {
                throw result.getException();
            }
            merged.addAll(result.getResult());
        }
        return merged;
    }

    abstract Table createTemplate();

    public <T, V> ArrayList<V> transform(final Collection<T> data, final Function<T, V> transformer) {
        notNull(transformer, "transformer may not be null");
        final ArrayList<V> transformedList = new ArrayList<V>();
        if (data != null) {
            for (T item : data) {
                transformedList.add(transformer.apply(item));
            }
        }
        return transformedList;
    }

    private class TableSummaryTransformer implements Function<Table, TableSummary> {
        @Override
        public TableSummary apply(final Table table) {
            if (table == null) {
                return null;
            }
            final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
            return table.summarise(gameRules);
        }
    }
}
