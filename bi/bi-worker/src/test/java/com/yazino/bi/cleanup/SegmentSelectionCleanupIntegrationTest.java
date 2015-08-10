package com.yazino.bi.cleanup;

import com.yazino.bi.aggregator.AggregatorLockDao;
import com.yazino.configuration.YazinoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Timestamp;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.core.Is.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static utils.ParamBuilder.emptyParams;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
//@Transactional
//@TransactionConfiguration
public class SegmentSelectionCleanupIntegrationTest {

    public static final String STRATA_DATABASE_CLEANUP_SEGMENT_SELECTION_ENABLE =
            "strata.database.cleanup.segment_selection.enable";
    @Autowired
    private SegmentSelectionCleanup segmentSelectionCleanup;

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    AggregatorLockDao aggregatorLockDao;

    @Autowired
    private YazinoConfiguration yazinoConfiguration;

    @Before
    public void setUp() throws Exception {
        namedParameterJdbcTemplate.update("delete from aggregator_lock where id='DBCleanup'", emptyParams());
        namedParameterJdbcTemplate.update("delete from campaign_run_audit", emptyParams());
        namedParameterJdbcTemplate.update("delete from segment_selection", emptyParams());
        yazinoConfiguration.clearProperty(STRATA_DATABASE_CLEANUP_SEGMENT_SELECTION_ENABLE);
        yazinoConfiguration.addProperty(STRATA_DATABASE_CLEANUP_SEGMENT_SELECTION_ENABLE, Boolean.TRUE);
    }

    @Test
    public void runShouldDeleteSegmentsOlderThanAMonth() {
        namedParameterJdbcTemplate.update(
                "insert into campaign_run_audit(campaign_id, run_id, status, run_ts) values (-1,-10,'success',:date) ",
                of("date", new Timestamp(now().minusMonths(1).minusDays(1).getMillis())));
        namedParameterJdbcTemplate.update(
                "insert into campaign_run_audit(campaign_id, run_id, status, run_ts) values (-1,-11,'success',:date) ",
                of("date", new Timestamp(now().minusYears(1).plusDays(1).getMillis())));
        namedParameterJdbcTemplate.update(
                "insert into segment_selection (campaign_run_id, player_id) values (-10,-666) ", emptyParams());
        namedParameterJdbcTemplate.update(
                "insert into segment_selection (campaign_run_id, player_id) values (-10,-777) ", emptyParams());
        namedParameterJdbcTemplate.update(
                "insert into segment_selection (campaign_run_id, player_id) values (-11,-777) ", emptyParams());
        assertThat(getSegmentCount(), is(3));
        segmentSelectionCleanup.run();

        assertThat(getSegmentCount(), is(0));
    }

    @Test
    public void runShouldReturnNothingIfDisabled() {
        yazinoConfiguration.clearProperty(STRATA_DATABASE_CLEANUP_SEGMENT_SELECTION_ENABLE);
        yazinoConfiguration.addProperty(STRATA_DATABASE_CLEANUP_SEGMENT_SELECTION_ENABLE, Boolean.FALSE);
        namedParameterJdbcTemplate.update(
                "insert into campaign_run_audit(campaign_id, run_id, status, run_ts) values (-1,-10,'success',:date) ",
                of("date", new Timestamp(now().minusMonths(1).minusDays(1).getMillis())));
        namedParameterJdbcTemplate.update(
                "insert into segment_selection (campaign_run_id, player_id) values (-10,-666) ", emptyParams());
        assertThat(getSegmentCount(), is(1));
        segmentSelectionCleanup.run();
        assertThat(getSegmentCount(), is(1));
    }

    @Test
    public void runShouldNotDeleteSegmentsNewerThanAMonth() {
        namedParameterJdbcTemplate.update(
                "insert into campaign_run_audit(campaign_id, run_id, status, run_ts) values (-1,-10,'success',:date) ",
                of("date", new Timestamp(now().minusDays(0).getMillis())));
        namedParameterJdbcTemplate.update(
                "insert into campaign_run_audit(campaign_id, run_id, status, run_ts) values (-1,-11,'success',:date) ",
                of("date", new Timestamp(now().minusDays(27).getMillis())));
        namedParameterJdbcTemplate.update(
                "insert into segment_selection (campaign_run_id, player_id) values (-10,-666) ", emptyParams());
        namedParameterJdbcTemplate.update(
                "insert into segment_selection (campaign_run_id, player_id) values (-10,-777) ", emptyParams());
        namedParameterJdbcTemplate.update(
                "insert into segment_selection (campaign_run_id, player_id) values (-11,-777) ", emptyParams());
        assertThat(getSegmentCount(), is(3));
        segmentSelectionCleanup.run();
        assertThat(getSegmentCount(), is(3));
    }

    private int getSegmentCount() {
        return namedParameterJdbcTemplate.queryForInt(
                "select count(*) from segment_selection", emptyParams());
    }
}