package strata.server.worker.audit.persistence;

import com.yazino.platform.audit.message.SessionKey;
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

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static utils.PostgresTestValueHelper.createAnAccount;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class PostgresSessionKeyDAOIntegrationTest {
    private static final BigDecimal ACCOUNT_ID = BigDecimal.TEN;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(-3141592L);
    private static final String SESSION_KEY = "aSessionKey";
    private static final String REFERRER = "aReferrer";
    private static final String PLATFORM = "FACEBOOK";
    private static final String PLATFORM_WEB = "WEB";
    private static final String IP_ADDRESS = "127.0.0.1";
    public static final String LOGIN_URL = "http://loginUrl";
    private static final String SELECT_BY_ACCOUNT_AND_SESSION = "SELECT * FROM ACCOUNT_SESSION WHERE ACCOUNT_ID=? AND SESSION_ID=?";
    public static final HashMap<String,Object> EMPTY_CLIENT_CONTEXT = new HashMap<String, Object>();

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresSessionKeyDAO underTest;

    @Before
    public void cleanUp() {
        jdbc.update("DELETE FROM ACCOUNT_SESSION");
        createAnAccount(jdbc, ACCOUNT_ID);
    }

    @SuppressWarnings("NullableProblems")
    @Test(expected = NullPointerException.class)
    public void aNullSessionKeyCausesANullPointerException() {
        underTest.saveAll(null);
    }

    @SuppressWarnings("NullableProblems")
    @Test
    public void aSessionKeyIsIgnoredIfTheAccountIdIsNull() {
        jdbc = mock(JdbcTemplate.class);
        underTest = new PostgresSessionKeyDAO(jdbc);

        final SessionKey sessionKey = aSessionKey();
        sessionKey.setAccountId(null);

        underTest.saveAll(asList(sessionKey));

        verifyZeroInteractions(jdbc);
    }

    @SuppressWarnings("NullableProblems")
    @Test
    public void aSessionKeyIsIgnoredIfTheSessionKeyIsNull() {
        jdbc = mock(JdbcTemplate.class);
        underTest = new PostgresSessionKeyDAO(jdbc);

        final SessionKey sessionKey = aSessionKey();
        sessionKey.setSessionKey(null);

        underTest.saveAll(asList(sessionKey));

        verifyZeroInteractions(jdbc);
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        jdbc = mock(JdbcTemplate.class);
        underTest = new PostgresSessionKeyDAO(jdbc);

        when(jdbc.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(asList(aSessionKey()));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        jdbc = mock(JdbcTemplate.class);
        underTest = new PostgresSessionKeyDAO(jdbc);

        when(jdbc.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(asList(aSessionKey()));
    }

    @Test
    public void aSessionKeyIsWrittenToTheDatabase() {
        underTest.saveAll(asList(aSessionKey()));

        verifyDefaultSessionFor(SESSION_KEY);
    }

    @Test
    public void multipleSessionKeysAreWrittenToTheDatabase() {
        underTest.saveAll(asList(aSessionKeyFor("session1"), aSessionKeyFor("session2")));

        verifyDefaultSessionFor("session1");
        verifyDefaultSessionFor("session2");
    }

    private void verifyDefaultSessionFor(final String sessionKey) {
        final Map<String, Object> record = readRecord(ACCOUNT_ID, BigDecimal.valueOf(sessionKey.hashCode()));
        assertThat(record.get("IP_ADDRESS").toString(), is(equalTo(IP_ADDRESS)));
        assertThat(record.get("REFERER").toString(), is(equalTo(REFERRER)));
        assertThat(record.get("PLATFORM").toString(), is(PLATFORM));
        assertThat(record.get("SESSION_KEY").toString(), is(sessionKey));
        assertThat(record.get("START_PAGE").toString(), is(LOGIN_URL));
    }

    @Test
    public void aSessionKeyWithALongReferrerWritesATruncatedReferrerToTheDatabase() {
        final SessionKey sessionKey = aSessionKey();
        sessionKey.setReferrer(aReferrerOfLength(4123));

        underTest.saveAll(asList(sessionKey));

        final Map<String, Object> record = readRecord(ACCOUNT_ID, BigDecimal.valueOf(SESSION_KEY.hashCode()));
        assertThat(record.get("IP_ADDRESS").toString(), is(equalTo(IP_ADDRESS)));
        assertThat(record.get("REFERER").toString(), is(equalTo(aReferrerOfLength(2048))));
        assertThat(record.get("PLATFORM").toString(), is(PLATFORM));
        assertThat(record.get("SESSION_KEY").toString(), is(SESSION_KEY));
        assertThat(record.get("START_PAGE").toString(), is(LOGIN_URL));
    }

    @Test
    public void aSessionKeyForYazinoWebIsWrittenToTheDatabase() {
        underTest.saveAll(asList(aSessionKeyYazino()));

        final Map<String, Object> record = readRecord(ACCOUNT_ID, SESSION_ID);
        assertThat(record.get("IP_ADDRESS").toString(), is(equalTo(IP_ADDRESS)));
        assertThat(record.get("REFERER").toString(), is(equalTo(REFERRER)));
        assertThat(record.get("PLATFORM").toString(), is(PLATFORM_WEB));
        assertThat(record.get("SESSION_KEY").toString(), is(SESSION_KEY));
        assertThat(record.get("START_PAGE").toString(), is(LOGIN_URL));
    }

    @Test
    public void aSessionKeyForYazinoWebNoUrlIsWrittenToTheDatabase() {
        underTest.saveAll(asList(aSessionKeyWithNoURL()));

        final Map<String, Object> record = readRecord(ACCOUNT_ID, SESSION_ID);
        assertThat(record.get("IP_ADDRESS").toString(), is(equalTo(IP_ADDRESS)));
        assertThat(record.get("REFERER").toString(), is(equalTo(REFERRER)));
        assertThat(record.get("PLATFORM").toString(), is(PLATFORM_WEB));
        assertThat(record.get("SESSION_KEY").toString(), is(SESSION_KEY));
        assertNull(record.get("START_PAGE"));
    }


    private Map<String, Object> readRecord(final BigDecimal accountId, final BigDecimal sessionId) {
        return jdbc.queryForMap(SELECT_BY_ACCOUNT_AND_SESSION, accountId, sessionId);
    }

    private SessionKey aSessionKey() {
        return aSessionKeyFor(SESSION_KEY);
    }

    private SessionKey aSessionKeyFor(final String sessionKey) {
        return new SessionKey(BigDecimal.valueOf(sessionKey.hashCode()), ACCOUNT_ID, BigDecimal.ONE, sessionKey, IP_ADDRESS, REFERRER, PLATFORM, LOGIN_URL, EMPTY_CLIENT_CONTEXT);
    }

    private SessionKey aSessionKeyYazino() {
        return new SessionKey(SESSION_ID, ACCOUNT_ID, BigDecimal.ONE, SESSION_KEY, IP_ADDRESS, REFERRER, PLATFORM_WEB, LOGIN_URL, EMPTY_CLIENT_CONTEXT);
    }

    private SessionKey aSessionKeyWithNoURL() {
        return new SessionKey(SESSION_ID, ACCOUNT_ID, BigDecimal.ONE, SESSION_KEY, IP_ADDRESS, REFERRER, PLATFORM_WEB, null, EMPTY_CLIENT_CONTEXT);
    }

    private String aReferrerOfLength(final int length) {
        final StringBuilder referrer = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            referrer.append((char) (97 + (i % 26)));
        }
        return referrer.toString();
    }
}
