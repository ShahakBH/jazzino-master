package com.yazino.platform.gamehost;

import com.yazino.game.api.*;
import com.yazino.platform.gamehost.postprocessing.Postprocessor;
import com.yazino.platform.gamehost.preprocessing.CommandPreprocessor;
import com.yazino.platform.gamehost.preprocessing.EventPreprocessor;
import com.yazino.platform.gamehost.wallet.TableBoundGamePlayerWalletFactory;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Responsible for ensuring theres a game against the specified table.
 */
public class GameInitialiser {
    private static final Logger LOG = LoggerFactory.getLogger(GameInitialiser.class);

    private List<CommandPreprocessor> commandPreInitialisationProcessors = new ArrayList<CommandPreprocessor>();
    private List<EventPreprocessor> eventPreInitialisationProcessors = new ArrayList<EventPreprocessor>();
    private List<Postprocessor> postInitialisationProcessors = new ArrayList<Postprocessor>();

    private ExecutionResult createNextGame(final TableBoundGamePlayerWalletFactory gamePlayerWalletFactory,
                                           final ExternalGameService externalGameService,
                                           final Table table,
                                           final String auditLabel,
                                           final GameRules gameRules) {
        table.setGameId(table.getGameId() + 1);
        table.clearEvents();

        final ExecutionContext executionContext = new ExecutionContext(table, gamePlayerWalletFactory,
                externalGameService, auditLabel);
        final ExecutionResult result = gameRules.startNextGame(executionContext);

        table.setIncrementOfGameStart(executionContext.getNextIncrement());
        table.setLastGame(table.getCurrentGame());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: Starting next game", table.getTableId());
        }

        return result;
    }

    private ExecutionResult createFirstGame(final TableBoundGamePlayerWalletFactory gamePlayerWalletFactory,
                                            final Table table,
                                            final String auditLabel,
                                            final GameRules gameRules,
                                            final Collection<PlayerAtTableInformation> players) {
        table.setGameId(1L);
        table.clearEvents();

        final GameCreationContext creationContext = new GameCreationContext(table.getGameId(), gamePlayerWalletFactory,
                table.getCombinedGameProperties(), auditLabel, table.incrementDefaultedToOne(), players);
        final ExecutionResult result = gameRules.startNewGame(creationContext);
        table.setIncrementOfGameStart(creationContext.getIncrement());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Table {}: Starting new game", table.getTableId());
        }

        return result;
    }

    public boolean runPreProcessors(final GameRules gameRules, final GameInitialisationContext context) {
        boolean shouldContinue = true;
        if (context.isRunPreProcessors()) {
            if (context.getCommand() != null) {
                shouldContinue = runCommandPreProcessors(gameRules, context);
            }
            if (context.getEvent() != null) {
                shouldContinue = shouldContinue && runEventPreProcessors(context);
            }
        }
        return shouldContinue;
    }

    public void initialiseGame(final GameInitialisationContext context) {
        ExecutionResult creationResult;
        final Table table = context.getGamePlayerWalletFactory().getTable();
        final String auditLabel = context.getGamePlayerWalletFactory().getAuditLabel();

        if (isFirstGame(table)) {
            creationResult = createFirstGame(context.getGamePlayerWalletFactory(), table,
                    auditLabel, context.getGameRules(), context.getPlayers());
        } else if (isEndOfPreviousGame(context.getGameRules(), table) && context.isAllowedToMoveToNextGame()) {
            creationResult = createNextGame(context.getGamePlayerWalletFactory(), context.getExternalGameService(), table,
                    auditLabel, context.getGameRules());
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Incorrect state for creating a new game against table [{}]"
                        + ", should have been first game or end of current game", table.getTableId());
            }
            return;
        }

        if (context.isRunPostProcessors()) {
            runPostProcessors(creationResult, context, postInitialisationProcessors);
        }
    }

    private boolean runEventPreProcessors(final GameInitialisationContext context) {
        for (EventPreprocessor processor : eventPreInitialisationProcessors) {
            final boolean doContinue = processor.preprocess(context.getEvent(),
                    context.getGamePlayerWalletFactory().getTable());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table %s: preprocessor [{}] yielded a [{}] result",
                        context.getGamePlayerWalletFactory().getTable().getTableId(),
                        processor.getClass().getSimpleName(), doContinue);
            }
            if (!doContinue) {
                return false;
            }
        }
        return true;
    }

    private boolean runCommandPreProcessors(final GameRules gameRules, final GameInitialisationContext context) {
        final Table table = context.getGamePlayerWalletFactory().getTable();
        final String auditLabel = context.getGamePlayerWalletFactory().getAuditLabel();

        for (CommandPreprocessor processor : commandPreInitialisationProcessors) {
            final boolean doContinue = processor.preProcess(gameRules, context.getCommand(),
                    context.getPlayerBalance(), table, auditLabel, context.getDocuments());
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
                                          final GameInitialisationContext context,
                                          final List<Postprocessor> processors) {
        final Table table = context.getGamePlayerWalletFactory().getTable();
        final String auditLabel = context.getGamePlayerWalletFactory().getAuditLabel();
        for (Postprocessor processor : processors) {
            processor.postProcess(result, context.getCommand(), table, auditLabel, context.getDocuments(),
                    context.getPlayerId());
            if (LOG.isDebugEnabled()) {
                LOG.debug("Table {}: after running postprocessor [{}], documents were [{}] ",
                        table.getTableId(), processor.getClass().getSimpleName(), context.documents);
            }
        }
    }

    private static boolean isEndOfPreviousGame(final GameRules gameRules, final Table table) {
        return gameRules.isComplete(table.getCurrentGame());
    }

    private static boolean isFirstGame(final Table table) {
        return table.getCurrentGame() == null;
    }

    public void setCommandPreInitialisationProcessors(final List<CommandPreprocessor> processors) {
        notNull(processors, "processors was null");
        this.commandPreInitialisationProcessors = processors;
    }

    public void setEventPreInitialisationProcessors(final List<EventPreprocessor> processors) {
        notNull(processors, "processors was null");
        this.eventPreInitialisationProcessors = processors;
    }

    public void setPostInitialisationProcessors(final List<Postprocessor> processors) {
        notNull(processors, "processors was null");
        this.postInitialisationProcessors = processors;
    }

    public static class GameInitialisationContext {
        private final ScheduledEvent event;
        private final Command command;
        private final TableBoundGamePlayerWalletFactory gamePlayerWalletFactory;
        private final ExternalGameService externalGameService;
        private final GameRules gameRules;
        private BigDecimal playerId;
        private BigDecimal playerBalance = BigDecimal.ZERO;
        private Collection<PlayerAtTableInformation> players = Collections.emptyList();
        private List<HostDocument> documents = new ArrayList<HostDocument>();
        private boolean runPreProcessors = true;
        private boolean runPostProcessors = true;
        private boolean allowedToMoveToNextGame = false;

        public GameInitialisationContext(final ScheduledEvent event,
                                         final TableBoundGamePlayerWalletFactory gamePlayerWalletFactory,
                                         final ExternalGameService externalGameService,
                                         final GameRules gameRules) {
            this.event = event;
            this.gamePlayerWalletFactory = gamePlayerWalletFactory;
            this.externalGameService = externalGameService;
            this.gameRules = gameRules;
            this.command = null;
            this.playerId = null;
        }

        public GameInitialisationContext(final Command command,
                                         final TableBoundGamePlayerWalletFactory gamePlayerWalletFactory,
                                         final ExternalGameService externalGameService,
                                         final GameRules gameRules,
                                         final BigDecimal playerBalance) {
            this.command = command;
            this.gamePlayerWalletFactory = gamePlayerWalletFactory;
            this.externalGameService = externalGameService;
            this.gameRules = gameRules;
            this.playerBalance = playerBalance;
            if (command.getPlayer() != null) {
                this.playerId = command.getPlayer().getId();
            } else {
                this.playerId = null;
            }
            this.event = null;
        }

        public GameInitialisationContext(final TableBoundGamePlayerWalletFactory gamePlayerWalletFactory,
                                         final ExternalGameService externalGameService,
                                         final GameRules gameRules,
                                         final Collection<PlayerAtTableInformation> players) {
            this.gamePlayerWalletFactory = gamePlayerWalletFactory;
            this.externalGameService = externalGameService;
            this.gameRules = gameRules;
            this.players = players;
            this.event = null;
            this.command = null;
        }

        public ScheduledEvent getEvent() {
            return event;
        }

        public Command getCommand() {
            return command;
        }

        public TableBoundGamePlayerWalletFactory getGamePlayerWalletFactory() {
            return gamePlayerWalletFactory;
        }

        public GameRules getGameRules() {
            return gameRules;
        }

        public List<HostDocument> getDocuments() {
            return documents;
        }

        public void setDocuments(final List<HostDocument> documents) {
            this.documents = documents;
        }

        public boolean isAllowedToMoveToNextGame() {
            return allowedToMoveToNextGame;
        }

        public void setAllowedToMoveToNextGame(final boolean allowedToMoveToNextGame) {
            this.allowedToMoveToNextGame = allowedToMoveToNextGame;
        }

        public BigDecimal getPlayerBalance() {
            return playerBalance;
        }

        public void setPlayerBalance(final BigDecimal playerBalance) {
            this.playerBalance = playerBalance;
        }

        public BigDecimal getPlayerId() {
            return playerId;
        }

        public boolean isRunPreProcessors() {
            return runPreProcessors;
        }

        public void setRunPreProcessors(final boolean runPreProcessors) {
            this.runPreProcessors = runPreProcessors;
        }

        public boolean isRunPostProcessors() {
            return runPostProcessors;
        }

        public void setRunPostProcessors(final boolean runPostProcessors) {
            this.runPostProcessors = runPostProcessors;
        }

        public Collection<PlayerAtTableInformation> getPlayers() {
            return players;
        }

        public ExternalGameService getExternalGameService() {
            return externalGameService;
        }
    }

}
