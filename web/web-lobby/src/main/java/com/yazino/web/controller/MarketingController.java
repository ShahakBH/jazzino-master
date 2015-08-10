package com.yazino.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * This class handles requests for static content independent of whether you are logged in or not.
 */
@Controller
public class MarketingController {
    static final String PAGE_NAME = "pageName";
    static final String WHO = "person";

    @RequestMapping({"/mytable", "/public/mytable"})
    public String processMyTableMarketing() {
        return "myTableMarketing";
    }

    @RequestMapping({"/aboutus", "/aboutUs", "/public/aboutus", "/public/aboutUs"})
    public String processAboutUs(final ModelMap modelMap) {
        modelMap.addAttribute(PAGE_NAME, "about-us");
        return "aboutUs";
    }

    @RequestMapping({"/sitemap", "/siteMap", "/public/sitemap", "/public/siteMap"})
    public String processSiteMap() {
        return "siteMap";
    }

    @RequestMapping({"/contactus", "/contactUs", "/contact", "/public/contactus",
            "/public/contact", "/public/contactUs"})
    public String processContactUs(final ModelMap modelMap) {
        modelMap.addAttribute(PAGE_NAME, "contact-us");
        return "contactUs";
    }

    @RequestMapping("/jobs")
    public String jobs() {
        return "jobs";
    }

    @RequestMapping({"/management", "/management/hussein"})
    public String managementPageHussein(final ModelMap modelMap) {
        modelMap.addAttribute(WHO, "hussein");
        return "management";
    }

    @RequestMapping("/management/melissa")
    public String managementPageMelissa(final ModelMap modelMap) {
        modelMap.addAttribute(WHO, "melissa");

        return "management";
    }

    @RequestMapping("/management/alyssa")
    public String managementPageAlyssa(final ModelMap modelMap) {
        modelMap.addAttribute(WHO, "alyssa");

        return "management";
    }

    @RequestMapping("/management/john")
    public String managementPageJon(final ModelMap modelMap) {
        modelMap.addAttribute(WHO, "john");

        return "management";
    }

    @RequestMapping("/management/guillaume")
    public String managementPageGuillaume(final ModelMap modelMap) {
        modelMap.addAttribute(WHO, "guillaume");

        return "management";
    }

    @RequestMapping("/management/stephan")
    public String managementPageStephan(final ModelMap modelMap) {
        modelMap.addAttribute(WHO, "stephan");

        return "management";
    }


}
