package com.yazino.web.controller;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.spring.security.AllowPublicAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class RobotsController {
    private final YazinoConfiguration yazinoConfiguration;

    @Autowired
    public RobotsController(final YazinoConfiguration yazinoConfiguration) {
        notNull(yazinoConfiguration, "yazinoConfiguration may not be null");

        this.yazinoConfiguration = yazinoConfiguration;
    }

    @AllowPublicAccess
    @RequestMapping({"/publicCommand/robots", "/robots", "/robots.txt"})
    public String robots(final HttpServletResponse response) throws IOException {
        if (yazinoConfiguration.getBoolean("robots.reject")) {
            return "robots";

        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
    }
}
