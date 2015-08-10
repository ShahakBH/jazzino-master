package com.yazino.platform.gamehost;

import com.yazino.game.api.*;
import com.yazino.platform.gamehost.external.ExternalGameRequestService;
import com.yazino.platform.gamehost.external.TableExternalGameService;
import com.yazino.platform.gamehost.postprocessing.Postprocessor;
import com.yazino.platform.gamehost.preprocessing.EventPreprocessor;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWallet;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWalletFactory;
import com.yazino.platform.gamehost.wallet.TableBoundGamePlayerWalletFactory;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.service.audit.Auditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Executes events.
 */
public class GameEventExecutor extends BaseGameExecutor<EventPreprocessor, ScheduledEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(GameEventExecutor.class);

    @Autowired
    public GameEventExecutor(final GameRepository gameRepository,
                             final Auditor auditor,
                             final BufferedGameHostWalletFactory bufferedGameHostWalletFactory,
                             final ExternalGameRequestService novomaticRequestService) {
        super(bufferedGameHostWalletFactory, auditor, gameRepository, novomaticRequestService);
    }

    @Override
    public boolean isApplicableForExecution(final Table table,
                                            final ScheduledEvent event) {
        return true;
    }

    @Override
    public BufferedGameHostWallet newBufferedGameHostWallet(final Table table,
                                                            final ScheduledEvent event) {
        return getBufferedGameHostWalletFactory().create(table.getTableId());
    }

    @Override
    GameInitialiser.GameInitialisationContext createContext(final ScheduledEvent event,
                                                            final GameRules gameRules,
                                                            final TableBoundGamePlayerWalletFactory walletFactory, TableExternalGameService externalGameService) {
        final GameInitialiser.GameInitialisationContext initialisationContext
                = new GameInitialiser.GameInitialisationContext(event, walletFactory, externalGameService, gameRules);
        initialisationContext.setAllowedToMoveToNextGame(event.isNoProcessingEvent());
        return initialisationContext;
    }

    @Override
    void postExecution(final ExecutionResult result,
                       final GameRules gameRules, final Table table,
                       final String auditLabel,
                       final List<HostDocument> documents,
                       final ScheduledEvent event) {
        runPostProcessors(result, documents, table, auditLabel, getPostExecutionProcessors());
        table.removePlayersNoLongerAtTable(gameRules);
    }

    @Override
    ExecutionResult doExecution(final ExecutionContext context,
                                final Table table,
                                final GameRules gameRules,
                                final List<HostDocument> documents,
                                final ScheduledEvent event) {
        try {
            return gameRules.execute(context, event);
        } catch (GameException gex) {
            final String message = String.format("Table %s: caught game exception during event processing",
                    table.getTableId());
            LOG.info(message, gex);
            if (gex.isThrowForEvents()) {
                throw new IllegalStateException(message, gex);
            }
        } catch (Throwable e) {
            final String message = String.format("Table %s: caught exception during event processing",
                    table.getTableId());
            LOG.error(message, e);
            throw new IllegalStateException(message, e);
        }
        return null;
    }

    @Override
    boolean isOkToProceedWithExecution(final GameRules gameRules, final TableBoundGamePlayerWalletFactory walletFactory,
                                       final List<HostDocument> documents,
                                       final ScheduledEvent event) {
        final Table table = walletFactory.getTable();
        return runPreProcessors(table, event, getPreExecutionProcessors());
    }

    private static boolean runPreProcessors(final Table table,
                                            final ScheduledEvent event,
                                            final List<EventPreprocessor> processors) {
        for (EventPreprocessor processor : processors) {
            final boolean doContinue = processor.preprocess(event, table);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Table %s: preprocessor [%s] yielded a [%s] result",
                        table.getTableId(), processor.getClass().getSimpleName(), doContinue));
            }
            if (!doContinue) {
                return false;
            }
        }
        return true;
    }

    private static void runPostProcessors(final ExecutionResult result,
                                          final List<HostDocument> documents,
                                          final Table table,
                                          final String auditLabel,
                                          final List<Postprocessor> processors) {
        for (Postprocessor processor : processors) {
            processor.postProcess(result, null, table, auditLabel, documents, null);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Table %s: after running postprocessor [%s], documents were [%s] ",
                        table.getTableId(), processor.getClass().getSimpleName(), documents));
            }
        }
    }


}
