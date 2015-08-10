package com.yazino.spring.mvc;

import com.yazino.web.util.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;

import static org.apache.commons.lang3.Validate.notNull;

public class RenderPerformanceTrackingViewResolver implements ViewResolver, Ordered {

    private final ViewResolver delegate;
    private final Environment environment;

    private int order;

    @Autowired
    public RenderPerformanceTrackingViewResolver(final ViewResolver delegate,
                                                 final Environment environment) {
        notNull(delegate, "delegate may not be null");
        notNull(environment, "environment may not be null");

        this.delegate = delegate;
        this.environment = environment;
    }

    @Override
    public View resolveViewName(final String viewName,
                                final Locale locale) throws Exception {
        final View resolvedView = delegate.resolveViewName(viewName, locale);
        if (environment.isDevelopment() && resolvedView != null) {
            return new RenderPerformanceTrackingView(resolvedView);
        }
        return resolvedView;
    }

    public void setOrder(final int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
