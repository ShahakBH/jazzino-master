package com.yazino.web.interceptor;

import com.yazino.web.util.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Stores a timestamp in the request to allow for performance tracking of request handling.
 */
public class PerformanceTrackingInterceptor extends HandlerInterceptorAdapter {

    private final Environment environment;

    @Autowired
    public PerformanceTrackingInterceptor(final Environment environment) {
        notNull(environment, "environment may not be null");

        this.environment = environment;
    }

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws Exception {
        if (environment.isDevelopment()) {
            request.setAttribute("handleStartNano", System.nanoTime());
        }
        return super.preHandle(request, response, handler);
    }
}
