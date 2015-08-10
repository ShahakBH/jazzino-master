package com.yazino.web.controller;


import com.yazino.spring.security.AllowPublicAccess;
import com.yazino.web.security.LogoutHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class LogoutController {
    private static final Logger LOG = LoggerFactory.getLogger(LogoutController.class);

    private final LogoutHelper logoutHelper;
    private final String logoutAction;

    @Autowired
    public LogoutController(final LogoutHelper logout,
                            @Value("${senet.web.host}") final String logoutAction) {
        notNull(logout, "logout may not be null");
        notNull(logoutAction, "logoutAction may not be null");

        this.logoutHelper = logout;
        this.logoutAction = logoutAction;
    }

    @RequestMapping(value = {"/logout", "/lobby/logout"}, method = RequestMethod.GET)
    public void logout(final HttpSession session,
                       final HttpServletRequest request,
                       final HttpServletResponse response)
            throws IOException {
        logoutHelper.logout(session, request, response);
        redirect(response);
    }

    @AllowPublicAccess
    @RequestMapping("/blocked")
    public ModelAndView blocked(final HttpSession session,
                                final HttpServletRequest request,
                                final HttpServletResponse response,
                                @RequestParam(required = false) final String reason) {
        logoutHelper.logout(session, request, response);
        return new ModelAndView("blocked")
                .addObject("reason", messageFor(reason));
    }

    private String messageFor(final String reason) {
        if (reason == null) {
            return null;
        }

        switch (reason) {
            case "payment":
                return "Sorry, your card provider has not authorised this transaction. "
                        + "For your protection, we have automatically blocked your Yazino account.";

            default:
                return null;
        }
    }

    private void redirect(final HttpServletResponse response) throws IOException {
        try {
            response.sendRedirect(logoutAction);
        } catch (IOException e) {
            LOG.warn("Redirection to {} failed: {}", logoutAction, e.getMessage());
        } catch (IllegalStateException e) {
            // committed response, ignore
        }
    }
}
