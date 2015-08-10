package strata.server.worker.audit.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.audit.message.SessionKey;
import com.yazino.platform.session.SessionClientContextKey;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.util.List;
import java.util.Map;

import static com.yazino.bi.persistence.InsertStatementBuilder.sqlBigDecimal;
import static com.yazino.bi.persistence.InsertStatementBuilder.sqlString;

@Repository
public class PostgresClientContextDAO extends PostgresDWDAO<SessionKey> {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresClientContextDAO.class);
    public static final String DEVICE_ID_KEY = SessionClientContextKey.DEVICE_ID.name();

    public PostgresClientContextDAO() {
        // CGLib  constructor
        super(null);
    }

    @Autowired
    public PostgresClientContextDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    protected String[] getBatchUpdates(final List<SessionKey> events) {


        String [] statements;
        try {
            String clientContextInsertStatements = createClientContextInsertStatementFor(events);
            statements = ArrayUtils.toArray(clientContextInsertStatements);
        } catch (IllegalStateException e) {
            LOG.debug("no client context to persist");
            statements = ArrayUtils.toArray("");
        }
        return statements;
    }


    private String createClientContextInsertStatementFor(final List<SessionKey> sessionKeys) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("CLIENT_CONTEXT",
                "SESSION_ID", "DEVICE_ID");
        for (SessionKey sessionKey : sessionKeys) {
            if (sessionKey.getClientContext() == null || sessionKey.getSessionKey() == null) {
                LOG.warn("Received client context {} /or session key is null: {} on platform {}", sessionKey.getClientContext(),
                        sessionKey, sessionKey.getPlatform());
                continue;
            }

            String deviceId = null;
            Map<String, Object> clientContext = sessionKey.getClientContext();
            if (clientContext.containsKey(DEVICE_ID_KEY)) {
                deviceId = (String) clientContext.get(DEVICE_ID_KEY);
                LOG.debug("setting device_id {}");
            } else {
                LOG.debug("No values in clientContext i.e DEVICE_ID on platform {}, {}", sessionKey.getPlatform(), sessionKey.getLoginUrl());
                continue;
            }

            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(sessionKey.getSessionId()),
                    sqlString(deviceId));
        }

        return insertBuilder.toSql();
    }
}
