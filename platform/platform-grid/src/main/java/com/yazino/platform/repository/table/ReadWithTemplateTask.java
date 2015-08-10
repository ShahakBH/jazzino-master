package com.yazino.platform.repository.table;

import com.gigaspaces.async.AsyncResult;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.TableSummary;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.DistributedTask;
import com.yazino.game.api.GameRules;

import javax.annotation.Resource;
import java.util.List;

@AutowireTask
public abstract class ReadWithTemplateTask implements DistributedTask<TableSummary, TableSummary> {
    private static final long serialVersionUID = -1600986019225748578L;

    @Resource(name = "gigaSpace")
    private transient GigaSpace tableGigaSpace;

    @Resource(name = "gameRepository")
    private transient GameRepository gameRepository;

    @Override
    public TableSummary execute() throws Exception {
        final Table table = tableGigaSpace.read(createTemplate());
        if (table == null) {
            return null;
        }
        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        return table.summarise(gameRules);
    }

    @Override
    public TableSummary reduce(final List<AsyncResult<TableSummary>> results) throws Exception {
        for (AsyncResult<TableSummary> result : results) {
            if (result.getException() != null) {
                throw result.getException();
            }
            if (result.getResult() != null) {
                return result.getResult();
            }
        }
        return null;
    }

    abstract Table createTemplate();
}
