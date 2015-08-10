package strata.server.worker.tracking;

import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.platform.tracking.TrackingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("trackingEventConsumer")
public class TrackingEventConsumer implements QueueMessageConsumer<TrackingEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(TrackingEventConsumer.class);

    private final TrackingService trackingService;

    @Autowired
    public TrackingEventConsumer(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @Override
    public void handle(TrackingEvent event) {
        try {
            trackingService.track(event.getPlatform(), event.getPlayerId(), event.getName(), event.getEventProperties(), event.getReceived());
        } catch (Exception e) {
            LOG.warn("Unable to handle {}", event, e);
        }
    }
}
