package strata.server.worker.audit.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.audit.message.SessionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.sqlBigDecimal;
import static com.yazino.bi.persistence.InsertStatementBuilder.sqlString;

@Repository
public class PostgresSessionKeyDAO extends PostgresDWDAO<SessionKey> {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresSessionKeyDAO.class);

    private static final int MAX_REFERRER_SIZE = 2048;


    PostgresSessionKeyDAO() {
        // CGLib constructor
        super(null);
    }

    @Autowired
    public PostgresSessionKeyDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    protected String[] getBatchUpdates(final List<SessionKey> events) {
        return new String[]{createAccountSessionInsertStatementFor(events)};
    }

    private String truncateReferrer(final String referrer) {
        if (referrer == null) {
            return null;
        }

        if (referrer.length() > MAX_REFERRER_SIZE) {
            return referrer.substring(0, MAX_REFERRER_SIZE);
        }

        return referrer;
    }

    private String createAccountSessionInsertStatementFor(final List<SessionKey> sessionKeys) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("ACCOUNT_SESSION",
                "SESSION_ID", "ACCOUNT_ID", "SESSION_KEY", "IP_ADDRESS", "REFERER", "PLATFORM", "START_PAGE");
        for (SessionKey sessionKey : sessionKeys) {
            if (sessionKey.getAccountId() == null || sessionKey.getSessionKey() == null) {
                LOG.error("Received invalid account ID and/or session key: {}", sessionKey);
                continue;
            }

            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(sessionKey.getSessionId()),
                    sqlBigDecimal(sessionKey.getAccountId()),
                    sqlString(sessionKey.getSessionKey()),
                    sqlString(sessionKey.getIpAddress()),
                    sqlString(truncateReferrer(sessionKey.getReferrer())),
                    sqlString(sessionKey.getPlatform()),
                    sqlString(sessionKey.getLoginUrl()));
        }

        return insertBuilder.toSql();
    }
}
