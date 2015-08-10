package com.yazino.platform.player.updater;

import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.CommunityService;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileUpdateResponse;
import com.yazino.platform.player.persistence.PlayerProfileDao;
import com.yazino.platform.reference.Currency;
import com.yazino.platform.reference.ReferenceService;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ExternalPlayerProfileUpdaterTest {
    private final CommunityService communityService = mock(CommunityService.class);
    private final PlayerService playerService = mock(PlayerService.class);
    private final PlayerProfileDao playerProfileDao = mock(PlayerProfileDao.class);
    private final ReferenceService referenceService = mock(ReferenceService.class);

    private final ExternalPlayerProfileUpdater underTest = new ExternalPlayerProfileUpdater(
            playerProfileDao, communityService, referenceService, playerService);

    @Test
    public void theUpdaterDoesNotAcceptYazinoProfiles() {
        assertThat(underTest.accepts("YAZINO"), is(equalTo(false)));
    }

    @Test
    public void theUpdaterDoesNotAcceptNullProviders() {
        assertThat(underTest.accepts(null), is(equalTo(false)));
    }

    @Test
    public void theUpdaterAcceptsFacebookProfiles() {
        assertThat(underTest.accepts("FACEBOOK"), is(equalTo(true)));
    }

    @Test
    public void theUpdaterAcceptsPlayForFunProfiles() {
        assertThat(underTest.accepts("PLAY_FOR_FUN"), is(equalTo(true)));
    }

    @Test
    public void updateFails_whenPlayerIdIsNull() throws Exception {
        PlayerProfile userProfile = new PlayerProfile();
        Assert.assertNull(userProfile.getPlayerId());
        PlayerProfileUpdateResponse response = underTest.update(userProfile, "ignored", "aPictureUrl");
        assertFalse(response.isSuccessful());
        assertFalse(response.getErrors().isEmpty());
    }

    @Test
    public void updateFails_whenPlayerDoesNotExist() {
        BigDecimal playerId = BigDecimal.ONE;
        when(playerProfileDao.findByPlayerId(playerId)).thenReturn(null);
        PlayerProfile userProfile = new PlayerProfile();
        userProfile.setPlayerId(playerId);
        PlayerProfileUpdateResponse response = underTest.update(userProfile, "ignored", "aPictureUrl");
        assertFalse(response.isSuccessful());
        assertFalse(response.getErrors().isEmpty());
    }

    @Test
    public void updateSucceeds_responseSuccessful() {
        BigDecimal playerId = BigDecimal.ONE;
        PlayerProfile existingProfile = aUserProfileFor(playerId);
        when(playerProfileDao.findByPlayerId(playerId)).thenReturn(existingProfile);
        when(playerService.getBasicProfileInformation(existingProfile.getPlayerId()))
                .thenReturn(aProfileFor(playerId));
        PlayerProfile userProfile = aUserProfileFor(playerId);
        PlayerProfileUpdateResponse response = underTest.update(userProfile, "ignored", "aPictureUrl");
        assertTrue(response.isSuccessful());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    public void updateSucceeds_responseHasCorrectUserProfile() {
        BigDecimal playerId = BigDecimal.ONE;
        PlayerProfile existingProfile = aUserProfileFor(playerId);
        existingProfile.setCountry("US");
        when(playerProfileDao.findByPlayerId(playerId)).thenReturn(existingProfile);
        when(playerService.getBasicProfileInformation(existingProfile.getPlayerId()))
                .thenReturn(aProfileFor(playerId));
        PlayerProfile updatedProfile = aUserProfileFor(playerId);
        updatedProfile.setCountry("GB");
        PlayerProfileUpdateResponse response = underTest.update(updatedProfile, "ignored", "aPictureUrl");
        assertEquals("GB", response.getUpdatedUserProfile().getCountry());
    }

    @Test
    public void updateSucceeds_playerUpdatedWithCorrectDetails() throws Exception {
        BigDecimal playerId = BigDecimal.TEN;
        PlayerProfile existingProfile = aUserProfileFor(playerId);
        existingProfile.setCountry("US");
        existingProfile.setDisplayName("TEST 1");
        when(playerProfileDao.findByPlayerId(playerId)).thenReturn(existingProfile);
        PlayerProfile updatedProfile = aUserProfileFor(playerId);
        updatedProfile.setCountry("GB");
        updatedProfile.setDisplayName("TEST 2");
        when(playerService.getBasicProfileInformation(existingProfile.getPlayerId())).thenReturn(aProfileFor(playerId));
        when(referenceService.getPreferredCurrency("GB")).thenReturn(Currency.EUR);

        underTest.update(updatedProfile, "ignored", "/foo");
        PaymentPreferences paymentPreferences = new PaymentPreferences(Currency.EUR);
        verify(communityService).asyncUpdatePlayer(playerId, "TEST 2", "/foo", paymentPreferences);
    }

    @Test
    public void update_savesPlayerProfile() throws Exception {
        BigDecimal playerId = BigDecimal.TEN;
        PlayerProfile existingProfile = aUserProfileFor(playerId);
        existingProfile.setCountry("US");
        existingProfile.setDisplayName("TEST 1");
        when(playerProfileDao.findByPlayerId(playerId)).thenReturn(existingProfile);
        PlayerProfile updatedProfile = aUserProfileFor(playerId);
        updatedProfile.setDisplayName("TEST 2");
        when(playerService.getBasicProfileInformation(existingProfile.getPlayerId()))
                .thenReturn(aProfileFor(playerId));

        underTest.update(updatedProfile, "ignored", "/foo");

        verify(playerProfileDao).save(any(PlayerProfile.class));
    }

    private PlayerProfile aUserProfileFor(final BigDecimal playerId) {
        final PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setPlayerId(playerId);
        playerProfile.setGuestStatus(GuestStatus.GUEST);
        return playerProfile;
    }

    private BasicProfileInformation aProfileFor(final BigDecimal playerId) {
        return new BasicProfileInformation(playerId, "name", "pictureUrl", BigDecimal.TEN);
    }

}
