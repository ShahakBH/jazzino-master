package com.yazino.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PublishController {
    private static final String VIEW_NAME_CURRENT = "partials/publishCurrent";
    private static final String VIEW_NAME_ORIGINAL = "partials/publishOriginal";

    @RequestMapping("/lobby/publishCurrent")
    public String showPublishCurrentPage() {
        return VIEW_NAME_CURRENT;
    }

    @RequestMapping("/lobby/publishOriginal")
    public String showPublishOriginalPage() {
        return VIEW_NAME_ORIGINAL;
    }
}
