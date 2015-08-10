package com.yazino.web.controller;

import com.yazino.platform.session.SessionService;
import com.yazino.web.util.WebApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class LoadBalancerController {
    private static final Logger LOG = LoggerFactory.getLogger(LoadBalancerController.class);

    private static final String DEFAULT_SUSPENSION_FILE = "/etc/senet/suspendedFromLoadBalancer";

    private WebApiResponses webApiResponses;
    private final File suspensionFile;
    private final SessionService sessionService;

    @Autowired
    public LoadBalancerController(final SessionService sessionService,
                                  final WebApiResponses webApiResponses) {
        notNull(sessionService, "sessionService may not be null");
        notNull(webApiResponses, "webApiResponses may not be null");

        this.sessionService = sessionService;
        this.webApiResponses = webApiResponses;
        this.suspensionFile = new File(DEFAULT_SUSPENSION_FILE);
    }

    LoadBalancerController(final SessionService sessionService,
                           final WebApiResponses webApiResponses,
                           final File suspensionFile) {
        notNull(sessionService, "sessionService may not be null");
        notNull(webApiResponses, "webApiResponses may not be null");
        notNull(suspensionFile, "suspensionFile may not be null");

        this.sessionService = sessionService;
        this.webApiResponses = webApiResponses;
        this.suspensionFile = suspensionFile;
    }

    @RequestMapping(value = "/command/loadBalancerStatus", produces = MediaType.APPLICATION_JSON_VALUE)
    public void checkStatus(final HttpServletResponse response) throws IOException {
        webApiResponses.writeOk(response, singletonMap("status", findStatus()));
    }

    private String findStatus() {
        if (suspensionFile.exists()) {
            return "suspended";
        }

        if (gridConnectionFails()) {
            return "grid-error";
        }

        return "okay";
    }

    private boolean gridConnectionFails() {
        try {
            sessionService.countSessions(false);
            return false;

        } catch (Exception e) {
            LOG.error("Couldn't access grid", e);
            return true;
        }
    }

}
