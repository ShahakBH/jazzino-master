package com.yazino.platform.processor;

import org.openspaces.core.GigaSpace;
import org.openspaces.events.polling.SimplePollingContainerConfigurer;
import org.openspaces.events.polling.SimplePollingEventListenerContainer;
import org.openspaces.events.polling.receive.SingleTakeReceiveOperationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class PersisterContainerSpawner {
    private static final Logger LOG = LoggerFactory.getLogger(PersisterContainerSpawner.class);

    private final List<SimplePollingEventListenerContainer> containers = new ArrayList<SimplePollingEventListenerContainer>();
    private final Collection<Persister<?>> persisters;
    private final GigaSpace gigaSpace;

    private boolean containersCreated;

    @Autowired
    public PersisterContainerSpawner(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                     @Qualifier("persister") final Collection<Persister<?>> persisters) {
        notNull(gigaSpace, "gigaSpace may not be null");
        notNull(persisters, "persisters may not be null");

        this.gigaSpace = gigaSpace;
        this.persisters = persisters;
    }

    public <T> void createContainers() {
        if (!containersCreated) {
            for (Persister<?> persister : persisters) {
                try {
                    for (int i = 0; i < PersistenceRequest.ROUTING_MODULUS; i++) {
                        containers.add(createAndStartContainer(persister, i));
                    }

                } catch (Exception e) {
                    LOG.error("Failed to create persistence processors for persister {}", persister);
                }
            }
        }

        containersCreated = true;
    }

    @PreDestroy
    public void destroyContainers() throws Exception {
        for (SimplePollingEventListenerContainer container : containers) {
            try {
                container.shutdown();

            } catch (Exception e) {
                LOG.error("Failed to shutdown container {}", container, e);
            }
        }
        containers.clear();
    }

    private <T> SimplePollingEventListenerContainer createAndStartContainer(final Persister<T> persister,
                                                                            final int selector)
            throws IllegalAccessException, InstantiationException {
        LOG.info("Creating container for {} with selector: {}", persister, selector);

        final PersistenceRequest<T> templateWithSelector = persister.getPersistenceRequestClass().newInstance();
        templateWithSelector.setSelector(selector);
        templateWithSelector.setStatus(PersistenceRequest.Status.PENDING);

        final SimplePollingEventListenerContainer pollingEventListenerContainer
                = new SimplePollingContainerConfigurer(gigaSpace)
                .template(templateWithSelector)
                .eventListenerMethod(persister, "persist")
                .receiveOperationHandler(new SingleTakeReceiveOperationHandler())
                .name(String.format("%s-processor-%s", persister.getClass().getSimpleName(), selector))
                .concurrentConsumers(1)
                .maxConcurrentConsumers(1)
                .pollingContainer();

        pollingEventListenerContainer.start();

        return pollingEventListenerContainer;
    }
}

