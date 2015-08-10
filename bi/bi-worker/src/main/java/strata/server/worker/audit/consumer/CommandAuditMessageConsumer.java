package strata.server.worker.audit.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.audit.message.CommandAudit;
import com.yazino.platform.audit.message.CommandAuditMessage;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import strata.server.worker.audit.persistence.PostgresCommandAuditDAO;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang3.Validate.notNull;

@Component
public class CommandAuditMessageConsumer implements QueueMessageConsumer<CommandAuditMessage>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(CommandAuditMessageConsumer.class);

    private final ThreadLocal<List<CommandAudit>> batchedMessages = new ThreadLocal<List<CommandAudit>>() {
        @Override
        protected List<CommandAudit> initialValue() {
            return new ArrayList<>();
        }
    };

    private final YazinoConfiguration yazinoConfiguration;
    private PostgresCommandAuditDAO postgresCommandAuditDao;

    @Autowired
    public CommandAuditMessageConsumer(final YazinoConfiguration yazinoConfiguration,
                                       final PostgresCommandAuditDAO postgresCommandAuditDao) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");
        notNull(postgresCommandAuditDao, "postgresCommandAuditDAO may not be null");

        this.postgresCommandAuditDao = postgresCommandAuditDao;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @Override
    public void handle(final CommandAuditMessage message) {
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            if (message.getCommands() != null) {
                batchedMessages.get().addAll(message.getCommands());
            }
        }
    }

    @Override
    public void consumerCommitting() {
        final List<CommandAudit> messagesForThread = batchedMessages.get();
        if (!messagesForThread.isEmpty()) {
            LOG.debug("Committing {} queued messages", messagesForThread.size());

            postgresCommandAuditDao.saveAll(newArrayList(messagesForThread));
            messagesForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }
}
