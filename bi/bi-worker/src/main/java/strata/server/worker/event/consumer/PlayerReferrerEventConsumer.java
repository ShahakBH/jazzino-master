package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.PlayerReferrerEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresPlayerReferrerDWDAO;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.Validate.notNull;

@Component
@Qualifier("playerReferrerEventConsumer")
public class PlayerReferrerEventConsumer implements QueueMessageConsumer<PlayerReferrerEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerReferrerEventConsumer.class);

    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresPlayerReferrerDWDAO postgresPlayerReferrerDWDAO;

    private final ThreadLocal<Map<BigDecimal, PlayerReferrerEvent>> batchedEvents = new ThreadLocal<Map<BigDecimal, PlayerReferrerEvent>>() {
        @Override
        protected Map<BigDecimal, PlayerReferrerEvent> initialValue() {
            return new HashMap<>();
        }
    };

    @Autowired
    public PlayerReferrerEventConsumer(final YazinoConfiguration yazinoConfiguration,
                                       final PostgresPlayerReferrerDWDAO postgresPlayerReferrerDWDAO) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(postgresPlayerReferrerDWDAO, "postgresPlayerReferrerDWDAO may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.postgresPlayerReferrerDWDAO = postgresPlayerReferrerDWDAO;
    }

    @Override
    public void handle(final PlayerReferrerEvent message) {
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            addToBatch(message);
        }
    }

    private void addToBatch(final PlayerReferrerEvent message) {
        final PlayerReferrerEvent existingEvent = batchedEvents.get().get(message.getPlayerId());
        if (existingEvent == null) {
            batchedEvents.get().put(message.getPlayerId(), message);
        } else {
            if (message.getGameType() != null) {
                existingEvent.setGameType(message.getGameType());
            }
            if (message.getPlatform() != null) {
                existingEvent.setPlatform(message.getPlatform());
            }
            if (StringUtils.isBlank(existingEvent.getRef())
                    || StringUtils.isNotBlank(message.getRef()) && !"INVITE".equals(message.getRef())) {
                existingEvent.setRef(message.getRef());
            }
        }
    }

    @Override
    public void consumerCommitting() {
        final Map<BigDecimal, PlayerReferrerEvent> eventsForThread = batchedEvents.get();
        if (!eventsForThread.isEmpty()) {
            LOG.debug("Committing {} queued events", eventsForThread.size());

            postgresPlayerReferrerDWDAO.save(newHashSet(eventsForThread.values()));
            eventsForThread.clear();
        }
    }
}
