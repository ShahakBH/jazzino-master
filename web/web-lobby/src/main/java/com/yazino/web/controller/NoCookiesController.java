package com.yazino.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class NoCookiesController {

    @RequestMapping({"/noCookies", "/public/noCookies"})
    public String index() {
        return "noCookies";
    }
}
