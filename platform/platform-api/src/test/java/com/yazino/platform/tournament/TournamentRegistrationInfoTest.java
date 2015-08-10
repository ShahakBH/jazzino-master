package com.yazino.platform.tournament;

import org.joda.time.DateTime;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.*;

/**
 * Tests the {@link com.yazino.platform.tournament.TournamentRegistrationInfo} class.
 */
public class TournamentRegistrationInfoTest {

    private static final BigDecimal FRIEND_ONE = BigDecimal.valueOf(1);
    private static final BigDecimal FRIEND_TWO = BigDecimal.valueOf(2);

    @Test
    public void shouldReturnNumberOfFriendsRegistered() {
        assertEquals(0, aRegistrationInfoWithPlayers().countMatchingPlayersRegistered(newHashSet(FRIEND_ONE, FRIEND_TWO)));
        assertEquals(1, aRegistrationInfoWithPlayers(FRIEND_ONE).countMatchingPlayersRegistered(newHashSet(FRIEND_ONE, FRIEND_TWO)));
        assertEquals(2, aRegistrationInfoWithPlayers(FRIEND_ONE, FRIEND_TWO).countMatchingPlayersRegistered(newHashSet(FRIEND_ONE, FRIEND_TWO)));
    }

    @Test
    public void shouldRecogniseRegisterPlayers() {
        assertFalse(aRegistrationInfoWithPlayers().isRegistered(FRIEND_ONE));
        assertFalse(aRegistrationInfoWithPlayers().isRegistered(FRIEND_TWO));
        assertTrue(aRegistrationInfoWithPlayers(FRIEND_ONE).isRegistered(FRIEND_ONE));
        assertTrue(aRegistrationInfoWithPlayers(FRIEND_TWO).isRegistered(FRIEND_TWO));
    }

    @Test
    public void shouldRecogniseRegisterPlayersAddedInBulk() {
        assertTrue(aRegistrationInfoWithPlayers(FRIEND_ONE, FRIEND_TWO).isRegistered(FRIEND_ONE));
        assertTrue(aRegistrationInfoWithPlayers(FRIEND_ONE, FRIEND_TWO).isRegistered(FRIEND_TWO));
    }

    private TournamentRegistrationInfo aRegistrationInfoWithPlayers(final BigDecimal... playerIds) {
        return new TournamentRegistrationInfo(BigDecimal.ZERO, new DateTime(), BigDecimal.ZERO, BigDecimal.ZERO,
                "aName", "aDescription", "aVariationTemplateName", new HashSet<>(Arrays.asList(playerIds)));
    }
}
