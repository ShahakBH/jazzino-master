package strata.server.worker;

import com.yazino.platform.event.message.PlayerProfileEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;

public class DummyPlayerProfileEventQueuePublishService implements QueuePublishingService<PlayerProfileEvent> {

    @Override
    public void send(final PlayerProfileEvent message) {
        // ignored
    }
}
