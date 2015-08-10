package com.yazino.web.service;

import com.googlecode.ehcache.annotations.*;
import com.yazino.platform.community.GiftService;
import com.yazino.platform.gifting.*;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.GiftingPromotionService;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class GiftLobbyService {
    private static final Logger LOG = LoggerFactory.getLogger(GiftLobbyService.class);

    private static final String PERIOD_CACHE_NAME = "giftPeriodCache";
    private static final String STATUS_CACHE_NAME = "giftingStatusCache";
    private static final String GIFT_CACHE_NAME = "giftCache";
    private static final String PROMOTION_CACHE_NAME = "giftingPromotionsCache";
    // You can find the indices in the keys via https://code.google.com/p/ehcache-spring-annotations/wiki/ListCacheKeyGenerator
    private static final int KEY_INDEX_ARG_VALUES = 4;

    private final GiftService giftService;
    private final GiftingPromotionService giftingPromotionService;
    private final CacheManager cacheManager;

    GiftLobbyService() {
        // CGLib constructor
        this.giftService = null;
        this.giftingPromotionService = null;
        this.cacheManager = null;
    }

    @Autowired
    public GiftLobbyService(final GiftService giftService,
                            final GiftingPromotionService giftingPromotionService,
                            @Qualifier("ehCacheManager") final CacheManager cacheManager) {
        notNull(giftService, "giftService may not be null");
        notNull(giftingPromotionService, "giftingPromotionService may not be null");
        notNull(cacheManager, "cacheManager may not be null");

        this.giftService = giftService;
        this.giftingPromotionService = giftingPromotionService;
        this.cacheManager = cacheManager;
    }

    public Set<BigDecimal> giveGiftsToAllFriends(final BigDecimal sendingPlayer,
                                                 final BigDecimal sessionId) {
        ensureInitialised();

        final Set<BigDecimal> giftIds = giftService.giveGiftsToAllFriends(sendingPlayer, sessionId);
        clearStatusCacheFor(sendingPlayer);
        return giftIds;
    }

    public Set<BigDecimal> giveGifts(final BigDecimal sendingPlayer,
                                     final Set<BigDecimal> recipientPlayers,
                                     final BigDecimal sessionId) {
        ensureInitialised();

        final Set<BigDecimal> giftIds = giftService.giveGifts(sendingPlayer, recipientPlayers, sessionId);
        clearStatusCacheFor(sendingPlayer);
        return giftIds;
    }

    @Cacheable(cacheName = GIFT_CACHE_NAME,
            keyGenerator = @KeyGenerator(
                    name = "HashCodeCacheKeyGenerator",
                    properties = @Property(name = "includeMethod", value = "false")
            )
    )
    public Set<Gift> getAvailableGifts(final BigDecimal playerId) {
        ensureInitialised();

        return giftService.getAvailableGifts(playerId);
    }

    @Cacheable(cacheName = STATUS_CACHE_NAME, keyGenerator = @KeyGenerator(name = "ListCacheKeyGenerator"))
    public Set<GiftableStatus> getGiftableStatusForPlayers(final BigDecimal sendingPlayer,
                                                           final Set<BigDecimal> friendIds) {
        ensureInitialised();

        return giftService.getGiftableStatusForPlayers(sendingPlayer, friendIds);
    }

    public void acknowledgeViewedGifts(final BigDecimal playerId,
                                       final Set<BigDecimal> giftIds) {
        ensureInitialised();

        giftService.acknowledgeViewedGifts(playerId, giftIds);
    }

    @TriggersRemove(cacheName = GIFT_CACHE_NAME,
            keyGenerator = @KeyGenerator(
                    name = "HashCodeCacheKeyGenerator",
                    properties = @Property(name = "includeMethod", value = "false")
            )
    )
    public BigDecimal collectGift(@PartialCacheKey final BigDecimal playerId,
                                  final BigDecimal giftId,
                                  final CollectChoice choice,
                                  final BigDecimal sessionId)
            throws GiftCollectionFailure {
        ensureInitialised();

        return giftService.collectGift(playerId, giftId, choice, sessionId);
    }

    @Cacheable(cacheName = PERIOD_CACHE_NAME)
    public DateTime getEndOfGiftPeriod() {
        ensureInitialised();

        return giftService.getEndOfGiftPeriod();
    }

    public PlayerCollectionStatus pushPlayerCollectionStatus(final BigDecimal playerId) {
        ensureInitialised();

        return giftService.pushPlayerCollectionStatus(playerId);
    }

    @Cacheable(cacheName = PROMOTION_CACHE_NAME,
            keyGenerator = @KeyGenerator(
                    name = "HashCodeCacheKeyGenerator",
                    properties = @Property(name = "includeMethod", value = "false")
            )
    )
    public List<AppToUserGift> getGiftingPromotions(final BigDecimal playerId) {
        ensureInitialised();

        return giftingPromotionService.getGiftingPromotions(playerId);
    }

    @TriggersRemove(cacheName = PROMOTION_CACHE_NAME,
            keyGenerator = @KeyGenerator(
                    name = "HashCodeCacheKeyGenerator",
                    properties = @Property(name = "includeMethod", value = "false")
            )
    )
    public boolean logPlayerReward(@PartialCacheKey final BigDecimal playerId,
                                   final Long promotionId,
                                   final BigDecimal sessionId) {
        ensureInitialised();

        return giftingPromotionService.logPlayerReward(playerId, promotionId, sessionId);
    }

    @SuppressWarnings("unchecked")
    private void clearStatusCacheFor(final BigDecimal sendingPlayer) {
        try {
            final Set<Object> keysToClear = new HashSet<>();

            final Cache statusCache = cacheManager.getCache(STATUS_CACHE_NAME);
            for (List<List<Object>> key : (List<List<List<Object>>>) statusCache.getKeysNoDuplicateCheck()) {
                final List<Object> argValues = key.get(KEY_INDEX_ARG_VALUES);
                if (((Comparable) argValues.get(0)).compareTo(sendingPlayer) == 0) {
                    keysToClear.add(key);
                }
            }

            statusCache.removeAll(keysToClear);
            LOG.debug("Removed {} items from {} for player {}", keysToClear.size(), STATUS_CACHE_NAME, sendingPlayer);

        } catch (Exception e) {
            LOG.error("{} cache clear failed for player {}", STATUS_CACHE_NAME, sendingPlayer, e);
        }
    }

    private void ensureInitialised() {
        if (giftService == null || cacheManager == null || giftingPromotionService == null) {
            throw new IllegalStateException("Logic may not be directly invoked on a proxy instance");
        }
    }
}
