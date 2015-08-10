package strata.server.lobby.promotion.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.DailyAwardPromotionTestingService;
import strata.server.lobby.api.promotion.domain.PlayerPromotionStatus;
import strata.server.lobby.promotion.persistence.PlayerPromotionStatusDao;

import java.math.BigDecimal;

@Service
public class ProgressiveDailyAwardTestingService implements DailyAwardPromotionTestingService {

    private final PlayerPromotionStatusDao playerPromotionStatusDao;

    @Autowired
    public ProgressiveDailyAwardTestingService(final PlayerPromotionStatusDao playerPromotionStatusDao) {
        this.playerPromotionStatusDao = playerPromotionStatusDao;
    }


    @Override
    public PlayerPromotionStatus getPlayerPromotionStatus(final BigDecimal playerID) {
        return playerPromotionStatusDao.get(playerID);
    }

    @Override
    public PlayerPromotionStatus setDailyAwardStatus(final PlayerPromotionStatus playerPromotionStatus) {
        return playerPromotionStatusDao.save(playerPromotionStatus);
    }

}
