package strata.server.worker.event.persistence;

import com.yazino.platform.event.message.GiftSentEvent;
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
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
@TransactionConfiguration
@Transactional
public class PostgresGiftSentEventDaoIntegrationTest {

    public static final BigDecimal PLAYER_ONE = BigDecimal.ONE;
    public static final BigDecimal PLAYER_TEN = BigDecimal.TEN;
    public static final BigDecimal SESSION_ID = new BigDecimal(12312412443l);
    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresGiftSentEventDao underTest;


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
        underTest = new PostgresGiftSentEventDao(mockTemplate);

        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new CannotGetJdbcConnectionException("aTestException", new SQLException()));

        underTest.saveAll(newArrayList(aGiftSentEvent(23l, PLAYER_ONE, PLAYER_TEN, SESSION_ID)));
    }

    private GiftSentEvent aGiftSentEvent(final long giftId, final BigDecimal sender, final BigDecimal receiver, final BigDecimal sessionId) {
        return new GiftSentEvent(BigDecimal.valueOf(giftId), sender, receiver, new DateTime().plusHours(2), new DateTime(), sessionId);
    }

    @Test
    public void exceptionsThatAreNotConnectionProblemsDoNotPropagateTheException() {
        final JdbcTemplate mockTemplate = mock(JdbcTemplate.class);
        underTest = new PostgresGiftSentEventDao(mockTemplate);
        when(mockTemplate.batchUpdate(Mockito.any(String[].class))).thenThrow(
                new BadSqlGrammarException("aTestException", "someSql", new SQLException()));

        underTest.saveAll(newArrayList(aGiftSentEvent(23l, PLAYER_ONE, PLAYER_TEN, SESSION_ID)));
    }

    @Test
    public void shouldSaveGiftSentEvent() {
        underTest.saveAll(newArrayList(aGiftSentEvent(23l, PLAYER_ONE, PLAYER_TEN, SESSION_ID)));

        final List<GiftSentEvent> actualGiftsSent = jdbc.query("SELECT * from GIFTS_SENT", new RowMapper<GiftSentEvent>() {
            @Override
            public GiftSentEvent mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final BigDecimal giftId = rs.getBigDecimal("gift_id");
                final BigDecimal senderId = rs.getBigDecimal("SENDER_ID");
                final BigDecimal receiverId = rs.getBigDecimal("RECEIVER_ID");
                final DateTime expiry = getDateTimeWrapper(rs.getTimestamp("EXPIRY_TS"));
                final DateTime sent = getDateTimeWrapper(rs.getTimestamp("SENT_TS"));
                final BigDecimal sessionId = rs.getBigDecimal("SESSION_ID");

                return new GiftSentEvent(giftId, senderId, receiverId, expiry, sent, sessionId);
            }
        });

        Assert.assertThat(actualGiftsSent, hasItem(aGiftSentEvent(23l, PLAYER_ONE, PLAYER_TEN, SESSION_ID)));
    }

    @Test
    public void shouldSaveMultipleGiftSentEvents() {
        underTest.saveAll(newArrayList(
                        aGiftSentEvent(23l, PLAYER_ONE, PLAYER_TEN, SESSION_ID),
                        aGiftSentEvent(25l, PLAYER_TEN, PLAYER_ONE, SESSION_ID))
        );

        final List<GiftSentEvent> actualGiftsSent = jdbc.query("SELECT * from GIFTS_SENT", new RowMapper<GiftSentEvent>() {
            @Override
            public GiftSentEvent mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final BigDecimal giftId = rs.getBigDecimal("gift_id");
                final BigDecimal senderId = rs.getBigDecimal("SENDER_ID");
                final BigDecimal receiverId = rs.getBigDecimal("RECEIVER_ID");
                final DateTime expiry = getDateTimeWrapper(rs.getTimestamp("EXPIRY_TS"));
                final DateTime sent = getDateTimeWrapper(rs.getTimestamp("SENT_TS"));
                final BigDecimal sessionId = rs.getBigDecimal("SESSION_ID");

                return new GiftSentEvent(giftId, senderId, receiverId, expiry, sent, SESSION_ID);
            }
        });

        Assert.assertThat(actualGiftsSent, hasItems(aGiftSentEvent(23l, PLAYER_ONE, PLAYER_TEN, SESSION_ID), aGiftSentEvent(25l, PLAYER_TEN, PLAYER_ONE, SESSION_ID)));
    }

    public DateTime getDateTimeWrapper(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        } else {
            return new DateTime(timestamp.getTime());
        }

    }
}
