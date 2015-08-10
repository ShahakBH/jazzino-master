package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.BonusCollectedEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresBonusCollectedEventDao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
@Qualifier("bonusCollectedEventConsumer")
public class BonusCollectedEventConsumer implements QueueMessageConsumer<BonusCollectedEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(BonusCollectedEventConsumer.class);

    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresBonusCollectedEventDao postgresBonusCollectedEventDao;

    private final ThreadLocal<Map<BigDecimal, BonusCollectedEvent>> batchedEvents = new ThreadLocal<Map<BigDecimal, BonusCollectedEvent>>() {
        @Override
        protected Map<BigDecimal, BonusCollectedEvent> initialValue() {
            return new HashMap<>();
        }
    };

    @Autowired
    public BonusCollectedEventConsumer(final YazinoConfiguration yazinoConfiguration,
                                       final PostgresBonusCollectedEventDao postgresBonusCollectedEventDao) {
        this.yazinoConfiguration = yazinoConfiguration;
        this.postgresBonusCollectedEventDao = postgresBonusCollectedEventDao;
    }

    @Override
    public void handle(final BonusCollectedEvent message) {
        LOG.debug("consumer received GiftCollectedEvent message");
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            batchedEvents.get().put(message.getPlayerId(), message);
        }
    }

    @Override
    public void consumerCommitting() {
        final Map<BigDecimal, BonusCollectedEvent> eventsForThread = batchedEvents.get();
        if (!eventsForThread.isEmpty()) {
            LOG.debug("Committing {} queued events", eventsForThread.size());

            postgresBonusCollectedEventDao.saveAll(new ArrayList<BonusCollectedEvent>(eventsForThread.values()));
            eventsForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }


    }
}
