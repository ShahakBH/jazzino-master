package com.yazino.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class FriendsController {
    @RequestMapping({"/lobby/friends", "/friends"})
    public String friends() {
        return "friendsMarketing";
    }
}
