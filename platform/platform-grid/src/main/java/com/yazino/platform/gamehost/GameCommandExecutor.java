package com.yazino.platform.gamehost;

import com.yazino.game.api.*;
import com.yazino.platform.account.GameHostWallet;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.gamehost.external.ExternalGameRequestService;
import com.yazino.platform.gamehost.external.TableExternalGameService;
import com.yazino.platform.gamehost.postprocessing.Postprocessor;
import com.yazino.platform.gamehost.preprocessing.CommandPreprocessor;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWallet;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWalletFactory;
import com.yazino.platform.gamehost.wallet.TableBoundGamePlayerWalletFactory;
import com.yazino.platform.messaging.ObservableDocumentContext;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.ErrorHostDocument;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.community.PlayerSessionSummary;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.service.audit.Auditor;
import com.yazino.platform.table.PlayerInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Executes commands.
 */
public class GameCommandExecutor extends BaseGameExecutor<CommandPreprocessor, Command> {
    private static final Logger LOG = LoggerFactory.getLogger(GameCommandExecutor.class);

    private final PlayerRepository playerRepository;
    private final DestinationFactory destinationFactory;

    @Autowired
    public GameCommandExecutor(final GameRepository gameRepository,
                               final Auditor auditor,
                               final BufferedGameHostWalletFactory bufferedGameHostWalletFactory,
                               final ExternalGameRequestService novomaticRequestService,
                               final PlayerRepository playerRepository,
                               final DestinationFactory destinationFactory) {
        super(bufferedGameHostWalletFactory, auditor, gameRepository, novomaticRequestService);
        notNull(playerRepository, "playerRepository was null");
        notNull(gameRepository, "gameRepository was null");
        notNull(destinationFactory, "destinationFactory was null");

        this.playerRepository = playerRepository;
        this.destinationFactory = destinationFactory;
    }

    @Override
    public boolean isApplicableForExecution(final Table table,
                                            final Command command) {
        return !(command == null
                || command.getGameId() == null
                || (command.getGameId() != -1 && !command.getGameId().equals(table.getGameId())));
    }

    @Override
    public GameInitialiser.GameInitialisationContext createContext(
            final Command command,
            final GameRules gameRules,
            final TableBoundGamePlayerWalletFactory walletFactory,
            final TableExternalGameService externalGameService) {
        BigDecimal playerBalance = BigDecimal.ZERO;

        if (hasInitiatingPlayer(command)) {
            final PlayerInformation information = informationForPlayer(walletFactory.getTable(), command.getPlayer(),
                    walletFactory.getGameHostWallet());
            if (information != null) {
                playerBalance = information.getCachedBalance();
            } else {
                playerBalance = playerBalance;
            }
        }

        return new GameInitialiser.GameInitialisationContext(
                command, walletFactory, externalGameService, gameRules, playerBalance);
    }

    @Override
    public BufferedGameHostWallet newBufferedGameHostWallet(final Table table,
                                                            final Command command) {
        return getBufferedGameHostWalletFactory().create(table.getTableId(), command.getUuid());
    }

    @Override
    public boolean isOkToProceedWithExecution(final GameRules gameRules,
                                              final TableBoundGamePlayerWalletFactory walletFactory,
                                              final List<HostDocument> documents,
                                              final Command command) {
        final Table table = walletFactory.getTable();
        final String auditLabel = walletFactory.getAuditLabel();
        BigDecimal playerBalance = BigDecimal.ZERO;

        PlayerInformation information = null;

        if (hasInitiatingPlayer(command)) {
            final GamePlayer player = command.getPlayer();
            information = informationForPlayer(table, player, walletFactory.getGameHostWallet());
            playerBalance = information.getCachedBalance();
        }

        final boolean continueExecution = runPreProcessors(gameRules, table, command, playerBalance,
                auditLabel, documents, getPreExecutionProcessors());
        if (continueExecution && information != null) {
            table.addPlayerToTable(information);
        }
        return continueExecution;
    }

    @Override
    protected ExecutionResult doExecution(final ExecutionContext context,
                                          final Table table,
                                          final GameRules gameRules,
                                          final List<HostDocument> documents,
                                          final Command initCommand) {
        Command command = initCommand;
        if (hasInitiatingPlayer(command)) {
            final GamePlayer player = command.getPlayer();
            final PlayerInformation information = getCachedInformationForPlayer(table, player);
            if (information != null) {
                command = new Command(command, information.getName());
            }
        }

        try {
            return gameRules.execute(context, command);
        } catch (GameException e) {
            documents.add(createErrorDocument(gameRules, table, command, e));
        }
        return null;
    }

    @Override
    void postExecution(final ExecutionResult result,
                       final GameRules gameRules, final Table table,
                       final String auditLabel,
                       final List<HostDocument> documents,
                       final Command command) {
        final BigDecimal playerId = getInitiatingPlayerId(command);
        runPostProcessors(result, command, table, auditLabel, documents, playerId, getPostExecutionProcessors());
        table.removePlayersNoLongerAtTable(gameRules);
    }

    private static BigDecimal getInitiatingPlayerId(final Command command) {
        if (hasInitiatingPlayer(command)) {
            return command.getPlayer().getId();
        } else {
            return null;
        }
    }

    private static boolean hasInitiatingPlayer(final Command command) {
        return command.getPlayer() != null && command.getPlayer().getId() != null;
    }

    private PlayerInformation informationForPlayer(final Table table,
                                                   final GamePlayer gamePlayer,
                                                   final GameHostWallet wallet) {
        final BigDecimal playerId = gamePlayer.getId();
        final PlayerInformation playerInformation = table.playerAtTable(playerId);
        if (playerInformation != null) {
            return playerInformation;

        } else {
            final PlayerSessionSummary player = playerRepository.findSummaryByPlayerAndSession(playerId, gamePlayer.getSessionId());
            if (player == null) {
                throw new IllegalArgumentException("Invalid player: " + playerId);
            }
            final BigDecimal accountId = player.getAccountId();
            try {
                final BigDecimal balance = wallet.getBalance(accountId);
                return new PlayerInformation(player.getPlayerId(), player.getName(), player.getAccountId(), player.getSessionId(), balance);
            } catch (WalletServiceException e) {
                throw new RuntimeException(String.format(
                        "Account balance could not be retrieved for player %s with account ID %s",
                        playerId, accountId), e);
            }
        }
    }

    private PlayerInformation getCachedInformationForPlayer(final Table table,
                                                            final GamePlayer player) {
        final PlayerInformation information = table.playerAtTable(player.getId());
        if (information == null) {
            LOG.warn(String.format("Expected (but not found) cached information for player [%s] against table [%s]",
                    player.getId(), table.getTableId()));
        }
        return information;
    }

    private HostDocument createErrorDocument(final GameRules gameRules,
                                             final Table table,
                                             final Command command,
                                             final GameException e) {
        BigDecimal playerBalance = BigDecimal.ZERO;
        final GamePlayer player = command.getPlayer();
        final PlayerInformation information = getCachedInformationForPlayer(table, player);
        if (information != null) {
            playerBalance = information.getCachedBalance();
        }
        final boolean isAPlayer = table.getCurrentGame() != null && gameRules.isAPlayer(table.getCurrentGame(), player);
        final ObservableDocumentContext context = new ObservableDocumentContext.Builder(
                table.getTableId(), table.getGameId(), table.buildObservableStatus(gameRules, player, playerBalance),
                table.incrementDefaultedToOne(), table.incrementOfGameStartDefaultedToOne())
                .withCommand(command)
                .withIsAPlayer(isAPlayer)
                .withMessage(e.getParameterisedMessage())
                .withPlayerIds(table.playerIds(gameRules))
                .withPlayerBalance(playerBalance)
                .build();

        return new ErrorHostDocument(context, destinationFactory.player(player.getId()));
    }

    private static boolean runPreProcessors(final GameRules gameRules,
                                            final Table table,
                                            final Command command,
                                            final BigDecimal playerBalance,
                                            final String auditLabel,
                                            final List<HostDocument> documentsToSend,
                                            final List<CommandPreprocessor> processors) {
        for (CommandPreprocessor processor : processors) {
            final boolean doContinue = processor.preProcess(gameRules, command, playerBalance, table, auditLabel, documentsToSend);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: preprocessor [{}] yielded a [{}] result",
                        table.getTableId(), processor.getClass().getSimpleName(), doContinue);
            }
            if (!doContinue) {
                return false;
            }
        }
        return true;
    }

    private static void runPostProcessors(final ExecutionResult result,
                                          final Command command,
                                          final Table table,
                                          final String auditLabel,
                                          final List<HostDocument> documents,
                                          final BigDecimal initialitingPlayerId,
                                          final List<Postprocessor> processors) {
        for (Postprocessor processor : processors) {
            processor.postProcess(result, command, table, auditLabel, documents, initialitingPlayerId);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: after running postprocessor [{}], documents were [{}] ",
                        table.getTableId(), processor.getClass().getSimpleName(), documents);
            }
        }
    }

}
