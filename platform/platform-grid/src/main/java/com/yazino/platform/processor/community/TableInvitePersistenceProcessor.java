package com.yazino.platform.processor.community;

import com.yazino.platform.model.community.TableInvite;
import com.yazino.platform.model.community.TableInvitePersistenceRequest;
import com.yazino.platform.persistence.community.TableInviteDAO;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 3)
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class TableInvitePersistenceProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(TableInvitePersistenceProcessor.class);

    private static final TableInvitePersistenceRequest TEMPLATE = new TableInvitePersistenceRequest();

    private TableInviteDAO tableInviteDAO;

    @SpaceDataEvent
    public void processTableInvitePersistenceRequest(final TableInvitePersistenceRequest request) {
        notNull(request, "request must not be null");
        final TableInvite tableInvite = request.getTableInvite();

        if (LOG.isDebugEnabled()) {
            LOG.debug("processing request " + request.getSpaceId() + " with table invite" + tableInvite);
        }

        tableInviteDAO.save(tableInvite);

        if (LOG.isDebugEnabled()) {
            LOG.debug("inserted table invite using dao " + tableInvite);
        }
    }

    @Autowired(required = true)
    public void setTableInviteDAO(final TableInviteDAO tableInviteDAO) {
        notNull(tableInviteDAO, "tableInviteDAO must not be null");
        this.tableInviteDAO = tableInviteDAO;
    }

    @EventTemplate
    public TableInvitePersistenceRequest template() {
        return TEMPLATE;
    }


}
