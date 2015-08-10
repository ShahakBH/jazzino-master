package com.yazino.web.controller.gameserver;

import com.yazino.spring.security.AllowPublicAccess;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import com.yazino.web.domain.DefaultPicture;
import com.yazino.web.data.PictureRepository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;

@Controller
public class PlayerPictureController {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerPictureController.class);

    private PictureRepository pictureRepository;
    private DefaultPicture defaultPicture;

    @Autowired(required = true)
    public void setPictureRepository(@Qualifier("pictureRepository") final PictureRepository pictureRepository) {
        this.pictureRepository = pictureRepository;
    }

    @Autowired(required = true)
    public void setDefaultPicture(@Qualifier("defaultPicture") final DefaultPicture defaultPicture) {
        this.defaultPicture = defaultPicture;
    }

    @AllowPublicAccess
    @RequestMapping({"/game-server/command/picture", "/api/v1.0/player/picture"})
    public void handlePicture(final HttpServletRequest request,
                              final HttpServletResponse response) throws Exception {
        String playerIdParameter = request.getParameter("accountid");
        if (StringUtils.isBlank(playerIdParameter)) {
            playerIdParameter = request.getParameter("playerid");
        }

        if (StringUtils.isBlank(playerIdParameter) || !StringUtils.isNumeric(playerIdParameter)) {
            LOG.debug("No/invalid player ID supplied to picture controller, sending default picture");
            response.sendRedirect(defaultPicture.getUrl());
            return;
        }

        String pictureUrl;
        try {
            LOG.debug("Requesting picture for {}", playerIdParameter);
            pictureUrl = pictureRepository.getPicture(new BigDecimal(playerIdParameter));

        } catch (Exception e) {
            LOG.warn("Error retrieving picture for player {}", playerIdParameter, e);
            pictureUrl = defaultPicture.getUrl();
        }

        response.sendRedirect(pictureUrl);
    }
}
