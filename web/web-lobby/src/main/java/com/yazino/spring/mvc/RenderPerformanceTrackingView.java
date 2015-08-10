package com.yazino.spring.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public class RenderPerformanceTrackingView implements View {
    private static final Logger LOG = LoggerFactory.getLogger(RenderPerformanceTrackingView.class);

    private static final long NANO_TO_MS = 1000000L;

    private final View delegate;

    public RenderPerformanceTrackingView(final View delegate) {
        notNull(delegate, "delegate may not be null");

        this.delegate = delegate;
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public void render(final Map<String, ?> model,
                       final HttpServletRequest request,
                       final HttpServletResponse response) throws Exception {
        final long renderStartNano = System.nanoTime();
        ((Map) model).put("renderStartNano", renderStartNano);
        ((Map) model).put("System", System.class);

        delegate.render(model, request, response);

        LOG.debug("View rendered in {} ms", ((System.nanoTime() - renderStartNano) / NANO_TO_MS));
    }
}
