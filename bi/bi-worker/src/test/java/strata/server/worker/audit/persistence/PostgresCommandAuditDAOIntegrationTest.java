package strata.server.worker.audit.persistence;

import com.yazino.logging.appender.ListAppender;
import com.yazino.platform.audit.message.CommandAudit;
import org.hamcrest.Matcher;
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

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static utils.PostgresTestValueHelper.createAPlayer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
@Transactional
public class PostgresCommandAuditDAOIntegrationTest {

    private static final String UUID_1 = "aUuidForJDBCCommandAuditDAOTest_1";
    private static final String UUID_2 = "aUuidForJDBCCommandAuditDAOTest_2";
    private static final int PLAYER_ID = 300;

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresCommandAuditDAO underTest;

    @Before
    public void cleanUp() {
        jdbc.update("DELETE FROM AUDIT_COMMAND WHERE PLAYER_ID=" + PLAYER_ID);
        jdbc.update("DELETE FROM AUDIT_COMMAND WHERE COMMAND_ID IN (?,?)", UUID_1, UUID_2);
        createAPlayer(jdbc, PLAYER_ID);
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresCommandAuditDAO(mockTemplate);

        when(mockTemplate.batchUpdate(new String[]{anyString()})).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(asList(aCommandAudit(UUID_1)));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresCommandAuditDAO(mockTemplate);
        when(mockTemplate.update(Mockito.anyString())).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(asList(aCommandAudit(UUID_1)));
    }

    @Test
    public void anEmptyListIsIgnored() {
        final int preRecordCount = jdbc.queryForInt("SELECT COUNT(*) FROM AUDIT_COMMAND");

        underTest.saveAll(new ArrayList<CommandAudit>());

        final int postRecordCount = jdbc.queryForInt("SELECT COUNT(*) FROM AUDIT_COMMAND");
        assertThat(postRecordCount, is(equalTo(preRecordCount)));
    }

    @Test
    public void aCommandAuditIsSavedToTheDatabase() {
        underTest.saveAll(asList(aCommandAudit(UUID_1)));

        verifyRecordMatches(aCommandAudit(UUID_1), readRecordByUUID(UUID_1));
    }

    @Test
    public void multipleCommandAuditsMayBeSavedToTheDatabase() {
        underTest.saveAll(asList(aCommandAudit(UUID_1), aCommandAudit(UUID_2)));

        verifyRecordMatches(aCommandAudit(UUID_1), readRecordByUUID(UUID_1));
        verifyRecordMatches(aCommandAudit(UUID_2), readRecordByUUID(UUID_2));
    }

    private void verifyRecordMatches(final CommandAudit commandAudit, final Map<String, Object> record) {
        assertThat(record.get("AUDIT_LABEL").toString(), is(equalTo(commandAudit.getAuditLabel())));
        assertThat(record.get("HOSTNAME").toString(), is(equalTo(commandAudit.getHostname())));
        assertThat((Timestamp) record.get("AUDIT_TS"), is(equalTo(new Timestamp(commandAudit.getTimeStamp().getTime()))));
        assertThat((new BigDecimal(record.get("TABLE_ID").toString())), is(comparesEqualTo(commandAudit.getTableId())));
        assertThat(new BigDecimal(record.get("GAME_ID").toString()), is(comparesEqualTo(BigDecimal.valueOf(commandAudit.getGameId()))));
        assertThat((new BigDecimal(record.get("PLAYER_ID").toString())), is(comparesEqualTo(commandAudit.getPlayerId())));
        assertThat(record.get("COMMAND_TYPE").toString(), is(equalTo(commandAudit.getType())));
        assertThat(record.get("COMMAND_ARGS").toString(), is(equalTo("[arg1, arg2]")));
    }

    private Map<String, Object> readRecordByUUID(final String uuid) {
        return jdbc.queryForMap("SELECT * FROM AUDIT_COMMAND WHERE COMMAND_ID=?", uuid);
    }

    private CommandAudit aCommandAudit(final String uuid) {
        return new CommandAudit("anAuditLabelFor" + uuid, "aHostname", new Date(1000000000000L), BigDecimal.valueOf(100),
                200L, "aCommandType", new String[]{"arg1", "arg2"}, BigDecimal.valueOf(PLAYER_ID), uuid);
    }

}
