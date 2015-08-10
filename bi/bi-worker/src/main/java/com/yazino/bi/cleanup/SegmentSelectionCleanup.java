package com.yazino.bi.cleanup;

import com.yazino.bi.aggregator.AggregatorLockDao;
import com.yazino.configuration.YazinoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

import static org.joda.time.DateTime.now;

@Service
public class SegmentSelectionCleanup {

    private final DatabaseCleanup databaseCleanup;
    private static final String ID = "segment_selection";
    private final YazinoConfiguration configuration;

    @Autowired
    public SegmentSelectionCleanup(final AggregatorLockDao aggregatorLockDao,
                                   final YazinoConfiguration configuration,
                                   @Qualifier("externalDwNamedJdbcTemplate")
                                   final NamedParameterJdbcTemplate externalNamedParameterJdbcTemplate) {
        this.configuration = configuration;

        this.databaseCleanup = new DatabaseCleanup(
                aggregatorLockDao,
                configuration,
                externalNamedParameterJdbcTemplate,
                ID);
    }

    @Scheduled(cron = "${strata.database.cleanup.segment_selection.timing}")
    public void run() {
        final int daysOffset = configuration.getInteger("strata.database.cleanup.segment_selection.offset_in_days", 28);
        databaseCleanup.run(
                "delete from segment_selection ss using "
                        + "campaign_run_audit cr where cr.run_id = ss.campaign_run_id "
                        + "and run_ts < :cutoff",
                new MapSqlParameterSource("cutoff", new Timestamp(now().minusDays(daysOffset).getMillis()))
        );

    }
}
