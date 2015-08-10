package strata.server.worker.event.persistence;

import com.yazino.bi.persistence.InsertStatementBuilder;
import com.yazino.platform.event.message.TableEvent;
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

import static com.yazino.bi.persistence.InsertStatementBuilder.sqlBigDecimal;
import static com.yazino.bi.persistence.InsertStatementBuilder.sqlString;

@Repository
public class PostgresTableDWDAO extends PostgresDWDAO<TableEvent> {

    private static final String SQL_EXECUTE_VARIATION_UPDATES = "UPDATE GAME_VARIATION_TEMPLATE "
            + "SET GAME_VARIATION_TEMPLATE_ID = stage.GAME_VARIATION_TEMPLATE_ID,GAME_TYPE = stage.GAME_TYPE,NAME = stage.NAME "
            + "FROM STG_GAME_VARIATION_TEMPLATE stage "
            + "WHERE GAME_VARIATION_TEMPLATE.GAME_VARIATION_TEMPLATE_ID = stage.GAME_VARIATION_TEMPLATE_ID";

    private static final String SQL_EXECUTE_VARIATION_INSERTS = "INSERT INTO GAME_VARIATION_TEMPLATE "
            + "SELECT stage.* FROM STG_GAME_VARIATION_TEMPLATE stage "
            + "LEFT JOIN GAME_VARIATION_TEMPLATE target ON stage.GAME_VARIATION_TEMPLATE_ID = target.GAME_VARIATION_TEMPLATE_ID "
            + "WHERE target.GAME_VARIATION_TEMPLATE_ID IS NULL";

    private static final String SQL_CLEAN_VARIATION_STAGING = "DELETE FROM STG_GAME_VARIATION_TEMPLATE";

    PostgresTableDWDAO() {  // CGLib constructor
        super(null);
    }

    @Autowired
    public PostgresTableDWDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        super(template);
    }


    @Override
    protected String[] getBatchUpdates(final List<TableEvent> events) {
        return new String[]{
                createTableInsertStatementFor(events),
                createGameVariationInsertStatementFor(events),
                SQL_EXECUTE_VARIATION_UPDATES,
                SQL_EXECUTE_VARIATION_INSERTS,
                SQL_CLEAN_VARIATION_STAGING};
    }

    private String createTableInsertStatementFor(final Collection<TableEvent> tableEvents) {
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("TABLE_DEFINITION", "TABLE_ID", "GAME_VARIATION_TEMPLATE_ID");
        for (TableEvent tableEvent : tableEvents) {
            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(tableEvent.getTableId()),
                    sqlBigDecimal(tableEvent.getTemplateId()));
        }

        return insertBuilder.toSql();
    }

    private String createGameVariationInsertStatementFor(final Collection<TableEvent> tableEvents) {
        final Set<BigDecimal> processedGameVariations = new HashSet<BigDecimal>();
        InsertStatementBuilder insertBuilder = new InsertStatementBuilder("STG_GAME_VARIATION_TEMPLATE",
                "GAME_VARIATION_TEMPLATE_ID", "GAME_TYPE", "NAME");
        for (TableEvent tableEvent : tableEvents) {
            if (processedGameVariations.contains(tableEvent.getTemplateId())) {
                continue;
            }
            processedGameVariations.add(tableEvent.getTemplateId());

            insertBuilder = insertBuilder.withValues(
                    sqlBigDecimal(tableEvent.getTemplateId()),
                    sqlString(tableEvent.getGameTypeId()),
                    sqlString(tableEvent.getTemplateName()));
        }

        return insertBuilder.toSql();
    }

}
