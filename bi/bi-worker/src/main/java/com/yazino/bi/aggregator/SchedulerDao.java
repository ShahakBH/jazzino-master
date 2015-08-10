package com.yazino.bi.aggregator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

@Repository
public class SchedulerDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SchedulerDao(@Qualifier("externalDwJdbcTemplate") final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> getScheduledAggregators() {
        List<String> aggregators = newArrayList();
        final List<Map<String, Object>> results = jdbcTemplate.queryForList("select distinct aggregator from scheduled_aggregators");
        for (Map<String, Object> line : results) {
            final String aggregator = (String) line.get("aggregator");
            aggregators.add(aggregator);
            jdbcTemplate.execute("delete from scheduled_aggregators where aggregator='" + aggregator + "'");
        }

        return aggregators;
    }
}
