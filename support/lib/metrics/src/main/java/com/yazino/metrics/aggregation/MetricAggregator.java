package com.yazino.metrics.aggregation;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.metrics.model.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class MetricAggregator {
    private static final Logger LOG = LoggerFactory.getLogger(MetricAggregator.class);

    private static final String UPDATE_WINDOW = "metrics.update.window";
    private static final int DEFAULT_UPDATE_WINDOW = 120;

    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public MetricAggregator(final YazinoConfiguration yazinoConfiguration) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
    }

    public Metric aggregate(final long baseTimestamp,
                            final Map<String, Metric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return null;
        }

        final int updateWindowInSeconds = yazinoConfiguration.getInt(UPDATE_WINDOW, DEFAULT_UPDATE_WINDOW);

        Metric aggregatedMetric = null;
        for (Map.Entry<String, Metric> nameAndMetric : metrics.entrySet()) {
            final Metric metric = nameAndMetric.getValue();
            if (metric.getTimestamp().plusSeconds(updateWindowInSeconds).isBefore(baseTimestamp)) {
                LOG.debug("Ignored metric timestamped {} from source {} as it is outside the update window",
                        metric.getTimestamp(), nameAndMetric.getKey());

            } else if (aggregatedMetric == null) {
                aggregatedMetric = metric;

            } else {
                aggregatedMetric = aggregatedMetric.add(metric);
            }
        }

        return aggregatedMetric;
    }

}
