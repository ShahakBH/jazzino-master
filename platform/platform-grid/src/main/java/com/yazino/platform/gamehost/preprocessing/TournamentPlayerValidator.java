package com.yazino.platform.gamehost.preprocessing;


import com.yazino.platform.messaging.ObservableDocumentContext;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.ErrorHostDocument;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.table.TableType;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.Command;
import com.yazino.game.api.GameException;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.GameRules;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Validates that the player executing the command is at the tournament table.
 */
public class TournamentPlayerValidator implements CommandPreprocessor {

    private final DestinationFactory destinationFactory;

    @Autowired
    public TournamentPlayerValidator(final DestinationFactory destinationFactory) {
        notNull(destinationFactory, "destinationFactory was null");

        this.destinationFactory = destinationFactory;
    }

    @Override
    public boolean preProcess(final GameRules gameRules,
                              final Command command,
                              final BigDecimal playerBalance,
                              final Table table,
                              final String auditLabel,
                              final List<HostDocument> documentsToSend) {
        if (!tableAllowsUnknownPlayerCommand(table, command)) {
            documentsToSend.add(createErrorDocument(gameRules, table, command, command.getPlayer(),
                    BigDecimal.ZERO, new GameException("You are not allowed to play at this table")));
            return false;
        }

        return true;
    }

    private boolean tableAllowsUnknownPlayerCommand(final Table table,
                                                    final Command command) {
        final GamePlayer player = command.getPlayer();
        return !(player != null && table.playerAtTable(player.getId()) == null
                && table.resolveType() == TableType.TOURNAMENT);
    }

    private HostDocument createErrorDocument(final GameRules gameRules,
                                             final Table table,
                                             final Command command,
                                             final GamePlayer player,
                                             final BigDecimal playerBalance,
                                             final GameException gex) {
        final boolean isAPlayer = table.getCurrentGame() != null && gameRules.isAPlayer(table.getCurrentGame(), player);
        final ObservableDocumentContext context = new ObservableDocumentContext.Builder(
                table.getTableId(), table.getGameId(), table.buildObservableStatus(gameRules, player, playerBalance),
                table.incrementDefaultedToOne(), table.incrementOfGameStartDefaultedToOne())
                .withCommand(command)
                .withIsAPlayer(isAPlayer)
                .withMessage(gex.getParameterisedMessage())
                .withPlayerIds(table.playerIds(gameRules))
                .withPlayerBalance(playerBalance)
                .build();
        return new ErrorHostDocument(context, destinationFactory.player(player.getId()));
    }


}
