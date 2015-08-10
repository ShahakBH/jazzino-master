package com.yazino.platform.community;

import com.yazino.platform.gifting.*;
import org.joda.time.DateTime;
import org.openspaces.remoting.Routing;

import java.math.BigDecimal;
import java.util.Set;

public interface GiftService {

    Set<BigDecimal> giveGiftsToAllFriends(@Routing BigDecimal sendingPlayer,
                                          BigDecimal sessionId);

    Set<BigDecimal> giveGifts(@Routing BigDecimal sendingPlayer,
                              Set<BigDecimal> recipientPlayers,
                              BigDecimal sessionId);

    Set<Gift> getAvailableGifts(@Routing BigDecimal playerId);

    Set<GiftableStatus> getGiftableStatusForPlayers(@Routing BigDecimal sendingPlayer,
                                                    Set<BigDecimal> friendIds);

    void acknowledgeViewedGifts(@Routing BigDecimal playerId,
                                Set<BigDecimal> giftIds);

    BigDecimal collectGift(@Routing BigDecimal playerId,
                           BigDecimal giftId,
                           CollectChoice choice,
                           BigDecimal sessionId)
            throws GiftCollectionFailure;

    DateTime getEndOfGiftPeriod();

    PlayerCollectionStatus pushPlayerCollectionStatus(@Routing BigDecimal playerId);

}
