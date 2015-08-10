package com.yazino.promotions;

import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import org.joda.time.DateTime;
import strata.server.lobby.api.promotion.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PromotionDao {
    /**
     * Creates a new promotion, returning its generated id
     *
     * @param promo promotion to create
     * @return id of new promotion
     */
    Long create(Promotion promo);

    void update(Promotion promo);

    void delete(Long promoId);

    void addPlayersTo(Long promoId, Set<BigDecimal> playerIds);

    void updatePlayerCountInPromotion(Long promoId);

    List<Promotion> findWebPromotions(BigDecimal playerId, DateTime applicableDate);

    List<Promotion> findWebPromotionsOrderedByPriority(BigDecimal playerId, DateTime applicableDate);

    List<Promotion> findPromotionsForCurrentTime(BigDecimal playerId,
                                                 PromotionType type,
                                                 Platform platform);

    /**
     * Finds promotions spanning given date for given player and promotion type. Filters promotions without required
     * configuration items.
     *
     * @param playerId    player in promotion
     * @param type        promotion type to load
     * @param platform    web or iOS
     * @param currentTime date promos must span
     * @return list of promos spanning given date
     */
    List<Promotion> findPromotionsFor(BigDecimal playerId,
                                      PromotionType type,
                                      Platform platform,
                                      DateTime currentTime);

    List<DailyAwardPromotion> getWebDailyAwardPromotions(final BigDecimal playerId,
                                                         final DateTime currentTime);

    List<ProgressiveDailyAwardPromotion> getProgressiveDailyAwardPromotion(final BigDecimal playerId,
                                                                           final DateTime currentTime,
                                                                           final ProgressiveAwardEnum progressiveAward);


    List<DailyAwardPromotion> getIosDailyAwardPromotions(final BigDecimal playerId,
                                                         final DateTime currentTime);

    void addLastReward(PromotionPlayerReward promotionPlayerReward);

    /**
     * Gets the buy chip promotion to apply to the given player. May return an empty Map if no promotions apply.
     * If multiple promotions apply (i.e. overlap given <code>applicableDate</code>), then the promotion with the
     * highest priority is returned. If several applicable promotions have equal highest priority, return promotion with
     * earliest start date.
     *
     * @param playerId       player id
     * @param platform       web or iOS
     * @param applicableDate the promotion date to check against
     * @return the promotion to apply or null if no promotions are applicable
     */
    Map<PaymentPreferences.PaymentMethod, Promotion> getBuyChipsPromotions(BigDecimal playerId,
                                                                           Platform platform,
                                                                           DateTime applicableDate);

    Promotion findById(Long promoId);

    List<Promotion> findPromotionsByTypeOrderByPriority(BigDecimal playerId,
                                                        PromotionType type,
                                                        Platform platform,
                                                        DateTime currentTime);

    List<BigDecimal> getProgressiveAwardPromotionValueList();

    List<PromotionPlayerReward> findPromotionPlayerRewards(BigDecimal playerId, DateTime topUpDate);

    void associateMarketingGroupMembersWithPromotion(int marketingGroupId, Long promoId);
}
