package com.yazino.web.controller.profile;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Controller
public class DefaultProfileTabRedirector {

    public static final String PARTIAL_PARAM_KEY = "partial";
    public static final String DEFAULT_TAB_URI = "/player/profile";

    @RequestMapping("/player")
    public View defaultTabRedirect(final HttpServletRequest request) throws UnsupportedEncodingException {
        final String[] partialParams = request.getParameterValues(PARTIAL_PARAM_KEY);
        String queryString = "";
        if (partialParams != null) {
            queryString = "?" + PARTIAL_PARAM_KEY + "=" + URLEncoder.encode(partialParams[0], "utf-8");
        }
        return new RedirectView(DEFAULT_TAB_URI + queryString, false, false, false);
    }

}
