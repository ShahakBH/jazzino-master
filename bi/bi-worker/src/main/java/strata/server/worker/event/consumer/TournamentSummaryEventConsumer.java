package strata.server.worker.event.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.event.message.TournamentSummaryEvent;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresTournamentDWDAO;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.Validate.notNull;

@Component
@Qualifier("tournamentSummaryEventConsumer")
public class TournamentSummaryEventConsumer implements QueueMessageConsumer<TournamentSummaryEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(TournamentSummaryEventConsumer.class);

    private final ThreadLocal<Set<TournamentSummaryEvent>> batchedEvents = new ThreadLocal<Set<TournamentSummaryEvent>>() {
        @Override
        protected Set<TournamentSummaryEvent> initialValue() {
            return new HashSet<>();
        }
    };

    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresTournamentDWDAO externalTournamentDAO;

    @Autowired
    public TournamentSummaryEventConsumer(final YazinoConfiguration yazinoConfiguration,
                                          final PostgresTournamentDWDAO externalTournamentDAO) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(externalTournamentDAO, "externalTournamentDAO may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.externalTournamentDAO = externalTournamentDAO;
    }

    @Override
    public void handle(final TournamentSummaryEvent message) {
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            batchedEvents.get().add(message);
        }
    }

    @Override
    public void consumerCommitting() {
        final Set<TournamentSummaryEvent> eventsForThread = batchedEvents.get();
        if (!eventsForThread.isEmpty()) {
            LOG.debug("Committing {} queued events", eventsForThread.size());

            externalTournamentDAO.save(newHashSet(eventsForThread));
            eventsForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }
}
