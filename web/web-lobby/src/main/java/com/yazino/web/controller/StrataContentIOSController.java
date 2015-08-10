package com.yazino.web.controller;

import com.yazino.spring.security.AllowPublicAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;

/**
 * Redirects calls for the legacy strata.content context to the cdn (or whatever's specified by senet.web.content).
 * Used by old IOS clients.
 */
@Controller
@AllowPublicAccess
@RequestMapping("/strata.content/mobile/ios/**")
public class StrataContentIOSController {
    private static final Logger LOG = LoggerFactory.getLogger(StrataContentIOSController.class);

    private static final String ORIGINAL_CONTEXT = "/strata.content";

    private final String webContentURI;

    @Autowired
    public StrataContentIOSController(@Value("${senet.web.content}") String webContentURI) {
        this.webContentURI = webContentURI;
    }

    @RequestMapping(method = RequestMethod.GET)
    public RedirectView redirect(HttpServletRequest request) throws MalformedURLException {
        StringBuffer url = request.getRequestURL();
        String sourceURL = url.toString();
        int contentEndIndex = url.indexOf(ORIGINAL_CONTEXT) + ORIGINAL_CONTEXT.length();
        url.replace(0, contentEndIndex, webContentURI);
        String targetURL = url.toString();

        LOG.debug("Redirecting {} to {}", sourceURL, targetURL);
        RedirectView view = new RedirectView(targetURL);
        view.setExposeModelAttributes(false);
        view.setExpandUriTemplateVariables(false);
        view.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        return view;
    }

}
