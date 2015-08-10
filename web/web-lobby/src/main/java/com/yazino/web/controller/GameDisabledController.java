package com.yazino.web.controller;


import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GameDisabledController {

    @RequestMapping("/publicCommand/gameDisabled")
    public String request(
            final ModelMap modelMap,
            @RequestParam(value = "gameType", required = false) final String gameType) {

        modelMap.put("gameType", gameType.replace("_", " "));
        return "gameDisabled";
    }

    @RequestMapping("/disabled/{gameType}")
    public String gameDisabled(final ModelMap modelMap,
                               @PathVariable("gameType") final String gameType) {
        return request(modelMap, gameType);
    }
}
