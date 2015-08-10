package com.yazino.web.controller.gameserver;

import com.yazino.spring.security.AllowPublicAccess;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class StatisticsController {

    @AllowPublicAccess
    @RequestMapping({"/game-server/command/statistics", "/game-server/command/statistics/"})
    public void handleRequest(final HttpServletResponse response) throws IOException {
        // This does nothing, as we use the access logs to track statistics.

        response.setContentType("text/plain");
        response.getOutputStream().write('\n');
    }

}
