package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.EmailValidationEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresEmailValidationDWDAO;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.Validate.notNull;

@Component
@Qualifier("emailValidationEventConsumer")
public class EmailValidationEventConsumer implements QueueMessageConsumer<EmailValidationEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(EmailValidationEventConsumer.class);

    private final ThreadLocal<Map<String, EmailValidationEvent>> batchedEvents = new ThreadLocal<Map<String, EmailValidationEvent>>() {
        @Override
        protected Map<String, EmailValidationEvent> initialValue() {
            return new HashMap<>();
        }
    };

    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresEmailValidationDWDAO postgresEmailValidationDWDAO;

    @Autowired
    public EmailValidationEventConsumer(final YazinoConfiguration yazinoConfiguration,
                                        final PostgresEmailValidationDWDAO postgresEmailValidationDWDAO) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(postgresEmailValidationDWDAO, "postgresEmailValidationDWDAO may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.postgresEmailValidationDWDAO = postgresEmailValidationDWDAO;
    }

    @Override
    public void handle(final EmailValidationEvent message) {
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            batchedEvents.get().put(message.getEmailAddress(), message);
        }
    }

    @Override
    public void consumerCommitting() {
        final Map<String, EmailValidationEvent> eventsForThread = batchedEvents.get();
        if (!eventsForThread.isEmpty()) {
            LOG.debug("Committing {} queued events", eventsForThread.size());

            postgresEmailValidationDWDAO.saveAll(newArrayList(eventsForThread.values()));
            eventsForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }
}
