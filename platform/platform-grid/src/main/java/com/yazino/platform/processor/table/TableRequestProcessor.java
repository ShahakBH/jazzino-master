package com.yazino.platform.processor.table;

import com.yazino.platform.model.table.TableRequestWrapper;
import com.yazino.platform.processor.table.handler.TableRequestHandler;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This processor handles all request that write to tables, hence avoiding
 * the need for pessimistic locking.
 */
@Service
public class TableRequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(TableRequestProcessor.class);

    private final List<TableRequestHandler> requestHandlers = new ArrayList<TableRequestHandler>();

    @Autowired
    public TableRequestProcessor(@Qualifier("tableRequestHandler") final Collection<TableRequestHandler> handlers) {
        notNull(handlers, "Handlers may not be null");

        requestHandlers.addAll(handlers);
    }

    @EventTemplate
    public TableRequestWrapper eventTemplate() {
        return new TableRequestWrapper();
    }

    @SuppressWarnings("unchecked")
    @SpaceDataEvent
    public void process(final TableRequestWrapper request) {
        if (request == null) {
            return;
        }

        if (request.getRequestType() == null) {
            LOG.error("Invalid request, no type specified: {}", request);
            return;
        }

        try {
            for (final TableRequestHandler requestHandler : requestHandlers) {
                if (requestHandler.accepts(request.getRequestType())) {
                    requestHandler.handle(request.getTableRequest());
                    return;
                }
            }

            LOG.error("Unknown table request type received: {}: request is {}",
                    request.getRequestType(), request.getTableRequest());

        } catch (final Throwable e) {
            LOG.error("Uncaught handler error", e);
        }
    }

}
