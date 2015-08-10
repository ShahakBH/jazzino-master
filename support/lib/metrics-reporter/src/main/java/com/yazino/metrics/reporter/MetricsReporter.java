package com.yazino.metrics.reporter;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.yazino.configuration.YazinoConfiguration;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.management.VMManagement;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * A reporter which publishes metric values to our Metrics aggregation server.
 */
@Service
public class MetricsReporter extends ScheduledReporter {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsReporter.class);

    private static final String REPORTER_NAME = "metrics-reporter";
    private static final String PROPERTY_REPORT_PERIOD_SECONDS = "metrics.aggregation.report-period";
    private static final int DEFAULT_REPORT_PERIOD_SECONDS = 30;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final YazinoConfiguration yazinoConfiguration;
    private final MetricsClient client;

    private int processId;
    private String hostname;

    @Autowired
    public MetricsReporter(final MetricRegistry registry,
                           final MetricsClient client,
                           final YazinoConfiguration yazinoConfiguration) {
        super(registry, REPORTER_NAME, MetricFilter.ALL, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);

        LOG.debug("Creating new reporter with name {} and client ID {}", REPORTER_NAME, clientId());

        notNull(client, "client may not be null");
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.client = client;
        this.yazinoConfiguration = yazinoConfiguration;
    }

    @PostConstruct
    public void start() {
        final int reportPeriod = yazinoConfiguration.getInt(PROPERTY_REPORT_PERIOD_SECONDS, DEFAULT_REPORT_PERIOD_SECONDS);
        start(reportPeriod, TimeUnit.SECONDS);
    }

    @Override
    public void start(final long period, final TimeUnit unit) {
        if (!started.get()) {
            LOG.info("Starting reporting every {} {}", period, unit);
            super.start(period, unit);
            started.set(true);
        } else {
            LOG.debug("Not starting as we've already started");
        }
    }

    @Override
    public void stop() {
        LOG.debug("Stopping reporter for client ID {}", clientId());

        started.set(false);
        super.stop();
    }

    @Override
    public void close() {
        LOG.debug("Closing reporter for client ID {}", clientId());

        started.set(false);
        super.close();
    }

    @Override
    public void report(final SortedMap<String, Gauge> gauges,
                       final SortedMap<String, Counter> counters,
                       final SortedMap<String, Histogram> histograms,
                       final SortedMap<String, Meter> meters,
                       final SortedMap<String, Timer> timers) {
        final long timestamp = DateTimeUtils.currentTimeMillis();

        try {
            final Map<String, Object> report = new HashMap<>();

            addIfNotEmpty(report, "meters", reportOnMeters(meters, timestamp));

            sendIfNotEmpty(report);

        } catch (Exception e) {
            LOG.warn("Unable to report to aggregation server", e);
        }
    }

    private void sendIfNotEmpty(final Map<String, Object> report) {
        final String clientId = clientId();
        if (!report.isEmpty()) {
            LOG.debug("Reporting statistics to server with client ID {}", clientId);

            report.put("clientId", clientId);
            client.send(report);

        } else {
            LOG.debug("No statistics to report for client ID {}", clientId);
        }
    }

    protected String clientId() {
        if (processId == 0) {
            processId = processId();
            hostname = hostname();
        }

        return String.format("%s:%d", hostname, processId);
    }

    private void addIfNotEmpty(final Map<String, Object> report,
                               final String groupName,
                               final List<Map<String, Object>> reportedValues) {
        if (!reportedValues.isEmpty()) {
            report.put(groupName, reportedValues);
        }
    }

    private List<Map<String, Object>> reportOnMeters(final SortedMap<String, Meter> meters, final long timestamp)
            throws IOException {
        if (meters == null) {
            return Collections.emptyList();
        }

        final List<Map<String, Object>> reportedMeters = new ArrayList<>(meters.size());
        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            reportedMeters.add(reportMeter(entry.getKey(), entry.getValue(), timestamp));
        }
        return reportedMeters;
    }

    private Map<String, Object> reportMeter(final String name,
                                            final Metered meter,
                                            final long timestamp)
            throws IOException {
        final Map<String, Object> reportedMeter = new HashMap<>();
        reportedMeter.put("name", name);
        reportedMeter.put("timestamp", timestamp);
        reportedMeter.put("count", meter.getCount());
        reportedMeter.put("m1Rate", meter.getOneMinuteRate());
        reportedMeter.put("m5Rate", meter.getFiveMinuteRate());
        reportedMeter.put("m15Rate", meter.getFifteenMinuteRate());
        reportedMeter.put("meanRate", meter.getMeanRate());

        return reportedMeter;
    }

    public String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            final String systemHostname = System.getenv("HOSTNAME");
            if (systemHostname != null) {
                return systemHostname;
            }
            return "Unknown";
        }
    }

    private int processId() {
        try {
            final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            final Field jvm = runtime.getClass().getDeclaredField("jvm");
            jvm.setAccessible(true);
            final VMManagement management = (VMManagement) jvm.get(runtime);
            java.lang.reflect.Method pidMethod = management.getClass().getDeclaredMethod("getProcessId");
            pidMethod.setAccessible(true);
            return (Integer) pidMethod.invoke(management);

        } catch (Exception e) {
            LOG.error("Failed to find PID", e);
            return -1;
        }
    }

}
