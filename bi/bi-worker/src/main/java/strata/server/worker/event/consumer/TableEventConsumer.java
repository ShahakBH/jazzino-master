package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.TableEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresTableDWDAO;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.Validate.notNull;

@Component
@Qualifier("tableEventConsumer")
public class TableEventConsumer implements QueueMessageConsumer<TableEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(TableEventConsumer.class);

    private final ThreadLocal<Set<TableEvent>> batchedEvents = new ThreadLocal<Set<TableEvent>>() {
        @Override
        protected Set<TableEvent> initialValue() {
            return new HashSet<>();
        }
    };

    private final YazinoConfiguration configuration;
    private final PostgresTableDWDAO externalTableDWDAO;

    @Autowired
    public TableEventConsumer(final YazinoConfiguration configuration,
                              final PostgresTableDWDAO externalTableDWDAO) {
        notNull(configuration, "configuration may not be null");
        notNull(externalTableDWDAO, "externalTableDWDAO may not be null");

        this.configuration = configuration;
        this.externalTableDWDAO = externalTableDWDAO;
    }

    @Override
    public void handle(final TableEvent message) {
        if (configuration.getBoolean("data-warehouse.write.enabled")) {
            batchedEvents.get().add(message);
        }
    }

    @Override
    public void consumerCommitting() {
        final Set<TableEvent> eventsForThread = batchedEvents.get();
        if (!eventsForThread.isEmpty()) {
            LOG.debug("Committing {} queued events", eventsForThread.size());

            externalTableDWDAO.saveAll(newArrayList(eventsForThread));
            eventsForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }
}
