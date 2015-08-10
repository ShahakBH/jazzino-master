package com.yazino.tracking;

import com.yazino.bi.tracking.TrackingDao;
import com.yazino.bi.tracking.TrackingEvent;
import com.yazino.platform.Platform;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
@Transactional
public class TrackingDaoIntegrationTest {

    @Autowired
    private TrackingDao underTest;

    @Autowired
    private JdbcTemplate dwJdbcTemplate;

    @Before
    public void setup() {
        deleteTestData();
//        insertTestData();
    }

    @After
    public void cleanUp() {
        deleteTestData();
    }

    @Test
    public void save_shouldPersistTrackedEvent() {
        Platform platform = platform(1);
        BigDecimal playerId = playerId(1);
        String name = name(1);
        DateTime received = received(1);
        Map<String, String> properties = properties(1);
        properties.put("K1", "V1");
        properties.put("K2", "V2");
        properties.put("K3", "V3");

        underTest.save(platform, playerId, name, properties, received);

        assertTrue(hasMatchingEvent(platform, playerId, name, properties, received));
    }

    @Test
    public void findMostRecent_shouldSelectMostRecentEvents() {
        int eventCount = 10;
        insertTrackingEvents(eventCount);
        int requestedQuantity = 5;
        List<TrackingEvent> mostRecentEvents = underTest.findMostRecentEvents(requestedQuantity);
        assertEquals(requestedQuantity, mostRecentEvents.size());
        for (int i = 0; i < requestedQuantity; i++) {
            int k = eventCount - (i + 1);
            System.out.println(received(k).getZone());
            System.out.println(new DateTime().getZone());
            System.out.println(new DateTime().getMillis());
            System.out.println(mostRecentEvents.get(i).getReceived());
            System.out.println(mostRecentEvents.get(i).getReceived().getMillis());
            System.out.println(received(k).getMillis());
            System.out.println(received(k).getMillis());
            assertEquals(mostRecentEvents.get(i), new TrackingEvent(platform(k), playerId(k), name(k), properties(k), received(k)));
        }
    }

    @Test
    public void findMostRecent_shouldReturnEmptySetIfNoneAvailable() {
        int availableQuantity = 10;
        int requestedQuantity = 10;
        insertTrackingEvents(availableQuantity);
        List<TrackingEvent> mostRecentEvents = underTest.findMostRecentEvents(requestedQuantity);
        assertEquals(availableQuantity, mostRecentEvents.size());
    }

    @Test
    public void findMostRecent_shouldReturnAllIfLessThanRequestedAmountAvailable() {
        int requestedQuantity = 10;
        List<TrackingEvent> mostRecentEvents = underTest.findMostRecentEvents(requestedQuantity);
        assertTrue(mostRecentEvents.isEmpty());
    }

    private void insertTrackingEvents(int quantity) {
        for (int i = 0; i < quantity; i++) {
            insertTrackingEvent(platform(i), playerId(i), name(i), properties(i), received(i));
        }
    }

    private void insertTrackingEvent(Platform platform, BigDecimal playerId, String name, Map<String, String> properties, DateTime received) {
        SimpleJdbcInsert insert =
                new SimpleJdbcInsert(dwJdbcTemplate)
                        .withTableName("TRACKING_EVENT")
                        .usingGeneratedKeyColumns("id");

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("platform", platform.name());
        parameters.put("player_id", playerId);
        parameters.put("name", name);
        parameters.put("received", received.toDate());
        Number trackingEventId = insert.executeAndReturnKey(parameters);

        for (Map.Entry<String, String> property : properties.entrySet()) {
            dwJdbcTemplate.update("insert into TRACKING_EVENT_PROPERTY(`tracking_event_id`, `key`, `value`, `received`) values(?, ?, ?, ?)",
                    trackingEventId, property.getKey(), property.getValue(), received.toDate());
        }
    }

    private boolean hasMatchingEvent(Platform platform, BigDecimal playerId, String name, Map<String, String> properties, DateTime received) {
        Long id = dwJdbcTemplate.queryForLong("select id from TRACKING_EVENT where platform = ? and player_id = ? and name = ? and received = ?",
                platform.name(), playerId, name, received.toDate());
        if (id == null) {
            return false;
        }
        for (Map.Entry<String, String> property : properties.entrySet()) {
            int count = dwJdbcTemplate.queryForInt("select count(1) from TRACKING_EVENT_PROPERTY where tracking_event_id = ? and `key` = ? and `value` = ? and received = ?",
                    id, property.getKey(), property.getValue(), received.toDate());
            if (count != 1) {
                return false;
            }
        }
        return true;
    }

    private Platform platform(int i) {
        Platform[] values = Platform.values();
        return values[i % values.length];
    }

    private BigDecimal playerId(int i) {
        return BigDecimal.valueOf(100 + i);
    }

    private String name(int i) {
        return "NAME_" + i;
    }

    private Map<String, String> properties(int i) {
        return new HashMap<String, String>();
    }

    private DateTime received(int i) {
        return new DateTime(2012, 12, 31, 23, 59).plusYears(i);
    }

    private void deleteTestData() {
        dwJdbcTemplate.execute("delete from TRACKING_EVENT_PROPERTY");
        dwJdbcTemplate.execute("delete from TRACKING_EVENT");
    }

}
