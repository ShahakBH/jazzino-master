package com.yazino.platform.processor.table;

import com.yazino.platform.event.message.TableEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TablePersistenceRequest;
import com.yazino.platform.persistence.table.TableDAO;
import com.yazino.platform.repository.table.TableRepository;
import org.openspaces.core.GigaSpace;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.transaction.annotation.Transactional;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class TablePersistenceProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(TablePersistenceProcessor.class);

    private static final TablePersistenceRequest TEMPLATE = new TablePersistenceRequest();

    private final TableDAO tableDAO;
    private final TableRepository tableRepository;
    private final GigaSpace gigaSpace;
    private final QueuePublishingService<TableEvent> eventService;

    // CGLib constructor
    TablePersistenceProcessor() {
        tableDAO = null;
        tableRepository = null;
        gigaSpace = null;
        eventService = null;
    }

    @Autowired
    public TablePersistenceProcessor(final TableDAO tableDAO,
                                     final TableRepository tableRepository,
                                     @Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                     @Qualifier("tableEventQueuePublishingService") final QueuePublishingService<TableEvent> eventService) {
        notNull(tableDAO, "tableDAO may not be null");
        notNull(tableRepository, "tableRepository may not be null");
        notNull(gigaSpace, "gigaSpace may not be null");
        notNull(eventService, "eventService may not be null");

        this.tableDAO = tableDAO;
        this.tableRepository = tableRepository;
        this.gigaSpace = gigaSpace;
        this.eventService = eventService;
    }

    @EventTemplate
    public TablePersistenceRequest receivedTemplate() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    @Transactional
    public TablePersistenceRequest persist(final TablePersistenceRequest request) {
        LOG.debug("entering request {}", request);

        try {
            final Table table = tableRepository.findById(request.getTableId());
            if (table == null) {
                tryRemovingMatchingRequests(request);
                return null;
            }

            final boolean newTable = tableDAO.save(table);
            if (newTable) {
                eventService.send(new TableEvent(table.getTableId(), table.getGameType().getId(),
                        table.getTemplateId(), table.getTemplateName()));
            }

        } catch (DeadlockLoserDataAccessException e) {
            LOG.info("Table save was rejected due to deadlock, retrying", e);
            return request;

        } catch (Throwable t) {
            LOG.error("Table save failed", t);
            return requestInErrorState(request);
        }

        tryRemovingMatchingRequests(request);
        return null;
    }

    private TablePersistenceRequest requestInErrorState(final TablePersistenceRequest request) {
        request.setStatus(TablePersistenceRequest.STATUS_ERROR);
        return request;
    }

    private void tryRemovingMatchingRequests(final TablePersistenceRequest request) {
        try {
            gigaSpace.takeMultiple(new TablePersistenceRequest(request.getTableId()), Integer.MAX_VALUE);

            final TablePersistenceRequest errorTemplate = new TablePersistenceRequest(request.getTableId());
            errorTemplate.setStatus(TablePersistenceRequest.STATUS_ERROR);
            gigaSpace.takeMultiple(errorTemplate, Integer.MAX_VALUE);

        } catch (Throwable t) {
            LOG.error("Exception removing matching requests see stack trace: ", t);
        }
    }
}
