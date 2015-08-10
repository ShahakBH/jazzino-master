package com.yazino.web.controller.gameserver;

import com.yazino.spring.security.AllowPublicAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Writer;

@Controller
public class CredentialsController {
    private LobbySessionCache lobbySessionCache;

    @Autowired(required = true)
    public void setLobbySessionCache(@Qualifier("lobbySessionCache") final LobbySessionCache lobbySessionCache) {
        this.lobbySessionCache = lobbySessionCache;
    }

    @AllowPublicAccess
    @RequestMapping("/game-server/command/credentials")
    public ModelAndView handleCommand(final HttpServletRequest request,
                                      final HttpServletResponse response) throws Exception {
        response.setContentType("text/javascript");

        final Writer output = response.getWriter();
        final LobbySession session = lobbySessionCache.getActiveSession(request);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        if (session == null) {
            output.write("/*not logged on */");
        } else {
            output.write("window.strataDevApi.setPartnerId(\"" + session.getPartnerId() + "\");\n");
            output.write("window.strataDevApi.setPlayerId(\"" + session.getPlayerId() + "\");\n");
            output.write("window.strataDevApi.setSessionKey(\"" + session.getLocalSessionKey() + "\");\n");
            output.write("window.strataDevApi.setPlayerName(\"" + session.getPlayerName() + "\");\n");
        }
        output.flush();
        output.close();
        return null;
    }
}
