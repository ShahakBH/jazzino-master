package com.yazino.web.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Validate.notNull;

public final class RequestParameterUtils {
    private static final Logger LOG = LoggerFactory.getLogger(RequestParameterUtils.class);

    private static final int HTTP_INVALID_REQUEST = 400;

    private RequestParameterUtils() {
        // utility class
    }

    public static boolean hasParameter(final String parameterName,
                                       final Object parameterValue,
                                       final HttpServletRequest request,
                                       final HttpServletResponse response) {
        notNull(parameterName, "parameterName may not be null");
        notNull(request, "request may not be null");
        notNull(response, "response may not be null");

        if (isNullOrBlank(parameterValue)) {
            LOG.warn(parameterName + " is missing from request " + request.getRequestURI());

            try {
                response.sendError(HTTP_INVALID_REQUEST);
            } catch (Exception e) {
                // ignored
            }

            return false;
        }
        return true;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private static boolean isNullOrBlank(final Object parameterValue) {
        if (parameterValue == null) {
            return true;
        }
        if (parameterValue instanceof String) {
            return isBlank(parameterValue.toString());
        }
        return false;
    }

}
