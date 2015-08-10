package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.GiftCollectedEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresGiftCollectedEventDao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
@Qualifier("giftCollectedEventConsumer")
public class GiftCollectedEventConsumer implements QueueMessageConsumer<GiftCollectedEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(GiftCollectedEventConsumer.class);

    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresGiftCollectedEventDao postgresGiftCollectedEventDao;

    private final ThreadLocal<Map<BigDecimal, GiftCollectedEvent>> batchedEvents = new ThreadLocal<Map<BigDecimal, GiftCollectedEvent>>() {
        @Override
        protected Map<BigDecimal, GiftCollectedEvent> initialValue() {
            return new HashMap<>();
        }
    };

    @Autowired
    public GiftCollectedEventConsumer(final YazinoConfiguration yazinoConfiguration,
                                      final PostgresGiftCollectedEventDao postgresGiftCollectedEventDao) {
        this.yazinoConfiguration = yazinoConfiguration;
        this.postgresGiftCollectedEventDao = postgresGiftCollectedEventDao;
    }

    @Override
    public void handle(final GiftCollectedEvent message) {
        LOG.debug("consumer received GiftCollectedEvent message");
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            batchedEvents.get().put(message.getGiftId(), message);
        }
    }

    @Override
    public void consumerCommitting() {
        final Map<BigDecimal, GiftCollectedEvent> eventsForThread = batchedEvents.get();
        if (!eventsForThread.isEmpty()) {
            LOG.debug("Committing {} queued events", eventsForThread.size());

            postgresGiftCollectedEventDao.saveAll(new ArrayList<GiftCollectedEvent>(eventsForThread.values()));
            eventsForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }


    }
}
