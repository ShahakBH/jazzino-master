package com.yazino.platform.gamehost;

import com.yazino.game.api.ExecutionContext;
import com.yazino.game.api.ExecutionResult;
import com.yazino.game.api.GameRules;
import com.yazino.platform.gamehost.external.ExternalGameRequestService;
import com.yazino.platform.gamehost.external.TableExternalGameService;
import com.yazino.platform.gamehost.postprocessing.Postprocessor;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWallet;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWalletFactory;
import com.yazino.platform.gamehost.wallet.TableBoundGamePlayerWalletFactory;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.service.audit.Auditor;
import com.yazino.platform.util.JavaUUIDSource;
import com.yazino.platform.util.UUIDSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Commonality between {@link GameCommandExecutor} and {@link GameEventExecutor}.
 */
public abstract class BaseGameExecutor<P, O> {
    private static final Logger LOG = LoggerFactory.getLogger(BaseGameExecutor.class);

    private final GameRepository gameRepository;
    private final Auditor auditor;
    private final BufferedGameHostWalletFactory bufferedGameHostWalletFactory;
    private final ExternalGameRequestService externalGameRequestService;

    private List<P> preExecutionProcessors = new ArrayList<P>();
    private List<Postprocessor> postExecutionProcessors = new ArrayList<Postprocessor>();

    private GameInitialiser gameInitialiser = new GameInitialiser();
    private UUIDSource uuidSource = new JavaUUIDSource();

    public BaseGameExecutor(final BufferedGameHostWalletFactory bufferedGameHostWalletFactory,
                            final Auditor auditor,
                            final GameRepository gameRepository,
                            final ExternalGameRequestService externalGameRequestService) {

        notNull(bufferedGameHostWalletFactory, "bufferedGameHostWalletFactory was null");
        notNull(auditor, "auditor was null");
        notNull(gameRepository, "gameRepository was null");
        notNull(externalGameRequestService, "novomaticRequestService was null");

        this.bufferedGameHostWalletFactory = bufferedGameHostWalletFactory;
        this.auditor = auditor;
        this.gameRepository = gameRepository;
        this.externalGameRequestService = externalGameRequestService;
    }

    public List<HostDocument> execute(final Table table,
                                      final O object) {

        if (!isApplicableForExecution(table, object)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Table %s: %s was not valid for this table [gameId:%s], ignoring",
                        table.getTableId(), object.getClass().getSimpleName(), table.getGameId()));
            }
            return Collections.emptyList();
        }

        final String auditLabel = auditor.newLabel();

        final BufferedGameHostWallet bufferedWalletService = newBufferedGameHostWallet(table, object);
        final TableBoundGamePlayerWalletFactory gamePlayerWalletFactory = new TableBoundGamePlayerWalletFactory(
                table, bufferedWalletService, auditLabel, uuidSource);
        final TableExternalGameService externalGameService = new TableExternalGameService(
                table.getTableId(), externalGameRequestService, auditor);

        final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());

        final List<HostDocument> documents = new ArrayList<HostDocument>();

        final GameInitialiser.GameInitialisationContext initialisationContext = createContext(
                object, gameRules, gamePlayerWalletFactory, externalGameService);

        final boolean shouldContinue = gameInitialiser.runPreProcessors(gameRules, initialisationContext);

        if (!shouldContinue) {
            documents.addAll(initialisationContext.getDocuments());
            bufferedWalletService.flush();
            externalGameService.flush();
            return documents;
        }

        if (!isGameInProgress(gameRules, table)) {
            gameInitialiser.initialiseGame(initialisationContext);
            documents.addAll(initialisationContext.getDocuments());
        }

        if (!isOkToProceedWithExecution(gameRules, gamePlayerWalletFactory, documents, object)) {
            bufferedWalletService.flush();
            externalGameService.flush();
            return documents;
        }

        final ExecutionContext context = new ExecutionContext(table, gamePlayerWalletFactory, externalGameService, auditLabel);

        final ExecutionResult result = doExecution(context, table, gameRules, documents, object);
        bufferedWalletService.flush();
        externalGameService.flush();
        postExecution(result, gameRules, table, auditLabel, documents, object);

        return documents;
    }

    abstract boolean isApplicableForExecution(Table table, O object);

    abstract boolean isOkToProceedWithExecution(final GameRules gameRules,
                                                TableBoundGamePlayerWalletFactory walletFactory,
                                                List<HostDocument> documents,
                                                O object);

    abstract ExecutionResult doExecution(ExecutionContext context,
                                         Table table,
                                         GameRules gameRules,
                                         List<HostDocument> documents, O object);

    abstract void postExecution(ExecutionResult result,
                                final GameRules gameRules, Table table,
                                String auditLabel,
                                List<HostDocument> documents,
                                O object);

    abstract BufferedGameHostWallet newBufferedGameHostWallet(Table table, O object);

    abstract GameInitialiser.GameInitialisationContext createContext(O object,
                                                                     GameRules gameRules,
                                                                     TableBoundGamePlayerWalletFactory walletFactory,
                                                                     TableExternalGameService
                                                                             externalGameService);

    private boolean isGameInProgress(final GameRules gameRules, final Table table) {
        return table.getCurrentGame() != null && !gameRules.isComplete(table.getCurrentGame());
    }

    public void setPreExecutionProcessors(final List<P> processors) {
        notNull(processors, "PreExecution processors was null");
        this.preExecutionProcessors = processors;
    }

    public List<P> getPreExecutionProcessors() {
        return preExecutionProcessors;
    }

    public void setPostExecutionProcessors(final List<Postprocessor> processors) {
        notNull(processors, "PostExecution processors was null");
        this.postExecutionProcessors = processors;
    }

    public List<Postprocessor> getPostExecutionProcessors() {
        return postExecutionProcessors;
    }

    public void setUuidSource(final UUIDSource uuidSource) {
        notNull(uuidSource, "uuidSource was null");
        this.uuidSource = uuidSource;
    }

    public UUIDSource getUuidSource() {
        return uuidSource;
    }

    @Autowired(required = true)
    public void setGameInitialiser(final GameInitialiser gameInitialiser) {
        notNull(uuidSource, "gameInitialiser was null");
        this.gameInitialiser = gameInitialiser;
    }

    public GameInitialiser getGameInitialiser() {
        return gameInitialiser;
    }

    public GameRepository getGameRepository() {
        return gameRepository;
    }

    public Auditor getAuditor() {
        return auditor;
    }

    public BufferedGameHostWalletFactory getBufferedGameHostWalletFactory() {
        return bufferedGameHostWalletFactory;
    }
}
