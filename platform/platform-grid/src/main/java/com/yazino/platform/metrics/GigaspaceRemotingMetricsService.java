package com.yazino.platform.metrics;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.gigaspaces.async.AsyncResult;
import com.yazino.platform.grid.Executor;
import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.remoting.RemotingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingMetricsService implements MetricsService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingMetricsService.class);

    private final Executor executor;

    @Autowired
    public GigaspaceRemotingMetricsService(final Executor executor) {
        notNull(executor, "executor may not be null");

        this.executor = executor;
    }

    @Override
    public Map<String, BigDecimal> getMeters(final Range range) {
        notNull(range, "range may not be null");

        return executor.mapReduce(new GetMetersTask(range));
    }

    @AutowireTask
    public static class GetMetersTask implements DistributedTask<HashMap<String, BigDecimal>, HashMap<String, BigDecimal>> {
        private static final long serialVersionUID = -1482878486569787066L;
        private static final int RESULT_SCALE = 3;

        @Resource
        private transient MetricRegistry metrics;

        private final Range range;

        public GetMetersTask(final Range range) {
            this.range = range;
        }

        @Override
        public HashMap<String, BigDecimal> execute() throws Exception {
            LOG.debug("Getting meters for period: {}", range);

            final HashMap<String, BigDecimal> meterValues = new HashMap<>();

            for (Map.Entry<String, Meter> meterAndName : metrics.getMeters().entrySet()) {
                final Meter meter = meterAndName.getValue();
                meterValues.put(meterAndName.getKey(), valueOf(meter));
            }

            return meterValues;
        }

        private BigDecimal valueOf(final Meter meter) {
            switch (range) {
                case FIFTEEN_MINUTES:
                    return new BigDecimal(meter.getFifteenMinuteRate());

                case FIVE_MINUTES:
                    return new BigDecimal(meter.getFiveMinuteRate());

                case ONE_MINUTE:
                    return new BigDecimal(meter.getOneMinuteRate());

                case MEAN:
                    return new BigDecimal(meter.getMeanRate());

                case COUNT:
                    return new BigDecimal(meter.getCount());

                default:
                    throw new IllegalArgumentException("Unknown range: " + range);
            }
        }

        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        @Override
        public HashMap<String, BigDecimal> reduce(final List<AsyncResult<HashMap<String, BigDecimal>>> results) throws Exception {
            final HashMap<String, BigDecimal> mergedResults = new HashMap<>();

            if (results != null) {
                for (final AsyncResult<HashMap<String, BigDecimal>> result : results) {
                    if (result.getException() != null) {
                        LOG.error("Remote invocation failed", result.getException());
                        continue;
                    }

                    final Map<String, BigDecimal> partitionResult = result.getResult();
                    if (partitionResult != null) {
                        for (String name : partitionResult.keySet()) {
                            final BigDecimal newValue = defaultIfNull(partitionResult.get(name), BigDecimal.ZERO)
                                    .setScale(RESULT_SCALE, RoundingMode.HALF_EVEN);
                            if (mergedResults.containsKey(name)) {
                                mergedResults.put(name, mergedResults.get(name).add(newValue));
                            } else {
                                mergedResults.put(name, newValue);
                            }
                        }
                    }
                }
            }

            return mergedResults;
        }
    }
}
