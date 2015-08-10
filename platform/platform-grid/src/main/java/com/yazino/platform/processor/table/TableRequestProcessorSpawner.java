package com.yazino.platform.processor.table;

import com.yazino.platform.model.table.TableRequestWrapper;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.openspaces.core.GigaSpace;
import org.openspaces.events.polling.SimplePollingContainerConfigurer;
import org.openspaces.events.polling.SimplePollingEventListenerContainer;
import org.openspaces.events.polling.receive.SingleTakeReceiveOperationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class TableRequestProcessorSpawner {
    private static final Logger LOG = LoggerFactory.getLogger(TableRequestProcessorSpawner.class);

    private final GigaSpace gigaSpace;
    private final TableRequestProcessor tableRequestProcessor;
    private final List<SimplePollingEventListenerContainer> containers = new ArrayList<SimplePollingEventListenerContainer>();

    private final int numberOfProcessors;

    @Autowired
    public TableRequestProcessorSpawner(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                        final TableRequestProcessor tableRequestProcessor) {
        notNull(gigaSpace, "gigaSpace may not be null");
        notNull(tableRequestProcessor, "tableRequestProcessor may not be null");

        this.gigaSpace = gigaSpace;
        this.tableRequestProcessor = tableRequestProcessor;

        this.numberOfProcessors = TableRequestWrapper.ROUTING_MODULUS;
    }

    @PostConstruct
    public void createContainers() throws Exception {
        for (int i = 0; i < numberOfProcessors; i++) {
            containers.add(createAndStartContainer(i));
        }
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

    private SimplePollingEventListenerContainer createAndStartContainer(final int selector) {
        LOG.info("Creating container with selector: {}", selector);

        final TableRequestWrapper template = new TableRequestWrapper(selector);

        final SimplePollingEventListenerContainer pollingEventListenerContainer
                = new SimplePollingContainerConfigurer(gigaSpace)
                .template(template)
                .eventListenerAnnotation(tableRequestProcessor)
                .receiveOperationHandler(new SingleTakeReceiveOperationHandler())
                .name("processor-" + selector)
                .concurrentConsumers(1)
                .maxConcurrentConsumers(1)
                .pollingContainer();

        pollingEventListenerContainer.start();
        return pollingEventListenerContainer;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final TableRequestProcessorSpawner rhs = (TableRequestProcessorSpawner) obj;
        return new EqualsBuilder()
                .append(tableRequestProcessor, rhs.tableRequestProcessor)
                .append(numberOfProcessors, rhs.numberOfProcessors)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 19)
                .append(tableRequestProcessor)
                .append(numberOfProcessors)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(tableRequestProcessor)
                .append(numberOfProcessors)
                .toString();
    }

}
