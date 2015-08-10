package com.yazino.web.controller;


import com.yazino.platform.account.WalletService;
import com.yazino.platform.community.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class PlayerBalanceController {
    private final PlayerService playerService;
    private final WalletService walletService;

    @Autowired
    public PlayerBalanceController(final PlayerService playerService,
                                   final WalletService walletService) {
        notNull(playerService, "playerService is null");
        notNull(walletService, "walletService is null");

        this.playerService = playerService;
        this.walletService = walletService;
    }

    @RequestMapping({"/lobbyCommand/balance", "/player/balance"})
    public void balance(@RequestParam(value = "playerId", required = false) final String playerId,
                        final HttpServletResponse response) throws IOException {
        String balance = "0";
        if (playerId != null) {
            try {
                balance = walletService.getBalance(
                        playerService.getAccountId(new BigDecimal(playerId))).toPlainString();
            } catch (Throwable e) {
                //ignore
            }
        }
        response.setContentType("text/javascript");
        response.getWriter().write("{\"balance\": " + balance + "}");
    }

}
