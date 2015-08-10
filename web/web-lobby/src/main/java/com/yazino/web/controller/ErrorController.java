package com.yazino.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class ErrorController {
    @RequestMapping("/error/fileNotFound")
    public String processFileNotFound(final HttpServletRequest request) {
        if (clientAcceptsHTML(request)) {
            return "error404";
        }
        return null;
    }

    @RequestMapping("/error/sessionExpired")
    public String processSessionExpired(final HttpServletRequest request) {
        if (clientAcceptsHTML(request)) {
            return "error401";
        }
        return null;
    }

    @RequestMapping("/error/internalError")
    public String processInternalError(final HttpServletRequest request) {
        if (clientAcceptsHTML(request)) {
            return "error500";
        }
        return null;
    }

    private boolean clientAcceptsHTML(final HttpServletRequest request) {
        final String acceptHeader = request.getHeader("Accept");
        return acceptHeader == null || acceptHeader.contains("text/html") || acceptHeader.contains("*/*");
    }
}
