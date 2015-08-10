package com.yazino.platform.repository.community;

import com.gigaspaces.client.CountModifiers;
import com.gigaspaces.client.ReadModifiers;
import com.j_spaces.core.client.SQLQuery;
import com.yazino.platform.gifting.CollectChoice;
import com.yazino.platform.gifting.PlayerCollectionStatus;
import com.yazino.platform.grid.SpaceAccess;
import com.yazino.platform.model.community.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("giftRepository")
public class GigaspaceGiftRepository implements GiftRepository {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceGiftRepository.class);

    private final SpaceAccess space;

    @Autowired
    public GigaspaceGiftRepository(final SpaceAccess space) {
        notNull(space, "space may not be null");

        this.space = space;
    }

    @Override
    public Set<BigDecimal> findLocalRecipientsBySender(final BigDecimal sendingPlayerId,
                                                       final DateTime createdSince) {
        notNull(sendingPlayerId, "sendingPlayerId may not be null");
        notNull(createdSince, "createdSince may not be null");

        final SQLQuery<Gift> query = new SQLQuery<>(Gift.class,
                String.format("sendingPlayer = %s and createdInMillis > %s", sendingPlayerId.toPlainString(), createdSince.getMillis()))
                .setProjections("recipientPlayer");
        final Gift[] gifts = space.local().readMultiple(query, Integer.MAX_VALUE, ReadModifiers.DIRTY_READ);
        if (gifts == null) {
            return Collections.emptySet();
        }

        final Set<BigDecimal> recipients = new HashSet<>();
        for (Gift gift : gifts) {
            recipients.add(gift.getRecipientPlayer());
        }
        return recipients;
    }

    @Override
    public Gift findByRecipientAndId(final BigDecimal recipientPlayerId,
                                     final BigDecimal giftId) {
        notNull(recipientPlayerId, "recipientPlayerId may not be null");
        notNull(giftId, "giftId may not be null");

        return space.forRouting(recipientPlayerId).readById(Gift.class, giftId, recipientPlayerId, 0, ReadModifiers.DIRTY_READ);
    }

    @Override
    public Gift lockByRecipientAndId(final BigDecimal recipientPlayerId,
                                     final BigDecimal giftId) {
        notNull(recipientPlayerId, "recipientPlayerId may not be null");
        notNull(giftId, "giftId may not be null");

        final Gift gift = space.forRouting(recipientPlayerId).readById(Gift.class, giftId, recipientPlayerId, 0, ReadModifiers.EXCLUSIVE_READ_LOCK);
        if (gift == null) {
            throw new ConcurrentModificationException(String.format("Gift %s is locked or does not exist", giftId));
        }
        return gift;
    }

    public Set<Gift> findByRecipient(final BigDecimal recipientPlayerId) {
        notNull(recipientPlayerId, "recipientPlayerId may not be null");

        final Gift[] gifts = space.forRouting(recipientPlayerId).readMultiple(new Gift(recipientPlayerId), Integer.MAX_VALUE, ReadModifiers.DIRTY_READ);
        if (gifts != null) {
            return new HashSet<>(Arrays.asList(gifts));
        }
        return Collections.emptySet();
    }

    @Override
    public Set<Gift> findAvailableByRecipient(final BigDecimal recipientPlayerId) {
        notNull(recipientPlayerId, "recipientPlayerId may not be null");

        final SQLQuery<Gift> query = new SQLQuery<>(Gift.class,
                String.format("recipientPlayer = %s and (expiryInMillis >= %s or acknowledged = false) and collectedInMillis is null",
                        recipientPlayerId.toPlainString(), DateTimeUtils.currentTimeMillis())
        );
        final Gift[] gifts = space.local().readMultiple(query, Integer.MAX_VALUE, ReadModifiers.DIRTY_READ);
        if (gifts != null) {
            return new HashSet<>(Arrays.asList(gifts));
        }
        return Collections.emptySet();
    }

    @Override
    public int countCollectedOn(final BigDecimal recipientPlayerId,
                                final DateTime startOfDay) {
        notNull(recipientPlayerId, "recipientPlayerId may not be null");
        notNull(startOfDay, "startOfDay may not be null");

        final SQLQuery<Gift> query = new SQLQuery<>(Gift.class, String.format("recipientPlayer = %s and collectedInMillis >= %s",
                recipientPlayerId.toPlainString(), startOfDay.getMillis()));
        return space.forRouting(recipientPlayerId).count(query, CountModifiers.DIRTY_READ);
    }

    @Override
    public int countAvailableForCollection(final BigDecimal recipientPlayerId) {
        notNull(recipientPlayerId, "recipientPlayerId may not be null");

        final SQLQuery<Gift> query = new SQLQuery<>(Gift.class, String.format("recipientPlayer = %s and expiryInMillis >= %s and collectedInMillis is null",
                recipientPlayerId.toPlainString(), DateTimeUtils.currentTimeMillis()));
        return space.forRouting(recipientPlayerId).count(query, CountModifiers.DIRTY_READ);
    }

    @Override
    public void save(final Gift gift) {
        notNull(gift, "gift may not be null");

        space.forRouting(gift).writeMultiple(
                new Object[]{gift, new GiftPersistenceRequest(gift.getRecipientPlayer(), gift.getId())});
        LOG.debug("Wrote gift {}", gift);
    }

    @Override
    public void cleanUpOldGifts(final int retentionInHours) {
        final DateTime expiryTime = new DateTime().minusHours(retentionInHours);
        LOG.debug("Cleaning up gifts older than {}", expiryTime);

        final SQLQuery<Gift> query = new SQLQuery<>(Gift.class, String.format("createdInMillis < %s", expiryTime.getMillis()));
        final Gift[] giftsToBeRemoved = space.local().readMultiple(query, Integer.MAX_VALUE, ReadModifiers.DIRTY_READ);
        if (giftsToBeRemoved != null) {
            for (Gift gift : giftsToBeRemoved) {
                space.local().clear(gift);
            }
            LOG.debug("{} gifts cleaned up", giftsToBeRemoved.length);
        }
    }

    @Override
    public void publishReceived(final BigDecimal recipientPlayerId) {
        notNull(recipientPlayerId, "recipientPlayerId may not be null");

        LOG.debug("Publishing Gift received for player {}", recipientPlayerId);

        space.forRouting(recipientPlayerId).write(new PublishStatusRequest(recipientPlayerId, PublishStatusRequestType.GIFT_RECEIVED));
    }

    @Override
    public void publishCollectionStatus(final BigDecimal playerId,
                                        final PlayerCollectionStatus playerCollectionStatus) {
        notNull(playerId, "playerId may not be null");
        notNull(playerCollectionStatus, "playerCollectionStatus may not be null");

        LOG.debug("Publishing gifting status for player {} = {} ", playerId, playerCollectionStatus);

        final Map<String, Object> arguments = new HashMap<>();
        arguments.put(PublishStatusRequestArgument.PLAYER_COLLECTION_STATUS.name(), playerCollectionStatus);
        space.forRouting(playerId).write(new PublishStatusRequestWithArguments(
                playerId, PublishStatusRequestType.GIFTING_PLAYER_COLLECTION_STATUS, arguments));
    }

    @Override
    public void requestSendGifts(final BigDecimal sendingPlayerId,
                                 final BigDecimal sessionId,
                                 final Map<BigDecimal, BigDecimal> recipientPlayerIdToGiftIds) {
        notNull(sendingPlayerId, "sendingPlayer may not be null");
        notNull(recipientPlayerIdToGiftIds, "recipientPlayerIdToGiftIds may not be null");
        notNull(sessionId, "sessionId may not be null");

        final SendGiftRequest[] giftRequests = new SendGiftRequest[recipientPlayerIdToGiftIds.size()];
        int index = 0;
        for (BigDecimal recipientPlayerId : recipientPlayerIdToGiftIds.keySet()) {
            final BigDecimal giftId = recipientPlayerIdToGiftIds.get(recipientPlayerId);
            LOG.debug("Requesting send of gift from {} to {} as ID {}", sendingPlayerId, recipientPlayerId, giftId);
            giftRequests[index++] = new SendGiftRequest(sendingPlayerId, recipientPlayerId, giftId, sessionId);
        }

        space.global().writeMultiple(giftRequests);
    }

    @Override
    public void requestAcknowledgement(final BigDecimal recipientPlayerId,
                                       final Set<BigDecimal> giftIds) {
        notNull(recipientPlayerId, "recipientPlayerId may not be null");

        if (giftIds == null || giftIds.isEmpty()) {
            return;
        }

        LOG.debug("Requesting acknowledgement by recipient {} of gifts {}", recipientPlayerId, giftIds);

        final AcknowledgeGiftRequest[] giftRequests = new AcknowledgeGiftRequest[giftIds.size()];
        int index = 0;
        for (BigDecimal giftId : giftIds) {
            giftRequests[index++] = new AcknowledgeGiftRequest(recipientPlayerId, giftId);
        }
        space.forRouting(recipientPlayerId).writeMultiple(giftRequests);
    }

    @Override
    public void requestCollection(final BigDecimal recipientPlayerId,
                                  final BigDecimal giftId,
                                  final BigDecimal sessionId,
                                  final BigDecimal giftWinnings,
                                  final CollectChoice choice) {
        notNull(recipientPlayerId, "recipientPlayerId may not be null");
        notNull(giftId, "giftId may not be null");
        notNull(sessionId, "sessionId may not be null");
        notNull(giftWinnings, "giftWinnings may not be null");
        notNull(choice, "choice may not be null");

        LOG.debug("Requesting collection of gift {} by recipient {}", giftId, recipientPlayerId);

        space.forRouting(recipientPlayerId).write(new CollectGiftRequest(recipientPlayerId, giftId, sessionId, giftWinnings, choice));
    }
}
