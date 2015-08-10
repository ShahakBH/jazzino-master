package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.PlayerProfileEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.consumer.crm.CRMRegistrar;
import strata.server.worker.event.persistence.PostgresPlayerProfileDWDAO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
@Qualifier("playerProfileEventConsumer")
public class PlayerProfileEventConsumer implements QueueMessageConsumer<PlayerProfileEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerProfileEventConsumer.class);

    private final ThreadLocal<Map<BigDecimal, PlayerProfileEvent>> batchedEvents = new ThreadLocal<Map<BigDecimal, PlayerProfileEvent>>() {
        @Override
        protected Map<BigDecimal, PlayerProfileEvent> initialValue() {
            return new HashMap<>();
        }
    };

    private final PostgresPlayerProfileDWDAO postgresProfileDWDAO;
    private final YazinoConfiguration yazinoConfiguration;
    private final CRMRegistrar crmRegistrar;

    @Autowired
    public PlayerProfileEventConsumer(final PostgresPlayerProfileDWDAO postgresProfileDWDAO,
                                      final YazinoConfiguration yazinoConfiguration,
                                      final CRMRegistrar crmRegistrar) {
        this.postgresProfileDWDAO = postgresProfileDWDAO;
        this.yazinoConfiguration = yazinoConfiguration;
        this.crmRegistrar = crmRegistrar;
    }

    @Override
    public void handle(final PlayerProfileEvent message) {
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            batchedEvents.get().put(message.getPlayerId(), message);
        }

        try {
            if (message.isNewPlayer()) {
                crmRegistrar.register(message);
            }
        } catch (Exception e) {
            LOG.error("Unable to register new player with CRM.", e);
        }
    }

    @Override
    public void consumerCommitting() {
        final Map<BigDecimal, PlayerProfileEvent> eventsForThread = batchedEvents.get();
        if (!eventsForThread.isEmpty()) {
            LOG.debug("Committing {} queued events", eventsForThread.size());

            postgresProfileDWDAO.saveAll(new ArrayList<PlayerProfileEvent>(eventsForThread.values()));
            eventsForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }


}
