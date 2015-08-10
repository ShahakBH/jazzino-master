package com.yazino.engagement.campaign.reporting.dao;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class PsqlCampaignAuditDaoIntegrationTest {

    public static final long CAMPAIGN_RUN_ID = 2l;
    public static final String NAME = "Name of Campaign";
    public static final int SIZE = 100;
    public static final DateTime RUN_TS = new DateTime();
    public static final String STATUS = "success";
    public static final String MESSAGE = "this is a message";
    public static final long PROMO_ID = 342L;
    private final Long CAMPAIGN_ID = 1l;
    private CampaignAuditDao underTest;

    @Autowired
    NamedParameterJdbcTemplate template;

    @Before
    public void setUp() throws Exception {
        template.update("DELETE FROM CAMPAIGN_RUN_AUDIT", new HashMap<String,Object>());
        underTest = new PsqlCampaignAuditDao(template);
    }

    @Test
    public void persistCampaignRunShouldWriteRecordToDb(){

        underTest.persistCampaignRun(CAMPAIGN_ID, CAMPAIGN_RUN_ID, NAME, SIZE, RUN_TS, STATUS, MESSAGE, PROMO_ID);

        final Map<String, Object> actual = template.queryForObject("SELECT * from CAMPAIGN_RUN_AUDIT",
                new HashMap<String, Object>(),
                new RowMapper<Map<String, Object>>() {
            @Override
            public Map<String, Object> mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                final HashMap<String, Object> resultMap = new HashMap<String, Object>();
                resultMap.put("campaign_id", rs.getLong("campaign_id"));
                resultMap.put("run_id", rs.getLong("run_id"));
                resultMap.put("name", rs.getString("name"));
                resultMap.put("size", rs.getInt("segment_size"));
                resultMap.put("run_ts", rs.getTimestamp("run_ts"));
                resultMap.put("status", rs.getString("status"));
                resultMap.put("message", rs.getString("message"));
                resultMap.put("promo_id", rs.getLong("promo_id"));
                return resultMap;
            }
        });

        assertThat((Long) actual.get("campaign_id"), equalTo(CAMPAIGN_ID));
        assertThat((Long) actual.get("run_id"), equalTo(CAMPAIGN_RUN_ID));
        assertThat((Long) actual.get("promo_id"), equalTo(PROMO_ID));
        assertThat((String)(actual.get("name")), equalTo(NAME));
        assertThat((Integer) actual.get("size") , equalTo(SIZE));
        assertThat((Timestamp) actual.get("run_ts"), equalTo(new Timestamp(RUN_TS.getMillis())));
        assertThat((String)(actual.get("status")), equalTo(STATUS));
        assertThat((String)(actual.get("message")), equalTo(MESSAGE));

    }
}
