package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.event.message.EmailValidationEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.util.Collection;
import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.sqlString;

@Repository
public class PostgresEmailValidationDWDAO extends PostgresDWDAO<EmailValidationEvent> {

    private static final String SQL_EXECUTE_UPDATES = "UPDATE EMAIL_VALIDATION SET STATUS= stage.STATUS "
            + "FROM STG_EMAIL_VALIDATION stage "
            + "WHERE EMAIL_VALIDATION.EMAIL_ADDRESS= stage.EMAIL_ADDRESS";

    private static final String SQL_EXECUTE_INSERTS = "INSERT INTO EMAIL_VALIDATION "
            + "SELECT stage.* FROM STG_EMAIL_VALIDATION stage "
            + "LEFT JOIN EMAIL_VALIDATION target ON stage.EMAIL_ADDRESS= target.EMAIL_ADDRESS "
            + "WHERE target.EMAIL_ADDRESS IS NULL";

    private static final String SQL_CLEAN_STAGING = "DELETE FROM STG_EMAIL_VALIDATION";

    PostgresEmailValidationDWDAO() {
        // CBLib constructor
        super(null);
    }

    @Autowired
    public PostgresEmailValidationDWDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }

    private String createInsertStatementFor(final Collection<EmailValidationEvent> emailValidationEvents) {
        // The PostgreSQL 8 driver doesn't appear to optimise batched INSERTS in the same way MySQL does. Yay.

        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("STG_EMAIL_VALIDATION", "EMAIL_ADDRESS", "STATUS");
        for (EmailValidationEvent emailValidationEvent : emailValidationEvents) {
            insertBuilder = insertBuilder.withValues(
                    sqlString(emailValidationEvent.getEmailAddress()),
                    sqlString(emailValidationEvent.getStatus()));
        }

        return insertBuilder.toSql();
    }

    @Override
    protected String[] getBatchUpdates(final List<EmailValidationEvent> events) {
        return new String[]{
                createInsertStatementFor(events),
                SQL_EXECUTE_UPDATES,
                SQL_EXECUTE_INSERTS,
                SQL_CLEAN_STAGING
        };

    }

}
