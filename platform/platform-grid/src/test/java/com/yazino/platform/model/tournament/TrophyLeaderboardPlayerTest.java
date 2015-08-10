package com.yazino.platform.model.tournament;

import com.yazino.platform.tournament.TrophyLeaderboardPlayer;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TrophyLeaderboardPlayerTest {

    @Test
    public void getPayoutShouldReturnDefaultPayoutAmount() {
        final TrophyLeaderboardPlayer player1 = new TrophyLeaderboardPlayer(1, BigDecimal.valueOf(1), "player 1", 10, "picture 1");
        assertThat(player1.getFinalPayoutWithDefault(BigDecimal.ZERO), is(BigDecimal.ZERO));

    }

    @Test
    public void getPayoutShouldReturnPayoutAmountIfSet() {
        final TrophyLeaderboardPlayer player1 = new TrophyLeaderboardPlayer(1, BigDecimal.valueOf(1), "player 1", 10, "picture 1");
        player1.setFinalPayout(BigDecimal.TEN);
        assertThat(player1.getFinalPayoutWithDefault(BigDecimal.ZERO), is(BigDecimal.TEN));

    }

}
