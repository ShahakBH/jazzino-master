package strata.server.worker.audit.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.audit.message.Transaction;
import com.yazino.platform.audit.message.TransactionProcessedMessage;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import strata.server.worker.audit.persistence.PostgresTransactionLogDAO;
import strata.server.worker.audit.playedtracking.PlayerPlayedTracking;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Component
public class TransactionProcessedMessageConsumer implements QueueMessageConsumer<TransactionProcessedMessage>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionProcessedMessageConsumer.class);

    private final ThreadLocal<List<Transaction>> batchedMessages = new ThreadLocal<List<Transaction>>() {
        @Override
        protected List<Transaction> initialValue() {
            return new ArrayList<>();
        }
    };

    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresTransactionLogDAO externalTransactionLogDAO;
    private final PlayerPlayedTracking playerPlayedTracking;

    @Autowired
    public TransactionProcessedMessageConsumer(final YazinoConfiguration yazinoConfiguration,
                                               final PostgresTransactionLogDAO externalTransactionLogDAO,
                                               final PlayerPlayedTracking playerPlayedTracking) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(externalTransactionLogDAO, "externalTransactionLogDAO may not be null");
        notNull(playerPlayedTracking, "playerPlayedTracking may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.externalTransactionLogDAO = externalTransactionLogDAO;
        this.playerPlayedTracking = playerPlayedTracking;
    }

    @Override
    public void handle(final TransactionProcessedMessage message) {
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            LOG.debug("Writing transaction to external datawarehouse");
            if (message.getTransactions() != null) {
                batchedMessages.get().addAll(message.getTransactions());
            }
        }

        try {
            playerPlayedTracking.track(message.getTransactions());

        } catch (Exception e) {
            LOG.error("Could not track transactions from message {}", message, e);
        }
    }

    @Override
    public void consumerCommitting() {
        final List<Transaction> messagesForThread = batchedMessages.get();
        if (!messagesForThread.isEmpty()) {
            LOG.debug("Committing {} queued messages", messagesForThread.size());

            externalTransactionLogDAO.saveAll(new ArrayList<Transaction>(messagesForThread));
            messagesForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }

}
