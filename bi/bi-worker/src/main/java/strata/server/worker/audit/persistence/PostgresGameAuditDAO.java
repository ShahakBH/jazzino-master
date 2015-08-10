package strata.server.worker.audit.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.audit.message.GameAudit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.List;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;

@Repository
public class PostgresGameAuditDAO extends PostgresDWDAO<GameAudit> {
    private static final int MAX_LENGTH_OF_OBSERVABLE_STATUS = 65535;


    PostgresGameAuditDAO() {
        // CGLib constructor
        super(null);
    }

    @Autowired
    public PostgresGameAuditDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }

    @Override
    protected String[] getBatchUpdates(final List<GameAudit> events) {
        return createInsertStatementsFor(events);
    }

    private String[] createInsertStatementsFor(final List<GameAudit> gameAudits) {
        boolean hasPlayers = false;

        InsertStatementBuilder insertGameBuilder = new InsertStatementBuilder("AUDIT_CLOSED_GAME",
                "AUDIT_LABEL", "AUDIT_TS", "HOSTNAME", "TABLE_ID", "GAME_ID", "GAME_INCREMENT",
                "OBSERVABLE_STATUS", "INTERNAL_STATUS");
        InsertStatementBuilder insertPlayerBuilder = new InsertStatementBuilder("AUDIT_CLOSED_GAME_PLAYER",
                "AUDIT_LABEL", "AUDIT_TS", "HOSTNAME", "TABLE_ID", "GAME_ID", "PLAYER_ID");
        for (GameAudit gameAudit : gameAudits) {
            insertGameBuilder = insertGameBuilder.withValues(
                    sqlString(gameAudit.getAuditLabel()),
                    sqlTimestamp(gameAudit.getTimeStamp()),
                    sqlString(gameAudit.getHostname()),
                    sqlBigDecimal(gameAudit.getTableId()),
                    sqlLong(gameAudit.getGameId()),
                    sqlLong(gameAudit.getIncrement()),
                    sqlString(truncateAtMaxSize(gameAudit.getObservableStatusXml(), MAX_LENGTH_OF_OBSERVABLE_STATUS)),
                    sqlString(gameAudit.getInternalStatusXml()));

            for (BigDecimal playerId : gameAudit.getPlayerIds()) {
                hasPlayers = true;
                insertPlayerBuilder = insertPlayerBuilder.withValues(
                        sqlString(gameAudit.getAuditLabel()),
                        sqlTimestamp(new Timestamp(gameAudit.getTimeStamp().getTime())),
                        sqlString(gameAudit.getHostname()),
                        sqlBigDecimal(gameAudit.getTableId()),
                        sqlLong(gameAudit.getGameId()),
                        sqlBigDecimal(playerId));
            }
        }

        if (hasPlayers) {
            return new String[]{insertGameBuilder.toSql(), insertPlayerBuilder.toSql()};
        }
        return new String[]{insertGameBuilder.toSql()};
    }

    String truncateAtMaxSize(final String observableStatusXml, int maxLengthInBytes) {
        final int length = observableStatusXml.getBytes(Charset.forName("UTF-8")).length;
        if (length > maxLengthInBytes) {
            return truncateWhenUTF8(observableStatusXml, maxLengthInBytes);
        }
        return observableStatusXml;
    }

    private String truncateWhenUTF8(String s, int maxBytes) {
        //from http://stackoverflow.com/questions/119328/how-do-i-truncate-a-java-string-to-fit-in-a-given-number-of-bytes-once-utf-8-en
        int b = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            // ranges from http://en.wikipedia.org/wiki/UTF-8
            int skip = 0;
            int more;
            if (c <= 0x007f) {
                more = 1;
            } else if (c <= 0x07FF) {
                more = 2;
            } else if (c <= 0xd7ff) {
                more = 3;
            } else if (c <= 0xDFFF) {
                // surrogate area, consume next char as well
                more = 4;
                skip = 1;
            } else {
                more = 3;
            }

            if (b + more > maxBytes) {
                return s.substring(0, i);
            }
            b += more;
            i += skip;
        }
        return s;
    }
}
