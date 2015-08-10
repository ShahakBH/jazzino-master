package strata.server.lobby.promotion.service;

import com.yazino.promotions.PromotionDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import strata.server.lobby.api.promotion.DailyAwardConfig;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.PromotionMaintenanceService;
import strata.server.lobby.promotion.tools.PromotionFunctions;

import java.math.BigDecimal;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class RemotingPromotionMaintenanceService implements PromotionMaintenanceService {
    private static final Logger LOG = LoggerFactory.getLogger(RemotingPromotionMaintenanceService.class);

    private final PromotionDao promotionDao;

    private final Promotion defaultDailyPromotion;

    private final PromotionFunctions promotionFunctions;

    @Autowired
    public RemotingPromotionMaintenanceService(
            final PromotionDao promotionDao,
            final PromotionFunctions promotionFunctions,
            @Qualifier("defaultDailyPromotion") final Promotion defaultDailyPromotion
    ) {
        notNull(promotionDao, "promotionDao is null");
        notNull(promotionFunctions, "promotionFunctions is null");
        notNull(defaultDailyPromotion, "defaultDailyPromotion is null");

        this.promotionDao = promotionDao;
        this.promotionFunctions = promotionFunctions;
        this.defaultDailyPromotion = defaultDailyPromotion;
    }

    @Override
    public Long create(final Promotion promo) {
        notNull(promo, "promo is null");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating promotion: " + promo);
        }
        final int seed = promotionFunctions.generateSeed();
        promo.setSeed(seed);
        return promotionDao.create(promo);
    }

    @Override
    public void update(final Promotion promo) {
        notNull(promo, "promo is null");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating promotion: " + promo);
        }
        promotionDao.update(promo);
    }

    @Override
    public void delete(final Long promoId) {
        notNull(promoId, "promoId is null");
        if (LOG.isInfoEnabled()) {
            LOG.info(String.format("Deleting promotion[promoId=%s] ", promoId));
        }
        promotionDao.delete(promoId);
    }

    @Transactional
    @Override
    public void addPlayersTo(final Long promoId, final Set<BigDecimal> playerIds) {
        notNull(promoId, "Promotion id is null");
        notNull(playerIds, "playerIds is null");
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Adding players to promotion[promoId=%s, playerIds=%s]", promoId, playerIds));
        }
        // TODO add players in batches of 1000 say if any batch fails add to latest status
        promotionDao.addPlayersTo(promoId, playerIds);
        promotionDao.updatePlayerCountInPromotion(promoId);
    }


    @Override
    public DailyAwardConfig getDefaultDailyAwardConfiguration() {
        return new DailyAwardConfig(defaultDailyPromotion.getConfiguration());
    }

}
