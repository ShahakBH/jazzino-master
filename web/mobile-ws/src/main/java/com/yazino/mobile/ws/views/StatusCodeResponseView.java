package com.yazino.mobile.ws.views;

import org.apache.commons.lang3.Validate;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * A view that will write a status code to a response.
 */
public class StatusCodeResponseView implements View {

    private final int mStatusCode;
    private String mContentType = MediaType.ALL.getType();

    public StatusCodeResponseView(final int statusCode) {
        mStatusCode = statusCode;
    }

    @Override
    public String getContentType() {
        return mContentType;
    }

    public void setContentType(final String contentType) {
        Validate.notNull(contentType);
        mContentType = contentType;
    }

    @Override
    public void render(final Map<String, ?> model, final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {
        response.getWriter().write(mStatusCode + " - " + request.getPathInfo());
        response.setStatus(mStatusCode);
        response.flushBuffer();
    }
}
