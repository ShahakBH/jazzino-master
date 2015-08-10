package com.yazino.platform.metrics;

import java.math.BigDecimal;
import java.util.Map;

public interface MetricsService {

    enum Range {
        ONE_MINUTE,
        FIVE_MINUTES,
        FIFTEEN_MINUTES,
        MEAN,
        COUNT
    }

    Map<String, BigDecimal> getMeters(Range range);

}
