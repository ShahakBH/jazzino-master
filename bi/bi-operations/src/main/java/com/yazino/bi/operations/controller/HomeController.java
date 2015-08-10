package com.yazino.bi.operations.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import com.yazino.bi.operations.util.SecurityInformationHelper;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class HomeController {
    private final SecurityInformationHelper security;

    @Autowired
    public HomeController(final SecurityInformationHelper security) {
        notNull(security, "security may not be null");

        this.security = security;
    }

    @RequestMapping({"/", "/home", "/index", "/index.htm", "/index.html"})
    public String home(final HttpServletResponse response) throws IOException {
        if (hasOneOfRoles("ROLE_SUPPORT", "ROLE_SUPPORT_MANAGER")) {
            return "redirect:/playerSearch";
        } else if (hasOneOfRoles("ROLE_MANAGEMENT", "ROLE_AD_TRACKING")) {
            return "redirect:/adTrackingReportDefinition";
        } else if (hasOneOfRoles("ROLE_MARKETING")) {
            return "redirect:/promotion/list";
        } else if (hasOneOfRoles("ROLE_ROOT")) {
            return "redirect:/userAdmin";
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return null;
    }

    private boolean hasOneOfRoles(final String... roles) {
        for (String role : roles) {
            if (security.getAccessRoles() != null && security.getAccessRoles().contains(role)) {
                return true;
            }
        }
        return false;
    }

}
