package com.yazino.platform.gamehost;

import com.yazino.game.api.*;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.gamehost.external.ExternalGameRequestService;
import com.yazino.platform.gamehost.external.TableExternalGameService;
import com.yazino.platform.gamehost.postprocessing.PlayerRemovalProcessor;
import com.yazino.platform.gamehost.postprocessing.Postprocessor;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWallet;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWalletFactory;
import com.yazino.platform.gamehost.wallet.TableBoundGamePlayerWalletFactory;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.messaging.host.MessageHostDocument;
import com.yazino.platform.messaging.host.ShutdownMessageHostDocument;
import com.yazino.platform.model.community.PlayerSessionSummary;
import com.yazino.platform.model.table.CommandWrapper;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.service.audit.Auditor;
import com.yazino.platform.table.PlayerInformation;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.util.UUIDSource;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

public class GameHost {
    private static final Logger LOG = LoggerFactory.getLogger(GameHost.class);

    private final List<PlayerRemovalProcessor> playerRemovalProcessors = new ArrayList<>();

    private final Auditor auditor;
    private final GameRepository gameRepository;
    private final DestinationFactory destinationFactory;
    private final PlayerRepository playerRepository;
    private final BufferedGameHostWalletFactory bufferedGameHostWalletFactory;
    private final ExternalGameRequestService externalGameRequestService;
    private final UUIDSource uuidSource;
    private final GameEventExecutor eventExecutor;
    private final GameCommandExecutor commandExecutor;
    private final GameInitialiser gameInitialiser;

    @Autowired
    public GameHost(final Auditor auditor,
                    final GameRepository gameRepository,
                    final DestinationFactory destinationFactory,
                    final PlayerRepository playerRepository,
                    final BufferedGameHostWalletFactory bufferedGameHostWalletFactory,
                    final ExternalGameRequestService externalGameRequestService,
                    final UUIDSource uuidSource,
                    final GameEventExecutor eventExecutor,
                    final GameCommandExecutor commandExecutor,
                    final GameInitialiser gameInitialiser) {
        notNull(auditor, "Auditor may not be null");
        notNull(gameRepository, "Game Repository may not be null");
        notNull(destinationFactory, "Destination Factory may not be null");
        notNull(playerRepository, "playerRepository may not be null");
        notNull(bufferedGameHostWalletFactory, "bufferedGameHostWalletFactory may not be null");
        notNull(externalGameRequestService, "novomaticRequestService may not be null");
        notNull(uuidSource, "uuidSource may not be null");
        notNull(eventExecutor, "eventExecutor may not be null");
        notNull(commandExecutor, "commandExecutor may not be null");
        notNull(gameInitialiser, "gameInitialiser may not be null");

        this.uuidSource = uuidSource;
        this.auditor = auditor;
        this.gameRepository = gameRepository;
        this.destinationFactory = destinationFactory;
        this.playerRepository = playerRepository;
        this.bufferedGameHostWalletFactory = bufferedGameHostWalletFactory;
        this.externalGameRequestService = externalGameRequestService;
        this.eventExecutor = eventExecutor;
        this.commandExecutor = commandExecutor;
        this.gameInitialiser = gameInitialiser;

    }

    private boolean validTransactionResult(final Table table,
                                           final long gameId,
                                           final TransactionResult transactionResult) {
        final boolean hasCurrentGame = table.getCurrentGame() != null && !rulesFor(table.getGameTypeId()).isComplete(table.getCurrentGame());
        if (LOG.isDebugEnabled()) {
            LOG.debug("validTransactionResult gameId:{} hasCurrentGame:{} table.gameId:{} table.isClosed:{}",
                    gameId, hasCurrentGame, table.getGameId(), table.isClosed());
        }
        return transactionResult != null
                && hasCurrentGame
                && table.getGameId().equals(gameId)
                && !table.isClosed();
    }


    public List<HostDocument> processTransactionResult(final Table table,
                                                       final long gameId,
                                                       final TransactionResult transactionResult) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: Processing transaction result for table", table.getTableId());
        }

        final List<HostDocument> documentsToSend = new ArrayList<>();

        final String auditLabel = auditor.newLabel();

        final BufferedGameHostWallet bufferedWalletService = bufferedGameHostWalletFactory.create(table.getTableId());
        final TableExternalGameService externalGameService = new TableExternalGameService(
                table.getTableId(), externalGameRequestService, auditor);
        try {
            if (validTransactionResult(table, gameId, transactionResult)) {
                final String gameTypeId = table.getGameTypeId();
                final GameRules gameRules = rulesFor(gameTypeId);

                if (transactionResult.getAccountId() != null) {
                    final PlayerInformation playerInformation
                            = table.playerWithAccountId(transactionResult.getAccountId());
                    if (playerInformation != null && transactionResult.getBalance() != null) {
                        playerInformation.setCachedBalance(transactionResult.getBalance());
                    }
                }

                final TableBoundGamePlayerWalletFactory gamePlayerWalletFactory = new TableBoundGamePlayerWalletFactory(
                        table, bufferedWalletService, auditLabel, uuidSource);

                final ExecutionContext context = new ExecutionContext(table, gamePlayerWalletFactory, externalGameService, auditLabel);
                final ExecutionResult executionResult = gameRules.processTransactionResult(context, transactionResult);
                if (executionResult != null) {
                    for (final Postprocessor postprocessor : commandExecutor.getPostExecutionProcessors()) {
                        postprocessor.postProcess(executionResult, null,
                                table, context.getAuditLabel(), documentsToSend, transactionResult.getPlayerId());
                    }
                    table.removePlayersNoLongerAtTable(gameRules);
                }
            }
            bufferedWalletService.flush();
            externalGameService.flush();


        } catch (GameException e) {

            logGameException(table, bufferedWalletService, e);

        }

        return documentsToSend;
    }

    public List<HostDocument> execute(Table table, ExternalCallResult externalCallResult) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: Processing external call result for table: {}", table.getTableId(), externalCallResult);
        }

        final List<HostDocument> documentsToSend = new ArrayList<HostDocument>();

        final String auditLabel = auditor.newLabel();

        final BufferedGameHostWallet bufferedWalletService = bufferedGameHostWalletFactory.create(table.getTableId());
        final TableExternalGameService externalGameService = new TableExternalGameService(
                table.getTableId(), externalGameRequestService, auditor);
        try {
            final String gameTypeId = table.getGameTypeId();
            final GameRules gameRules = rulesFor(gameTypeId);

            final TableBoundGamePlayerWalletFactory gamePlayerWalletFactory = new TableBoundGamePlayerWalletFactory(
                    table, bufferedWalletService, auditLabel, uuidSource);

            final ExecutionContext context = new ExecutionContext(table, gamePlayerWalletFactory, externalGameService, auditLabel);
            final ExecutionResult executionResult = gameRules.processExternalCallResult(context, externalCallResult);
            if (executionResult != null) {
                for (final Postprocessor postprocessor : commandExecutor.getPostExecutionProcessors()) {
                    postprocessor.postProcess(executionResult, null,
                            table, context.getAuditLabel(), documentsToSend, externalCallResult.getPlayerId());
                }
                table.removePlayersNoLongerAtTable(gameRules);
            }
            bufferedWalletService.flush();
            externalGameService.flush();
        } catch (GameException e) {
            logGameException(table, bufferedWalletService, e);
        }
        return documentsToSend;
    }

    private GameRules rulesFor(final String gameTypeId) {
        return gameRepository.getGameRules(gameTypeId);
    }

    private void logGameException(final Table table,
                                  final BufferedGameHostWallet bufferedWalletService,
                                  final GameException e) {
        if (bufferedWalletService.numberOfPendingTransactions() > 0) {
            LOG.warn("Table {}: pending transactions {}, caught exception",
                    table.getTableId(), bufferedWalletService.numberOfPendingTransactions(), e);
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: caught exception", table.getTableId(), e);
            }
        }
    }


    private List<HostDocument> execute(final Table table, final Command command) {
        return commandExecutor.execute(table, command);
    }


    public List<HostDocument> execute(final Table table, final ScheduledEvent event) {
        return eventExecutor.execute(table, event);
    }


    public List<HostDocument> forceNewGame(final Table table,
                                           final Collection<PlayerAtTableInformation> playersAtTable,
                                           final Map<BigDecimal, BigDecimal> playerIdToAccountIdOverrides) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: entering forceNewGame: {}", table.getTableId(), ToStringBuilder.reflectionToString(table));
        }
        if (table.getTableStatus() == TableStatus.error) {
            LOG.debug("Table {}: table in error state, returning", table.getTableId());
            return Collections.emptyList();
        }

        final String auditLabel = auditor.newLabel();

        for (final BigDecimal playerId : playerIdToAccountIdOverrides.keySet()) {
            PlayerInformation playerInformation = table.playerAtTable(playerId);
            if (playerInformation == null) {
                final PlayerSessionSummary playerDetails = getPlayerDetails(playerId, null);

                playerInformation = new PlayerInformation(playerId);
                playerInformation.setName(playerDetails.getName());
                playerInformation.setSessionId(playerDetails.getSessionId());
                table.addPlayerToTable(playerInformation);
            }
            playerInformation.setAccountId(playerIdToAccountIdOverrides.get(playerId));
            playerInformation.setCachedBalance(balanceFor(playerInformation.getPlayerId(),
                    playerInformation.getAccountId(), table.getTableId(), null));
        }

        table.setShowInLobby(false);
        table.open();
        table.nextIncrement();
        if (table.getTableStatus() != TableStatus.open) {
            table.setTableStatus(TableStatus.error);
            table.setMonitoringMessage("Could not open table");
        }

        final BufferedGameHostWallet bufferedWalletService = bufferedGameHostWalletFactory.create(table.getTableId());
        final TableBoundGamePlayerWalletFactory gamePlayerWalletFactory
                = new TableBoundGamePlayerWalletFactory(table, bufferedWalletService, auditLabel, uuidSource);
        final TableExternalGameService externalGameService = new TableExternalGameService(table.getTableId(),
                externalGameRequestService, auditor);

        final GameRules gameRules = rulesFor(table.getGameTypeId());

        final List<HostDocument> documentsToSend = new ArrayList<>();


        final GameInitialiser.GameInitialisationContext context = new GameInitialiser.GameInitialisationContext(
                gamePlayerWalletFactory, externalGameService, gameRules, playersAtTable);
        context.setAllowedToMoveToNextGame(true);
        context.setRunPreProcessors(false);
        context.setDocuments(documentsToSend);

        final boolean shouldContinue = gameInitialiser.runPreProcessors(gameRules, context);
        if (!shouldContinue) {
            bufferedWalletService.flush();
            externalGameService.flush();
            return documentsToSend;
        }

        gameInitialiser.initialiseGame(context);
        bufferedWalletService.flush();
        externalGameService.flush();
        return documentsToSend;
    }

    private PlayerSessionSummary getPlayerDetails(final BigDecimal playerId, final BigDecimal sessionId) {
        final PlayerSessionSummary player = playerRepository.findSummaryByPlayerAndSession(playerId, sessionId);
        if (player == null) {
            throw new IllegalArgumentException("Could not find player details for player " + playerId);
        }
        return player;
    }


    public List<HostDocument> attemptClose(final Table table) {
        final GameStatus currentGame = table.getCurrentGame();
        final GameRules gameRules = rulesFor(table.getGameTypeId());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: attempting to table (status? {}, current game == null? {}, can be closed? {})",
                    table.getTableId(), table.getTableStatus(), currentGame == null,
                    currentGame != null && gameRules.canBeClosed(currentGame));
        }

        final List<HostDocument> documentsToSend = new ArrayList<>();

        if (table.getTableStatus() == TableStatus.closing && (currentGame == null || gameRules.canBeClosed(currentGame))) {
            final Set<BigDecimal> playerIds = table.playerIds(gameRules);
            if (!playerIds.isEmpty()) {
                documentsToSend.add(new MessageHostDocument(
                        table.getGameId(), null, table.getTableId(),
                        new ParameterisedMessage("the table is closed"),
                        destinationFactory.players(playerIds)));
            }

            removeAllPlayers(table);
            table.setTableStatus(TableStatus.closed);

            table.setCurrentGame(null);
            table.setLastGame(null);
        }

        return documentsToSend;
    }

    public void removeAllPlayers(final Table table) {
        if (table == null) {
            return;
        }

        final BufferedGameHostWallet bufferedWalletService = bufferedGameHostWalletFactory.create(table.getTableId());
        final TableBoundGamePlayerWalletFactory gamePlayerWalletFactory = new TableBoundGamePlayerWalletFactory(
                table, bufferedWalletService, auditor.newLabel(), uuidSource);
        final TableExternalGameService externalGameService = new TableExternalGameService(
                table.getTableId(), externalGameRequestService, auditor);

        for (PlayerRemovalProcessor playerRemovalProcessor : playerRemovalProcessors) {
            playerRemovalProcessor.removeAllPlayers(table, gamePlayerWalletFactory);
        }
        table.removeAllPlayers();

        bufferedWalletService.flush();
        externalGameService.flush();

    }


    public void setPlayerRemovalProcessors(final Collection<PlayerRemovalProcessor> playerRemovalProcessors) {
        this.playerRemovalProcessors.clear();
        if (playerRemovalProcessors != null) {
            this.playerRemovalProcessors.addAll(playerRemovalProcessors);
        }
    }

    public List<HostDocument> execute(final Table table,
                                      final CommandWrapper commandWrapper) {
        if (commandWrapper.getPlayerId() == null) {
            return execute(table, commandWrapper.toAnonymousCommand());
        }

        final PlayerInformation playerInformation = table.playerAtTable(commandWrapper.getPlayerId());

        if (playerInformation != null) {
            playerInformation.setSessionId(commandWrapper.getSessionId());
            return execute(table, commandWrapper.toCommand(playerInformation.getName()));
        } else {
            return execute(table, commandWrapper.toCommandMissingName());
        }
    }

    /**
     * Close a table immediately (or as immediately as possible).
     *
     * @param table the table to close.
     * @return a list of documents to be sent as result of shutdown
     */
    public List<HostDocument> shutdown(final Table table) {
        notNull(table, "table may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing shutdown request received for table {}", table.getTableId());
        }

        final List<HostDocument> documentsToSend = new ArrayList<>();

        if (table.getCurrentGame() == null) {
            return documentsToSend;
        }

        final Map<GamePlayer, BigDecimal> refundAmounts = calculateRefundForPlayers(table);

        final String auditLabel = auditor.newLabel();

        final BufferedGameHostWallet bufferedWalletService = bufferedGameHostWalletFactory.create(table.getTableId());
        final TableBoundGamePlayerWalletFactory gamePlayerWalletFactory = new TableBoundGamePlayerWalletFactory(
                table, bufferedWalletService, auditLabel, uuidSource);
        final TableExternalGameService externalGameService = new TableExternalGameService(
                table.getTableId(), externalGameRequestService, auditor);


        for (Map.Entry<GamePlayer, BigDecimal> refundEntry : refundAmounts.entrySet()) {
            final GamePlayer player = refundEntry.getKey();
            final BigDecimal amount = refundEntry.getValue();
            try {
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Table {} - Refunding {} to {} due to shutdown", table.getTableId(), amount, player.getId());
                    }
                    gamePlayerWalletFactory.forPlayer(player).increaseBalanceBy(amount, auditLabel, "Shutdown refund");
                }
                documentsToSend.add(new ShutdownMessageHostDocument(destinationFactory.player(
                        player.getId()), table.getTableId(), amount));
            } catch (GameException e) {
                LOG.error("Table {} - Error while paying shutdown refund to {}",
                        table.getTableId(), player.getId(), e);
            }
        }

        bufferedWalletService.flush();
        externalGameService.flush();
        return documentsToSend;
    }

    private Map<GamePlayer, BigDecimal> calculateRefundForPlayers(final Table table) {
        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
        final Collection<PlayerAtTableInformation> allPlayers = table.playersAtTableInformation(gameRules);
        final Map<GamePlayer, BigDecimal> refundAmounts = new HashMap<>();
        for (PlayerAtTableInformation player : allPlayers) {
            BigDecimal currentRefundAmount = refundAmounts.get(player.getPlayer());
            if (currentRefundAmount == null) {
                currentRefundAmount = BigDecimal.ZERO;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {} - Adding {} to player {} total refund amount",
                        table.getTableId(), player.getInvestedChips(), player.getPlayer().getId());
            }
            currentRefundAmount = currentRefundAmount.add(player.getInvestedChips());
            refundAmounts.put(player.getPlayer(), currentRefundAmount);
        }
        return refundAmounts;
    }

    private BigDecimal balanceFor(final BigDecimal playerId,
                                  final BigDecimal accountId,
                                  final BigDecimal tableId,
                                  final String auditLabel) {
        final BigDecimal playerBalance;
        try {
            final BufferedGameHostWallet gameHostWallet;
            if (auditLabel != null) {
                gameHostWallet = bufferedGameHostWalletFactory.create(tableId, auditLabel);
            } else {
                gameHostWallet = bufferedGameHostWalletFactory.create(tableId);
            }
            playerBalance = gameHostWallet.getBalance(accountId);

        } catch (WalletServiceException e) {
            throw new RuntimeException(String.format(
                    "Account balance could not be retrieved for player %s with account ID %s",
                    playerId, accountId), e);
        }

        return playerBalance;

    }

    public GameEventExecutor getEventExecutor() {
        return eventExecutor;
    }

    public GameCommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

}
