package com.yazino.platform.gamehost.postprocessing;


import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.processor.table.GameCompletePublisher;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.service.audit.Auditor;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.Command;
import com.yazino.game.api.ExecutionResult;
import com.yazino.game.api.GameRules;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class TableAuditPostprocessor implements Postprocessor {

    private final Auditor auditor;
    private final GameCompletePublisher gameCompletePublisher;
    private final GameRepository gameRepository;

    @Autowired
    public TableAuditPostprocessor(final Auditor auditor,
                                   final GameCompletePublisher gameCompletePublisher,
                                   final GameRepository gameRepository) {
        notNull(auditor, "auditor may not be null");
        notNull(gameCompletePublisher, "gameCompletePublisher may not be null");
        notNull(gameRepository, "gameRepository may not be null");

        this.auditor = auditor;
        this.gameCompletePublisher = gameCompletePublisher;
        this.gameRepository = gameRepository;
    }

    public void postProcess(final ExecutionResult executionResult,
                            final Command command,
                            final Table table,
                            final String auditLabel,
                            final List<HostDocument> documentsToSend,
                            final BigDecimal initiatingPlayerId) {
        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        if (table.getCurrentGame() != null && gameRules.isComplete(table.getCurrentGame())) {
            auditor.audit(auditLabel, table, gameRules);
            gameCompletePublisher.publishCompletedGame(table.getCurrentGame(),
                    table.getGameTypeId(), table.getTableId(), table.getClientId());
        }
    }
}
