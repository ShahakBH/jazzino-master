package com.yazino.platform.processor.table.handler;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.messaging.host.HostDocumentPublisher;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableControlMessage;
import com.yazino.platform.model.table.TableControlMessageType;
import com.yazino.platform.model.table.TableRequestType;
import com.yazino.platform.persistence.table.TableDAO;
import com.yazino.platform.repository.table.TableRepository;
import com.yazino.platform.table.TableStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * this processor acts on table control messages
 */

@Service
@Qualifier("tableRequestHandler")
public class TableControlHandler implements TableRequestHandler<TableControlMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(TableControlHandler.class);
    private TableRepository tableRepository;
    private TableDAO tableDAO;
    private GameHost gameHost;
    private HostDocumentPublisher hostDocumentPublisher;

    @Autowired
    public void setHostDocumentPublisher(final HostDocumentPublisher hostDocumentPublisher) {
        this.hostDocumentPublisher = hostDocumentPublisher;
    }

    @Autowired
    public void setTableRepository(final TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    @Autowired
    public void setTableDAO(final TableDAO tableDAO) {
        this.tableDAO = tableDAO;
    }

    @Autowired
    public void setGameHost(final GameHost gameHost) {
        this.gameHost = gameHost;
    }

    @Override
    public boolean accepts(final TableRequestType requestType) {
        return requestType == TableRequestType.CONTROL;
    }

    @Transactional
    public void handle(final TableControlMessage tableControlMessage) {
        LOG.debug("Processing table control message {}", tableControlMessage);

        if (tableControlMessage.getMessageType() == TableControlMessageType.LOAD) {
            processLoad(tableControlMessage.getTableId());
            return;
        }

        Table table = tableRepository.findById(tableControlMessage.getTableId());
        if (table == null) {
            if (tableControlMessage.getMessageType() == TableControlMessageType.UNLOAD) {
                return;
            }

            table = processLoad(tableControlMessage.getTableId());
        }

        try {
            switch (tableControlMessage.getMessageType()) {
                case UNLOAD:
                    processUnload(table);
                    return;
                case REOPEN:
                    processReopen(table);
                    return;
                case CLOSE:
                    processClose(table);
                    return;
                case SHUTDOWN:
                    processShutdown(table);
                    return;
                case RESET:
                    processReset(table);
                    return;

                default:
                    LOG.error("Unknown table control message type {} for table {}",
                            tableControlMessage.getMessageType(), tableControlMessage.getTableId());
            }
        } catch (Throwable t) {
            LOG.error("error processing table control request ", t);
        }
    }

    private void processReset(final Table table) {
        gameHost.removeAllPlayers(table);
        table.reset();
        tableRepository.save(table);
    }


    private Table processLoad(final BigDecimal tableId) {
        Table table = tableRepository.findById(tableId);
        if (table != null) {
            LOG.debug("table already loaded");
            return table;
        }
        table = tableDAO.findById(tableId);
        if (table == null) {
            throw new RuntimeException("Cannot find table " + tableId + " in repository");
        }
        tableRepository.save(table);
        return table;
    }

    private void processUnload(final Table table) {
        gameHost.removeAllPlayers(table);
        tableRepository.save(table);
        tableRepository.unload(table.getTableId());
    }

    private void processClose(final Table table) {
        table.close();
        tableRepository.save(table);
    }

    private void processShutdown(final Table table) {
        if (table.isTableOpen() && table.getShowInLobby()) {
            final List<HostDocument> documents = gameHost.shutdown(table);
            table.setTableStatus(TableStatus.closed);
            tableRepository.save(table);
            hostDocumentPublisher.publish(documents);
        }
    }

    private void processReopen(final Table table) {
        table.open();
        tableRepository.save(table);
    }

}
