package com.yazino.spring.mvc;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A view for redirecting to external sites.
 * <p/>
 * This requires two modifications:
 * <ul>
 * <li>The model is not exposed</li>
 * <li>JSessionID is not added to the URL</li>
 * </ul>
 */
public class ExternalRedirectView extends RedirectView {

    private HttpStatus statusCode;

    public ExternalRedirectView(final String url) {
        super(url, false, true, false);
    }

    @Override
    public void setStatusCode(final HttpStatus statusCode) {
        this.statusCode = statusCode;
        super.setStatusCode(statusCode);
    }

    @Override
    protected void sendRedirect(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final String targetUrl,
                                final boolean http10Compatible) throws IOException {
        if (http10Compatible) {
            if (this.statusCode != null) {
                response.setStatus(this.statusCode.value());
                response.setHeader("Location", targetUrl);
            } else {
                // Send status code 302 by default.
                response.sendRedirect(targetUrl);
            }

        } else {
            response.setStatus(getHttp11StatusCode(request, response, targetUrl).value());
            response.setHeader("Location", targetUrl);
        }
    }
}
