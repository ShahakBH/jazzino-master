package strata.server.lobby.promotion.service;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.DailyAwardPromotionService;
import strata.server.lobby.api.promotion.message.TopUpAcknowledgeRequest;
import strata.server.lobby.api.promotion.message.TopUpRequest;
import strata.server.lobby.promotion.persistence.PlayerPromotionStatusDao;

@Service
public class TopUpService {
    private static final Logger LOG = LoggerFactory.getLogger(TopUpService.class);

    private final DailyAwardPromotionService dailyAwardPromotionService;
    private final PlayerPromotionStatusDao playerPromotionStatusDao;

    @Autowired
    public TopUpService(DailyAwardPromotionService dailyAwardPromotionService, final PlayerPromotionStatusDao playerPromotionStatusDao) {
        Validate.notNull(dailyAwardPromotionService, "dailyAwardPromotionService cannot be null");
        Validate.notNull(playerPromotionStatusDao, "playerPromotionStatusDao cannot be null");
        this.dailyAwardPromotionService = dailyAwardPromotionService;
        this.playerPromotionStatusDao = playerPromotionStatusDao;
    }

    public void topUpPlayer(TopUpRequest topUpRequest) {
        LOG.debug("top up request: {}", topUpRequest);
        try {
            dailyAwardPromotionService.awardDailyTopUp(topUpRequest);
        } catch (Exception e) {
            LOG.error("Failed to top up player. request={}", topUpRequest, e);
        }
    }

    public void acknowledgeTopUpForPlayer(final TopUpAcknowledgeRequest topUpAcknowledgeRequest) {
        try {
            playerPromotionStatusDao.saveAcknowledgeTopUpForPlayer(
                    topUpAcknowledgeRequest.getPlayerId(), topUpAcknowledgeRequest.getTopUpDate());
        } catch (Exception e) {
            LOG.error("Failed to acknowledge top up request for player={}", topUpAcknowledgeRequest.getPlayerId());
        }
    }
}
