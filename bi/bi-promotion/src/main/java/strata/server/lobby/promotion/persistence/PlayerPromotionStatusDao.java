package strata.server.lobby.promotion.persistence;

import org.joda.time.DateTime;
import strata.server.lobby.api.promotion.domain.PlayerPromotionStatus;

import java.math.BigDecimal;


public interface PlayerPromotionStatusDao {

    PlayerPromotionStatus get(BigDecimal playerId);

    PlayerPromotionStatus save(PlayerPromotionStatus playerPromotionStatus);

    void saveAcknowledgeTopUpForPlayer(BigDecimal playerId, DateTime acknowledgeDate);
}
