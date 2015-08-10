package strata.server.lobby.api.promotion.domain.builder;

import org.joda.time.DateTime;
import strata.server.lobby.api.promotion.ProgressiveAwardEnum;
import strata.server.lobby.api.promotion.domain.PlayerPromotionStatus;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class PlayerPromotionStatusBuilder {

    private BigDecimal playerId;
    private DateTime lastPlayed;
    private DateTime lastTopup;
    private int consecutiveDaysPlayed;
    private boolean topUpAcknowledged;

    public PlayerPromotionStatusBuilder() {
    }

    public PlayerPromotionStatusBuilder(final BigDecimal playerId,
                                        final DateTime lastPlayed,
                                        final DateTime lastTopup,
                                        final int consecutiveDaysPlayed,
                                        final boolean topUpAcknowledged) {
        this.playerId = playerId;
        this.lastPlayed = lastPlayed;
        this.consecutiveDaysPlayed = consecutiveDaysPlayed;
        this.lastTopup = lastTopup;
        this.topUpAcknowledged = topUpAcknowledged;
    }

    public PlayerPromotionStatusBuilder(final PlayerPromotionStatus playerPromotionStatus) {
        this(playerPromotionStatus.getPlayerId(),
                playerPromotionStatus.getLastPlayed(),
                playerPromotionStatus.getLastTopup(),
                playerPromotionStatus.getConsecutiveDaysPlayed(),
                playerPromotionStatus.isTopUpAcknowledged());
    }

    public PlayerPromotionStatusBuilder withPlayerId(final BigDecimal newPlayerId) {
        this.playerId = newPlayerId;
        return this;
    }

    public PlayerPromotionStatusBuilder withLastPlayed(final DateTime newLastPlayed) {
        this.lastPlayed = newLastPlayed;
        return this;
    }

    public PlayerPromotionStatusBuilder withConsecutiveDaysPlayed(final int newConsecutiveDaysPlayed) {
        this.consecutiveDaysPlayed = newConsecutiveDaysPlayed;
        return this;
    }

    public PlayerPromotionStatusBuilder withLastTopupDate(final DateTime newLastTopup) {
        this.lastTopup = newLastTopup;
        return this;
    }

    public PlayerPromotionStatusBuilder withLastTopupDateAsTimestamp(final Timestamp newLastTopup) {
        if (newLastTopup != null) {
            this.lastTopup = new DateTime(newLastTopup);
        } else {
            this.lastTopup = null;
        }
        return this;
    }

    public PlayerPromotionStatusBuilder withLastPlayedDateAsTimestamp(final Timestamp newLastPlayedDate) {
        if (newLastPlayedDate != null) {
            this.lastPlayed = new DateTime(newLastPlayedDate);
        } else {
            this.lastPlayed = null;
        }
        return this;

    }

    public PlayerPromotionStatusBuilder withConsecutiveDaysPlayed(
            final ProgressiveAwardEnum progressiveAwardEnumForConsecutiveDaysPlayed) {
        consecutiveDaysPlayed = progressiveAwardEnumForConsecutiveDaysPlayed.getConsecutiveDaysPlayed();
        return this;
    }

    public PlayerPromotionStatusBuilder withTopUpAcknowledged(final boolean ack) {
        topUpAcknowledged = ack;
        return this;
    }

    public PlayerPromotionStatus build() {
        return new PlayerPromotionStatus(playerId, lastPlayed, lastTopup, consecutiveDaysPlayed, topUpAcknowledged);
    }
}
