package com.yazino.controller;

import com.yazino.host.session.StandaloneSessionService;
import com.yazino.model.StandalonePlayer;
import com.yazino.model.StandalonePlayerService;
import com.yazino.model.session.StandalonePlayerSession;
import com.yazino.platform.Partner;
import com.yazino.platform.community.BasicProfileInformation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.util.HashMap;

@Controller
public class PlayerController {

    public static final String DEFAULT_VIEW = "players/list";
    public static final String PLAYER_SUCCESSFULLY_CREATED = "Player successfully created.";
    private final StandalonePlayerService playerService;
    private final StandalonePlayerSession playerSession;
    private final StandaloneSessionService sessionService;

    @Autowired
    public PlayerController(final StandalonePlayerService playerService,
                            final StandalonePlayerSession playerSession,
                            final StandaloneSessionService sessionService) {
        this.playerService = playerService;
        this.playerSession = playerSession;
        this.sessionService = sessionService;
    }

    @RequestMapping("/players/list")
    public ModelAndView players() {
        return getDefaultView();
    }

    @RequestMapping("/players/new")
    public ModelAndView newPlayer(@RequestParam(value = "return", required = false) final String returnUrl) {
        return new ModelAndView("players/create", "return", returnUrl);
    }

    @RequestMapping(value = "/players/create", method = RequestMethod.POST)
    public ModelAndView createPlayer(@RequestParam(value = "name") final String name,
                                     @RequestParam(value = "return", required = false) final String returnUrl) {
        final BigDecimal playerId = playerService.createPlayer(name);
        playerSession.setPlayer(playerId, name);
        sessionService.createSession(new BasicProfileInformation(playerId, name, "picture", playerId),
                Partner.YAZINO, "", "", "", null, null, new HashMap<String, Object>());
        if (!StringUtils.isBlank(returnUrl)) {
            return new ModelAndView(new RedirectView(returnUrl), "message", PLAYER_SUCCESSFULLY_CREATED);
        }
        return getDefaultView(PLAYER_SUCCESSFULLY_CREATED);
    }

    @RequestMapping(value = "/players/play_as/{playerId}", method = RequestMethod.GET)
    public ModelAndView playAs(@PathVariable final String playerId) {
        final BigDecimal realPlayerId = new BigDecimal(playerId);
        final StandalonePlayer player = playerService.findById(realPlayerId);
        playerSession.setPlayer(player.getPlayerId(), player.getName());
        final BasicProfileInformation profile = new BasicProfileInformation(realPlayerId,
                player.getName(),
                "picture",
                realPlayerId);
        sessionService.createSession(profile, Partner.YAZINO, "", "", "", null, null, new HashMap<String, Object>());
        return getDefaultView("Logged in as " + player.getName());
    }

    private ModelAndView getDefaultView() {
        final ModelAndView modelAndView = new ModelAndView(DEFAULT_VIEW);
        modelAndView.addObject("players", playerService.findAll());
        return modelAndView;
    }

    private ModelAndView getDefaultView(final String message) {
        final ModelAndView modelAndView = getDefaultView();
        modelAndView.addObject("message", message);
        return modelAndView;
    }
}
