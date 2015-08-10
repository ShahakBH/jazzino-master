package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.event.message.TournamentPlayerSummary;
import com.yazino.platform.event.message.TournamentSummaryEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import strata.server.worker.persistence.PostgresDWDAO;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.yazino.bi.persistence.InsertStatementBuilder.*;

@Repository
public class PostgresTournamentDWDAO extends PostgresDWDAO<TournamentSummaryEvent> {

    private static final String SQL_EXECUTE_VARIATION_UPDATES = "UPDATE TOURNAMENT_VARIATION_TEMPLATE "
            + "SET TOURNAMENT_VARIATION_TEMPLATE_ID = stage.TOURNAMENT_VARIATION_TEMPLATE_ID,GAME_TYPE = stage.GAME_TYPE "
            + "FROM STG_TOURNAMENT_VARIATION_TEMPLATE stage "
            + "WHERE TOURNAMENT_VARIATION_TEMPLATE.TOURNAMENT_VARIATION_TEMPLATE_ID = stage.TOURNAMENT_VARIATION_TEMPLATE_ID";

    private static final String SQL_EXECUTE_VARIATION_INSERTS = "INSERT INTO TOURNAMENT_VARIATION_TEMPLATE "
            + "SELECT stage.* FROM STG_TOURNAMENT_VARIATION_TEMPLATE stage "
            + "LEFT JOIN TOURNAMENT_VARIATION_TEMPLATE target ON stage.TOURNAMENT_VARIATION_TEMPLATE_ID = target.TOURNAMENT_VARIATION_TEMPLATE_ID "
            + "WHERE target.TOURNAMENT_VARIATION_TEMPLATE_ID IS NULL";

    private static final String SQL_CLEAN_VARIATION_STAGING = "DELETE FROM STG_TOURNAMENT_VARIATION_TEMPLATE";

    PostgresTournamentDWDAO() {
        // CGLib constructor
        super(null);
    }

    @Autowired
    public PostgresTournamentDWDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }

    private String createPlayerInsertStatementFor(final Collection<TournamentSummaryEvent> tournamentSummaryEvents) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("TOURNAMENT_PLAYER", "PLAYER_ID", "TOURNAMENT_ID");
        for (TournamentSummaryEvent event : tournamentSummaryEvents) {
            for (TournamentPlayerSummary playerSummary : event.getPlayers()) {
                insertBuilder = insertBuilder.withValues(
                        sqlBigDecimal(playerSummary.getId()),
                        sqlBigDecimal(event.getTournamentId()));
            }
        }

        return insertBuilder.toSql();
    }

    private String createTournamentSummaryInsertStatementFor(final Collection<TournamentSummaryEvent> tournamentSummaryEvents) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("TOURNAMENT_SUMMARY",
                "TOURNAMENT_ID", "TOURNAMENT_NAME", "TOURNAMENT_FINISHED_TS");
        for (TournamentSummaryEvent event : tournamentSummaryEvents) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(event.getTournamentId()),
                    sqlString(event.getTournamentName()),
                    sqlTimestamp(event.getFinishedTs()));
        }

        return insertBuilder.toSql();
    }

    private String createTournamentInsertStatementFor(final Collection<TournamentSummaryEvent> tournamentSummaryEvents) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("TOURNAMENT",
                "TOURNAMENT_ID", "TOURNAMENT_START_TS", "TOURNAMENT_VARIATION_TEMPLATE_ID");
        for (TournamentSummaryEvent event : tournamentSummaryEvents) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(event.getTournamentId()),
                    sqlTimestamp(event.getStartTs()),
                    sqlBigDecimal(event.getTemplateId()));
        }

        return insertBuilder.toSql();
    }

    private String createTournamentVariationInsertStatementFor(final Collection<TournamentSummaryEvent> tournamentSummaryEvents) {
        final Set<BigDecimal> processedVariations = new HashSet<BigDecimal>();
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("STG_TOURNAMENT_VARIATION_TEMPLATE",
                "TOURNAMENT_VARIATION_TEMPLATE_ID", "GAME_TYPE");
        for (TournamentSummaryEvent event : tournamentSummaryEvents) {
            if (processedVariations.contains(event.getTemplateId())) {
                continue;
            }
            processedVariations.add(event.getTemplateId());

            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(event.getTemplateId()),
                    sqlString(event.getGameType()));
        }

        return insertBuilder.toSql();
    }

    @Override
    protected String[] getBatchUpdates(final List<TournamentSummaryEvent> events) {
        return new String[]{
                createPlayerInsertStatementFor(events),
                createTournamentVariationInsertStatementFor(events),
                createTournamentInsertStatementFor(events),
                createTournamentSummaryInsertStatementFor(events),
                SQL_EXECUTE_VARIATION_UPDATES,
                SQL_EXECUTE_VARIATION_INSERTS,
                SQL_CLEAN_VARIATION_STAGING
        };
    }

}
