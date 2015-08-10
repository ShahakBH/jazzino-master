package com.yazino.test;

import com.yazino.game.api.*;
import com.yazino.game.api.statistic.GameStatistic;
import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.novomatic.NovomaticFakeClient;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.gamehost.GamePlayerService;
import com.yazino.platform.gamehost.external.NovomaticRequest;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.table.CommandWrapper;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.processor.external.NovomaticRequestExecutor;
import com.yazino.platform.table.PlayerInformation;
import com.yazino.platform.table.TableStatus;
import com.yazino.platform.test.InMemoryPlayerRepository;
import com.yazino.test.game.*;
import com.yazino.test.statistic.InMemoryGameStatisticsPublisher;
import com.yazino.test.wallet.IntegrationWalletService;
import senet.server.host.local.InMemoryNovomaticGameRequestService;

import java.math.BigDecimal;
import java.util.*;

public class GameIntegration {

    private final GamePlayerService playerService;
    private final InMemoryNovomaticGameRequestService externalGameRequestService;
    private final GameHost host;
    private final GameMessageDispatcher dispatcher;

    private final IntegrationWalletService wallet;
    private final InMemoryPlayerRepository playerRepository;
    private final InMemoryGameStatisticsPublisher gameStatisticsPublisher;
    private final SettableTimeSource timeSource;
    private final TestRandomizer randomizer;
    private final GameRules gameRules;
    private final NovomaticRequestExecutor novomaticRequestExecutor = new NovomaticRequestExecutor(new NovomaticFakeClient());

    private BigDecimal highestPlayerId = BigDecimal.ZERO;

    GameIntegration(final GamePlayerService playerService,
                    final IntegrationWalletService wallet,
                    final InMemoryNovomaticGameRequestService externalGameRequestService,
                    final GameHost host,
                    final GameMessageDispatcher dispatcher,
                    final InMemoryPlayerRepository playerRepository,
                    final SettableTimeSource timeSource,
                    final InMemoryGameStatisticsPublisher gameStatisticsPublisher,
                    final TestRandomizer randomizer,
                    final GameRules gameRules) {
        this.playerService = playerService;
        this.wallet = wallet;
        this.externalGameRequestService = externalGameRequestService;
        this.host = host;
        this.timeSource = timeSource;
        this.dispatcher = dispatcher;
        this.playerRepository = playerRepository;
        this.gameStatisticsPublisher = gameStatisticsPublisher;
        this.randomizer = randomizer;
        this.gameRules = gameRules;
    }

    private Table table;

    private final List<PlayerCommand> lastCommands = new ArrayList<PlayerCommand>();


    public Table createTable(final GameType gameType,
                             final Map<String, String> properties) {
        table = new Table(gameType, BigDecimal.ONE, null, true);
        table.setTableId(BigDecimal.valueOf(1L));
        table.setTableStatus(TableStatus.open);
        table.setVariationProperties(properties);
        gameStatisticsPublisher.getStatistics().clear();
        return table;
    }

    public GameStatus getCurrentGameStatus() {
        return table.getCurrentGame();
    }

    public void execute(final PlayerCommand command) {
        final List<HostDocument> execute = host.execute(table, toWrapper(command));
        for (HostDocument hostDocument : execute) {
            hostDocument.send(dispatcher);
        }
        lastCommands.add(command);
    }

    private CommandWrapper toWrapper(final PlayerCommand command) {
        final CommandWrapper commandWrapper = new CommandWrapper(table.getTableId(), table.getGameId(),
                command.getPlayerId(),
                null,
                command.getCommandType(),
                command.getArgs());
        commandWrapper.setRequestId("uuid");
        return commandWrapper;
    }

    public void moveTime(final Long timeChange) {
        timeSource.addMillis(timeChange);
        excutePendingEvents();
        executePendingTransactionsInCascade();
        executePendingExternalRequests();
    }

    private void executePendingExternalRequests() {
        final Collection<NovomaticRequest> requests = externalGameRequestService.getAndClearPendingRequests();
        for (NovomaticRequest request : requests) {
            ExternalCallResult result = novomaticRequestExecutor.execute(request);
            final List<HostDocument> hostDocuments = host.execute(table, result);
            for (HostDocument hostDocument : hostDocuments) {
                hostDocument.send(dispatcher);
            }
        }
        if (!requests.isEmpty()) {
            moveTime(0L);
        }
    }

    public int excutePendingEvents() {
        int totalEventCount = 0;
        int count;
        do {
            count = 0;
            final List<ScheduledEvent> pendingEvents = table.getPendingEvents(timeSource);
            for (ScheduledEvent event : pendingEvents) {
                for (HostDocument hostDocument : host.execute(table, event)) {
                    hostDocument.send(dispatcher);
                }
                count++;
                totalEventCount++;
            }
        } while (count > 0);
        return totalEventCount;
    }

    public int executePendingTransactionsInCascade() {
        return executeTransactions(true);
    }

    public int executePendingTransactions() {
        return executeTransactions(false);
    }

    private int executeTransactions(final boolean cascade) {
        int totalTransactions = 0;
        int count;
        do {
            count = 0;
            for (TransactionResult transactionResult : wallet.pendingResults()) {
                for (HostDocument hostDocument : host.processTransactionResult(table,
                        table.getGameId(),
                        transactionResult)) {
                    hostDocument.send(dispatcher);
                }
                count++;
                totalTransactions++;
            }
        } while (count > 0 && cascade);
        return totalTransactions;
    }

    public void clear() {
        lastCommands.clear();
        dispatcher.clear();
    }

    public List<PlayerCommand> getLastCommands() {
        return lastCommands;
    }

    public void startNewGame(final Collection<BigDecimal> playerIds) {
        final Collection<PlayerAtTableInformation> players = new ArrayList<PlayerAtTableInformation>();
        for (BigDecimal player : playerIds) {
            final Map<String, String> properties = Collections.<String, String>emptyMap();
            players.add(new PlayerAtTableInformation(playerService.getPlayer(player), properties));
        }
        host.forceNewGame(table, players, new HashMap<BigDecimal, BigDecimal>());
    }

    public BigDecimal createPlayer(final BigDecimal playerId,
                                   final BigDecimal startingBalance) {
        wallet.createAccountIfRequired(playerId, "player " + playerId, startingBalance);
        final Player profileInformation = new Player(playerId,
                "player " + playerId,
                playerId,
                "picture-player-" + playerId,
                null, null, null);
        playerRepository.save(profileInformation);
        if (playerId.compareTo(highestPlayerId) > 0) {
            highestPlayerId = playerId;
        }
        return playerId;
    }

    public BigDecimal createPlayer(final BigDecimal initialBalance) {
        return createPlayer(highestPlayerId.add(BigDecimal.ONE), initialBalance);
    }

    public void addPlayerToTable(final BigDecimal playerId) {
        table.addPlayerToTable(new PlayerInformation(playerId, "player " + playerId, playerId, BigDecimal.TEN, BigDecimal.ZERO));
    }

    public BigDecimal getBalance(final BigDecimal playerId) {
        return wallet.getBalance(playerId);
    }

    public long getGameId() {
        if (table.getGameId() != null) {
            return table.getGameId();
        }
        return -1;
    }

    public void overrideGameProperty(final String property,
                                     final String value) {
        getOrCreateTable().getVariationProperties().put(property, value);
    }

    private Table getOrCreateTable() {
        if (table != null) {
            return table;
        }
        final Table dummy = new Table();
        dummy.setTableId(BigDecimal.valueOf(-1L));
        dummy.setTableStatus(TableStatus.open);
        dummy.setVariationProperties(Collections.<String, String>emptyMap());
        return dummy;
    }

    public Map<String, String> getGameProperties() {
        return table.getCombinedGameProperties();
    }

    public void addGameMessageListener(final BigDecimal playerId,
                                       final GameMessageListener listener) {
        dispatcher.addListener(playerId, listener);
    }

    public SettableTimeSource getTimeSource() {
        return timeSource;
    }

    public void updateGameStatus(final GameStatus status,
                                 final ScheduledEvent... scheduledEvents) {
        table.setCurrentGame(status);
        for (ScheduledEvent scheduledEvent : scheduledEvents) {
            table.addEvent(timeSource, scheduledEvent);
        }
        final Collection<PlayerAtTableInformation> playerInformation = gameRules.getPlayerInformation(status);
        for (PlayerAtTableInformation playerAtTableInformation : playerInformation) {
            addPlayerToTable(playerAtTableInformation.getPlayer().getId());
        }
    }


    public GameMessage getLastGameMessage(final BigDecimal playerId) {
        return dispatcher.getLastPlayerDocument(table.getTableId(), playerId);
    }

    public void resetPlayerBalance(final BigDecimal playerId,
                                   final BigDecimal balance) {
        final BigDecimal previousBalance = getBalance(playerId);
        try {
            wallet.postTransaction(playerId, balance.subtract(previousBalance), "Adjust Balance", "yazino-integration", TransactionContext.EMPTY);
        } catch (WalletServiceException e) {
            throw new RuntimeException(e);
        }
    }

    public void overrideGameProperties(final Map<String, String> props) {
        for (Map.Entry<String, String> entry : props.entrySet()) {
            overrideGameProperty(entry.getKey(), entry.getValue());
        }
    }

    public GameStatus getLastGameStatus() {
        return table.getLastGame();
    }

    public void setWalletToRejectTransactions(final boolean rejectTransactions) {
        wallet.rejectTransactions(rejectTransactions);
    }

    public void setWalletToGroundTransactions(final boolean groundTransactions) {
        wallet.groundTransactions(groundTransactions);
    }

    public TestRandomizer getRandomizer() {
        return randomizer;
    }

    public Collection<GameStatistic> getGameStatistics() {
        return gameStatisticsPublisher.getStatistics();
    }

    public void closeTable() {
        final GameStatus currentGame = table.getCurrentGame();
        if (currentGame == null || gameRules.getPlayerInformation(currentGame).size() == 0) {
            table.setTableStatus(TableStatus.closed);
        } else {
            table.setTableStatus(TableStatus.closing);
        }
    }

    public void resetTable() {
        if (table == null) {
            throw new IllegalStateException("Table was not created yet");
        }
        createTable(table.getGameType(), table.getVariationProperties());
    }

    public BigDecimal getTotalStakeAmount() {
        return wallet.totalStake();
    }

    public long getTotalStakeCount() {
        return wallet.totalStakeCount();
    }

    public BigDecimal getTotalPayoutAmount() {
        return wallet.totalPayout();
    }

    public long getTotalPayoutCount() {
        return wallet.totalPayoutCount();
    }

    public GameRules getGameRules() {
        return gameRules;
    }
}
