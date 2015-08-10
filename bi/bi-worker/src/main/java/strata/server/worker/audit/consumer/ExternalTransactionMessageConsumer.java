package strata.server.worker.audit.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.audit.message.ExternalTransaction;
import com.yazino.platform.audit.message.ExternalTransactionMessage;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import strata.server.worker.audit.persistence.PostgresExternalTransactionDAO;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Component("externalTransactionMessageConsumer")
public class ExternalTransactionMessageConsumer implements QueueMessageConsumer<ExternalTransactionMessage>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalTransactionMessageConsumer.class);

    private final ThreadLocal<List<ExternalTransaction>> batchedMessages = new ThreadLocal<List<ExternalTransaction>>() {
        @Override
        protected List<ExternalTransaction> initialValue() {
            return new ArrayList<>();
        }
    };

    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresExternalTransactionDAO externalExternalTransactionDAO;

    ExternalTransactionMessageConsumer() {
        // CGLib constructor
        externalExternalTransactionDAO = null;
        yazinoConfiguration = null;
    }

    @Autowired
    public ExternalTransactionMessageConsumer(final YazinoConfiguration yazinoConfiguration,
                                              final PostgresExternalTransactionDAO externalExternalTransactionDAO) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(externalExternalTransactionDAO, "externalExternalTransactionDAO may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.externalExternalTransactionDAO = externalExternalTransactionDAO;
    }

    @Transactional
    @Override
    public void handle(final ExternalTransactionMessage message) {
        verifyInitialisation();

        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            batchedMessages.get().add(message);
        }
    }

    @Override
    public void consumerCommitting() {
        final List<ExternalTransaction> messagesForThread = batchedMessages.get();
        if (!messagesForThread.isEmpty()) {
            LOG.debug("Committing {} queued messages", messagesForThread.size());

            externalExternalTransactionDAO.saveAll(new ArrayList<ExternalTransaction>(messagesForThread));
            messagesForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }

    private void verifyInitialisation() {
        if (externalExternalTransactionDAO == null
                || yazinoConfiguration == null) {
            throw new IllegalStateException("Class was created with the CGLib constructor");
        }
    }
}
