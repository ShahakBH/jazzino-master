package strata.server.worker.event.consumer;


import com.yazino.bi.messaging.CommitAware;
import com.yazino.client.log.ClientLogEvent;
import com.yazino.client.log.ClientLogEventMessageType;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.event.persistence.PostgresAnalyticsLogDWDAO;
import strata.server.worker.event.persistence.PostgresClientLogDWDAO;

import java.util.ArrayList;
import java.util.List;

import static com.yazino.client.log.ClientLogEventMessageType.*;

@Component
@Qualifier("clientLogEventConsumer")
public class ClientLogEventConsumer implements QueueMessageConsumer<ClientLogEvent>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(ClientLogEventConsumer.class);

    private final PostgresClientLogDWDAO clientLogDWDAO;
    private final PostgresAnalyticsLogDWDAO analyticsLogDWDAO;

    private final ThreadLocal<List<ClientLogEvent>> batchedLogMessages = new ThreadLocal<List<ClientLogEvent>>() {
        @Override
        protected List<ClientLogEvent> initialValue() {
            return new ArrayList<>();
        }
    };

    private final ThreadLocal<List<ClientLogEvent>> batchedAnalyticMessages = new ThreadLocal<List<ClientLogEvent>>() {
        @Override
        protected List<ClientLogEvent> initialValue() {
            return new ArrayList<>();
        }
    };

    @Autowired
    public ClientLogEventConsumer(PostgresClientLogDWDAO clientLogDWDAO, final PostgresAnalyticsLogDWDAO analyticsLogDWDAO) {
        this.clientLogDWDAO = clientLogDWDAO;
        this.analyticsLogDWDAO = analyticsLogDWDAO;
    }

    @Override
    public void handle(ClientLogEvent clientLogEvent) {
        LOG.debug("Received event {}", clientLogEvent);

        try {
            final ClientLogEventMessageType messageType = valueOf(clientLogEvent.getMessageType());
            switch (messageType)    {
                case LOG_EVENT:
                    batchedLogMessages.get().add(clientLogEvent);
                    break;
                case LOG_ANALYTICS:
                    batchedAnalyticMessages.get().add(clientLogEvent);
                    break;
                default:
                    LOG.error("client log message type not implemented: {}", messageType.name());
            }
        } catch (Exception e)   {
            LOG.error("invalid messageType {}", clientLogEvent.getMessageType());
        }

    }

    @Override
    public void consumerCommitting() {
        saveAndClearLogMessages();
        saveAndClearAnalyticMessages();
    }

    private void saveAndClearLogMessages() {
        final List<ClientLogEvent> logMessagesForThread = batchedLogMessages.get();
        if (!logMessagesForThread.isEmpty()) {
            LOG.debug("Committing {} queued messages", logMessagesForThread.size());
            clientLogDWDAO.saveAll(new ArrayList<ClientLogEvent>(logMessagesForThread));
            logMessagesForThread.clear();
        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }

    private void saveAndClearAnalyticMessages() {
        final List<ClientLogEvent> analyticMessagesForThread = batchedAnalyticMessages.get();
        if (!analyticMessagesForThread.isEmpty()) {
            LOG.debug("Committing {} queued messages", analyticMessagesForThread.size());
            analyticsLogDWDAO.saveAll(new ArrayList<ClientLogEvent>(analyticMessagesForThread));
            analyticMessagesForThread.clear();
        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }
}
