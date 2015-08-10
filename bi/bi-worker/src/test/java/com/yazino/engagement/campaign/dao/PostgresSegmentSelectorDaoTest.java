package com.yazino.engagement.campaign.dao;

import com.yazino.bi.persistence.BatchResultSetExtractor;
import com.yazino.bi.persistence.BatchVisitor;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.engagement.campaign.domain.PlayerWithContent;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostgresSegmentSelectorDaoTest {
    private static final DateTime REPORT_TIME = new DateTime(2013, 1, 1, 10, 0, 0, 0);

    @Mock
    private NamedParameterJdbcTemplate template;
    @Mock
    private BatchVisitor<PlayerWithContent> batchVisitor;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private PostgresSegmentSelectorDao underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new PostgresSegmentSelectorDao(template, yazinoConfiguration);

        when(template.query(anyString(), any(BatchResultSetExtractor.class))).thenReturn(0);
    }

    @Test
    public void testFetchPlayersForCampaignShouldPassThroughCorrectSql() throws Exception {
        final String sql = "SELECT player_id FROM PLAYER_ACTIVITY_DAILY";

        underTest.fetchSegment(sql, REPORT_TIME, batchVisitor);

        verify(template).query(eq(sql), any(BatchResultSetExtractor.class));
    }

    @Test
    public void getDateIsSubstitutedInExecutedSql() throws Exception {
        final String originalSql = "SELECT player_id FROM PLAYER_ACTIVITY_DAILY "
                + "WHERE activity_ts >= getDate() - INTERVAL '1 day' "
                + "and activity_ts < geTdate()";
        final String expectedSql = "SELECT player_id FROM PLAYER_ACTIVITY_DAILY "
                + "WHERE activity_ts >= '2013-01-01 10:00:00'::timestamp - INTERVAL '1 day' "
                + "and activity_ts < '2013-01-01 10:00:00'::timestamp";

        underTest.fetchSegment(originalSql, REPORT_TIME, batchVisitor);

        verify(template).query(eq(expectedSql), any(BatchResultSetExtractor.class));
    }
}
