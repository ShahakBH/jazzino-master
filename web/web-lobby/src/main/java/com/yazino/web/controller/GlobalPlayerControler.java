package com.yazino.web.controller;

import com.yazino.web.domain.world.GlobalPlayersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.apache.commons.lang3.Validate.notNull;

@Controller
public class GlobalPlayerControler {

    private final GlobalPlayersRepository globalPlayersRepository;

    @Autowired(required = true)
    public GlobalPlayerControler(
            @Qualifier("globalPlayersRepository") final GlobalPlayersRepository globalPlayersRepository) {
        notNull(globalPlayersRepository, "globalPlayersRepository is null");
        this.globalPlayersRepository = globalPlayersRepository;
    }

    @RequestMapping("/lobbyCommand/globalPlayers")
    public void globalPlayers(final HttpServletResponse response) throws IOException {
        response.setContentType("text/javascript");
        response.getWriter().write(globalPlayersRepository.getPlayerLocations());
    }

}
