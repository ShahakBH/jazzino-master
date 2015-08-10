package strata.server.worker.audit.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.audit.message.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;

@Repository
public class PostgresTransactionLogDAO extends PostgresDWDAO<Transaction> {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresTransactionLogDAO.class);


    PostgresTransactionLogDAO() {
        // CGLib constructor
        super(null);
    }

    @Autowired
    public PostgresTransactionLogDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }

    @Override
    protected String[] getBatchUpdates(final List<Transaction> events) {
        return new String[]{createInsertStatementFor(events)};
    }

    private String createInsertStatementFor(final List<Transaction> transactions) {
        // The PostgreSQL 8 driver doesn't appear to optimise batched INSERTS in the same way MySQL does. Yay.

        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("TRANSACTION_LOG",
                "SESSION_ID", "ACCOUNT_ID", "AMOUNT", "TRANSACTION_TYPE", "REFERENCE",
                "TRANSACTION_TS", "RUNNING_BALANCE", "GAME_ID", "TABLE_ID");
        for (Transaction transaction : transactions) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(transaction.getSessionId()),
                    sqlBigDecimal(transaction.getAccountId()),
                    sqlBigDecimal(transaction.getAmount()),
                    sqlString(transaction.getType()),
                    sqlString(transaction.getReference()),
                    sqlTimestamp(timestampOf(transaction.getTimestamp())),
                    sqlBigDecimal(transaction.getRunningBalance()),
                    sqlLong(transaction.getGameId()),
                    sqlBigDecimal(transaction.getTableId()));
        }

        return insertBuilder.toSql();
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void logBatchExceptionsFrom(final Exception e) {
        if (e.getMessage().contains("Call getNextException")) {
            final BatchUpdateException batchUpdateException = batchUpdateExceptionFrom(e);
            if (batchUpdateException != null) {
                SQLException nextException = batchUpdateException;
                while ((nextException = nextException.getNextException()) != null) {
                    LOG.error("Batch update: next exception is {}", nextException);
                }
            }
        }
    }

    private BatchUpdateException batchUpdateExceptionFrom(final Throwable e) {
        if (e == null || e instanceof BatchUpdateException) {
            return (BatchUpdateException) e;
        }

        return batchUpdateExceptionFrom(e.getCause());
    }

    private Timestamp timestampOf(final Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return new Timestamp(timestamp);
    }
}
