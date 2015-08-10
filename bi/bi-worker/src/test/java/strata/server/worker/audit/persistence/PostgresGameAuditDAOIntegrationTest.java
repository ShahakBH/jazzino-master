package strata.server.worker.audit.persistence;

import com.yazino.platform.audit.message.GameAudit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import utils.PostgresTestValueHelper;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@Transactional("externalDwTransactionManager")
@DirtiesContext
public class PostgresGameAuditDAOIntegrationTest {

    private static final String AUDIT_LABEL = "anAuditLabelForJDBCGameAuditDAOTest_1";
    private static final int PLAYER_1 = 401;
    private static final int PLAYER_2 = 402;

    private static final Logger LOG = LoggerFactory.getLogger(PostgresGameAuditDAOIntegrationTest.class);
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresGameAuditDAO underTest;

    @Before
    public void cleanUp() {
        jdbc.update("DELETE FROM AUDIT_CLOSED_GAME WHERE AUDIT_LABEL=?", AUDIT_LABEL);
        jdbc.update("DELETE FROM AUDIT_CLOSED_GAME WHERE AUDIT_LABEL=?", "MASSIVE_LABEL");
        jdbc.update("DELETE FROM AUDIT_CLOSED_GAME_PLAYER WHERE AUDIT_LABEL=?", AUDIT_LABEL);
        jdbc.update("DELETE FROM lobby_user WHERE player_id IN(?,?)", PLAYER_1, PLAYER_2);
        PostgresTestValueHelper.createAPlayer(jdbc, PLAYER_1);
        PostgresTestValueHelper.createAPlayer(jdbc, PLAYER_2);

    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresGameAuditDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(asList(aGameAudit(AUDIT_LABEL)));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresGameAuditDAO(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.anyString(), Mockito.<BatchPreparedStatementSetter>any())).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(asList(aGameAudit(AUDIT_LABEL)));
    }

    @Test(expected = NullPointerException.class)
    public void nullGameAuditsThrowANullPointerException() {
        underTest.saveAll(null);
    }

    @Test
    public void aGameAuditIsSavedToTheDatabase() {
        underTest.saveAll(asList(aGameAudit(AUDIT_LABEL)));

        verifyGameMatches(aGameAudit(AUDIT_LABEL), readGameRecord(AUDIT_LABEL));
        verifyPlayerMatches(PLAYER_1, aGameAudit(AUDIT_LABEL), readPlayerRecordForId(PLAYER_1, AUDIT_LABEL));
        verifyPlayerMatches(PLAYER_2, aGameAudit(AUDIT_LABEL), readPlayerRecordForId(PLAYER_2, AUDIT_LABEL));
    }

    @Test
    public void aGameAuditWithNoPlayersIsSavedToTheDatabase() {
        final GameAudit gameAudit = aGameAudit(AUDIT_LABEL);
        gameAudit.setPlayerIds(Collections.<BigDecimal>emptySet());
        underTest.saveAll(asList(gameAudit));

        verifyGameMatches(gameAudit, readGameRecord(AUDIT_LABEL));
    }

    @Test
    public void multipleGameAuditsAreSavedToTheDatabase() {
        underTest.saveAll(asList(aGameAudit(AUDIT_LABEL), aGameAudit("auditLabel1"), aGameAudit("auditLabel2")));

        verifyGameMatches(aGameAudit(AUDIT_LABEL), readGameRecord(AUDIT_LABEL));
        verifyPlayerMatches(PLAYER_1, aGameAudit(AUDIT_LABEL), readPlayerRecordForId(PLAYER_1, AUDIT_LABEL));
        verifyPlayerMatches(PLAYER_2, aGameAudit(AUDIT_LABEL), readPlayerRecordForId(PLAYER_2, AUDIT_LABEL));
        verifyGameMatches(aGameAudit("auditLabel1"), readGameRecord("auditLabel1"));
        verifyPlayerMatches(PLAYER_1, aGameAudit("auditLabel1"), readPlayerRecordForId(PLAYER_1, "auditLabel1"));
        verifyPlayerMatches(PLAYER_2, aGameAudit("auditLabel1"), readPlayerRecordForId(PLAYER_2, "auditLabel1"));
        verifyGameMatches(aGameAudit("auditLabel2"), readGameRecord("auditLabel2"));
        verifyPlayerMatches(PLAYER_1, aGameAudit("auditLabel2"), readPlayerRecordForId(PLAYER_1, "auditLabel2"));
        verifyPlayerMatches(PLAYER_2, aGameAudit("auditLabel2"), readPlayerRecordForId(PLAYER_2, "auditLabel2"));
    }

    private void verifyGameMatches(final GameAudit gameAudit, final Map<String, Object> record) {
        assertThat(record.get("AUDIT_LABEL").toString(), is(equalTo(gameAudit.getAuditLabel())));
        assertThat(record.get("HOSTNAME").toString(), is(equalTo(gameAudit.getHostname())));
        assertThat((Timestamp) record.get("AUDIT_TS"), is(equalTo(new Timestamp(gameAudit.getTimeStamp().getTime()))));
        assertThat(((BigDecimal) record.get("TABLE_ID")), is(comparesEqualTo(gameAudit.getTableId())));
        assertThat((BigDecimal) record.get("GAME_ID"), is(comparesEqualTo(BigDecimal.valueOf(gameAudit.getGameId()))));
        assertThat((Integer) record.get("GAME_INCREMENT"), is(equalTo(gameAudit.getIncrement().intValue())));
        assertThat(record.get("OBSERVABLE_STATUS").toString(), is(equalTo(gameAudit.getObservableStatusXml())));
        assertThat(record.get("INTERNAL_STATUS").toString(), is(equalTo(gameAudit.getInternalStatusXml())));
    }

    private void verifyPlayerMatches(final long playerId, final GameAudit gameAudit, final Map<String, Object> record) {
        assertThat(record.get("AUDIT_LABEL").toString(), is(equalTo(gameAudit.getAuditLabel())));
        assertThat(record.get("HOSTNAME").toString(), is(equalTo(gameAudit.getHostname())));
        assertThat((Timestamp) record.get("AUDIT_TS"), is(equalTo(new Timestamp(gameAudit.getTimeStamp().getTime()))));
        assertThat(((BigDecimal) record.get("TABLE_ID")).setScale(0), is(equalTo(gameAudit.getTableId())));
        assertThat(((BigDecimal) record.get("GAME_ID")).setScale(0),
                is(equalTo(BigDecimal.valueOf(gameAudit.getGameId()))));
        assertThat((Long) record.get("PLAYER_ID"), is(equalTo(playerId)));
    }

    private Map<String, Object> readGameRecord(final String auditLabel) {
        return jdbc.queryForMap("SELECT * FROM AUDIT_CLOSED_GAME WHERE AUDIT_LABEL=?", auditLabel);
    }

    private Map<String, Object> readPlayerRecordForId(final int playerId, final String auditLabel) {
        return jdbc.queryForMap("SELECT * FROM AUDIT_CLOSED_GAME_PLAYER WHERE PLAYER_ID=? AND AUDIT_LABEL=?",
                playerId, auditLabel);
    }

    private GameAudit aGameAudit(final String auditLabel) {
        return new GameAudit(auditLabel, "aHostname", new Date(1000000), BigDecimal.valueOf(100), 200L, 300L,
                "anObservableStatus", "anInternalStatus", newHashSet(BigDecimal.valueOf(PLAYER_1),
                BigDecimal.valueOf(PLAYER_2)));
    }

}
