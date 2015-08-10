package com.yazino.web.controller.gameserver;

import com.yazino.spring.security.AllowPublicAccess;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import com.yazino.web.data.PictureRepository;
import com.yazino.web.data.LevelRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class PlayerPictureAndLevelController {

    private PictureRepository pictureRepository;
    private LevelRepository levelRepository;

    @Autowired(required = true)
    public void setPictureRepository(@Qualifier("pictureRepository") final PictureRepository pictureRepository) {
        this.pictureRepository = pictureRepository;
    }

    @Autowired(required = true)
    public void setLevelRepository(@Qualifier("levelRepository") final LevelRepository levelRepository) {
        this.levelRepository = levelRepository;
    }

    @AllowPublicAccess
    @RequestMapping("/game-server/command/pictureAndLevel")
    public void handlePicture(final HttpServletRequest request,
                              final HttpServletResponse response) throws Exception {
        final String gameType = request.getParameter("gameType");
        if (StringUtils.isBlank(gameType)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "gameType is required");
            return;
        }

        final String allPlayerIds = request.getParameter("playerIds");
        if (StringUtils.isBlank(allPlayerIds)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "playerIds is required");
            return;
        }

        final String[] tokens = allPlayerIds.split(",");
        final List<BigDecimal> playerIds = new ArrayList<>();
        for (String playerIdStr : tokens) {
            try {
                playerIds.add(new BigDecimal(playerIdStr.trim()));
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "invalid playerId: " + playerIdStr);
                return;
            }
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("[");
        final Iterator<BigDecimal> i = playerIds.iterator();
        while (i.hasNext()) {
            final BigDecimal playerId = i.next();
            final String pictureUrl = pictureRepository.getPicture(playerId);
            final Integer level = levelRepository.getLevel(playerId, gameType);
            final String entry = String.format("{\"picture\":\"%s\",\"level\":%s}", pictureUrl, level);
            builder.append(entry);
            if (i.hasNext()) {
                builder.append(",");
            }
        }

        builder.append("]");
        response.setContentType("text/javascript");
        response.getWriter().write(builder.toString());
    }
}
