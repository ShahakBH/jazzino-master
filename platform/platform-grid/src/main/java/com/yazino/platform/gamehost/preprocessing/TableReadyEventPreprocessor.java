package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.ScheduledEvent;

import static org.apache.commons.lang3.Validate.notNull;

public class TableReadyEventPreprocessor implements EventPreprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(TableReadyEventPreprocessor.class);

    private final GameRepository gameRepository;

    @Autowired
    public TableReadyEventPreprocessor(final GameRepository gameRepository) {
        notNull(gameRepository, "gameRepository may not be null");

        this.gameRepository = gameRepository;
    }

    public boolean preprocess(final ScheduledEvent event,
                              final Table table) {
        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        if (table.isClosed()
                || table.getCurrentGame() == null
                || gameRules.isComplete(table.getCurrentGame())
                || table.readyToBeClosed(gameRules)) {
            if (LOG.isDebugEnabled()) {
                LOG.info("Table {}: table is not ready for events (closed? {}, current game null? {}, game complete? {}, ready to be closed? {})",
                        table.getTableId(), table.isClosed(), table.getCurrentGame() == null,
                        table.getCurrentGame() != null && gameRules.isComplete(table.getCurrentGame()), table.readyToBeClosed(gameRules));
            }
            return false;
        }
        return true;
    }
}
