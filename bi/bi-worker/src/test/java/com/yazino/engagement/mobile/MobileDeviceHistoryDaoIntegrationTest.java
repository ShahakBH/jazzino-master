package com.yazino.engagement.mobile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static com.yazino.engagement.mobile.MobileDeviceEvent.ADDED;
import static com.yazino.engagement.mobile.MobileDeviceEvent.valueOf;
import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("MobileDeviceServiceIntegrationTest-context.xml")
@TransactionConfiguration
@Transactional
@DirtiesContext
public class MobileDeviceHistoryDaoIntegrationTest {

    private static final Map<String, Object> NO_PARAMS = emptyMap();
    public static final Long ROW_NUMBER = (-666L);

    @Autowired
    NamedParameterJdbcTemplate externalDwNamedJdbcTemplate;

    private MobileDeviceHistoryDao underTest;

    @Before
    public void setUp() throws Exception {
        externalDwNamedJdbcTemplate.update("DELETE FROM mobile_device_history h USING mobile_device d WHERE h.id = d.id AND d.player_id < 0", NO_PARAMS);
        externalDwNamedJdbcTemplate.update("DELETE FROM mobile_device WHERE player_id < 0", NO_PARAMS);
        externalDwNamedJdbcTemplate.update("INSERT INTO mobile_device (id,player_id, platform , app_id, game_type, device_id, push_token, active) values (-666,-1,'android','app_id','game_type','device_id','push_token',true )", NO_PARAMS);
        underTest = new MobileDeviceHistoryDao(externalDwNamedJdbcTemplate);

    }

    @Test
    public void recordEventShouldInsertRow() {

        underTest.recordEvent(ROW_NUMBER, ADDED, "detail");

        final Map<String, Object> result = externalDwNamedJdbcTemplate.queryForMap("select * from mobile_device_history where id = "+ ROW_NUMBER, NO_PARAMS);
        assertThat(valueOf((String) result.get("event")), is(ADDED));
        assertThat((String) result.get("detail"), is(equalTo("detail")));
        final Timestamp timestamp = (Timestamp) result.get("event_ts");
        assertThat(timestamp.getTime() > System.currentTimeMillis() - 10000, is(true));
    }

}
