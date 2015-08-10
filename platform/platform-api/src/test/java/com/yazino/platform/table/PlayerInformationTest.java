package com.yazino.platform.table;

import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PlayerInformationTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(77);
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(45435);
    private static final BigDecimal ANOTHER_ACCOUNT_ID = BigDecimal.valueOf(78);
    private static final BigDecimal CACHED_BALANCE = BigDecimal.valueOf(100);
    private static final String PLAYER_NAME = "aName";
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(3141592L);

    @Test(expected = IllegalStateException.class)
    public void anIllegalStateExceptionShouldBeThrownFromGetAccountIdWhenTheAccountIsNull() {
        final PlayerInformation playerInformation = new PlayerInformation(PLAYER_ID);

        playerInformation.getAccountId();
    }

    @Test
    public void settingTheAccountIdToANonNullValueWhileItIsNullShouldUpdateTheAccountId() {
        final PlayerInformation playerInformation = new PlayerInformation(PLAYER_ID);

        playerInformation.setAccountId(ACCOUNT_ID);

        assertThat(playerInformation.getAccountId(), is(equalTo(ACCOUNT_ID)));
    }

    @Test(expected = NullPointerException.class)
    public void aNullPointerExceptionShouldBeThrownWhenTheAccountIdIsSetToNull() {
        final PlayerInformation playerInformation = new PlayerInformation(PLAYER_ID);

        playerInformation.setAccountId(null);
    }


    /**
     * Tests for temporary hack
     */
    @Test
    public void temporaryHackSetsNameToDefaultWhenConstructorReceivesNullName() {
        final PlayerInformation playerInformation = new PlayerInformation(PLAYER_ID, null, ACCOUNT_ID, SESSION_ID, CACHED_BALANCE);
        assertThat(playerInformation.getName(), is(equalTo("Unknown")));
    }

    @Test
    public void temporaryHackDoesNotAffectNameWhenConstructorReceivesNonUllName() {
        final PlayerInformation playerInformation = new PlayerInformation(PLAYER_ID, PLAYER_NAME, ACCOUNT_ID, SESSION_ID, CACHED_BALANCE);
        assertThat(playerInformation.getName(), is(equalTo(PLAYER_NAME)));
    }

    @Test
    public void temporaryHackSetsNameToDefaultWhenNameSetToNull() {
        final PlayerInformation playerInformation = new PlayerInformation(PLAYER_ID, PLAYER_NAME, ACCOUNT_ID, SESSION_ID, CACHED_BALANCE);
        playerInformation.setName(null);
        assertThat(playerInformation.getName(), is(equalTo("Unknown")));
    }

    @Test
    public void temporaryHackDoesNotAffectNameWhenNameSetToNonNullValue() {
        final PlayerInformation playerInformation = new PlayerInformation(PLAYER_ID, PLAYER_NAME, ACCOUNT_ID, SESSION_ID, CACHED_BALANCE);
        playerInformation.setName(PLAYER_NAME);
        assertThat(playerInformation.getName(), is(equalTo(PLAYER_NAME)));
    }

    @Test
    public void temporaryHackSetsNameFieldToDefault() {
        final PlayerInformation playerInformation = new PlayerInformation(PLAYER_ID);
        assertThat(playerInformation.getName(), is(equalTo("Unknown")));
    }

    /**
     * End of Tests for temporary hack
     */

    private PlayerInformation aPopulatedPlayerInformation() {
        return new PlayerInformation(PLAYER_ID, PLAYER_NAME, ACCOUNT_ID, SESSION_ID, CACHED_BALANCE);
    }

    private PlayerInformation aPopulatedPlayerInformationWithNoAccountId() {
        final PlayerInformation playerInformation = new PlayerInformation(PLAYER_ID);
        playerInformation.setName(PLAYER_NAME);
        playerInformation.setCachedBalance(CACHED_BALANCE);
        return playerInformation;
    }
}
