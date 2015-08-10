package com.yazino.web.controller;

import com.yazino.web.util.WebApiResponses;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class VersionController {
    private static final Logger LOG = LoggerFactory.getLogger(VersionController.class);

    private static final String POM_PROPERTIES = "/META-INF/maven/com.yazino/web-lobby/pom.properties";
    private static final String DEVELOPMENT_VERSION = "development";

    private final WebApiResponses webApiResponses;

    @Autowired
    public VersionController(final WebApiResponses webApiResponses) {
        notNull(webApiResponses, "webApiResponses may not be null");

        this.webApiResponses = webApiResponses;
    }

    @RequestMapping("/command/version")
    public void showVersionInformation(final HttpServletRequest request,
                                       final HttpServletResponse response)
            throws IOException {
        final ServletContext servletContext = request.getSession().getServletContext();
        if (servletContext == null) {
            LOG.error("ServletContext has not been injected");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        try {
            webApiResponses.writeOk(response, singletonMap("version", readVersion(servletContext, POM_PROPERTIES)));

        } catch (Exception e) {
            LOG.error("POM properties could not be read", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private Object readVersion(final ServletContext servletContext,
                               final String propertiesFile)
            throws IOException {
        InputStream in = null;
        try {
            in = servletContext.getResourceAsStream(propertiesFile);
            if (in == null) {
                return DEVELOPMENT_VERSION;
            }

            final Properties mProps = new Properties();
            mProps.load(in);
            return mProps.get("version");

        } finally {
            IOUtils.closeQuietly(in);
        }
    }

}
