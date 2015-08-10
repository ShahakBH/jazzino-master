package strata.server.worker.audit.persistence;

import com.yazino.logging.appender.ListAppender;
import com.yazino.platform.audit.message.SessionKey;
import com.yazino.platform.session.SessionClientContextKey;
import org.hamcrest.Matcher;
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

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
public class PostgresClientContextDAOIntegrationTest {

    private static final BigDecimal ACCOUNT_ID = BigDecimal.TEN;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592);
    private static final String SESSION_KEY = "aSessionKey";
    private static final String REFERRER = "aReferrer";
    private static final String PLATFORM = "FACEBOOK";
    private static final String PLATFORM_WEB = "WEB";
    private static final String IP_ADDRESS = "127.0.0.1";
    public static final String LOGIN_URL = "http://loginUrl";
    public static final HashMap<String,Object> CLIENT_CONTEXT = new HashMap<String, Object>();


    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PostgresClientContextDAO underTest;

    @Before
    public void cleanUp() {
        jdbc.update("DELETE FROM CLIENT_CONTEXT");
    }

    @SuppressWarnings({"NullableProblems" })
    @Test(expected = NullPointerException.class)
    public void aNullSessionKeyCausesANullPointerException() {
        underTest.saveAll(null);
    }

    @SuppressWarnings({"NullableProblems" })
    @Test
    public void aSessionKeyIsIgnoredIfTheAccountIdIsNull() {
        jdbc = mock(JdbcTemplate.class);
        underTest = new PostgresClientContextDAO(jdbc);

        final SessionKey sessionKey = aSessionKey();
        sessionKey.setAccountId(null);

        underTest.saveAll(asList(sessionKey));

        verifyZeroInteractions(jdbc);
    }

    @SuppressWarnings({"NullableProblems" })
    @Test
    public void aSessionKeyIsIgnoredIfTheSessionKeyIsNull() {
        jdbc = mock(JdbcTemplate.class);
        underTest = new PostgresClientContextDAO(jdbc);

        final SessionKey sessionKey = aSessionKey();
        sessionKey.setSessionKey(null);

        underTest.saveAll(asList(sessionKey));

        verifyZeroInteractions(jdbc);
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        HashMap<String, Object> clientContext = new HashMap<String, Object>();
        clientContext.put(SessionClientContextKey.DEVICE_ID.name(), "unique identifier for client");

        jdbc = mock(JdbcTemplate.class);
        underTest = new PostgresClientContextDAO(jdbc);


        when(jdbc.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(asList(aSessionKeyWithCampaignContext(clientContext)));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        jdbc = mock(JdbcTemplate.class);

        HashMap<String, Object> clientContext = new HashMap<String, Object>();
        clientContext.put(SessionClientContextKey.DEVICE_ID.name(), "unique identifier for client");

        underTest = new PostgresClientContextDAO(jdbc);

        when(jdbc.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(asList(aSessionKeyWithCampaignContext(clientContext)));
    }


    @Test
    public void aSessionKeyWithClientContextShouldWriteClientContextToClientContextTable(){
        HashMap<String, Object> clientContext = new HashMap<String, Object>();
        clientContext.put(SessionClientContextKey.DEVICE_ID.name(), "unique identifier for client");

        underTest.saveAll(asList(aSessionKeyWithCampaignContext(clientContext)));

        final Map<String, Object> record = readFromClientContext(SESSION_ID);
        assertThat((String) record.get(SessionClientContextKey.DEVICE_ID.name()), is(equalTo("unique identifier for client")));
    }

    @Test
    public void aSessionKeyWithNullClientContextShouldNotWriteARecord() {
        underTest.saveAll(asList(aSessionKeyWithCampaignContext(null)));
        assertThat(jdbc.queryForInt("SELECT COUNT(*) from CLIENT_CONTEXT"), is(equalTo(0)));
    }

    @Test
    public void aSessionKeyWithEmptyClientContextShouldNotWriteARecord() {
        underTest.saveAll(asList(aSessionKeyWithCampaignContext(new HashMap<String, Object>())));
        assertThat(jdbc.queryForInt("SELECT COUNT(*) from CLIENT_CONTEXT"), is(equalTo(0)));
    }

    private Map<String, Object> readFromClientContext(final BigDecimal sessionId) {
        return jdbc.queryForMap("SELECT * FROM CLIENT_CONTEXT WHERE SESSION_ID = ?",sessionId);
    }

    private SessionKey aSessionKeyWithCampaignContext(final HashMap<String, Object> clientContext) {
        return new SessionKey(SESSION_ID, ACCOUNT_ID, BigDecimal.ONE, SESSION_KEY, IP_ADDRESS, REFERRER, PLATFORM_WEB, null, clientContext);
    }

    private SessionKey aSessionKey() {
        return new SessionKey(SESSION_ID, ACCOUNT_ID, BigDecimal.ONE, SESSION_KEY, IP_ADDRESS, REFERRER, PLATFORM, LOGIN_URL, CLIENT_CONTEXT);
    }

    private SessionKey aSessionKeyYazino() {
        return new SessionKey(SESSION_ID, ACCOUNT_ID, BigDecimal.ONE, SESSION_KEY, IP_ADDRESS, REFERRER, PLATFORM_WEB, LOGIN_URL, CLIENT_CONTEXT);
    }

    private SessionKey aSessionKeyWithNoURL() {
        return new SessionKey(SESSION_ID, ACCOUNT_ID, BigDecimal.ONE, SESSION_KEY, IP_ADDRESS, REFERRER, PLATFORM_WEB, null, CLIENT_CONTEXT);
    }



}
