package strata.server.worker.event.consumer;


import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.LeaderboardEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresLeaderboardDWDAO;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.Validate.notNull;

@Component
@Qualifier("leaderboardEventConsumer")
public class LeaderboardEventConsumer implements QueueMessageConsumer<LeaderboardEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(LeaderboardEventConsumer.class);

    private final ThreadLocal<Map<BigDecimal, LeaderboardEvent>> batchedEvents = new ThreadLocal<Map<BigDecimal, LeaderboardEvent>>() {
        @Override
        protected Map<BigDecimal, LeaderboardEvent> initialValue() {
            return new HashMap<>();
        }
    };

    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresLeaderboardDWDAO postgresLeaderboardDWDAO;

    @Autowired
    public LeaderboardEventConsumer(final YazinoConfiguration yazinoConfiguration,
                                    final PostgresLeaderboardDWDAO postgresLeaderboardDWDAO) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(postgresLeaderboardDWDAO, "postgresLeaderboardDWDAO may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.postgresLeaderboardDWDAO = postgresLeaderboardDWDAO;
    }

    @Override
    public void handle(final LeaderboardEvent message) {
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            batchedEvents.get().put(message.getLeaderboardId(), message);
        }
    }

    @Override
    public void consumerCommitting() {
        final Map<BigDecimal, LeaderboardEvent> eventsForThread = batchedEvents.get();
        if (!eventsForThread.isEmpty()) {
            LOG.debug("Committing {} queued events", eventsForThread.size());

            postgresLeaderboardDWDAO.save(newHashSet(eventsForThread.values()));
            eventsForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }
}
