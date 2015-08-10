package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.PlayerEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresPlayerDWDAO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
@Qualifier("playerEventConsumer")
public class PlayerEventConsumer implements QueueMessageConsumer<PlayerEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerEventConsumer.class);

    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresPlayerDWDAO postgresPlayerDWDAO;

    private final ThreadLocal<Map<BigDecimal, PlayerEvent>> batchedEvents = new ThreadLocal<Map<BigDecimal, PlayerEvent>>() {
        @Override
        protected Map<BigDecimal, PlayerEvent> initialValue() {
            return new HashMap<>();
        }
    };

    @Autowired
    public PlayerEventConsumer(final YazinoConfiguration yazinoConfiguration,
                               final PostgresPlayerDWDAO postgresPlayerDWDAO) {
        this.yazinoConfiguration = yazinoConfiguration;
        this.postgresPlayerDWDAO = postgresPlayerDWDAO;
    }

    @Override
    public void handle(final PlayerEvent message) {
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            batchedEvents.get().put(message.getPlayerId(), message);
        }
    }

    @Override
    public void consumerCommitting() {
        final Map<BigDecimal, PlayerEvent> eventsForThread = batchedEvents.get();
        if (!eventsForThread.isEmpty()) {
            LOG.debug("Committing {} queued events", eventsForThread.size());

            postgresPlayerDWDAO.saveAll(new ArrayList<PlayerEvent>(eventsForThread.values()));
            eventsForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }
}
