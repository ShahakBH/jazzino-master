package com.yazino.bi.aggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class AggregatorLastUpdateDAO {

    private final JdbcTemplate template;

    private static final Logger LOG = LoggerFactory.getLogger(AggregatorLastUpdateDAO.class);

    @Autowired
    public AggregatorLastUpdateDAO(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate template) {
        notNull(template, "template may not be null");

        this.template = template;
    }

    public Timestamp getLastRun(String aggregatorId) {
        try {
            return template.queryForObject("select last_run_ts from aggregator_last_update where id = ?", Timestamp.class, aggregatorId);
        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    public void updateLastRunFor(final String aggregatorId, final Timestamp timestamp) {
        LOG.info("updating last update for {} to {}", aggregatorId, timestamp.toString());
        int linesModified = template.update("update aggregator_last_update set last_run_ts = ? where id = ?", timestamp, aggregatorId);

        if (linesModified != 1) {
            template.update("insert into aggregator_last_update (id, last_run_ts) values (?,?)", aggregatorId, timestamp);
        }
    }


}
