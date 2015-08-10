package strata.server.worker.event.consumer.crm;

import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.platform.worker.message.PlayerVerifiedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PlayerVerifiedMessageConsumer implements QueueMessageConsumer<PlayerVerifiedMessage> {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerVerifiedMessageConsumer.class);

    @Override
    public void handle(final PlayerVerifiedMessage message) {
        if (message == null || message.getPlayerId() == null) {
            return;
        }

        LOG.debug("Received player verified message: {}", message);

        // No action required at present; should be removed if the new CRM integration does not require this notification
    }
}
