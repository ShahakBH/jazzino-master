package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.event.message.LeaderboardEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.math.BigDecimal;
import java.util.*;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;

@Repository
public class PostgresLeaderboardDWDAO extends PostgresDWDAO<LeaderboardEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresLeaderboardDWDAO.class);

    private static final String SQL_EXECUTE_UPDATES = "UPDATE LEADERBOARD"
            + " SET GAME_TYPE = stage.GAME_TYPE, END_TS = stage.END_TS "
            + "FROM STG_LEADERBOARD stage "
            + "WHERE LEADERBOARD.LEADERBOARD_ID = stage.LEADERBOARD_ID";

    private static final String SQL_EXECUTE_INSERTS = "INSERT INTO LEADERBOARD "
            + "SELECT stage.* FROM STG_LEADERBOARD stage "
            + "LEFT JOIN LEADERBOARD target ON stage.LEADERBOARD_ID = target.LEADERBOARD_ID "
            + "WHERE target.LEADERBOARD_ID IS NULL";

    private static final String SQL_CLEAN_STAGING = "DELETE FROM STG_LEADERBOARD";

    private static final String SQL_CLEAR_POSITIONS = "DELETE FROM LEADERBOARD_POSITION WHERE ";


    PostgresLeaderboardDWDAO() {
        // CGLib constructor
        super(null);
    }

    @Autowired
    public PostgresLeaderboardDWDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }


    private String createPositionsDeleteStatements(final Collection<LeaderboardEvent> leaderboardEvents) {
        final Set<BigDecimal> uniqueLeaderboardIds = new HashSet<BigDecimal>();
        for (LeaderboardEvent leaderboardEvent : leaderboardEvents) {
            uniqueLeaderboardIds.add(leaderboardEvent.getLeaderboardId());
        }

        boolean first = true;
        final StringBuilder deleteStatement = new StringBuilder(SQL_CLEAR_POSITIONS);
        for (BigDecimal leaderboardId : uniqueLeaderboardIds) {
            if (!first) {
                deleteStatement.append("OR ");
            } else {
                first = false;
            }
            deleteStatement.append("LEADERBOARD_ID=").append(sqlBigDecimal(leaderboardId));
        }
        return deleteStatement.toString();
    }

    private String generatePositionsInsertStatement(final Collection<LeaderboardEvent> leaderboardEvents) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("LEADERBOARD_POSITION",
                "LEADERBOARD_ID", "PLAYER_ID", "LEADERBOARD_POSITION");

        boolean valuesAdded = false;

        for (LeaderboardEvent leaderboardEvent : leaderboardEvents) {
            if (leaderboardEvent.getPlayerPositions().isEmpty()) {
                LOG.info("No positions or players specified for leaderboard {}", leaderboardEvent.getLeaderboardId());
                continue;
            }
            valuesAdded = true;

            for (int position : leaderboardEvent.getPlayerPositions().keySet()) {
                insertBuilder = insertBuilder.withValues(
                        sqlBigDecimal(leaderboardEvent.getLeaderboardId()),
                        sqlBigDecimal(leaderboardEvent.getPlayerPositions().get(position)),
                        sqlInt(position));
            }
        }

        if (!valuesAdded) {
            return null;
        }

        return insertBuilder.toSql();
    }

    private String createLeaderboardInsertStatementFor(final Collection<LeaderboardEvent> leaderboardEvents) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("LEADERBOARD",
                "LEADERBOARD_ID", "GAME_TYPE", "END_TS");
        for (LeaderboardEvent leaderboardEvent : leaderboardEvents) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(leaderboardEvent.getLeaderboardId()),
                    sqlString(leaderboardEvent.getGameType()),
                    sqlTimestamp(leaderboardEvent.getEndTs()));
        }

        return insertBuilder.toSql();
    }

    @Override
    protected String[] getBatchUpdates(final List<LeaderboardEvent> leaderboardEvents) {
        final List<String> statementsToExecute = new ArrayList<String>();
        statementsToExecute.add(createLeaderboardInsertStatementFor(leaderboardEvents));
        statementsToExecute.add(SQL_EXECUTE_UPDATES);
        statementsToExecute.add(SQL_EXECUTE_INSERTS);
        statementsToExecute.add(SQL_CLEAN_STAGING);
        statementsToExecute.add(createPositionsDeleteStatements(leaderboardEvents));

        final String positionSql = generatePositionsInsertStatement(leaderboardEvents);
        if (positionSql != null) {
            statementsToExecute.add(positionSql);
        }

        return statementsToExecute.toArray(new String[statementsToExecute.size()]);

    }
}
