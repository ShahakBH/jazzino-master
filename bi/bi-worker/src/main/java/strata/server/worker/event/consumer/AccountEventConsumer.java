package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.AccountEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresAccountDWDAO;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.Validate.notNull;

@Component
@Qualifier("accountEventConsumer")
public class AccountEventConsumer implements QueueMessageConsumer<AccountEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(AccountEventConsumer.class);

    private final ThreadLocal<Map<BigDecimal, AccountEvent>> batchedEvents = new ThreadLocal<Map<BigDecimal, AccountEvent>>() {
        @Override
        protected Map<BigDecimal, AccountEvent> initialValue() {
            return new HashMap<>();
        }
    };

    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresAccountDWDAO postgresAccountDWDAO;

    @Autowired
    public AccountEventConsumer(final YazinoConfiguration yazinoConfiguration,
                                final PostgresAccountDWDAO postgresAccountDWDAO) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(postgresAccountDWDAO, "postgresAccountDWDAO may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.postgresAccountDWDAO = postgresAccountDWDAO;
    }

    @Override
    public void handle(final AccountEvent message) {
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            batchedEvents.get().put(message.getAccountId(), message);
        }
    }

    @Override
    public void consumerCommitting() {
        final Map<BigDecimal, AccountEvent> eventsForThread = batchedEvents.get();
        if (!eventsForThread.isEmpty()) {
            LOG.debug("Committing {} queued events", eventsForThread.size());

            postgresAccountDWDAO.saveAll(newArrayList(eventsForThread.values()));
            eventsForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }
}
