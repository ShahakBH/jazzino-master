package com.yazino.bi.operations.persistence;

import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.PromotionType;
import com.yazino.bi.operations.model.PromotionPlayer;
import com.yazino.bi.operations.view.PromotionSearchType;

import java.util.List;

public interface PromotionDao {
    List<Promotion> find(PromotionSearchType searchType,
                         PromotionType promotionType);

    Promotion findById(PromotionSearchType searchType,
                       Long promotionId);

    Integer countPlayersInPromotion(PromotionSearchType searchType,
                                    Long promotionId);

    List<PromotionPlayer> findPlayers(PromotionSearchType searchType,
                                      Long promotionId,
                                      Integer firstPlayer,
                                      Integer numberOfPlayers);

    List<Long> findPromotionsOlderThan(int numberOfDays);

    void archivePromotion(Long promotionId);
}
