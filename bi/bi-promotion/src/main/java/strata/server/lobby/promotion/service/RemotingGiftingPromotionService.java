package strata.server.lobby.promotion.service;


import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.gifting.AppToUserGift;
import com.yazino.promotions.PromotionDao;
import com.yazino.promotions.PromotionPlayerReward;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.GiftingPromotion;
import strata.server.lobby.api.promotion.GiftingPromotionService;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.PromotionType;

import java.math.BigDecimal;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.math.BigDecimal.valueOf;
import static org.joda.time.DateTime.now;

@Service
public class RemotingGiftingPromotionService implements GiftingPromotionService {
    private final PromotionDao promotionDao;
    private final com.yazino.platform.community.PlayerService playerService;
    public static final String REWARD_DETAIL_TEMPLATE = "reward=%s";
    private static final Logger LOG = LoggerFactory.getLogger(RemotingGiftingPromotionService.class);

    @Autowired
    public RemotingGiftingPromotionService(final PromotionDao promotionDao, final PlayerService playerService) {
        this.promotionDao = promotionDao;
        this.playerService = playerService;
    }

    @Override
    public List<AppToUserGift> getGiftingPromotions(final BigDecimal playerId) {
        final List<Promotion> promotionsFor = promotionDao.findPromotionsFor(playerId, PromotionType.GIFTING, null, now());
        final List<AppToUserGift> gifts = Lists.transform(promotionsFor, new Function<Promotion, AppToUserGift>() {
            @Override
            public AppToUserGift apply(final Promotion promotion) {
                GiftingPromotion promo = (GiftingPromotion) promotion;
                return new AppToUserGift(
                        promo.getId(),
                        promo.getGameTypes(),
                        promo.getEndDate(),
                        promo.getReward(),
                        promo.getGiftTitle(),
                        promo.getGiftDescription());
            }
        });
        return newArrayList(gifts);
    }

    @Override
    public boolean logPlayerReward(BigDecimal playerId, Long promotionId, final BigDecimal sessionId) {
        //TODO race condition should this all be done in dao?
        final List<Promotion> promos = promotionDao.findPromotionsFor(playerId, PromotionType.GIFTING, null, now());
        for (Promotion promo : promos) {
            if (promo.getId().equals(promotionId)) {
                try {
                    final String status = format(REWARD_DETAIL_TEMPLATE, ((GiftingPromotion) promo).getReward());
                    final PromotionPlayerReward promotionPlayerReward = new PromotionPlayerReward(promotionId, playerId, false, now(), status);
                    promotionDao.addLastReward(promotionPlayerReward);
                    playerService.postTransaction(playerId,
                            sessionId,
                            valueOf(((GiftingPromotion) promo).getReward()),
                            "AppToUser Gift",
                            status);
                } catch (Exception e) {
                    LOG.error("Error crediting gifted chips", e);
                    return false;
                }

                return true;
            }
        }
        return false;
    }
}
