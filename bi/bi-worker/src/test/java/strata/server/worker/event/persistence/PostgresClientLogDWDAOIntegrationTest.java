package strata.server.worker.event.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yazino.client.log.ClientLogEvent;
import com.yazino.client.log.ClientLogEventMessageType;
import com.yazino.test.logback.ListAppender;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@DirtiesContext
@Transactional
public class PostgresClientLogDWDAOIntegrationTest {
    public static final DateTime TS = new DateTime(1999, 1, 1, 1, 1, 1, 1);
    @Autowired
    @Qualifier("externalDwJdbcTemplate")
    private JdbcTemplate jdbc;

    @Autowired
    private PostgresClientLogDWDAO underTest;

    private ObjectMapper mapper = new ObjectMapper();



    @SuppressWarnings("NullableProblems")
    @Test(expected = NullPointerException.class)
    public void shouldRefuseNullCollection() {
        underTest.saveAll(null);
    }

    @Test
    @Transactional
    public void saveSingleClientLogEventWithMinimumData() {
        underTest.saveAll(asList(new ClientLogEvent(TS, "{}", ClientLogEventMessageType.LOG_EVENT)));
        Map<String, Object> record = readRecord(TS);
        assertEquals("{}", record.get("payload").toString());
    }

    @Test
    @Transactional
    public void saveSingleClientLogEventWithAllData() throws IOException {
        String json = payload("Error Message", "123", "some stacktrace", "1", "2", "SLOTS", "1.0.0", "ANDROID", "S3", "11", "wifi");
        underTest.saveAll(asList(new ClientLogEvent(TS, json, ClientLogEventMessageType.LOG_EVENT)));
        Map<String, Object> record = readRecord(TS);
        verifyRecord(record, json, "Error Message", 123, "some stacktrace", new BigDecimal("1.00"), new BigDecimal("2.00"), "SLOTS", "1.0.0", "ANDROID", "S3", "11", "wifi");
    }

    @Test
    @Transactional
    public void saveSingleClientLogEventWithMalformedNumbers() throws IOException {
        String malformedNumber = "abc";
        String json = payload("Error Message", malformedNumber, "some stacktrace", malformedNumber, malformedNumber, "SLOTS", "1.0.0", "ANDROID", "S3", "11", "wifi");
        underTest.saveAll(asList(new ClientLogEvent(TS, json, ClientLogEventMessageType.LOG_EVENT)));
        Map<String, Object> record = readRecord(TS);
        assertNull(record.get("player_id"));
        assertNull(record.get("table_id"));
        assertNull(record.get("error_code"));
    }

    @Test
    public void saveSingleClientLogEventWithMalformedJson() {
        underTest.saveAll(asList(new ClientLogEvent(TS, "{not a json!", ClientLogEventMessageType.LOG_EVENT)));
        assertNotNull(readRecord(TS));
    }

    @Test
    @Transactional
    public void saveShouldTruncateMessageIfLongerThan255() throws IOException {
        ListAppender listAppender = ListAppender.addAppenderTo(PostgresClientLogDWDAO.class);
        String json = payload(StringUtils.rightPad("Error Message is longer than 255", 256, 'x'), "123", "some stacktrace", "1", "2", "SLOTS", "1.0.0", "ANDROID", "S3", "11", "wifi");

        underTest.saveAll(asList(new ClientLogEvent(TS, json, ClientLogEventMessageType.LOG_EVENT)));

        assertThat(listAppender.getMessages(),
                not(hasItem(containsString("ERROR: value too long for type character varying(255)"))));
    }

    @Test
    @Transactional
    public void saveShouldTruncateModelNameIfLongerThan32Characters() throws IOException {
        ListAppender listAppender = ListAppender.addAppenderTo(PostgresClientLogDWDAO.class);

        // this is a model name from production :sigh:
        String json = payload("anErrorMessage", "123", "some stacktrace", "1", "2", "SLOTS", "1.0.0", "ANDROID", "HTC Sensation XE with Beats Audio Z715e", "11", "wifi");

        underTest.saveAll(asList(new ClientLogEvent(TS, json, ClientLogEventMessageType.LOG_EVENT)));

        assertThat(listAppender.getMessages(), not(hasItem(containsString("ERROR: value too long for type character varying"))));
    }

    @Test
    @Transactional
    public void saveShouldNotTruncatePayloadAtAPointThatCausesProblemsWithEscaping() throws IOException {
        ListAppender listAppender = ListAppender.addAppenderTo(PostgresClientLogDWDAO.class);

        // an example from production
        String json = payload("Error decoding document. event.message.body=[{\"gameId\":\"60\", \"commandUUID\":\"abbdca7b-650a-40e3-8355-45dd93c8585b\","
                + " \"changes\":\"60\\t37502\\t21104384\\t150255629\\t150262058\\t150251220\\t150262370\\t21554623\\n9731\\n37637\\nLeave\\nNoTimeout\\n37580\\tWheelsCollected\\t",
                null, null, "150262370", "150228414", "SLOTS", "2.39.1", "ANDROID", "SCH-I535", "16", "mobile");

        underTest.saveAll(asList(new ClientLogEvent(TS, json, ClientLogEventMessageType.LOG_EVENT)));

        assertThat(listAppender.getMessages(), not(hasItem(containsString("ERROR: syntax error at or near"))));
    }

    @Test
    @Transactional
    public void saveShouldTruncatePayloadIfLongerThan64K() throws IOException {
        ListAppender listAppender = ListAppender.addAppenderTo(PostgresClientLogDWDAO.class);
        String json = payload(StringUtils.rightPad("Error Message is longer than 65535", 65536, 'x'), "123", "some stacktrace", "1", "2", "SLOTS", "1.0.0", "ANDROID", "S3", "11", "wifi");

        underTest.saveAll(asList(new ClientLogEvent(TS, json, ClientLogEventMessageType.LOG_EVENT)));

        assertThat(listAppender.getMessages(),
                not(hasItem(containsString("ERROR: value too long for type character varying(65535)"))));
    }

    @Test
    @Transactional
    public void saveShouldTruncateStacktraceIfLongerThan64K() throws IOException {
        ListAppender listAppender = ListAppender.addAppenderTo(PostgresClientLogDWDAO.class);
        String json = payload("Payload", "123", StringUtils.rightPad("some stacktrace longer than  65535", 65536, 'x'), "1", "2", "SLOTS", "1.0.0", "ANDROID", "S3", "11", "wifi");

        underTest.saveAll(asList(new ClientLogEvent(TS, json, ClientLogEventMessageType.LOG_EVENT)));

        assertThat(listAppender.getMessages(),
                not(hasItem(containsString("ERROR: value too long for type character varying(65535)"))));
    }

    @Test
    @Transactional
    public void saveMultipleClientLogEvents() throws IOException {
        DateTime ts1 = new DateTime(2001, 1, 1, 1, 1, 1);
        DateTime ts2 = new DateTime(2001, 1, 1, 1, 1, 2);
        DateTime ts3 = new DateTime(2001, 1, 1, 1, 1, 3);
        ClientLogEvent ev1 = new ClientLogEvent(ts1, payload("error1", null, null, null, null, null, null, null, null, null, null), ClientLogEventMessageType.LOG_EVENT);
        ClientLogEvent ev2 = new ClientLogEvent(ts2, payload("error2", null, null, null, null, null, null, null, null, null, null), ClientLogEventMessageType.LOG_EVENT);
        ClientLogEvent ev3 = new ClientLogEvent(ts3, payload("error3", null, null, null, null, null, null, null, null, null, null), ClientLogEventMessageType.LOG_EVENT);
        underTest.saveAll(asList(ev1, ev2, ev3));
        Map<String, Object> r1 = readRecord(ts1);
        assertEquals("error1", r1.get("message").toString());
        Map<String, Object> r2 = readRecord(ts2);
        assertEquals("error2", r2.get("message").toString());
        Map<String, Object> r3 = readRecord(ts3);
        assertEquals("error3", r3.get("message").toString());
    }

    private void verifyRecord(Map<String, Object> record,
                              String payload,
                              String message,
                              int errorCode,
                              String stacktrace,
                              BigDecimal playerId,
                              BigDecimal tableId,
                              String gameType,
                              String version,
                              String platform,
                              String model,
                              String api,
                              String net) {
        assertEquals(payload, record.get("payload").toString());
        assertEquals(message, record.get("message").toString());
        assertEquals(errorCode, Integer.parseInt(record.get("error_code").toString()));
        assertEquals(playerId, new BigDecimal(record.get("player_id").toString()));
        assertEquals(tableId, new BigDecimal(record.get("table_id").toString()));
        assertEquals(gameType, record.get("game_type").toString());
        assertEquals(version, record.get("version").toString());
        assertEquals(platform, record.get("platform").toString());
        assertEquals(stacktrace, record.get("stacktrace").toString());
        assertEquals(model, record.get("model").toString());
        assertEquals(api, record.get("api").toString());
        assertEquals(net, record.get("net").toString());
    }

    private String payload(String message,
                           String errorCode,
                           String stacktrace,
                           String playerId,
                           String tableId,
                           String gameType,
                           String version,
                           String platform,
                           String model,
                           String api,
                           String net) throws IOException {
        HashMap<String, Object> full_record = new HashMap<String, Object>();
        full_record.put("msg", message);
        full_record.put("code", errorCode);
        full_record.put("stacktrace", stacktrace);
        full_record.put("playerId", playerId);
        full_record.put("tableId", tableId);
        full_record.put("gameType", gameType);
        full_record.put("version", version);
        full_record.put("platform", platform);
        full_record.put("model", model);
        full_record.put("api", api);
        full_record.put("net", net);
        return mapper.writeValueAsString(full_record);
    }

    private Map<String, Object> readRecord(DateTime ts) {
        return jdbc.queryForMap("select * from client_log where log_ts= ?", new Timestamp(ts.getMillis()));
    }
}
