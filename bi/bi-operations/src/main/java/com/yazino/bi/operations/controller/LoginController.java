package com.yazino.bi.operations.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/login")
public class LoginController {

    @RequestMapping
    public String login() {
        return "login";
    }

    @RequestMapping("/error")
    public ModelAndView loginError(final HttpSession session) {
        return new ModelAndView("login")
                .addObject("error", lastSpringSecurityError(session));
    }

    private String lastSpringSecurityError(final HttpSession session) {
        final Exception lastException = (Exception) session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
        if (lastException != null) {
            return lastException.getMessage();
        }
        return null;
    }

}
