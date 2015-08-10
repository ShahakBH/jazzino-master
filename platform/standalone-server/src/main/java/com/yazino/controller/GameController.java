package com.yazino.controller;

import com.yazino.model.FlashvarsSource;
import com.yazino.model.StandaloneServerConfiguration;
import com.yazino.model.document.SendGameMessageRequest;
import com.yazino.model.session.StandalonePlayerSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentHeaderType;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class GameController {

    private final StandaloneServerConfiguration config;
    private final StandalonePlayerSession session;
    private final FlashvarsSource flashvarsSource;
    private final DocumentDispatcher documentDispatcher;

    @Autowired
    public GameController(final StandalonePlayerSession session,
                          final StandaloneServerConfiguration config,
                          final FlashvarsSource flashvarsSource,
                          @Qualifier("standaloneDocumentDispatcher")
                          final DocumentDispatcher documentDispatcher) {
        this.session = session;
        this.config = config;
        this.flashvarsSource = flashvarsSource;
        this.documentDispatcher = documentDispatcher;
    }

    @RequestMapping("/game/play")
    public ModelAndView play(final HttpServletRequest request) {
        final ModelAndView view = new ModelAndView("game/play");
        if (!session.isActive()) {
            view.addObject("message",
                    "You can't play without a session. <a href=\""
                            + request.getContextPath()
                            + "/players/new?return=/game/play\">Create one?</a>");
        }
        view.addObject("config", config);
        view.addObject("flashvars", flashvarsSource.getFlashvars());
        return view;
    }

    @RequestMapping(value = "/game/sendGameMessage", method = RequestMethod.POST)
    public ModelAndView sendGameMessage(@ModelAttribute("sendGameMessage") final SendGameMessageRequest request,
                                        final ModelMap map) {
        map.addAttribute("sendGameMessage", request);
        final Map<String, String> headers = new HashMap<String, String>();
        if (request.isTableMessage()) {
            headers.put(DocumentHeaderType.TABLE.getHeader(), "1");
        }
        String message = "Message sent.";
        try {
            final Document document = new Document(request.getType(), request.getBody(), headers);
            documentDispatcher.dispatch(document, request.convertPlayerIds());
        } catch (Exception e) {
            message = "Could not send message.";
        }
        return new ModelAndView("game/sendGameMessage", "message", message);
    }

    @RequestMapping(value = "/game/sendGameMessage", method = RequestMethod.GET)
    public String sendGameMessageForm(final ModelMap map) {
        map.addAttribute("sendGameMessage", new SendGameMessageRequest());
        return "game/sendGameMessage";
    }
}
