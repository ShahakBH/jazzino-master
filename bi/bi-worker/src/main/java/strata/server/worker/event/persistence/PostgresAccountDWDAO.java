package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.event.message.AccountEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.util.Collection;
import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.sqlBigDecimal;

@Repository
public class PostgresAccountDWDAO extends PostgresDWDAO<AccountEvent> {

    private static final String SQL_EXECUTE_UPDATES = "UPDATE ACCOUNT SET BALANCE = stage.BALANCE "
            + "FROM STG_ACCOUNT stage "
            + "WHERE ACCOUNT.ACCOUNT_ID = stage.ACCOUNT_ID";

    private static final String SQL_EXECUTE_INSERTS = "INSERT INTO ACCOUNT "
            + "SELECT stage.* FROM STG_ACCOUNT stage "
            + "LEFT JOIN ACCOUNT target ON stage.ACCOUNT_ID = target.ACCOUNT_ID "
            + "WHERE target.ACCOUNT_ID IS NULL";

    private static final String SQL_CLEAN_STAGING = "DELETE FROM STG_ACCOUNT";

    PostgresAccountDWDAO() {
        // CBLib constructor
        super(null);
    }

    @Autowired
    public PostgresAccountDWDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }

    private String createInsertStatementFor(final Collection<AccountEvent> accountEvents) {
        // The PostgreSQL 8 driver doesn't appear to optimise batched INSERTS in the same way MySQL does. Yay.

        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("STG_ACCOUNT", "ACCOUNT_ID", "BALANCE");
        for (AccountEvent accountEvent : accountEvents) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(accountEvent.getAccountId()),
                    sqlBigDecimal(accountEvent.getBalance()));
        }

        return insertBuilder.toSql();
    }

    @Override
    protected String[] getBatchUpdates(final List<AccountEvent> events) {
        return new String[]{
                createInsertStatementFor(events),
                SQL_EXECUTE_UPDATES,
                SQL_EXECUTE_INSERTS,
                SQL_CLEAN_STAGING
        };

    }

}
