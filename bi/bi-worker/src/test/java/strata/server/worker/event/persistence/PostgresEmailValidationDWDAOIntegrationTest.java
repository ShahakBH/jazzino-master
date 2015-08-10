package strata.server.worker.event.persistence;

import com.yazino.logging.appender.ListAppender;
import com.yazino.platform.event.message.EmailValidationEvent;
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
public class PostgresEmailValidationDWDAOIntegrationTest {

    private static final String EV2 = "test1";
    private static final String EV1 = "test2";
    private static final String DEFAULT_STATUS = "V";

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresEmailValidationDWDAO underTest;

    @Before
    @After
    public void cleanUp() {
        jdbc.update("delete from email_validation where email_address in (?,?)", "test1", "test2");
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresEmailValidationDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(asList(anEmailValidationEvent(EV2)));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresEmailValidationDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(asList(anEmailValidationEvent(EV2)));
    }

    @Test
    public void shouldLogCompleteDataDumpOnWriteFailure() {
        // GIVEN a JDBC template throws an unexpected SQL exception
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        // AND there is a logger capturing the data
        final ListAppender logAppender = ListAppender.addTo(PostgresDWDAO.class);

        // WHEN the save is tried
        final EmailValidationEvent emailValidationEventEvent = anEmailValidationEvent(EV2);
        new PostgresEmailValidationDWDAO(mockTemplate).saveAll(asList(emailValidationEventEvent));

        // THEN in the log we get all the data written
        Matcher<Iterable<? super String>> hasItemResult = hasItem(containsString(emailValidationEventEvent.toString()));
        assertThat((Iterable<? super String>) logAppender.getMessages(), hasItemResult);
    }

    @Test
    public void aEmailValidationEventIsSavedToTheDatabase() {
        underTest.saveAll(asList(anEmailValidationEvent(EV2)));

        verifyRecordMatches(anEmailValidationEvent(EV2), readRecordByEmailAddress(EV2));
    }

    @Test
    public void aEmailValidationEventUpdatesExistingEmailValidationInDatabase() {
        underTest.saveAll(asList(anEmailValidationEvent(EV2)));
        underTest.saveAll(asList(anEmailValidationEvent(EV2, "U")));

        verifyRecordMatches(anEmailValidationEvent(EV2, "U"), readRecordByEmailAddress(EV2));
    }

    @Test
    public void multipleEmailValidationEventsMayBeSavedToTheDatabase() {
        underTest.saveAll(asList(anEmailValidationEvent(EV2), anEmailValidationEvent(EV1)));

        verifyRecordMatches(anEmailValidationEvent(EV2), readRecordByEmailAddress(EV2));
        verifyRecordMatches(anEmailValidationEvent(EV1), readRecordByEmailAddress(EV1));
    }

    private EmailValidationEvent anEmailValidationEvent(final String emailAddress) {
        return anEmailValidationEvent(emailAddress, DEFAULT_STATUS);
    }

    private EmailValidationEvent anEmailValidationEvent(String id, String status) {
        return new EmailValidationEvent((id), (status));
    }


    private void verifyRecordMatches(final EmailValidationEvent emailValidationEvent, final Map<String, Object> record) {
        assertThat((record.get("email_address").toString()), is(equalTo(emailValidationEvent.getEmailAddress())));
        assertThat((record.get("status").toString()), is(equalTo(emailValidationEvent.getStatus())));
    }

    private Map<String, Object> readRecordByEmailAddress(final String emailAddress) {
        return jdbc.queryForMap("select * from email_validation where email_address=?", emailAddress);
    }
}

