package com.yazino.web.util;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Writes an object to the response as JSON.
 */
@Component
public class WebApiResponses {

    private static final int DEFAULT_STATUS_CODE = HttpStatus.SC_OK;
    public static final Map<String, String> NO_CONTENT = Collections.emptyMap(); // using an empty map to avoid EMPTY_BEAN
    // error without changing the configuration

    private final WebApiResponseWriter responseWriter;

    @Autowired
    public WebApiResponses(WebApiResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    public void write(final HttpServletResponse response, int httpStatusCode, final Object object) throws IOException {
        responseWriter.write(response, httpStatusCode, object);
    }

    public void write(final HttpServletResponse response, final Object object) throws IOException {
        responseWriter.write(response, object);
    }

    public void writeOk(final HttpServletResponse response, final Object object) throws IOException {
        write(response, DEFAULT_STATUS_CODE, object);
    }

    public void writeError(final HttpServletResponse response, int httpStatusCode, String message) throws IOException {
        write(response, httpStatusCode, new ErrorResponse(message));
    }

    public void writeNoContent(final HttpServletResponse response, int httpStatusCode) throws IOException {
        write(response, httpStatusCode, NO_CONTENT);
    }

    static class ErrorResponse {
        private String error;

        /* deserialisation support */
        private ErrorResponse() {
        }

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            ErrorResponse rhs = (ErrorResponse) obj;
            return new EqualsBuilder()
                    .append(this.error, rhs.error)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                    .append(error)
                    .toHashCode();
        }
    }

}
