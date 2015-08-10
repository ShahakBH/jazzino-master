package com.yazino.bi.tracking;

import com.yazino.platform.Platform;
import com.yazino.platform.util.BigDecimals;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TrackingDao {

    private static final Logger LOG = LoggerFactory.getLogger(TrackingDao.class);

    private final JdbcTemplate jdbcTemplate;
    private RowMapper<TrackingEvent> trackingEventRowMapper = new TrackingEventRowMapper();

    @Autowired
    public TrackingDao(@Qualifier("dwJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    public void save(Platform platform, BigDecimal playerId, String name, Map<String, String> properties, DateTime received) {
        SimpleJdbcInsert insert =
                new SimpleJdbcInsert(jdbcTemplate)
                        .withTableName("TRACKING_EVENT")
                        .usingGeneratedKeyColumns("id");

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("platform", platform.name());
        parameters.put("player_id", playerId);
        parameters.put("name", name);
        parameters.put("received", received.toDate());
        Number trackingEventId = insert.executeAndReturnKey(parameters);

        for (Map.Entry<String, String> property : properties.entrySet()) {
            jdbcTemplate.update("insert into TRACKING_EVENT_PROPERTY(`tracking_event_id`, `key`, `value`, `received`) values(?, ?, ?, ?)",
                    trackingEventId, property.getKey(), property.getValue(), received.toDate());
        }
    }

    public List<TrackingEvent> findMostRecentEvents(int quantity) {
        return jdbcTemplate.query("select * from TRACKING_EVENT order by RECEIVED desc limit ?", new Object[]{quantity}, trackingEventRowMapper);
    }

    private Map<String, String> findPropertiesByTrackingEventId(long id) {
        Map<String, String> properties = new HashMap<String, String>();
        SqlRowSet rs = jdbcTemplate.queryForRowSet("select * from TRACKING_EVENT_PROPERTY where TRACKING_EVENT_ID = ?", id);
        while (rs.next()) {
            String key = rs.getString("key");
            String value = rs.getString("value");
            properties.put(key, value);
        }
        return properties;
    }

    private class TrackingEventRowMapper implements RowMapper<TrackingEvent> {

        @Override
        public TrackingEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            Platform platform = Platform.safeValueOf(rs.getString("platform"));
            BigDecimal playerId = BigDecimals.strip(rs.getBigDecimal("player_id"));
            String name = rs.getString("name");
            DateTime received = new DateTime(rs.getTimestamp("received"));

            Map<String, String> properties = findPropertiesByTrackingEventId(rs.getLong("id"));
            return new TrackingEvent(platform, playerId, name, properties, received);
        }
    }
}
