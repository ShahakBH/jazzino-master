package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.PlayerLevelEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresPlayerLevelDWDAO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
@Qualifier("playerLevelEventConsumer")
public class PlayerLevelEventConsumer implements QueueMessageConsumer<PlayerLevelEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerLevelEventConsumer.class);

    private final ThreadLocal<Map<String, PlayerLevelEvent>> batchedEvents = new ThreadLocal<Map<String, PlayerLevelEvent>>() {
        @Override
        protected Map<String, PlayerLevelEvent> initialValue() {
            return new HashMap<>();
        }
    };
    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresPlayerLevelDWDAO postgresPlayerLevelDWDAO;

    @Autowired
    public PlayerLevelEventConsumer(final YazinoConfiguration yazinoConfiguration,
                                    final PostgresPlayerLevelDWDAO postgresPlayerLevelDWDAO) {
        this.yazinoConfiguration = yazinoConfiguration;
        this.postgresPlayerLevelDWDAO = postgresPlayerLevelDWDAO;
    }

    @Override
    public void handle(final PlayerLevelEvent message) {
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            batchedEvents.get().put(message.getPlayerId().concat(message.getGameType()), message);
        }
    }

    @Override
    public void consumerCommitting() {
        final Map<String, PlayerLevelEvent> eventsForThread = batchedEvents.get();
        if (!eventsForThread.isEmpty()) {
            LOG.debug("Committing {} queued events", eventsForThread.size());

            postgresPlayerLevelDWDAO.saveAll(new ArrayList<PlayerLevelEvent>(eventsForThread.values()));
            eventsForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }
}
