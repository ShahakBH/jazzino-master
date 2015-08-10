package com.yazino.web.payment.amazon;

import com.yazino.web.util.WebApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class HttpResponseHandler {
    private WebApiResponses responseWriter;

    @Autowired
    public HttpResponseHandler(final WebApiResponses responseWriter) {
        this.responseWriter = responseWriter;
    }

    public boolean safeWriteOk(HttpServletResponse response, Object result) {
        try {
            responseWriter.writeOk(response, result);
        } catch (IOException | IllegalStateException e) {
            return false;
        }
        return true;
    }

    public boolean safeWriteEmptyResponse(HttpServletResponse response, int httpStatus) {
        try {
            responseWriter.writeNoContent(response, httpStatus);
        } catch (IOException | IllegalStateException e) {
            return false;
        }
        return true;
    }
}
