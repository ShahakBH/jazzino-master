package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.InvitationEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresInvitationDWDAO;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.Validate.notNull;

@Component
@Qualifier("invitationEventConsumer")
public class InvitationEventConsumer implements QueueMessageConsumer<InvitationEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(InvitationEventConsumer.class);

    private final ThreadLocal<Map<String, InvitationEvent>> batchedEvents = new ThreadLocal<Map<String, InvitationEvent>>() {
        @Override
        protected Map<String, InvitationEvent> initialValue() {
            return new HashMap<>();
        }
    };

    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresInvitationDWDAO externalInvitationDao;

    @Autowired
    public InvitationEventConsumer(final YazinoConfiguration yazinoConfiguration,
                                   final PostgresInvitationDWDAO externalInvitationDao) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(externalInvitationDao, "externalInvitationDao may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.externalInvitationDao = externalInvitationDao;
    }

    @Override
    public void handle(final InvitationEvent message) {
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            batchedEvents.get().put(keyFor(message), message);
        }
    }

    @Override
    public void consumerCommitting() {
        final Map<String, InvitationEvent> eventsForThread = batchedEvents.get();
        if (!eventsForThread.isEmpty()) {
            LOG.debug("Committing {} queued events", eventsForThread.size());

            externalInvitationDao.save(newHashSet(eventsForThread.values()));
            eventsForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }

    private String keyFor(final InvitationEvent message) {
        return String.format("%s:%s:%s", message.getIssuingPlayerId(), message.getRecipientIdentifier(), message.getSource());
    }
}
