package com.yazino.platform.repository.community;

import com.yazino.platform.gifting.CollectChoice;
import com.yazino.platform.gifting.PlayerCollectionStatus;
import com.yazino.platform.model.community.Gift;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

public interface GiftRepository {

    Set<BigDecimal> findLocalRecipientsBySender(BigDecimal sendingPlayerId,
                                                DateTime createdSince);

    Set<Gift> findAvailableByRecipient(BigDecimal recipientPlayerId);

    Gift findByRecipientAndId(BigDecimal recipientPlayerId,
                              BigDecimal giftId);

    Gift lockByRecipientAndId(BigDecimal recipientPlayerId,
                              BigDecimal giftId);

    int countCollectedOn(BigDecimal recipientPlayerId,
                         DateTime startOfDay);

    int countAvailableForCollection(BigDecimal recipientPlayerId);

    void save(Gift gift);

    void cleanUpOldGifts(final int retentionInHours);

    void publishReceived(BigDecimal recipientPlayerId);

    void publishCollectionStatus(BigDecimal playerId,
                                 PlayerCollectionStatus playerCollectionStatus);

    void requestSendGifts(BigDecimal sendingPlayerId,
                          BigDecimal sessionId,
                          Map<BigDecimal, BigDecimal> recipientPlayerIdToGiftIds);

    void requestAcknowledgement(BigDecimal recipientPlayerId,
                                Set<BigDecimal> giftId);

    void requestCollection(BigDecimal recipientPlayerId,
                           BigDecimal giftId,
                           BigDecimal sessionId,
                           BigDecimal giftWinnings,
                           CollectChoice choice);
}
