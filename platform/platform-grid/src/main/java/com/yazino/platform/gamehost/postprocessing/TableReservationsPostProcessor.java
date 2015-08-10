package com.yazino.platform.gamehost.postprocessing;

import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.repository.table.TableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.Command;
import com.yazino.game.api.ExecutionResult;
import com.yazino.game.api.GameRules;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This processor is responsible for ensuring that the number of
 * {@link com.yazino.platform.model.table.TableReservation} objects in the space is
 * decremented when the state of the table changes, i.e. people move from a 'reserved' state to joined.
 */
public class TableReservationsPostProcessor implements Postprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(TableReservationsPostProcessor.class);

    private final TableRepository tableRepository;
    private final GameRepository gameRepository;

    @Autowired
    public TableReservationsPostProcessor(@Qualifier("tableRepository") final TableRepository tableRepository,
                                          final GameRepository gameRepository) {
        notNull(tableRepository, "tableRepository is null");
        notNull(gameRepository, "gameRepository is null");

        this.tableRepository = tableRepository;
        this.gameRepository = gameRepository;
    }

    @Override
    public void postProcess(final ExecutionResult executionResult,
                            final Command command,
                            final Table table,
                            final String auditLabel,
                            final List<HostDocument> documentsToSend,
                            final BigDecimal initiatingPlayerId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {} - Processing table reservations", table.getTableId());
        }
        if (executionResult == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {} - Execution result is not present. Ignoring...", table.getTableId());
            }
            return;
        }
        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        final Set<BigDecimal> playersBefore = table.playerIds(gameRules);
        final Set<BigDecimal> playersAfter = new HashSet<>(executionResult.playerIds());
        playersAfter.removeAll(playersBefore);
        for (BigDecimal newPlayerId : playersAfter) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {} - Removing reservation for new player {}", table.getTableId(), newPlayerId);
            }
            tableRepository.removeReservationForTable(table.getTableId(), newPlayerId);
        }
    }
}
