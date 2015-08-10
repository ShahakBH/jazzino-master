package strata.server.worker.audit.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.audit.message.GameAudit;
import com.yazino.platform.audit.message.GameAuditMessage;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import strata.server.worker.audit.persistence.PostgresGameAuditDAO;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Component
public class GameAuditMessageConsumer implements QueueMessageConsumer<GameAuditMessage>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(GameAuditMessageConsumer.class);

    private final ThreadLocal<List<GameAudit>> batchedMessages = new ThreadLocal<List<GameAudit>>() {
        @Override
        protected List<GameAudit> initialValue() {
            return new ArrayList<>();
        }
    };

    private final YazinoConfiguration yazinoConfiguration;
    private final PostgresGameAuditDAO externalGameAuditDAO;

    @Autowired
    public GameAuditMessageConsumer(final YazinoConfiguration yazinoConfiguration,
                                    final PostgresGameAuditDAO externalGameAuditDAO) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(externalGameAuditDAO, "externalGameAuditDAO may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
        this.externalGameAuditDAO = externalGameAuditDAO;
    }

    @Override
    public void handle(final GameAuditMessage message) {
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            LOG.debug("Writing external transaction to external datawarehouse");
            batchedMessages.get().add(message);
        }
    }

    @Override
    public void consumerCommitting() {
        final List<GameAudit> messagesForThread = batchedMessages.get();
        if (!messagesForThread.isEmpty()) {
            LOG.debug("Committing {} queued messages", messagesForThread.size());

            externalGameAuditDAO.saveAll(new ArrayList<GameAudit>(messagesForThread));
            messagesForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }
}
