package com.yazino.platform.processor.table.handler;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.messaging.host.HostDocumentPublisher;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableRequest;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.repository.table.TableRepository;
import com.yazino.platform.service.audit.Auditor;
import com.yazino.platform.table.TableStatus;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.yazino.game.api.GameRules;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public abstract class TablePersistingRequestHandler<T extends TableRequest> implements TableRequestHandler<T> {
    private static final Logger LOG = LoggerFactory.getLogger(TablePersistingRequestHandler.class);

    private GameHost gameHost;
    private TableRepository tableRepository;
    private Auditor auditor;
    private HostDocumentPublisher hostDocumentPublisher;
    private GameRepository gameRepository;

    protected TablePersistingRequestHandler() {
    }

    @Autowired(required = true)
    public void setHostDocumentPublisher(final HostDocumentPublisher hostDocumentPublisher) {
        notNull(hostDocumentPublisher, "hostDocumentPublisher may not be null");

        this.hostDocumentPublisher = hostDocumentPublisher;
    }

    @Autowired(required = true)
    public void setGameHost(final GameHost gameHost) {
        this.gameHost = gameHost;
    }

    @Autowired(required = true)
    public void setTableRepository(final TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    @Autowired(required = true)
    public void setAuditor(final Auditor auditor) {
        this.auditor = auditor;
    }

    @Autowired
    public void setGameRepository(final GameRepository gameRepository) {
        notNull(gameRepository, "gameRepository may not be null");
        this.gameRepository = gameRepository;
    }

    protected abstract List<HostDocument> execute(final T gameAction,
                                                  final GameHost execGameHost,
                                                  final Table table);

    @SuppressWarnings("unchecked")
    public void handle(final T gameAction) {
        notNull(gameAction, "Game Action may not be null");

        final Table table = tableRepository.findById(gameAction.getTableId());
        if (table == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Cannot find table by ID: {} gameAction: {}",
                        gameAction.getTableId(), ReflectionToStringBuilder.reflectionToString(gameAction));
            }
            return;
        }

        if (table.getTableStatus() == TableStatus.error) {
            LOG.debug("table {} is in error state: returning", gameAction.getTableId());
            return;
        }

        try {
            final TableStatus entryStatus = table.getTableStatus();

            final List<HostDocument> documentsToSend = execute(gameAction, gameHost, table);

            hostDocumentPublisher.publish(documentsToSend);

            if (table.getTableStatus() != entryStatus) {
                tableRepository.save(table);
            } else {
                tableRepository.nonPersistentSave(table);
            }
        } catch (Throwable t) {
            final String message = "Unexpected processing exception - shutting down table" + table.getTableId()
                    + " on game " + table.getGameId();
            LOG.error(message, t);
            LOG.error("Current game: {}", ReflectionToStringBuilder.reflectionToString(table.getCurrentGame()));
            LOG.error("Last game: {}", ReflectionToStringBuilder.reflectionToString(table.getLastGame()));

            final GameRules gameRules = gameRepository.getGameRules(table.getGameTypeId());
            final String auditLabel = auditor.newLabel();
            auditor.audit(auditLabel, table, gameRules);
            table.setTableStatus(TableStatus.error);

            final StringWriter stringWriter = new StringWriter();
            t.printStackTrace(new PrintWriter(stringWriter));
            table.setMonitoringMessage("Exception:  " + stringWriter.toString());
            tableRepository.save(table);
            gameHost.removeAllPlayers(table);
        }
    }
}
