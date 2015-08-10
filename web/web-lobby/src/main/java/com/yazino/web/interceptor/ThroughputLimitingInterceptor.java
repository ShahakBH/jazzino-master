package com.yazino.web.interceptor;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.web.util.MinuteBoundedCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ThroughputLimitingInterceptor extends HandlerInterceptorAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(ThroughputLimitingInterceptor.class);

    public static final boolean PROCESS_REQUEST = true;
    public static final boolean BLOCK_REQUEST = false;

    private final YazinoConfiguration yazinoConfiguration;
    private final MinuteBoundedCounter minuteBoundedCounter;

    @Autowired
    public ThroughputLimitingInterceptor(YazinoConfiguration yazinoConfiguration,
                                         MinuteBoundedCounter minuteBoundedCounter) {
        this.yazinoConfiguration = yazinoConfiguration;
        this.minuteBoundedCounter = minuteBoundedCounter;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws Exception {
        if (isNotApplicable(request)) {
            return PROCESS_REQUEST;
        }

        int hits = minuteBoundedCounter.incrementAndGet();
        int loggingFrequency = yazinoConfiguration.getInt("tracking.logging.frequency.blocked-requests");
        if (isBlockingRequests(hits)) {
            if (hits % loggingFrequency == 0) {
                LOG.warn("Blocking request (maximum hits per minute exceeded). {} hits recorded in current minute. Note: only logging every {} blocked "
                        + "requests", hits, loggingFrequency);
            }
            return BLOCK_REQUEST;
        } else {
            return PROCESS_REQUEST;
        }
    }

    private boolean isNotApplicable(HttpServletRequest request) {
        // This class was introduced to govern traffic to the TrackingController
        // To make it generally useful the hit counter would probably need to be per-url or per some other grouping
        // TODO consider merging into TrackingController
        return !request.getRequestURI().startsWith("/tracking/event");
    }

    private boolean isBlockingRequests(int hits) {
        return hits > yazinoConfiguration.getInt("tracking.max-hits-per-minute");
    }
}
