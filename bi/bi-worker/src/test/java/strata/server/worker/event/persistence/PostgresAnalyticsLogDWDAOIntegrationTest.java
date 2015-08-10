package strata.server.worker.event.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yazino.analytics.AnalyticsEntry;
import com.yazino.client.log.ClientLogEvent;
import com.yazino.client.log.ClientLogEventMessageType;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
@Transactional
public class PostgresAnalyticsLogDWDAOIntegrationTest {
    public static final String TYPE = "AnalyticsService";
    public static final String SESSION_ID = "550C5D08-7D4C-6127-23E2-D5AC5EA0A9DF";
    public static final String LABEL = "label value";
    public static final String VALUE = "value of value";
    public static final String ACTION = "created";
    public static final int EVENT_ID = 1;
    public static final int DELTA = 160;
    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresAnalyticsLogDWDAO underTest;
    private ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(20000);
        mapper = new ObjectMapper();
    }

    @After
    public void tearDown() throws Exception {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void saveAllShouldPersistSingleRecordToDatabase() throws Exception {

        AnalyticsEntry entry1 = new AnalyticsEntry(EVENT_ID, TYPE, SESSION_ID, LABEL, VALUE, ACTION, DELTA);
        final List<AnalyticsEntry> analyticEntries = newArrayList(entry1);
        final ClientLogEvent event = new ClientLogEvent(new DateTime(), generateJsonPayload(analyticEntries), ClientLogEventMessageType.LOG_ANALYTICS);

        underTest.saveAll(newArrayList(event));

        final AnalyticsEntry actual = readRecord(new DateTime(), DELTA);
        assertThat(actual, equalTo(entry1));
    }

    @Test
    public void saveAllShouldPersistSingleRecordToDatabaseIgnoringUnknownProperties() throws Exception {

        String events = "[{\"thispropertyisunknow\":56,\"eventId\":1,\"type\":\"AnalyticsService\",\"sessionId\":\"550C5D08-7D4C-6127-23E2-D5AC5EA0A9DF\",\"label\":\"label value\",\"value\":\"value of value\",\"action\":\"created\",\"delta\":160}]";
        final ClientLogEvent event = new ClientLogEvent(new DateTime(), events, ClientLogEventMessageType.LOG_ANALYTICS);

        underTest.saveAll(newArrayList(event));

        final AnalyticsEntry actual = readRecord(new DateTime(), DELTA);
        assertThat(actual, equalTo(new AnalyticsEntry(EVENT_ID, TYPE, SESSION_ID, LABEL, VALUE, ACTION, DELTA)));
    }

    @Test
    public void saveAllShouldPersistSingleRecordWithTwoJsonObjectsToDatabase() throws Exception {
        AnalyticsEntry entry1 = new AnalyticsEntry(EVENT_ID, TYPE, SESSION_ID, LABEL, VALUE, ACTION, DELTA);
        AnalyticsEntry entry2 = new AnalyticsEntry(EVENT_ID + 1, TYPE, SESSION_ID, LABEL, VALUE, ACTION, DELTA + 100);
        final List<AnalyticsEntry> analyticEntries = newArrayList(entry1, entry2);

        final ClientLogEvent event = new ClientLogEvent(new DateTime(), generateJsonPayload(analyticEntries), ClientLogEventMessageType.LOG_ANALYTICS);

        underTest.saveAll(newArrayList(event));

        assertThat(readRecord(new DateTime(), DELTA), equalTo(entry1));
        assertThat(readRecord(new DateTime(), DELTA + 100), equalTo(entry2));
    }

    @Test
    public void saveAllShouldPersistMultipleRecordsToDatabase() throws Exception {

        AnalyticsEntry analytic = new AnalyticsEntry(EVENT_ID, TYPE, SESSION_ID, LABEL, VALUE, ACTION, DELTA);
        AnalyticsEntry analytic2 = new AnalyticsEntry(EVENT_ID + 1, TYPE, SESSION_ID, LABEL, VALUE, ACTION, DELTA + 100);
        AnalyticsEntry analytic3 = new AnalyticsEntry(EVENT_ID + 2, TYPE, SESSION_ID, LABEL, VALUE, ACTION, DELTA + 500);

        List<AnalyticsEntry> logEventCtx = newArrayList(analytic, analytic2);
        List<AnalyticsEntry> logEventCtx2 = newArrayList(analytic3);

        ClientLogEvent clientLogEvent = new ClientLogEvent(new DateTime(), generateJsonPayload(logEventCtx), ClientLogEventMessageType.LOG_ANALYTICS);
        ClientLogEvent clientLogEvent2 = new ClientLogEvent(new DateTime().plusHours(1), generateJsonPayload(logEventCtx2), ClientLogEventMessageType.LOG_ANALYTICS);

        underTest.saveAll(newArrayList(clientLogEvent, clientLogEvent2));

        assertThat(readRecord(new DateTime(), DELTA), equalTo(analytic));
        assertThat(readRecord(new DateTime(), DELTA + 100), equalTo(analytic2));
        assertThat(readRecord(new DateTime().plusHours(1), DELTA + 500), equalTo(analytic3));
    }

    private String generateJsonPayload(List<AnalyticsEntry> analyticEntries) throws IOException {
        return mapper.writeValueAsString(analyticEntries);
    }

    private AnalyticsEntry readRecord(final DateTime ts, int delta) {
        return jdbc.queryForObject("select * from ANALYTICS where analytic_ts= ?", new RowMapper<AnalyticsEntry>() {
            @Override
            public AnalyticsEntry mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                return new AnalyticsEntry(rs.getInt("event_id"),
                                          rs.getString("type"),
                                          rs.getString("session_id"),
                                          rs.getString("label"),
                                          rs.getString("value"),
                                          rs.getString("action"),
                                          (int) (ts.getMillis() - new DateTime(rs.getTimestamp("analytic_ts")).getMillis()));
            }
        }, new Timestamp(ts.minusMillis(delta).getMillis()));
    }
}
