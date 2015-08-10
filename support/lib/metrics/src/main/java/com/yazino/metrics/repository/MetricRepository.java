package com.yazino.metrics.repository;

import com.yazino.metrics.model.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class MetricRepository {
    private static final Logger LOG = LoggerFactory.getLogger(MetricRepository.class);

    private final Map<String, Map<String, Metric>> metrics = new HashMap<>();
    private final ReadWriteLock metricsLock = new ReentrantReadWriteLock();

    public Map<String, Metric> byName(final String name) {
        notNull(name, "name may not be null");

        metricsLock.readLock().lock();
        try {
            return defaultIfNull(metrics.get(name), Collections.<String, Metric>emptyMap());

        } finally {
            metricsLock.readLock().unlock();
        }
    }

    public Metric byNameAndSource(final String name,
                                  final String source) {
        notNull(name, "name may not be null");

        metricsLock.readLock().lock();
        try {
            final Map<String, ? extends Metric> metricsForName = metrics.get(name);
            if (metricsForName != null) {
                return metricsForName.get(source);
            }
            return null;

        } finally {
            metricsLock.readLock().unlock();
        }
    }

    public void save(final String name,
                     final String source,
                     final Metric metric) {
        notBlank(name, "name may not be null or blank");
        notBlank(source, "source may not be null or blank");
        notNull(metric, "metric may not be null");

        metricsLock.writeLock().lock();
        try {
            Map<String, Metric> metricsForName = metrics.get(name);
            if (metricsForName == null) {
                metricsForName = new HashMap<>();
                metrics.put(name, metricsForName);
            }

            final Metric existingValue = metricsForName.get(source);
            if (existingValue != null && metric.getTimestamp().isBefore(existingValue.getTimestamp())) {
                LOG.warn("Ignored update to {} from {} as a new value exists in the repository ({} < {})",
                        name, source, metric.getTimestamp(), existingValue.getTimestamp());
            } else {
                metricsForName.put(source, metric);
            }

        } finally {
            metricsLock.writeLock().unlock();
        }
    }

}
