package com.yazino.web.service;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.Platform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * need a wrapper class because Spring can't deal with Caching under an interface ...
 */
@Service("safeBuyChipsPromotionServiceWrapper")
public class SafeBuyChipsPromotionServiceWrapper {
    private final SafeBuyChipsPromotionService safeBuyChipsPromotionService;

    //for EhChache
    public SafeBuyChipsPromotionServiceWrapper() {
        safeBuyChipsPromotionService = null;
    }

    @Autowired
    public SafeBuyChipsPromotionServiceWrapper(final SafeBuyChipsPromotionService safeBuyChipsPromotionService) {
        this.safeBuyChipsPromotionService = safeBuyChipsPromotionService;
    }

    @Cacheable(cacheName = "hasPromotionCache")
    public Boolean hasPromotion(final BigDecimal playerId, final Platform platform) {
        return safeBuyChipsPromotionService.hasPromotion(playerId, platform);
    }

}
