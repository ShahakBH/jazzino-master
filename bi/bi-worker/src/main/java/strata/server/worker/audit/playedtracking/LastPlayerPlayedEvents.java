package strata.server.worker.audit.playedtracking;

import com.yazino.platform.event.message.PlayerPlayedEvent;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import strata.server.worker.audit.playedtracking.repository.PlayerPlayedEventRepository;

import java.math.BigDecimal;

import static org.springframework.util.Assert.notNull;

@Component("lastPlayerPlayedEvents")
public class LastPlayerPlayedEvents {
    public static final int DEFAULT_TIMESTAMP = -1;

    private final PlayerPlayedEventRepository playerPlayedEventRepository;
    private final QueuePublishingService<PlayerPlayedEvent> publishingService;

    @Autowired
    public LastPlayerPlayedEvents(final PlayerPlayedEventRepository playerPlayedEventRepository,
                                  @Qualifier("playerPlayedEventPublishingService")
                                  final QueuePublishingService<PlayerPlayedEvent> publishingService) {
        notNull(playerPlayedEventRepository, "playerPlayedEventRepository is null");
        notNull(publishingService, "publishingService is null");

        this.playerPlayedEventRepository = playerPlayedEventRepository;
        this.publishingService = publishingService;
    }

    public void registerEvent(final BigDecimal accountId,
                              final BigDecimal playerId,
                              final DateTime timestamp) {
        if (playerId == null) {
            return;
        }
        notNull(accountId, "accountId is null");
        notNull(timestamp, "timestamp is null");

        final PlayerPlayedEvent event = new PlayerPlayedEvent(playerId, timestamp);
        playerPlayedEventRepository.store(accountId, event);
        publishingService.send(event);
    }

    public long getLastEventTimestampForAccount(final BigDecimal accountId) {
        notNull(accountId, "accountId is null");
        final PlayerPlayedEvent event = playerPlayedEventRepository.forAccount(accountId);
        if (event == null) {
            return DEFAULT_TIMESTAMP;
        }
        return event.getTime().getMillis();
    }

}
