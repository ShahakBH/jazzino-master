package com.yazino.platform.gamehost.preprocessing;

import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yazino.game.api.Command;
import com.yazino.game.api.GameRules;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

public class AcknowledgementPreprocessor implements CommandPreprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(AcknowledgementPreprocessor.class);

    public boolean preProcess(final GameRules gameRules, final Command command,
                              final BigDecimal playerBalance,
                              final Table table,
                              final String auditLabel,
                              final List<HostDocument> documentsToSend) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: entering preProcess with command {}", table.getTableId(), command);
        }

        if (Command.CommandType.Ack != command.getCommandType()) {
            return true;
        }

        try {
            if (isEmpty(command.getArgs())) {
                LOG.warn("Malformed ACK received for player {}", command.getPlayer().getId());

            } else if (table.getCurrentGame() != null
                    && gameRules.isAPlayer(table.getCurrentGame(), command.getPlayer())) {
                final long acknowledgedIncrement = Long.valueOf(command.getArgs()[0]);
                table.playerAcknowledgesIncrement(command.getPlayer().getId(), acknowledgedIncrement);
            }

        } catch (Throwable t) {
            LOG.error("Table {}: Error processing command {}",
                    table.getTableId(), ReflectionToStringBuilder.reflectionToString(command), t);
        }

        return false;
    }
}
