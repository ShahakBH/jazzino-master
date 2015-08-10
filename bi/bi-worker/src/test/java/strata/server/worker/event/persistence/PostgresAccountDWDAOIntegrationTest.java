package strata.server.worker.event.persistence;

import com.yazino.logging.appender.ListAppender;
import com.yazino.platform.event.message.AccountEvent;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import strata.server.worker.persistence.PostgresDWDAO;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
@Transactional
public class PostgresAccountDWDAOIntegrationTest {

    private static final String ID_1 = "1.00";
    private static final String ID_2 = "2.00";
    private static final double DEFAULT_BALANCE = 1000.23;

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresAccountDWDAO underTest;

    @Before
    @After
    public void cleanUp() {
        jdbc.update("delete from account where account_id in (?,?)", 1.00, 2.00);
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresAccountDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(asList(aAccountEvent(ID_1)));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresAccountDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(asList(aAccountEvent(ID_1)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldLogCompleteDataDumpOnWriteFailure() {
        // GIVEN a JDBC template throws an unexpected SQL exception
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        // AND there is a logger capturing the data
        final ListAppender logAppender = ListAppender.addTo(PostgresDWDAO.class);

        // WHEN the save is tried
        final AccountEvent accountEvent = aAccountEvent(ID_1);
        new PostgresAccountDWDAO(mockTemplate).saveAll(asList(accountEvent));

        // THEN in the log we get all the data written
        Matcher<Iterable<? super String>> hasItemResult = hasItem(containsString(accountEvent.toString()));
        assertThat((Iterable<? super String>) logAppender.getMessages(), hasItemResult);
    }

    @Test
    public void aAccountEventIsSavedToTheDatabase() {
        underTest.saveAll(asList(aAccountEvent(ID_1)));

        verifyRecordMatches(aAccountEvent(ID_1), readRecordByAccountId(ID_1));
    }

    @Test
    public void aAccountEventUpdatesExistingAccountInDatabase() {
        underTest.saveAll(asList(aAccountEvent(ID_1)));
        underTest.saveAll(asList(aAccountEvent(ID_1, 57.36)));

        verifyRecordMatches(aAccountEvent(ID_1, 57.36), readRecordByAccountId(ID_1));
    }

    @Test
    public void multipleAccountEventsMayBeSavedToTheDatabase() {
        underTest.saveAll(asList(aAccountEvent(ID_1), aAccountEvent(ID_2)));

        verifyRecordMatches(aAccountEvent(ID_1), readRecordByAccountId(ID_1));
        verifyRecordMatches(aAccountEvent(ID_2), readRecordByAccountId(ID_2));
    }

    private AccountEvent aAccountEvent(final String id) {
        return aAccountEvent(id, DEFAULT_BALANCE);
    }

    private AccountEvent aAccountEvent(String id, double balance) {
        return new AccountEvent(new BigDecimal(id), BigDecimal.valueOf(balance));
    }


    private void verifyRecordMatches(final AccountEvent accountEvent, final Map<String, Object> record) {
        assertThat(new BigDecimal(record.get("account_id").toString()), is(comparesEqualTo(accountEvent.getAccountId())));
        assertThat(new BigDecimal(record.get("balance").toString()), is(comparesEqualTo(accountEvent.getBalance())));
    }

    private Map<String, Object> readRecordByAccountId(final String accountId) {
        return jdbc.queryForMap("select * from account where account_id=?", new BigDecimal(accountId));
    }
}

