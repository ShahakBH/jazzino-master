package strata.server.lobby.promotion.service;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.player.PlayerProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.Promotion;

// TODO The caching layer can probably be dropped as the delegate no longer calls the database
@Service("promotionControlGroupService")
public class CachingPromotionControlGroupService implements PromotionControlGroupService {

    private final PromotionControlGroupService delegate;

    @Autowired
    public CachingPromotionControlGroupService(@Qualifier("delegatePromotionControlGroupService") final PromotionControlGroupService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Cacheable(cacheName = "controlGroupDetectionCache", selfPopulating = true)
    public boolean isControlGroupMember(final PlayerProfile playerProfile, final Promotion promotion) {
        return delegate.isControlGroupMember(playerProfile, promotion);
    }
}
