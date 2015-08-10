package strata.server.worker.event.persistence;

import com.yazino.platform.event.message.BonusCollectedEvent;
import com.yazino.test.ThreadLocalDateTimeUtils;
import com.yazino.yaps.JsonHelper;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Assert;
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
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static java.math.BigDecimal.valueOf;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
@TransactionConfiguration
@Transactional
public class PostgresBonusCollectedEventDaoIntegrationTest {

    @Before
    public void setup() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(now().withSecondOfMinute(30).getMillis());
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresBonusCollectedEventDao underTest;

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresBonusCollectedEventDao(mockTemplate);

        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(newArrayList(aBonusCollectedEvent(23l, now())));
    }

    private BonusCollectedEvent aBonusCollectedEvent(final long playerId, final DateTime collected) {
        return new BonusCollectedEvent(new BigDecimal(playerId), collected);
    }

    @Test
    public void eventShouldSerialize() {
        final BonusCollectedEvent bonusCollectedEvent = new BonusCollectedEvent(BigDecimal.ONE, now(DateTimeZone.UTC).minusYears(1));
        JsonHelper jsonHelper = new JsonHelper(true);
        final String serialized = jsonHelper.serialize(bonusCollectedEvent);
        System.out.println(serialized);
        final BonusCollectedEvent deserialized = jsonHelper.deserialize(BonusCollectedEvent.class, serialized);
        Assert.assertThat(bonusCollectedEvent, CoreMatchers.is(IsEqual.equalTo(deserialized)));
    }

    @Test
    public void saveAllShouldSaveEvent() {
        underTest.saveAll(newArrayList(aBonusCollectedEvent(1, now()), aBonusCollectedEvent(2, now().minusDays(1))));
        assertPlayersCollectedEventIs(1, now());
        assertPlayersCollectedEventIs(2, now().minusDays(1));
    }

    @Test
    public void saveAllShouldUpdateEvent() {
        underTest.saveAll(newArrayList(aBonusCollectedEvent(1, now())));
        underTest.saveAll(newArrayList(aBonusCollectedEvent(1, now().minusDays(1))));
        assertPlayersCollectedEventIs(1, now().minusDays(1));
    }

    @Test
    public void saveAllShouldUpdateAndSaveEvent() {
        underTest.saveAll(newArrayList(aBonusCollectedEvent(1, now())));
        underTest.saveAll(newArrayList(aBonusCollectedEvent(1, now().minusDays(1)), aBonusCollectedEvent(2, now().minusDays(1))));
        assertPlayersCollectedEventIs(1, now().minusDays(1));
        assertPlayersCollectedEventIs(2, now().minusDays(1));

    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresBonusCollectedEventDao(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(asList(aBonusCollectedEvent(1, now())));
    }

    @Test
    public void bonusTimeoutNotifierQueryShouldSelectPeopleCorrectly() {
        final List<BonusCollectedEvent> events = of(
                aBonusCollectedEvent(1, now()),//30secs past
                aBonusCollectedEvent(2, now().minusSeconds(30)),//on minute
                aBonusCollectedEvent(3, now().minusSeconds(62)),//in prev minute
                aBonusCollectedEvent(4, now().minusMinutes(2)),
                aBonusCollectedEvent(5, now().minusMinutes(6)),
                aBonusCollectedEvent(6, now().minusMinutes(6).withSecondOfMinute(0)),//at start of 6th minute
                aBonusCollectedEvent(7, now().minusMinutes(7).withSecondOfMinute(59))//at end of 7th minute agio
        );
        underTest.saveAll(events);

        final List<Map<String, Object>> playerIds = jdbc.queryForList("select * from lockout_bonus order by player_id");

        final List<Map<String, Object>> filteredPlayerIds = jdbc.queryForList(
                "select player_id from lockout_bonus " +
                        "where last_bonus_ts >= date_trunc('minute',current_timestamp-interval'0 HOURS 6 minutes') " +
                        "and last_bonus_ts < date_trunc('minute',current_timestamp-interval'0 HOURS 1 minute') " +
                        "order by player_id"
        );
        assertThat(filteredPlayerIds.size(), is(3));
        assertThat((BigDecimal) filteredPlayerIds.get(0).get("player_id"), comparesEqualTo(valueOf(4)));
    }


    private void assertPlayersCollectedEventIs(final int playerId, final DateTime now) {
        Map<String, Object> lockoutBonus = jdbc.queryForMap("select * from LOCKOUT_BONUS where player_id=" + playerId);
        assertThat((Timestamp) lockoutBonus.get("LAST_BONUS_TS"), equalTo(new Timestamp(now.getMillis())));
    }
}