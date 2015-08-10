package com.yazino.web.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping(value = "/progressBarStatus")
public class ProgressBarController {


    @RequestMapping(method = RequestMethod.GET)
    public void getProgressBarStatus(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON.getType());
        response.getWriter().write("{}");
    }

    @RequestMapping(method = RequestMethod.POST)
    public void updateProgressBarStatus(final HttpServletRequest request, final HttpServletResponse response,
                                        @RequestParam("fb_access_token") final String fbAccessToken)
            throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON.getType());
        response.getWriter().write("{}");
    }

    @RequestMapping(method = RequestMethod.POST, value = "/confirm")
    public void confirmProgressBarStatusWasDisplayed(final HttpServletRequest request,
                                                     final HttpServletResponse response)
            throws IOException {
        response.getWriter().write("{}");
    }

}
