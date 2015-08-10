package com.yazino.web.controller.gameserver;

import com.yazino.platform.account.WalletService;
import com.yazino.platform.community.PlayerService;
import com.yazino.spring.security.AllowPublicAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller("legacyPlayerBalanceController")
public class PlayerBalanceController {

    private WalletService walletService;
    private PlayerService playerService;
    private LobbySessionCache lobbySessionCache;

    @Autowired
    public PlayerBalanceController(final WalletService walletService,
                                   final PlayerService playerService,
                                   final LobbySessionCache lobbySessionCache) {
        this.walletService = walletService;
        this.playerService = playerService;
        this.lobbySessionCache = lobbySessionCache;
    }

    @AllowPublicAccess
    @RequestMapping("/game-server/command/balance")
    public void balance(final HttpServletRequest request,
                        final HttpServletResponse response) throws IOException {
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        String balance = "0";
        if (session != null) {
            try {
                balance = walletService.getBalance(
                        playerService.getAccountId(session.getPlayerId())).toString();
            } catch (Throwable e) {
                //ignore
            }
        }
        response.setContentType("text/javascript");
        response.getWriter().write("{\"balance\": " + balance + "}");
    }

}
