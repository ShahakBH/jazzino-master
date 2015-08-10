package com.yazino.platform.metrics;

import com.codahale.metrics.MetricRegistry;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.metrics.reporter.MetricsClient;
import com.yazino.metrics.reporter.MetricsReporter;
import org.openspaces.core.space.mode.PostPrimary;
import org.openspaces.core.space.mode.PreBackup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GigaspaceMetricsReporter extends MetricsReporter {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceMetricsReporter.class);

    @Autowired
    public GigaspaceMetricsReporter(final MetricRegistry registry,
                                    final MetricsClient client,
                                    final YazinoConfiguration yazinoConfiguration) {
        super(registry, client, yazinoConfiguration);
    }

    @PostPrimary
    public void start() {
        LOG.debug("Starting reporter for primary space with client ID {}", clientId());

        super.start();
    }

    @PreBackup
    public void stop() {
        LOG.debug("Starting reporter for backup space with client ID {}", clientId());

        super.stop();
    }
}
