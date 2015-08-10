package com.yazino.platform.player;

import com.yazino.platform.Partner;
import org.joda.time.DateTime;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

public class PlayerProfileTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;

    @Test
    public void shouldCreateIdenticalObjectFromSource() {
        PlayerProfile expectedProfile = aPlayerProfile();
        PlayerProfile actualProfile = PlayerProfile.copy(expectedProfile).asProfile();
        assertEquals(expectedProfile, actualProfile);
    }

    @Test
    public void shouldByDefaultHaveAStatusOfActive() {
        PlayerProfile playerProfile = PlayerProfile.withPlayerId(PLAYER_ID).asProfile();
        assertThat(playerProfile.getStatus(), is(equalTo(PlayerProfileStatus.ACTIVE)));
    }

    @Test
    public void shouldCreateUserProfileWithCorrectId() {
        PlayerProfile playerProfile = PlayerProfile.withPlayerId(PLAYER_ID).asProfile();
        assertEquals(PLAYER_ID, playerProfile.getPlayerId());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectPlayerId() {
        PlayerProfile playerProfile = PlayerProfile.withPlayerId(PLAYER_ID).withPlayerId(BigDecimal.valueOf(100)).asProfile();
        assertEquals(BigDecimal.valueOf(100), playerProfile.getPlayerId());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectEmailAddress() {
        String emailAddress = "lol@you.man";
        PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withEmailAddress(emailAddress).asProfile();
        assertEquals(emailAddress, userProfile.getEmailAddress());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectDisplayName() {
        String displayName = "My Awesome Display Name";
        PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withDisplayName(displayName).asProfile();
        assertEquals(displayName, userProfile.getDisplayName());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectRealName() {
        String realName = "John Doe";
        PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withRealName(realName).asProfile();
        assertEquals(realName, userProfile.getRealName());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectGender() {
        PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withGender(Gender.OTHER).asProfile();
        assertEquals(Gender.OTHER, userProfile.getGender());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectCountry() {
        String country = "My Country";
        PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withCountry(country).asProfile();
        assertEquals(country, userProfile.getCountry());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectFirstName() {
        String firstName = "John";
        PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withFirstName(firstName).asProfile();
        assertEquals(firstName, userProfile.getFirstName());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectLastName() {
        String lastName = "Doe";
        PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withLastName(lastName).asProfile();
        assertEquals(lastName, userProfile.getLastName());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectDateOfBirth() {
        DateTime dateOfBirth = new DateTime(13131);
        PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withDateOfBirth(dateOfBirth).asProfile();
        assertEquals(dateOfBirth, userProfile.getDateOfBirth());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectReferralIdentifier() {
        String referralIdentifier = "moooo";
        PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withReferralIdentifier(referralIdentifier).asProfile();
        assertEquals(referralIdentifier, userProfile.getReferralIdentifier());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectProviderName() {
        String providerName = ">_<";
        PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withProviderName(providerName).asProfile();
        assertEquals(providerName, userProfile.getProviderName());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectRpxProvider() {
        final String providerName = ">_<";
        final PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withRpxProvider(providerName).asProfile();
        assertEquals(providerName, userProfile.getRpxProvider());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectExternalId() {
        String externalId = ">^^<";
        PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withExternalId(externalId).asProfile();
        assertEquals(externalId, userProfile.getExternalId());
    }

    @Test
    public void shouldCreateUserProfileWithSuppliedStatus() {
        PlayerProfile playerProfile = PlayerProfile.withPlayerId(PLAYER_ID).withStatus(PlayerProfileStatus.BLOCKED).asProfile();
        assertThat(playerProfile.getStatus(), is(equalTo(PlayerProfileStatus.BLOCKED)));
    }

    @Test
    public void shouldCreateUserProfileWithCorrectSyncProfile() {
        boolean syncProfile = true;
        PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withSyncProfile(syncProfile).asProfile();
        assertEquals(syncProfile, userProfile.isSyncProfile());
    }

    @Test
    public void shouldCreateUserProfileWithCorrectGuestStatus() {
        GuestStatus guestStatus = GuestStatus.GUEST;
        PlayerProfile userProfile = PlayerProfile.withPlayerId(PLAYER_ID).withGuestStatus(guestStatus).asProfile();
        assertEquals(userProfile.getGuestStatus(), guestStatus);
    }

    private PlayerProfile aPlayerProfile() {
        return PlayerProfile.withPlayerId(PLAYER_ID)
                .withEmailAddress("emailAddress@emailAddress.com")
                .withDisplayName("displayName")
                .withRealName("realName")
                .withGender(Gender.OTHER)
                .withCountry("country")
                .withFirstName("firstName")
                .withLastName("lastName")
                .withDateOfBirth(new DateTime(1337))
                .withReferralIdentifier("referralIdentifier")
                .withPartnerId(Partner.YAZINO)
                .withProviderName("providerName")
                .withRpxProvider("rpxProvider")
                .withExternalId("externalId")
                .withStatus(PlayerProfileStatus.ACTIVE)
                .withSyncProfile(false)
                .withGuestStatus(GuestStatus.NON_GUEST)
                .asProfile();
    }

}
