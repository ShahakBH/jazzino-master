package strata.server.lobby.api.promotion.domain.builder;

import org.junit.Test;
import strata.server.lobby.api.promotion.domain.PlayerPromotionStatus;

import static org.junit.Assert.assertEquals;

public class PlayerPromotionStatusBuilderTest {

    @Test
    public void shouldWithLastTopupDateAsNullReturnNull() {
        PlayerPromotionStatus expectedPlayerPromotionStatus = new PlayerPromotionStatus(PlayerPromotionStatusTestBuilder.PLAYER_ID, null, null, 0, false);
        PlayerPromotionStatus actualPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PlayerPromotionStatusTestBuilder.PLAYER_ID)
                .withLastTopupDateAsTimestamp(null)
                .build();
        assertEquals(expectedPlayerPromotionStatus, actualPlayerPromotionStatus);
    }


    @Test
    public void shouldWithLastPlayedDateAsNullReturnNull() {
        PlayerPromotionStatus expectedPlayerPromotionStatus = new PlayerPromotionStatus(PlayerPromotionStatusTestBuilder.PLAYER_ID, null, null, 0, false);
        PlayerPromotionStatus actualPlayerPromotionStatus = new PlayerPromotionStatusBuilder()
                .withPlayerId(PlayerPromotionStatusTestBuilder.PLAYER_ID)
                .withLastPlayedDateAsTimestamp(null)
                .build();
        assertEquals(expectedPlayerPromotionStatus, actualPlayerPromotionStatus);
    }
}
