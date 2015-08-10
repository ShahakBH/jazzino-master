package strata.server.worker.audit.consumer;

import com.yazino.bi.messaging.CommitAware;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.campaign.dao.FacebookExclusionsDao;
import com.yazino.platform.audit.message.SessionKey;
import com.yazino.platform.audit.message.SessionKeyMessage;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import strata.server.worker.audit.persistence.PostgresClientContextDAO;
import strata.server.worker.audit.persistence.PostgresSessionKeyDAO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class SessionKeyMessageConsumer implements QueueMessageConsumer<SessionKeyMessage>, CommitAware {
    private static final Logger LOG = LoggerFactory.getLogger(SessionKeyMessageConsumer.class);

    private final PostgresSessionKeyDAO postgresSessionKeyDAO;
    private final PostgresClientContextDAO postgresClientContextDAO;
    private final FacebookExclusionsDao facebookExclusionDao;
    private final YazinoConfiguration yazinoConfiguration;

    private final ThreadLocal<Map<BigDecimal, SessionKey>> batchedMessages = new ThreadLocal<Map<BigDecimal, SessionKey>>() {
        @Override
        protected Map<BigDecimal, SessionKey> initialValue() {
            return new HashMap<>();
        }
    };



    @Autowired
    public SessionKeyMessageConsumer(final YazinoConfiguration yazinoConfiguration,
                                     final PostgresSessionKeyDAO postgresSessionKeyDAO,
                                     final PostgresClientContextDAO postgresClientContextDAO,
                                     final FacebookExclusionsDao facebookExclusionDao) {
        this.postgresSessionKeyDAO = postgresSessionKeyDAO;
        this.yazinoConfiguration = yazinoConfiguration;
        this.postgresClientContextDAO = postgresClientContextDAO;
        this.facebookExclusionDao = facebookExclusionDao;
    }

    @Override
    public void handle(final SessionKeyMessage message) {
        if (yazinoConfiguration.getBoolean("data-warehouse.write.enabled")) {
            if (message.getSessionKey() != null) {
                batchedMessages.get().put(message.getSessionKey().getSessionId(), message.getSessionKey());
            }
        }
    }

    @Override
    public void consumerCommitting() {
        final Collection<SessionKey> messagesForThread = batchedMessages.get().values();
        if (!messagesForThread.isEmpty()) {
            LOG.debug("Committing {} queued messages", messagesForThread.size());

            final ArrayList<SessionKey> events = new ArrayList<SessionKey>(messagesForThread);
            postgresSessionKeyDAO.saveAll(events);
            postgresClientContextDAO.saveAll(events);
            facebookExclusionDao.resetFacebookExclusions(events);
            messagesForThread.clear();

        } else {
            LOG.debug("Nothing to commit, returning");
        }
    }

}
