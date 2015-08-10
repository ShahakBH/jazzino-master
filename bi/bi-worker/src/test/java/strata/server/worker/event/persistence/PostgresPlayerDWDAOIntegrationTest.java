package strata.server.worker.event.persistence;

import com.yazino.platform.event.message.PlayerEvent;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static utils.PostgresTestValueHelper.createAPlayer;
import static utils.PostgresTestValueHelper.createAnAccount;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
@Transactional
public class PostgresPlayerDWDAOIntegrationTest {

    private static final BigDecimal ID_1 = BigDecimal.valueOf(-10);
    private static final BigDecimal ID_2 = BigDecimal.valueOf(-20);
    private static final DateTime DEFAULT_TS_CREATED = new DateTime(2012, 2, 14, 11, 29, 30, 0);
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(22);

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;
    @Autowired
    private PostgresPlayerDWDAO underTest;

    @Before
    public void cleanUp() {
//        jdbc.update("delete from audit_command where player_id in (?, ?)", ID_1, ID_2);
        jdbc.update("delete from lobby_user where player_id in (?, ?)", ID_1, ID_2);
        createAnAccount(jdbc, ACCOUNT_ID);
        createAPlayer(jdbc,ID_1,ID_2);
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresPlayerDWDAO(mockTemplate);
        when(mockTemplate.batchUpdate(any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(newArrayList(aPlayerEvent(ID_1)));
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresPlayerDWDAO(mockTemplate);
        when(mockTemplate.update(Mockito.anyString(), Mockito.<PreparedStatementSetter>any())).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(newArrayList(aPlayerEvent(ID_1)));
    }

    @Test
    public void aPlayerEventIsSavedToTheDatabase() {
        underTest.saveAll(newArrayList(aPlayerEvent(ID_1)));

        verifyRecordMatches(aPlayerEvent(ID_1), readRecordByAccountId(ID_1));
    }

    @Test
    public void multipleAccountEventsMayBeSavedToTheDatabase() {
        underTest.saveAll(newArrayList(aPlayerEvent(ID_1)));
        underTest.saveAll(newArrayList(aPlayerEvent(ID_2)));

        verifyRecordMatches(aPlayerEvent(ID_1), readRecordByAccountId(ID_1));
        verifyRecordMatches(aPlayerEvent(ID_2), readRecordByAccountId(ID_2));
    }

    private PlayerEvent aPlayerEvent(final BigDecimal id) {
        return new PlayerEvent(id,
                DEFAULT_TS_CREATED,
                ACCOUNT_ID,
                Collections.<String>emptySet()
        );
    }

    private void verifyRecordMatches(final PlayerEvent playerEvent, final Map<String, Object> record) {
        assertThat(new BigDecimal(record.get("player_id").toString()), is(comparesEqualTo(playerEvent.getPlayerId())));
        assertThat(new DateTime(((Timestamp) record.get("reg_ts")).getTime()), is(equalTo(playerEvent.getTsCreated())));
        assertThat(new BigDecimal(record.get("account_id").toString()), is(comparesEqualTo(playerEvent.getAccountId())));
    }

    private Map<String, Object> readRecordByAccountId(final BigDecimal playerId) {
        return jdbc.queryForMap("select * from lobby_user where player_id = ?", playerId);
    }
}

