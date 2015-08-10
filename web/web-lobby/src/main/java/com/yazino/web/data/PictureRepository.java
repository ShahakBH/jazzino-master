package com.yazino.web.data;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.community.PlayerService;
import com.yazino.web.domain.DefaultPicture;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class PictureRepository {
    private static final Logger LOG = LoggerFactory.getLogger(PictureRepository.class);

    private final PlayerService playerService;

    private DefaultPicture defaultPicture;

    //cglib
    protected PictureRepository() {
        this.playerService = null;
    }

    @Autowired
    public PictureRepository(final PlayerService playerService) {
        notNull(playerService, "playerService is null");

        this.playerService = playerService;
    }

    @Autowired
    public void setDefaultPicture(@Qualifier("defaultPicture") final DefaultPicture defaultPicture) {
        this.defaultPicture = defaultPicture;
    }

    @Cacheable(cacheName = "pictureCache")
    public String getPicture(final BigDecimal playerId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Requesting picture for " + playerId);
        }
        String pictureUrl;
        try {
            pictureUrl = playerService.getPictureUrl(playerId);
            if (StringUtils.isBlank(pictureUrl)) {
                pictureUrl = defaultPicture.getUrl();
            }
        } catch (Exception e) {
            LOG.warn("Error retrieving picture: " + e.getMessage());
            pictureUrl = defaultPicture.getUrl();
        }

        return pictureUrl;
    }
}
