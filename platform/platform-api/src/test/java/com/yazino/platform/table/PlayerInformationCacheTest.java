package com.yazino.platform.table;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class PlayerInformationCacheTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(10);
    private static final BigDecimal PLAYER_1_ID = BigDecimal.valueOf(1);
    private static final BigDecimal PLAYER_2_ID = BigDecimal.valueOf(2);
    private static final BigDecimal PLAYER_3_ID = BigDecimal.valueOf(3);

    private PlayerInformationCache unit;

    @Before
    public void setUp() throws Exception {
        unit = new PlayerInformationCache();
    }

    @Test
    public void aMissingPlayerWillReturnInANullValueFromGet() {
        assertThat(unit.get(PLAYER_ID), is(nullValue()));
    }

    @Test
    public void aPlayerCanBeAddedToTheCache() {
        unit.add(playerInfo());

        assertThat(unit.get(PLAYER_ID), is(not(nullValue())));
        assertThat(unit.get(PLAYER_ID), is(equalTo(playerInfo())));
    }

    private PlayerInformation playerInfo() {
        final PlayerInformation playerInformation = new PlayerInformation(PLAYER_ID);
        playerInformation.setName("aPlayer");
        playerInformation.setAccountId(BigDecimal.TEN);
        playerInformation.setCachedBalance(BigDecimal.valueOf(666));
        return playerInformation;
    }

    @Test
    public void onlyRequestedItemsShouldBeRetainedInTheCache() {
        unit.add(aPlayer(PLAYER_1_ID));
        unit.add(aPlayer(PLAYER_2_ID));
        unit.add(aPlayer(PLAYER_3_ID));

        unit.retainOnly(asList(PLAYER_1_ID, PLAYER_3_ID));

        assertThat(unit.get(PLAYER_1_ID), is(equalTo(aPlayer(PLAYER_1_ID))));
        assertThat(unit.get(PLAYER_3_ID), is(equalTo(aPlayer(PLAYER_3_ID))));
        assertThat(unit.get(PLAYER_2_ID), is(nullValue()));
    }

    private PlayerInformation aPlayer(final BigDecimal playerId) {
        return new PlayerInformation(playerId, "aName" + playerId, playerId, BigDecimal.ONE, BigDecimal.ZERO);
    }
}
