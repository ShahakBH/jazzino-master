package strata.server.worker.audit.playedtracking.consumerexample;

import org.springframework.stereotype.Component;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.platform.event.message.PlayerPlayedEvent;

@Component
public class PlayerPlayedConsumerExample implements QueueMessageConsumer<PlayerPlayedEvent> {

    @Override
    public void handle(PlayerPlayedEvent message) {
        System.out.println("Received " + message);
    }
}
