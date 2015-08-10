package com.yazino.platform.processor.external;

import com.yazino.game.api.ExternalCallResult;
import com.yazino.platform.gamehost.external.NovomaticRequest;
import com.yazino.platform.model.table.ExternalCallResultWrapper;
import com.yazino.platform.repository.table.TableRepository;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
public class NovomaticRequestProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(NovomaticRequestProcessor.class);
    private TableRepository tableRepository;
    private NovomaticRequestExecutor novomaticRequestExecutor;

    //CGLib constructor
    public NovomaticRequestProcessor() {
    }

    @Autowired
    public NovomaticRequestProcessor(TableRepository tableRepository, NovomaticRequestExecutor novomaticRequestExecutor) {
        this.tableRepository = tableRepository;
        this.novomaticRequestExecutor = novomaticRequestExecutor;
    }

    @EventTemplate
    public NovomaticRequest template() {
        return new NovomaticRequest();
    }

    @SpaceDataEvent
    public void process(final NovomaticRequest request) {
        LOG.debug("Processing {}", request);
        ExternalCallResult externalCallResult = novomaticRequestExecutor.execute(request);
        tableRepository.sendRequest(new ExternalCallResultWrapper(request.getTableId(), externalCallResult));
    }

}


