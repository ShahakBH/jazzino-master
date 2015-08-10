package strata.server.lobby.promotion.consumer;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import strata.server.lobby.api.promotion.ProgressiveAwardEnum;
import strata.server.lobby.api.promotion.domain.PlayerPromotionStatus;
import strata.server.lobby.api.promotion.domain.builder.PlayerPromotionStatusBuilder;
import strata.server.lobby.promotion.persistence.PlayerPromotionStatusDao;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import com.yazino.platform.event.message.PlayerPlayedEvent;

@Component
@Qualifier("playerPlayedEventConsumer")
public class PlayerPlayedEventConsumer implements QueueMessageConsumer<PlayerPlayedEvent> {

    private static final DateTimeZone NEW_YORK = DateTimeZone.forID("America/New_York");
    public static final int MAX_CONSECUTIVE_DAY_LIMIT = 5;
    private final PlayerPromotionStatusDao playerPromotionStatusDao;

    @Autowired
    public PlayerPlayedEventConsumer(final PlayerPromotionStatusDao playerPromotionStatusDao) {
        this.playerPromotionStatusDao = playerPromotionStatusDao;
    }

    @Transactional
    @Override
    public void handle(final PlayerPlayedEvent message) {
        final PlayerPromotionStatus playerPromotionStatus = playerPromotionStatusDao.get(message.getPlayerId());
        final PlayerPromotionStatusBuilder playerPromotionStatusBuilder =
                new PlayerPromotionStatusBuilder(playerPromotionStatus);

        if (playerPromotionStatus.getLastPlayed() != null) {
            final DateTime promotionNextPlayDay = new DateTime(playerPromotionStatus.getLastPlayed(), NEW_YORK)
                    .plusDays(1);

            final DateTime lastPlayedNYTime = new DateTime(message.getTime(), NEW_YORK);

            if (lastPlayedNYTime.getDayOfYear() == promotionNextPlayDay.getDayOfYear()) {
                if (playerPromotionStatus.getConsecutiveDaysPlayed() < MAX_CONSECUTIVE_DAY_LIMIT) {
                    final ProgressiveAwardEnum progressiveAwardEnumForConsecutiveDaysPlayed =
                            ProgressiveAwardEnum.getProgressiveAwardEnumForConsecutiveDaysPlayed(
                                    playerPromotionStatus.getConsecutiveDaysPlayed()
                            );
                    playerPromotionStatusBuilder.withConsecutiveDaysPlayed(
                            progressiveAwardEnumForConsecutiveDaysPlayed.getNext()
                    );
                }
            } else if (!lastPlayedNYTime.isBefore(promotionNextPlayDay)) {
                playerPromotionStatusBuilder.withConsecutiveDaysPlayed(1);
            }
        } else {
            playerPromotionStatusBuilder.withConsecutiveDaysPlayed(1);
        }

        playerPromotionStatusBuilder.withLastPlayed(message.getTime());
        final PlayerPromotionStatus updatedPlayerPromotionStatus = playerPromotionStatusBuilder.build();
        playerPromotionStatusDao.save(updatedPlayerPromotionStatus);
    }
}
