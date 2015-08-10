package com.yazino.host;

import com.yazino.platform.processor.table.handler.TableRequestHandler;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yazino.platform.model.table.TableRequestWrapper;

import javax.annotation.Resource;
import java.util.List;

@Component
public class TableRequestProcessor implements InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(TableRequestProcessor.class);
    private final TableRequestWrapperQueue requestQueue;

    @Autowired(required = true)
    @Resource(name = "tableRequestHandlers")
    private List<TableRequestHandler> requestHandlers;

    @Autowired
    public TableRequestProcessor(final TableRequestWrapperQueue requestQueue) {
        this.requestQueue = requestQueue;
    }

    @SuppressWarnings("unchecked")
    @SpaceDataEvent
    public void process(final TableRequestWrapper request) {
        if (request == null) {
            return;
        }

        if (request.getRequestType() == null) {
            LOG.error("Invalid request, no type specified: " + request);
            return;
        }

        try {
            for (final TableRequestHandler requestHandler : requestHandlers) {
                if (requestHandler.accepts(request.getRequestType())) {
                    requestHandler.handle(request.getTableRequest());
                    return;
                }
            }

            LOG.error("Unknown table request type received: " + request.getRequestType()
                    + ": request is " + request.getTableRequest());

        } catch (final Throwable e) {
            LOG.error("Uncaught handler error", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    final TableRequestWrapper request = requestQueue.getNextRequest();
                    process(request);
                }
            }
        }).start();

    }
}
