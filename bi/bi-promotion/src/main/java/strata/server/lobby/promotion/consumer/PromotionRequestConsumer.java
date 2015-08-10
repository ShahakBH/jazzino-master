package strata.server.lobby.promotion.consumer;

import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import strata.server.lobby.api.promotion.message.PromotionMessage;
import strata.server.lobby.api.promotion.message.TopUpAcknowledgeRequest;
import strata.server.lobby.api.promotion.message.TopUpRequest;
import strata.server.lobby.promotion.service.TopUpService;

import static org.apache.commons.lang3.Validate.notNull;

@Component("promotionRequestConsumer")
public class PromotionRequestConsumer implements QueueMessageConsumer<PromotionMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(PromotionRequestConsumer.class);

    @Autowired
    private final TopUpService topUpService;

    @Autowired
    public PromotionRequestConsumer(final TopUpService topUpService) {
        notNull(topUpService, "topUpService may not be null");
        this.topUpService = topUpService;
    }

    @Override
    public void handle(final PromotionMessage message) {
        LOG.debug("consuming [{}]", message);

        switch (message.getMessageType()) {
            case TOPUP_REQUEST:
                handleTopUpRequest((TopUpRequest) message);
                break;
            case ACKNOWLEDGEMENT:
                handleAcknowledgeRequest((TopUpAcknowledgeRequest) message);
                break;
            default:
                LOG.error("Cannot handle message type: " + message.getMessageType());
                break;
        }
    }

    private void handleTopUpRequest(final TopUpRequest topUpRequest) {
        if (topUpRequest.isInvalid()) {
            LOG.error("Could not process invalid top up request [{}]", topUpRequest);
        } else {
            topUpService.topUpPlayer(topUpRequest);
        }
    }

    private void handleAcknowledgeRequest(final TopUpAcknowledgeRequest topUpAcknowledgeRequest) {
        if (topUpAcknowledgeRequest.isInvalid()) {
            LOG.error("Could not process invalid top up acknowledgement request [{}]", topUpAcknowledgeRequest);
        } else {
            topUpService.acknowledgeTopUpForPlayer(topUpAcknowledgeRequest);
        }
    }
}
