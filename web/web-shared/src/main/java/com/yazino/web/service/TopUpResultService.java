package com.yazino.web.service;

import com.yazino.platform.Platform;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.DailyAwardPromotionService;
import strata.server.lobby.api.promotion.TopUpResult;
import strata.server.lobby.api.promotion.TopUpStatus;
import strata.server.lobby.api.promotion.message.PromotionMessage;
import strata.server.lobby.api.promotion.message.TopUpAcknowledgeRequest;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;
import static strata.server.lobby.api.promotion.TopUpStatus.ACKNOWLEDGED;

@Service("topupResultService")
public class TopUpResultService {
    private static final Logger LOG = LoggerFactory.getLogger(TopUpResultService.class);

    private final DailyAwardPromotionService dailyAwardPromotionService;
    private final QueuePublishingService<PromotionMessage> promotionMessageQueuePublishingService;

    public static class TopUpStatusCacheEntry implements Serializable {

        private static final long serialVersionUID = 1L;

        private final TopUpStatus topUpStatus;

        private final DateTime lastTopUpDate;

        public TopUpStatusCacheEntry(final TopUpStatus topUpStatus, final DateTime lastTopUpDate) {
            this.topUpStatus = topUpStatus;
            this.lastTopUpDate = lastTopUpDate;
        }

        public TopUpStatus getTopUpStatus() {
            return topUpStatus;
        }

        public DateTime getLastTopUpDate() {
            return lastTopUpDate;
        }
    }

    private final Ehcache topUpStatusCache;

    @Autowired
    public TopUpResultService(@Qualifier("topUpStatusCache") final Ehcache topUpStatusCache,
                              @Qualifier("dailyAwardPromotionService") final DailyAwardPromotionService dailyAwardPromotionService,
                              @Qualifier("promotionRequestQueuePublishingService") final QueuePublishingService<PromotionMessage> promotionMessageQueuePublishingService) {
        notNull(topUpStatusCache, "topUpStatusCache is null");
        notNull(dailyAwardPromotionService, "dailyAwardPromotionService is null");
        notNull(promotionMessageQueuePublishingService, "promotionMessagePublishingService is null");
        this.dailyAwardPromotionService = dailyAwardPromotionService;
        this.topUpStatusCache = topUpStatusCache;
        this.promotionMessageQueuePublishingService = promotionMessageQueuePublishingService;
    }

    public TopUpResult getTopUpResult(final BigDecimal playerId, final Platform platform) {
        LOG.debug("Requesting top up result for player {} on {}", playerId, platform.name());
        final Element element = topUpStatusCache.get(playerId);
        if (element == null) {
            LOG.debug("Requesting top up result from promotion service fo player {} on {}", playerId, platform.name());
            final TopUpResult topUpResult = dailyAwardPromotionService.getTopUpResult(playerId, platform);
            cacheTopUpResult(topUpResult);
            return topUpResult;
        } else {
            LOG.debug("Returning cached top up result for player {} on {}", playerId);
            final TopUpStatusCacheEntry value = (TopUpStatusCacheEntry) element.getObjectValue();
            return new TopUpResult(playerId, value.getTopUpStatus(), value.getLastTopUpDate());
        }
    }

    public void acknowledgeTopUpResult(TopUpAcknowledgeRequest topUpAcknowledgeRequest) {
        final BigDecimal playerId = topUpAcknowledgeRequest.getPlayerId();
        final Element element = topUpStatusCache.get(playerId);
        if (element == null) {
            promotionMessageQueuePublishingService.send(topUpAcknowledgeRequest);
        }

        TopUpStatusCacheEntry entry = new TopUpStatusCacheEntry(ACKNOWLEDGED, topUpAcknowledgeRequest.getTopUpDate());
        topUpStatusCache.put(new Element(playerId, entry));
    }

    public void clearTopUpStatus(BigDecimal playerId) {
        topUpStatusCache.remove(playerId);
    }

    private void cacheTopUpResult(final TopUpResult topUpResult) {
        if (topUpResult.getStatus() == ACKNOWLEDGED && topUpResult.getLastTopUpDate() != null) {
            final Element element = new Element(topUpResult.getPlayerId(),
                    new TopUpStatusCacheEntry(topUpResult.getStatus(), topUpResult.getLastTopUpDate()));
            topUpStatusCache.put(element);
        }
    }
}

