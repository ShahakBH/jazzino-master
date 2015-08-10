package com.yazino.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LegalController {

    @RequestMapping(value = "/legal/privacy")
    public String processPrivacy() {
        return "privacy";
    }

    @RequestMapping(value = "/legal/termsOfPurchase")
    public String processTermsOfPurchase() {
        return "termsOfPurchase";
    }

    @RequestMapping(value = "/legal/termsOfService")
    public String processTermsOfService() {
        return "termsOfService";
    }
}
