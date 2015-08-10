package com.yazino.platform.processor.community;

import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PublishStatusRequest;
import com.yazino.platform.model.community.PublishStatusRequestType;
import com.yazino.platform.model.community.PublishStatusRequestWithArguments;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.TableInviteRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.community.PlayerWorker;
import org.openspaces.core.GigaSpace;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 3)
public class PublishStatusProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(PublishStatusProcessor.class);

    private static final PublishStatusRequest TEMPLATE = new PublishStatusRequest();

    private final PlayerSessionRepository playerSessionRepository;
    private final TableInviteRepository tableInviteRepository;
    private final PlayerRepository playerRepository;
    private final InternalWalletService internalWalletService;
    private final GigaSpace localGigaSpace;

    private final DocumentDispatcher documentDispatcher;

    @Autowired(required = true)
    public PublishStatusProcessor(
            @Qualifier("spaceDocumentDispatcher") final DocumentDispatcher documentDispatcher,
            final PlayerRepository playerRepository,
            final PlayerSessionRepository playerSessionRepository,
            final InternalWalletService internalWalletService,
            @Qualifier("gigaSpace") final GigaSpace localGigaSpace,
            final TableInviteRepository tableInviteRepository) {
        this.documentDispatcher = documentDispatcher;
        this.playerRepository = playerRepository;
        this.playerSessionRepository = playerSessionRepository;
        this.internalWalletService = internalWalletService;
        this.localGigaSpace = localGigaSpace;
        this.tableInviteRepository = tableInviteRepository;
    }

    @EventTemplate
    public PublishStatusRequest template() {
        return TEMPLATE;
    }

    @SpaceDataEvent
    public void processRequest(final PublishStatusRequest request) {
        LOG.debug("entering processRequest {}", request);

        final Player player = playerRepository.findById(request.getPlayerId());
        if (player == null) {
            LOG.error("player not found {}", request.getPlayerId());
            return;
        }// no such player

        final PlayerWorker pw = new PlayerWorker();
        if (!playerSessionRepository.isOnline(player.getPlayerId())) {
            LOG.debug("player not online {}", request.getPlayerId());
            return;
        }

        switch (request.getRequestType()) {
            case COMMUNITY_STATUS:
                LOG.debug("creating request for all available StatusRequestTypes ");
                localGigaSpace.write(new PublishStatusRequest(request.getPlayerId(),
                        PublishStatusRequestType.PLAYER_BALANCE));
                localGigaSpace.write(new PublishStatusRequest(request.getPlayerId(),
                        PublishStatusRequestType.RELATIONSHIPS));
                break;
            case PLAYER_BALANCE:
                LOG.debug("Publishing PLAYER_BALANCE");
                documentDispatcher.dispatch(pw.buildPlayerBalanceDocument(player, internalWalletService), player.getPlayerId());
                break;
            case RELATIONSHIPS:
                LOG.debug("Publishing RELATIONSHIPS");
                documentDispatcher.dispatch(pw.buildRelationshipDocument(
                        player, playerSessionRepository, tableInviteRepository), player.getPlayerId());
                break;
            case GIFT_RECEIVED:
                LOG.debug("Publishing GIFT RECEIVED");
                documentDispatcher.dispatch(pw.buildGiftReceivedDocument(), player.getPlayerId());
                break;
            case GIFTING_PLAYER_COLLECTION_STATUS:
                LOG.debug("Publishing gifting player collection status");
                PublishStatusRequestWithArguments publishStatusRequestWithArguments = (PublishStatusRequestWithArguments) request;
                documentDispatcher.dispatch(pw.buildGiftingPlayerCollectionStatusDocument(publishStatusRequestWithArguments.getArguments()), player.getPlayerId());
                break;
            default:
                LOG.error("Unknown request type: {}", request.getRequestType());

        }
    }

}
