package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.GiftSentEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresGiftSentEventDao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
@Qualifier("giftSentEventConsumer")
public class GiftSentEventConsumer implements QueueMessageConsumer<GiftSentEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(GiftSentEventConsumer.class);

    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresGiftSentEventDao postgresGiftSentEventDao;

    private final ThreadLocal<Map<BigDecimal, GiftSentEvent>> batchedEvents = new ThreadLocal<Map<BigDecimal, GiftSentEvent>>() {
        @Override
        protected Map<BigDecimal, GiftSentEvent> initialValue() {
            return new HashMap<>();
        }
    };

    @Autowired
    public GiftSentEventConsumer(final YazinoConfiguration yazinoConfiguration,
                                 final PostgresGiftSentEventDao postgresGiftSentEventDao) {
        this.yazinoConfiguration = yazinoConfiguration;
        this.postgresGiftSentEventDao = postgresGiftSentEventDao;
    }

    @Override
    public void handle(final GiftSentEvent message) {
        LOG.debug("consumer received GiftSentEvent message");
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            LOG.debug("rec'd GSE: ID:{} Exp:{} Now:{} Type:{}",
                    message.getGiftId(),
                    message.getExpiry(),
                    message.getNow(),
                    message.getMessageType());
            batchedEvents.get().put(message.getGiftId(), message);
        }
    }

    @Override
    public void consumerCommitting() {
        final Map<BigDecimal, GiftSentEvent> eventsForThread = batchedEvents.get();
        if (!eventsForThread.isEmpty()) {
            LOG.debug("Committing {} queued events", eventsForThread.size());

            postgresGiftSentEventDao.saveAll(new ArrayList<>(eventsForThread.values()));
            eventsForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }


    }
}
