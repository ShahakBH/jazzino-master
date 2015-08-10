package com.yazino.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class RulesController {

    @RequestMapping({"/rules/texasHoldem",
            "/rules/TexasHoldem",
            "/rules/TEXAS_HOLDEM",
            "/rules/poker",
            "/rules/Poker",
            "/rules/POKER"})
    public String processTexasHoldemRules() {
        return "texasHoldemRules";
    }

    @RequestMapping({"/rules/blackjack",
            "/rules/blackJack",
            "/rules/BlackJack",
            "/rules/BLACKJACK"})
    public String processBlackjackRules() {
        return "blackjackRules";
    }

    @RequestMapping({"/rules/roulette",
            "/rules/Roulette",
            "/rules/ROULETTE"})
    public String processRouletteRules() {
        return "rouletteRules";
    }

    @RequestMapping({"/rules/slots",
            "/rules/Slots",
            "/rules/SLOTS",
            "/rules/wheeldeal",
            "/rules/wheelDeal",
            "/rules/WheelDeal",
            "/rules/WHEEL_DEAL"})
    public String processSlotsRules() {
        return "slotsRules";
    }

    @RequestMapping({"/rules/highstakes",
            "/rules/highStakes",
            "/rules/HighStakes",
            "/rules/HIGH_STAKES"})
    public String processHighStakesRules() {
        return "highStakesRules";
    }

}
