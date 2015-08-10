package strata.server.worker.event.persistence;

import com.yazino.platform.event.message.TableEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@Transactional
@DirtiesContext
public class PostgresTableDWDAOIntegrationTest {

    private static final BigDecimal TABLE_ID_1 = BigDecimal.valueOf(-1);
    private static final BigDecimal TABLE_ID_2 = BigDecimal.valueOf(-2);
    private static final BigDecimal TEMPLATE_ID = BigDecimal.TEN;

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PostgresTableDWDAO underTest;

    @Before
    @After
    public void cleanUp() {
        jdbc.update("DELETE FROM TABLE_DEFINITION WHERE TABLE_ID IN (?,?)", TABLE_ID_1, TABLE_ID_2);
        jdbc.update("DELETE FROM GAME_VARIATION_TEMPLATE WHERE GAME_VARIATION_TEMPLATE_ID = ?", TEMPLATE_ID);
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresTableDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.save(newHashSet(aTableEvent(TABLE_ID_1)));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresTableDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.save(newHashSet(aTableEvent(TABLE_ID_1)));
    }

    @Test
    public void tableEventIsSavedAcrossMultipleTables() throws Exception {
        TableEvent tableEvent = aTableEvent(TABLE_ID_1);
        underTest.save(newHashSet(tableEvent));

        verifyRecordMatches(tableEvent, readByTableId(TABLE_ID_1));

        Map<String, Object> gvtRecord = readVariationTemplateById(tableEvent.getTemplateId());
        assertThat((String) gvtRecord.get("GAME_TYPE"), is(equalTo(tableEvent.getGameTypeId())));
        assertThat((String) gvtRecord.get("NAME"), is(equalTo(tableEvent.getTemplateName())));
    }

    @Test
    public void aTableEventIsSavedToTheDatabase() {
        underTest.save(newHashSet((aTableEvent(TABLE_ID_1))));

        verifyRecordMatches(aTableEvent(TABLE_ID_1), readByTableId(TABLE_ID_1));
    }

    @Test
    public void multipleTableEventsMayBeSavedToTheDatabase() {
        underTest.save(newHashSet(aTableEvent(TABLE_ID_1), aTableEvent(TABLE_ID_2)));

        verifyRecordMatches(aTableEvent(TABLE_ID_1), readByTableId(TABLE_ID_1));
        verifyRecordMatches(aTableEvent(TABLE_ID_2), readByTableId(TABLE_ID_2));
    }

    private void verifyRecordMatches(final TableEvent event, final Map<String, Object> record) {
        assertThat(new BigDecimal(record.get("TABLE_ID").toString()), is(comparesEqualTo(event.getTableId())));
        assertThat(new BigDecimal(record.get("GAME_VARIATION_TEMPLATE_ID").toString()), is(comparesEqualTo(event.getTemplateId())));
    }

    private Map<String, Object> readByTableId(final BigDecimal tableId) {
        return jdbc.queryForMap("SELECT * FROM TABLE_DEFINITION WHERE TABLE_ID=?", tableId);
    }

    private Map<String, Object> readVariationTemplateById(final BigDecimal templateId) {
        return jdbc.queryForMap("SELECT * FROM GAME_VARIATION_TEMPLATE WHERE GAME_VARIATION_TEMPLATE_ID=?", templateId);
    }

    private TableEvent aTableEvent(BigDecimal tableId) {
        return new TableEvent(tableId, "TEST_GAME_TYPE", TEMPLATE_ID, "Test Variation Template");
    }

}
