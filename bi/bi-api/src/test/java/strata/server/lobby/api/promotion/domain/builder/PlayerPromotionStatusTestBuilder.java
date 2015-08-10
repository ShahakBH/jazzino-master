package strata.server.lobby.api.promotion.domain.builder;

import org.joda.time.DateTime;

import java.math.BigDecimal;

public class PlayerPromotionStatusTestBuilder {

    public static final DateTime LAST_PLAYED_DATE = new DateTime(2012, 3, 5, 18, 50, 0, 0);
    public static final DateTime LAST_TOPUP_DATE = new DateTime(2012, 3, 2, 18, 50, 0, 0);
    public static final BigDecimal PLAYER_ID = new BigDecimal(-10);
    public static final int CONSECUTIVE_DAYS_PLAYED = 3;

    public static PlayerPromotionStatusBuilder create() {
        return new PlayerPromotionStatusBuilder(PLAYER_ID, LAST_PLAYED_DATE, LAST_TOPUP_DATE, CONSECUTIVE_DAYS_PLAYED, false);
    }
}
