package strata.server.worker.audit.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.audit.message.CommandAudit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;

@Repository
public class PostgresCommandAuditDAO extends PostgresDWDAO<CommandAudit> {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresCommandAuditDAO.class);

    PostgresCommandAuditDAO() {
        // CGLib constructor
        super(null);
    }

    @Autowired
    public PostgresCommandAuditDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }

    private String createInsertStatementFor(final List<CommandAudit> commandAudits) {
        // The PostgreSQL 8 driver doesn't appear to optimise batched INSERTS in the same way MySQL does. Yay.

        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("AUDIT_COMMAND", "AUDIT_LABEL", "AUDIT_TS",
                "HOSTNAME", "COMMAND_ID", "TABLE_ID", "GAME_ID", "PLAYER_ID", "COMMAND_TYPE", "COMMAND_ARGS");
        for (CommandAudit commandAudit : commandAudits) {
            insertBuilder = insertBuilder.withValues(
                    sqlString(commandAudit.getAuditLabel()),
                    sqlTimestamp(new Timestamp(commandAudit.getTimeStamp().getTime())),
                    sqlString(commandAudit.getHostname()),
                    sqlString(commandAudit.getUuid()),
                    sqlBigDecimal(commandAudit.getTableId()),
                    sqlLong(commandAudit.getGameId()),
                    sqlBigDecimal(commandAudit.getPlayerId()),
                    sqlString(commandAudit.getType()),
                    sqlString(argsFor(commandAudit)));
        }

        return insertBuilder.toSql();
    }

    private String argsFor(final CommandAudit commandAudit) {
        String args = null;
        if (commandAudit.getArgs() != null) {
            args = Arrays.asList(commandAudit.getArgs()).toString();
        }
        return args;
    }

    @Override
    protected String[] getBatchUpdates(final List<CommandAudit> events) {
        return new String[]{createInsertStatementFor(events)};
    }
}
