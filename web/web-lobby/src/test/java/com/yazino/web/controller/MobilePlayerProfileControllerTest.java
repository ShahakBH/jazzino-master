package com.yazino.web.controller;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.*;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.data.CountryRepository;
import com.yazino.web.data.GenderRepository;
import com.yazino.web.domain.AvatarRepository;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.yazino.web.controller.MobilePlayerProfileController.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MobilePlayerProfileControllerTest {

    private final static String COUNTRY = "GB";
    private final static String EMAIL = "foo@bar.com";
    private final static String DISPLAY_NAME = "TestDisplayName";
    private final static DateTime DOB = new DateTime(1981, 7, 9, 0, 0, 0, 0);
    public static final BigDecimal PLAYER_ID = BigDecimal.TEN;

    @Mock
    private LobbySessionCache sessionCache;
    @Mock
    private PlayerProfileService profileService;
    @Mock
    private AvatarRepository avatarRepository;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private GenderRepository genderRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private WebApiResponses webApiResponses;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private final PlayerProfile profile = buildPlayerProfile();
    private MobilePlayerProfileController underTest;

    @Before
    public void setUp() {
        underTest = new MobilePlayerProfileController(
                sessionCache, profileService, avatarRepository, authenticationService,
                genderRepository, countryRepository, webApiResponses);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenFetchingProfileAndNoSession() throws Exception {
        underTest.fetchProfile(request, response);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenPlayerProfileCouldNotBeFound() throws Exception {
        when(sessionCache.getActiveSession(request)).thenReturn(new LobbySession(
                BigDecimal.valueOf(3141592), PLAYER_ID, DISPLAY_NAME, "sessionKey", Partner.YAZINO, "pictureUrl", "email", null, true, Platform.IOS, AuthProvider.YAZINO));
        when(profileService.findByPlayerId(any(BigDecimal.class))).thenReturn(null);
        underTest.fetchProfile(request, response);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldWritePlayerToResponse() throws Exception {
        setupProfile();
        when(profileService.findByPlayerId(any(BigDecimal.class))).thenReturn(profile);
        final Map<String, Object> playerModel = new HashMap<>();
        playerModel.put("provider", "");
        playerModel.put("displayName", DISPLAY_NAME);
        playerModel.put("emailAddress", EMAIL);
        playerModel.put("dateOfBirth", "");
        playerModel.put("gender", "");
        playerModel.put("country", "");
        playerModel.put("avatarURL", "");
        final Map<String, Object> expectedModel = new HashMap<>();
        expectedModel.put(PLAYER_ID.toPlainString(), playerModel);

        underTest.fetchProfile(request, response);

        final ArgumentCaptor<Map> jsonCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).writeOk(eq(response), jsonCaptor.capture());
        final Map<String, Object> model = jsonCaptor.getValue();
        assertThat(model, is(equalTo(expectedModel)));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenTryingToUpdateDisplayNameAndNoSession() throws Exception {
        underTest.updateDisplayName(request, response, "foo");
    }

    @Test
    public void shouldReturnErrorsWhenDisplayNameValidationFails() throws Exception {
        setupProfile();
        underTest.updateDisplayName(request, response, "");
        assertOutput(PARAM_DISPLAY_NAME, false);
        verifyZeroInteractions(profileService);
    }

    @Test
    public void shouldReturnErrorsWhenDisplayNameUpdateFails() throws Exception {
        setupProfile();
        when(profileService.updateDisplayName(eq(PLAYER_ID), anyString())).thenReturn(false);
        underTest.updateDisplayName(request, response, "fsfs");
        assertOutput(PARAM_DISPLAY_NAME, false);
    }

    @Test
    public void shouldReturnNoErrorsWhenDisplayNameUpdateSucceeds() throws Exception {
        setupProfile();
        when(profileService.updateDisplayName(eq(PLAYER_ID), anyString())).thenReturn(true);
        underTest.updateDisplayName(request, response, "bar");
        assertOutput(PARAM_DISPLAY_NAME, true);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenTryingToUpdateEmailAddressAndNoSession() throws Exception {
        underTest.updateEmailAddress(request, response, "foo@bar.com");
    }

    @Test
    public void shouldReturnErrorsWhenEmailAddressValidationFails() throws Exception {
        setupProfile();
        underTest.updateEmailAddress(request, response, "");
        assertOutput(PARAM_EMAIL_ADDRESS, false);
        verifyZeroInteractions(profileService);
    }

    @Test
    public void shouldReturnErrorsWhenEmailAddressUpdateFails() throws Exception {
        setupProfile();
        when(profileService.updateEmailAddress(eq(PLAYER_ID), anyString())).thenReturn(false);
        underTest.updateEmailAddress(request, response, "fsfs");
        assertOutput(PARAM_EMAIL_ADDRESS, false);
    }

    @Test
    public void shouldReturnNoErrorsWhenEmailAddressUpdateSucceeds() throws Exception {
        setupProfile();
        when(profileService.updateEmailAddress(eq(PLAYER_ID), anyString())).thenReturn(true);

        underTest.updateEmailAddress(request, response, "bar@foo.com");

        assertOutput(PARAM_EMAIL_ADDRESS, true);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenTryingToUpdateGenderAndNoSession() throws Exception {
        underTest.updateEmailAddress(request, response, "F");
    }

    @Test
    public void shouldReturnErrorsWhenGenderValidationFails() throws Exception {
        setupProfile();
        underTest.updateGender(request, response, "");
        assertOutput(PARAM_GENDER, false);
    }

    @Test
    public void shouldReturnErrorsWhenGenderUpdateFails() throws Exception {
        setupProfile();
        when(profileService.updatePlayerInfo(eq(PLAYER_ID), any(PlayerProfileSummary.class))).thenReturn(false);
        underTest.updateGender(request, response, "fsfs");
        assertOutput(PARAM_GENDER, false);
    }

    @Test
    public void shouldReturnNoErrorsWhenHasExistingGenderAndUpdateSucceeds() throws Exception {
        setupProfile();
        profile.setGender(Gender.MALE);
        when(profileService.updatePlayerInfo(eq(PLAYER_ID), any(PlayerProfileSummary.class))).thenReturn(true);
        underTest.updateGender(request, response, "F");
        assertOutput(PARAM_GENDER, true);

        ArgumentCaptor<PlayerProfileSummary> captor = ArgumentCaptor.forClass(PlayerProfileSummary.class);
        verify(profileService).updatePlayerInfo(eq(PLAYER_ID), captor.capture());
        assertEquals(Gender.FEMALE.getId(), captor.getValue().getGender());
    }

    @Test
    public void shouldReturnNoErrorsWhenNoGenderAndUpdateSucceeds() throws Exception {
        setupProfile();
        assertNull(profile.getGender());
        when(profileService.updatePlayerInfo(eq(PLAYER_ID), any(PlayerProfileSummary.class))).thenReturn(true);
        underTest.updateGender(request, response, "F");
        assertOutput(PARAM_GENDER, true);

        ArgumentCaptor<PlayerProfileSummary> captor = ArgumentCaptor.forClass(PlayerProfileSummary.class);
        verify(profileService).updatePlayerInfo(eq(PLAYER_ID), captor.capture());
        assertEquals(Gender.FEMALE.getId(), captor.getValue().getGender());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenTryingToUpdateCountryAndNoSession() throws Exception {
        underTest.updateCountry(request, response, "GB");
    }

    @Test
    public void shouldReturnErrorsWhenCountryValidationFails() throws Exception {
        setupProfile();
        underTest.updateCountry(request, response, "");
        assertOutput(PARAM_COUNTRY, false);
    }

    @Test
    public void shouldReturnErrorsWhenCountryUpdateFails() throws Exception {
        setupProfile();
        when(profileService.updatePlayerInfo(eq(PLAYER_ID), any(PlayerProfileSummary.class))).thenReturn(false);
        underTest.updateCountry(request, response, "fsfs");
        assertOutput(PARAM_COUNTRY, false);
    }

    @Test
    public void shouldReturnNoErrorsWhenHasExistingCountryAndUpdateSucceeds() throws Exception {
        setupProfile();
        profile.setCountry(COUNTRY);
        when(profileService.updatePlayerInfo(eq(PLAYER_ID), any(PlayerProfileSummary.class))).thenReturn(true);
        underTest.updateCountry(request, response, "IRL");
        assertOutput(PARAM_COUNTRY, true);

        ArgumentCaptor<PlayerProfileSummary> captor = ArgumentCaptor.forClass(PlayerProfileSummary.class);
        verify(profileService).updatePlayerInfo(eq(PLAYER_ID), captor.capture());
        assertEquals("IRL", captor.getValue().getCountry());
    }

    @Test
    public void shouldReturnNoErrorsWhenNoCountryAndUpdateSucceeds() throws Exception {
        setupProfile();
        assertNull(profile.getCountry());
        when(profileService.updatePlayerInfo(eq(PLAYER_ID), any(PlayerProfileSummary.class))).thenReturn(true);
        underTest.updateCountry(request, response, "IRL");
        assertOutput(PARAM_COUNTRY, true);

        ArgumentCaptor<PlayerProfileSummary> captor = ArgumentCaptor.forClass(PlayerProfileSummary.class);
        verify(profileService).updatePlayerInfo(eq(PLAYER_ID), captor.capture());
        assertEquals("IRL", captor.getValue().getCountry());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenTryingToUpdateDobAndNoSession() throws Exception {
        underTest.updateDateOfBirth(request, response, new DateTime());
    }

    @Test
    public void shouldReturnErrorsWhenDobValidationFails() throws Exception {
        setupProfile();
        underTest.updateDateOfBirth(request, response, null);
        assertOutput(PARAM_DOB, false);
    }

    @Test
    public void shouldReturnErrorsWhenDobUpdateFails() throws Exception {
        setupProfile();
        when(profileService.updatePlayerInfo(eq(PLAYER_ID), any(PlayerProfileSummary.class))).thenReturn(false);
        underTest.updateDateOfBirth(request, response, new DateTime());
        assertOutput(PARAM_DOB, false);
    }

    @Test
    public void shouldReturnNoErrorsWhenHasDobAndUpdateSucceeds() throws Exception {
        setupProfile();
        profile.setDateOfBirth(DOB);
        when(profileService.updatePlayerInfo(eq(PLAYER_ID), any(PlayerProfileSummary.class))).thenReturn(true);
        DateTime dob = new DateTime();
        underTest.updateDateOfBirth(request, response, dob);
        assertOutput(PARAM_DOB, true);

        ArgumentCaptor<PlayerProfileSummary> captor = ArgumentCaptor.forClass(PlayerProfileSummary.class);
        verify(profileService).updatePlayerInfo(eq(PLAYER_ID), captor.capture());
        assertEquals(dob, captor.getValue().getDateOfBirth());
    }

    @Test
    public void shouldReturnNoErrorsWhenNoDobAndUpdateSucceeds() throws Exception {
        setupProfile();
        assertNull(profile.getDateOfBirth());
        when(profileService.updatePlayerInfo(eq(PLAYER_ID), any(PlayerProfileSummary.class))).thenReturn(true);
        underTest.updateDateOfBirth(request, response, DOB);
        assertOutput(PARAM_DOB, true);

        ArgumentCaptor<PlayerProfileSummary> captor = ArgumentCaptor.forClass(PlayerProfileSummary.class);
        verify(profileService).updatePlayerInfo(eq(PLAYER_ID), captor.capture());
        assertEquals(DOB, captor.getValue().getDateOfBirth());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenTryingToUpdatePasswordAndNoSession() throws Exception {
        underTest.updatePassword(request, response, "foobar", "foobar");
    }

    @Test
    public void shouldReturnErrorsWhenPasswordValidationFails() throws Exception {
        setupProfile();
        when(profileService.findLoginEmailByPlayerId(PLAYER_ID)).thenReturn(EMAIL);
        PlayerProfileAuthenticationResponse authResponse = new PlayerProfileAuthenticationResponse(PLAYER_ID, true);
        when(authenticationService.authenticateYazinoUser(EMAIL, "foobar")).thenReturn(authResponse);

        underTest.updatePassword(request, response, "foobar", "barfoo");

        assertOutput(PARAM_PASSWORD, false);
    }

    @Test
    public void shouldReturnNoErrorsWhenPasswordUpdateSucceeds() throws Exception {
        setupProfile();
        when(profileService.findLoginEmailByPlayerId(PLAYER_ID)).thenReturn(EMAIL);
        PlayerProfileAuthenticationResponse authResponse = new PlayerProfileAuthenticationResponse(PLAYER_ID);
        when(authenticationService.authenticateYazinoUser(EMAIL, "foobar")).thenReturn(authResponse);
        underTest.updatePassword(request, response, "foobar", "barfoo");
        assertOutput(PARAM_PASSWORD, true);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenTryingToUpdateAvatarAndNoSession() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        underTest.updateAvatar(request, response, file);
    }

    @Test
    public void shouldReturnErrorsWhenAvatarValidationFails() throws Exception {
        setupProfile();
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("");
        underTest.updateAvatar(request, response, file);
        assertOutput(PARAM_AVATAR, false);
    }

    @Test
    public void shouldReturnErrorsWhenAvatarUpdateFails() throws Exception {
        setupProfile();
        when(profileService.updateAvatar(eq(PLAYER_ID), any(Avatar.class))).thenReturn(false);
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("fdsfds");
        when(file.getBytes()).thenReturn(new byte[]{0, 1, 20});
        underTest.updateAvatar(request, response, file);
        assertOutput(PARAM_AVATAR, false);
    }

    @Test
    public void shouldReturnNoErrorsWhenAvatarUpdateSucceeds() throws Exception {
        setupProfile();
        final byte[] avatarData = {0, 1, 20};
        final Avatar avatar = new Avatar("aPictureLocation", "aUrl");
        final MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("anOriginalFilename");
        when(file.getBytes()).thenReturn(avatarData);
        when(avatarRepository.storeAvatar("anOriginalFilename", avatarData)).thenReturn(avatar);
        when(profileService.updateAvatar(PLAYER_ID, avatar)).thenReturn(true);

        underTest.updateAvatar(request, response, file);

        assertOutput(PARAM_AVATAR, true);
    }

    @Test
    public void shouldReturnProfileSummaryWithNullGenderWhenNoGenderInProfile() throws Exception {
        setupProfile();

    }

    private void setupProfile() {
        when(sessionCache.getActiveSession(request))
                .thenReturn(new LobbySession(BigDecimal.valueOf(3141592), PLAYER_ID, DISPLAY_NAME, "sessionKey",
                        Partner.YAZINO, "pictureUrl", "email", null, true, Platform.IOS, AuthProvider.YAZINO));
        when(profileService.findByPlayerId(any(BigDecimal.class))).thenReturn(profile);
    }

    private static PlayerProfile buildPlayerProfile() {
        return PlayerProfile.withPlayerId(PLAYER_ID)
                .withDisplayName(DISPLAY_NAME)
                .withEmailAddress(EMAIL)
                .asProfile();
    }

    @SuppressWarnings("unchecked")
    private void assertOutput(String fieldName, boolean success) throws IOException {
        final ArgumentCaptor<Map> jsonCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).writeOk(eq(response), jsonCaptor.capture());
        final Map<String, Map<String, Object>> model = jsonCaptor.getValue();
        System.err.println(model);
        assertTrue(model.containsKey(fieldName));
        final Map<String, Object> fieldModel = model.get(fieldName);
        assertEquals(success, fieldModel.get("updated"));
        if (!success) {
            assertTrue(fieldModel.containsKey("errors"));
            List<String> errors = (List<String>) fieldModel.get("errors");
            assertTrue(errors.size() > 0);
        }
    }

}
