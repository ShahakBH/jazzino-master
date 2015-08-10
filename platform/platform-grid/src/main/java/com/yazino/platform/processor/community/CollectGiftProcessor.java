package com.yazino.platform.processor.community;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.audit.GiftAuditingService;
import com.yazino.platform.gifting.CollectChoice;
import com.yazino.platform.gifting.GiftCollectionFailure;
import com.yazino.platform.gifting.PlayerCollectionStatus;
import com.yazino.platform.model.community.CollectGiftRequest;
import com.yazino.platform.model.community.Gift;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.repository.community.GiftRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.community.GiftProperties;
import com.yazino.platform.util.BigDecimals;
import org.joda.time.DateTime;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.TransactionalEvent;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static com.yazino.platform.gifting.CollectionResult.UNKNOWN;
import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace")
@TransactionalEvent(transactionManager = "spaceTransactionManager")
public class CollectGiftProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(CollectGiftProcessor.class);

    private final GiftRepository giftRepository;
    private final PlayerRepository playerRepository;
    private final GiftAuditingService giftAuditingService;
    private final InternalWalletService internalWalletService;
    private final GiftProperties giftProperties;

    @Autowired
    public CollectGiftProcessor(final GiftRepository giftRepository,
                                final PlayerRepository playerRepository,
                                final GiftAuditingService giftAuditingService,
                                final InternalWalletService internalWalletService,
                                final GiftProperties giftProperties) {
        notNull(giftRepository, "giftRepository may not be null");
        notNull(playerRepository, "playerRepository may not be null");
        notNull(giftAuditingService, "giftAuditingService may not be null");
        notNull(internalWalletService, "internalWalletService may not be null");
        notNull(giftProperties, "giftingProperties may not be null");

        this.giftRepository = giftRepository;
        this.playerRepository = playerRepository;
        this.giftAuditingService = giftAuditingService;
        this.internalWalletService = internalWalletService;
        this.giftProperties = giftProperties;
    }

    @EventTemplate
    public CollectGiftRequest template() {
        return new CollectGiftRequest();
    }

    @SpaceDataEvent
    public void process(final CollectGiftRequest request) {
        LOG.debug("Processing request: {}", request);

        if (request == null) {
            return;
        }

        try {
            Gift gift = giftRepository.findByRecipientAndId(request.getRecipientPlayerId(), request.getGiftId());
            if (gift == null) {
                LOG.warn("Gift {} for recipient {} does not exist", request.getGiftId(), request.getRecipientPlayerId());
                return;
            }

            gift = giftRepository.lockByRecipientAndId(request.getRecipientPlayerId(), request.getGiftId());

            creditWinningsFor(request.getChoice(), request.getRecipientPlayerId(), request.getSessionId(), request.getWinnings());

            final DateTime collectionTime = new DateTime();
            gift.setCollected(collectionTime);
            giftRepository.save(gift);

            giftAuditingService.auditGiftCollected(request.getGiftId(), request.getChoice(), request.getWinnings(),
                    request.getSessionId(), collectionTime);

            pushPlayerCollectionStatus(request.getRecipientPlayerId());

            LOG.debug("Player {} has collected gift {} with winnings", request.getRecipientPlayerId(), request.getGiftId(), request.getWinnings());

        } catch (ConcurrentModificationException e) {
            throw e;

        } catch (Throwable e) {
            LOG.error("Acknowledgement failed for gift request {}", request, e);
        }
    }

    private void pushPlayerCollectionStatus(final BigDecimal playerId) {
        LOG.debug("Pushing collection status for player {}", playerId);

        final int collectedToday = giftRepository.countCollectedOn(playerId, giftProperties.startOfGiftPeriod());
        final PlayerCollectionStatus playerCollectionStatus = new PlayerCollectionStatus(
                giftProperties.remainingGiftCollections(collectedToday),
                giftRepository.countAvailableForCollection(playerId));
        giftRepository.publishCollectionStatus(playerId, playerCollectionStatus);
    }

    private void creditWinningsFor(final CollectChoice choice,
                                   final BigDecimal playerId,
                                   final BigDecimal sessionId,
                                   final BigDecimal giftWinnings)
            throws GiftCollectionFailure, WalletServiceException {
        if (BigDecimals.equalByComparison(BigDecimal.ZERO, giftWinnings)) {
            LOG.debug("Will not credit winnings of zero to playerId {}", playerId);
            return;
        }

        final Player player = playerRepository.findById(playerId);
        if (player == null) {
            LOG.error("Attempted collection of gift for non-existent player {}", playerId);
            throw new GiftCollectionFailure(UNKNOWN);
        }

        internalWalletService.postTransaction(player.getAccountId(), giftWinnings, "Collected Gift",
                choice.name(), transactionContext().withSessionId(sessionId).build());
    }

}
