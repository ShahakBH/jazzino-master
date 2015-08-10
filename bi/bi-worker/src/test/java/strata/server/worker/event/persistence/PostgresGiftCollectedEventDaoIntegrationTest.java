package strata.server.worker.event.persistence;

import com.yazino.platform.event.message.GiftCollectedEvent;
import com.yazino.platform.gifting.CollectChoice;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.yazino.platform.gifting.CollectChoice.GAMBLE;
import static com.yazino.platform.gifting.CollectChoice.TAKE_MONEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
@TransactionConfiguration
@Transactional
public class PostgresGiftCollectedEventDaoIntegrationTest {

    public static final BigDecimal PLAYER_ONE = BigDecimal.ONE;
    public static final BigDecimal PLAYER_TEN = BigDecimal.TEN;
    public static final String SELECT_FROM_GIFTS_COLLECTED = "SELECT * from GIFTS_COLLECTED order by GIFT_ID";
    public static final BigDecimal SESSION_ID = new BigDecimal(1231234142l);
    public static final DateTime COLLECT_TS = new DateTime(1000l);
    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresGiftCollectedEventDao underTest;


    @Before
    public void setup() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(1000l);
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = CannotGetJdbcConnectionException.class)
    public void connectionProblemsPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresGiftCollectedEventDao(mockTemplate);

        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(newArrayList(aGiftCollectedEvent(23l, GAMBLE, new BigDecimal(1000))));
    }

    private GiftCollectedEvent aGiftCollectedEvent(final long giftId, final CollectChoice choice, final BigDecimal amount) {
        return new GiftCollectedEvent(BigDecimal.valueOf(giftId), choice, amount, SESSION_ID, COLLECT_TS);
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresGiftCollectedEventDao(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(newArrayList(aGiftCollectedEvent(23l, GAMBLE, PLAYER_TEN)));
    }

    @Test
    public void shouldSaveGiftCollectedEvent() {
        underTest.saveAll(newArrayList(aGiftCollectedEvent(23l, GAMBLE, PLAYER_TEN)));

        final List<GiftCollectedEvent> actualGiftsCollected = jdbc.query(SELECT_FROM_GIFTS_COLLECTED, getRowMapper());

        GiftCollectedEvent expected = aGiftCollectedEvent(23l, GAMBLE, PLAYER_TEN);
        GiftCollectedEvent actualGiftCollected = actualGiftsCollected.iterator().next();

        assertEquals(expected, actualGiftCollected);
    }

    @Test
    public void shouldSaveMultipleGiftCollectedEvents() {
        underTest.saveAll(newArrayList(
                aGiftCollectedEvent(23l, GAMBLE, PLAYER_TEN),
                aGiftCollectedEvent(25l, TAKE_MONEY, PLAYER_ONE))
        );

        final List<GiftCollectedEvent> actualGiftsCollected = jdbc.query(SELECT_FROM_GIFTS_COLLECTED, getRowMapper());

        Assert.assertThat(actualGiftsCollected, hasItems(aGiftCollectedEvent(23l, GAMBLE, PLAYER_TEN), aGiftCollectedEvent(25l, TAKE_MONEY, PLAYER_ONE)));
    }

    private RowMapper<GiftCollectedEvent> getRowMapper() {
        return new RowMapper<GiftCollectedEvent>() {
            @Override
            public GiftCollectedEvent mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final BigDecimal giftId = rs.getBigDecimal("gift_id");
                final String choice = rs.getString("choice");
                final BigDecimal amount = rs.getBigDecimal("amount");
                final BigDecimal sessionId = rs.getBigDecimal("SESSION_ID");
                final DateTime collectTs = new DateTime(rs.getTimestamp("COLLECTED_TS"));

                return new GiftCollectedEvent(giftId, CollectChoice.valueOf(choice), amount, sessionId, collectTs);

            }
        };
    }

    public DateTime getDateTimeWrapper(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        } else {
            return new DateTime(timestamp.getTime());
        }

    }

    private void assertEquals(final GiftCollectedEvent expected, final GiftCollectedEvent actualGiftCollected) {
        assertThat(actualGiftCollected.getGiftId(), is(equalTo(expected.getGiftId())));
        assertThat(actualGiftCollected.getChoice(), is(equalTo(expected.getChoice())));
        assertThat(actualGiftCollected.getGiftAmount().setScale(2), is(equalTo(expected.getGiftAmount().setScale(2))));
        assertThat(actualGiftCollected.getSessionId(), is(equalTo(expected.getSessionId())));
        assertThat(actualGiftCollected.getCollectTs(), is(equalTo(expected.getCollectTs())));

    }
}
