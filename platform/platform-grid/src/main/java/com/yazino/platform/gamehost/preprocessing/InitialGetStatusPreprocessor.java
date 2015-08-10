package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.messaging.ObservableDocumentContext;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.messaging.host.InitialGameStatusHostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.service.audit.Auditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.Command;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.GameRules;
import com.yazino.game.api.ObservableStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

public class InitialGetStatusPreprocessor implements CommandPreprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(InitialGetStatusPreprocessor.class);

    private final DestinationFactory destinationFactory;
    private final Auditor auditor;

    @Autowired(required = true)
    public InitialGetStatusPreprocessor(final Auditor auditor,
                                        final DestinationFactory destinationFactory) {
        notNull(auditor, "auditor may not be null");
        notNull(destinationFactory, "destinationFactory may not be null");

        this.auditor = auditor;
        this.destinationFactory = destinationFactory;
    }

    public boolean preProcess(final GameRules gameRules,
                              final Command command,
                              final BigDecimal playerBalance,
                              final Table table,
                              final String auditLabel,
                              final List<HostDocument> documentsToSend) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: entering preprocess", table.getTableId());
        }
        if (Command.CommandType.InitialGetStatus != command.getCommandType() || table.getCurrentGame() == null) {
            return true;
        }

        Long fromIncrement = null;
        if (command.getArgs() != null && command.getArgs().length > 0) {
            try {
                fromIncrement = Long.parseLong(command.getArgs()[0]);
            } catch (Throwable t) {
                LOG.error("Error processing InitialGetStatus arg {}", command.getArgs()[0]);
            }
        }
        final GamePlayer player = command.getPlayer();
        final ObservableStatus status = table.buildObservableStatus(gameRules, player, playerBalance);

        final Set<BigDecimal> playerIds = table.playerIds(gameRules);
        final ObservableDocumentContext.Builder context = new ObservableDocumentContext.Builder(
                table.getTableId(), table.getGameId(), status, table.incrementDefaultedToOne(),
                table.incrementOfGameStartDefaultedToOne())
                .withCommand(command)
                .withIsAPlayer(gameRules.isAPlayer(table.getCurrentGame(), player))
                .withPlayerIds(playerIds)
                .withPlayerBalance(playerBalance)
                .withTableProperties(table.getCombinedGameProperties());

        if (fromIncrement == null) {
            context.withAcknowlegedIncrement(context.getIncrementOfGameStart() - 1);
        } else {
            context.withAcknowlegedIncrement(fromIncrement);
        }

        documentsToSend.add(new InitialGameStatusHostDocument(context.build(),
                destinationFactory.player(player.getId())));

        auditor.audit(auditLabel, command);

        return false;
    }
}
