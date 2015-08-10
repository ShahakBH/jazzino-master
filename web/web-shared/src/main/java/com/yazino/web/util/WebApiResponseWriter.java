package com.yazino.web.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class WebApiResponseWriter {
    private static final Logger LOG = LoggerFactory.getLogger(WebApiResponseWriter.class);

    private final ObjectMapper mapper = new ObjectMapper();

    public WebApiResponseWriter() {
        mapper.registerModule(new JodaModule());
    }

    /**
     * @deprecated stick with the default constructor, please.
     */
    public WebApiResponseWriter(final boolean includeOnlyNonNull) {
        this();
        if (includeOnlyNonNull) {
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
    }

    public void write(final HttpServletResponse response,
                      final Object object) throws IOException {
        if (response.isCommitted()) {
            LOG.warn("Cannot write response with object {}; response is already committed", object);
            return;
        }

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        mapper.writeValue(response.getWriter(), object);
        response.flushBuffer();
    }

    public void write(final HttpServletResponse response,
                      final int httpStatusCode,
                      final Object object) throws IOException {
        if (response.isCommitted()) {
            LOG.warn("Cannot write response with status {} and object {}; response is already committed", httpStatusCode, object);
            return;
        }

        response.setStatus(httpStatusCode);
        write(response, object);
    }
}
