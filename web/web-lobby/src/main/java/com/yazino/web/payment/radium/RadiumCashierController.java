package com.yazino.web.payment.radium;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Controller("radiumCashierController")
@RequestMapping("/payment/radium/")// NOTE! urlrewrite.xml is used to modify this from /strata.server.lobby.radium/
public class RadiumCashierController {
    private static final Logger LOG = LoggerFactory.getLogger(RadiumCashierController.class);

    private final RadiumService radiumService;

    @Autowired
    public RadiumCashierController(final RadiumService radiumService) {
        this.radiumService = radiumService;
    }


    @RequestMapping("/callback")
    public void callback(@RequestParam("amount") final String chipAmount,
                         @RequestParam("appId") final String appId,
                         @RequestParam("hash") final String hash,
                         @RequestParam("trackId") final String trackId,
                         @RequestParam("userId") final String userId,
                         @RequestParam("pid") final String pid,
                         final HttpServletRequest request,
                         final HttpServletResponse response)
            throws ServletException {
        if (radiumService.payoutChipsAndNotifyPlayer(chipAmount, appId, hash, trackId,
                userId, pid, request.getRemoteAddr())) {
            sendResponse(response, "1");
        } else {
            sendResponse(response, "0");
        }

    }

    private void sendResponse(final HttpServletResponse response, final String responseCode) {
        try {
            response.setContentType("text/plain");
            response.getWriter().write(responseCode);
        } catch (IOException e) {
            LOG.error("Couldn't write response", e);
            throw new RuntimeException("Response write failed", e);
        }
    }


}
