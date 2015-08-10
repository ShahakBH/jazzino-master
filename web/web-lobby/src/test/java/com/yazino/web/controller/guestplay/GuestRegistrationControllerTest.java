package com.yazino.web.controller.guestplay;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.player.LoginResult;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileRegistrationResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.web.controller.MobileLoginController;
import com.yazino.web.service.GameAvailability;
import com.yazino.web.service.GameAvailabilityService;
import com.yazino.web.service.YazinoWebLoginService;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionReference;
import com.yazino.web.session.ReferrerSessionCache;
import com.yazino.web.util.JsonHelper;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static com.yazino.platform.Platform.ANDROID;
import static com.yazino.web.controller.guestplay.BusinessError.*;
import static com.yazino.web.service.GameAvailabilityService.Availability.AVAILABLE;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GuestRegistrationControllerTest {
    private static final String DISPLAY_NAME = "Guest";
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final String EMAIL = "guest@example.com";
    private static final String PASSWORD = "password";
    private static final String REMOTE_ADDRESS = "remoteAddress";
    private static final String REFERRER = "referrer";
    private static final String GAME_TYPE = "gameType";
    private static final String AVATAR_URL = "http://avatar.url";
    private static final String DEFAULT_AVATAR_URL = "http://content/defaultAvatar";
    private static final String SESSION_KEY = "session-key";
    private static final boolean NEW_PLAYER_FLAG = true;
    private static final BigDecimal SESSION_ID = BigDecimal.valueOf(67676789);
    private static final AuthProvider A_AUTH_PROVIDER = AuthProvider.YAHOO;
    private static final Platform PLATFORM = ANDROID;
    private static final String DEFAULT_PARTNER_ID = "YAZINO";
    public static final String PARTNER_ID = "YAZINO";

    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private HttpServletResponse httpResponse;
    @Mock
    private PrintWriter printWriter;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private ReferrerSessionCache referrerSessionCache;
    @Mock
    private WebApiResponses responseWriter;
    @Mock
    private GameAvailabilityService gameAvailabilityService;
    @Mock
    private YazinoWebLoginService yazinoWebLoginService;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private GuestRegistrationController underTest;

    @Before
    public void setUp() throws IOException {
        when(httpResponse.getWriter()).thenReturn(printWriter);
        when(httpRequest.getRemoteAddr()).thenReturn(REMOTE_ADDRESS);
        when(referrerSessionCache.getReferrer()).thenReturn(REFERRER);
        when(yazinoConfiguration.getString("strata.lobby.partnerid", "YAZINO")).thenReturn(DEFAULT_PARTNER_ID);
        when(yazinoConfiguration.getString("senet.web.content")).thenReturn("http://content");
        when(yazinoConfiguration.getString("senet.web.defaultAvatarPath")).thenReturn("/defaultAvatar");

        underTest = new GuestRegistrationController(yazinoWebLoginService, referrerSessionCache,
                authenticationService, gameAvailabilityService, responseWriter, yazinoConfiguration);
    }

    @Test
    public void registerGuestShouldDelegateToAuthenticationService() throws IOException {
        ArgumentCaptor<PlayerProfile> profile = ArgumentCaptor.forClass(PlayerProfile.class);
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(), anyString(), any(Platform.class), anyString(), anyString())).thenReturn(new PlayerProfileRegistrationResponse());

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL, PARTNER_ID);

        verify(authenticationService).registerYazinoUser(eq(EMAIL), eq(PASSWORD), profile.capture(), eq(REMOTE_ADDRESS), eq(REFERRER), eq(PLATFORM), eq(AVATAR_URL), eq(GAME_TYPE));
        assertThat(profile.getValue().getDisplayName(), equalTo(DISPLAY_NAME));
        assertThat(profile.getValue().getGuestStatus(), equalTo(GuestStatus.GUEST));
    }

    @Test
    public void registerGuestShouldSetEmailAddressOnProfile() throws IOException {
        ArgumentCaptor<PlayerProfile> profile = ArgumentCaptor.forClass(PlayerProfile.class);
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(), anyString(), any(Platform.class), anyString(), anyString())).thenReturn(new PlayerProfileRegistrationResponse());

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL, PARTNER_ID);

        verify(authenticationService).registerYazinoUser(eq(EMAIL),
                eq(PASSWORD),
                profile.capture(),
                eq(REMOTE_ADDRESS),
                eq(REFERRER),
                eq(PLATFORM),
                eq(AVATAR_URL),
                eq(GAME_TYPE));
        assertThat(profile.getValue().getEmailAddress(), equalTo(EMAIL));
    }

    @Test
    public void registerGuestShouldUseTheDefaultAvatarIfTheAvatarUrlIsBlank() throws IOException {
        ArgumentCaptor<PlayerProfile> profile = ArgumentCaptor.forClass(PlayerProfile.class);
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(), anyString(), any(Platform.class), anyString(), anyString())).thenReturn(new PlayerProfileRegistrationResponse());

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, "  ", PARTNER_ID);

        verify(authenticationService).registerYazinoUser(eq(EMAIL),
                eq(PASSWORD),
                profile.capture(),
                eq(REMOTE_ADDRESS),
                eq(REFERRER),
                eq(PLATFORM),
                eq(DEFAULT_AVATAR_URL),
                eq(GAME_TYPE));
        assertThat(profile.getValue().getEmailAddress(), equalTo(EMAIL));
    }

    @Test
    public void registerGuestShouldUseTheDefaultAvatarIfTheAvatarUrlIsNull() throws IOException {
        ArgumentCaptor<PlayerProfile> profile = ArgumentCaptor.forClass(PlayerProfile.class);
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(), anyString(), any(Platform.class), anyString(), anyString())).thenReturn(new PlayerProfileRegistrationResponse());

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, null, PARTNER_ID);

        verify(authenticationService).registerYazinoUser(eq(EMAIL),
                eq(PASSWORD),
                profile.capture(),
                eq(REMOTE_ADDRESS),
                eq(REFERRER),
                eq(PLATFORM),
                eq(DEFAULT_AVATAR_URL),
                eq(GAME_TYPE));
        assertThat(profile.getValue().getEmailAddress(), equalTo(EMAIL));
    }

    @Test
    public void registerGuestShouldUseTheDefaultAvatarIfTheAvatarUrlIsDefaultString() throws IOException {
        ArgumentCaptor<PlayerProfile> profile = ArgumentCaptor.forClass(PlayerProfile.class);
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(), anyString(), any(Platform.class), anyString(), anyString())).thenReturn(new PlayerProfileRegistrationResponse());

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, "default", PARTNER_ID);

        verify(authenticationService).registerYazinoUser(eq(EMAIL),
                eq(PASSWORD),
                profile.capture(),
                eq(REMOTE_ADDRESS),
                eq(REFERRER),
                eq(PLATFORM),
                eq(DEFAULT_AVATAR_URL),
                eq(GAME_TYPE));
        assertThat(profile.getValue().getEmailAddress(), equalTo(EMAIL));
    }

    @Test
    public void registerGuestShouldSetPartnerOnProfile() throws IOException {
        ArgumentCaptor<PlayerProfile> profile = ArgumentCaptor.forClass(PlayerProfile.class);
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(), anyString(), any(Platform.class), anyString(), anyString())).thenReturn(new PlayerProfileRegistrationResponse());

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL, PARTNER_ID);

        verify(authenticationService).registerYazinoUser(eq(EMAIL), eq(PASSWORD), profile.capture(), eq(REMOTE_ADDRESS), eq(REFERRER), eq(PLATFORM), eq(AVATAR_URL), eq(GAME_TYPE));
        assertThat(profile.getValue().getPartnerId(), equalTo(Partner.YAZINO));
    }

    @Test
    public void registerGuest_1_0_ShouldUseDefaultPartnerId() throws IOException {
        ArgumentCaptor<PlayerProfile> profile = ArgumentCaptor.forClass(PlayerProfile.class);
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(), anyString(), any(Platform.class), anyString(), anyString())).thenReturn(new PlayerProfileRegistrationResponse());

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL);

        verify(authenticationService).registerYazinoUser(eq(EMAIL),
                eq(PASSWORD),
                profile.capture(),
                eq(REMOTE_ADDRESS),
                eq(REFERRER),
                eq(PLATFORM),
                eq(AVATAR_URL),
                eq(GAME_TYPE));
        assertThat(profile.getValue().getPartnerId(), equalTo(Partner.YAZINO));
    }

    @Test
    public void registerGuestShouldLoginNewlyRegisteredUserWhenSuccessful() throws IOException {
        givenAuthServiceWillReturnProfileForPlayer();
        givenLoginNewlyRegisteredUserWillReturnSession();
        givenGameIsAvailable();

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL, PARTNER_ID);

        verify(yazinoWebLoginService).loginNewlyRegisteredUser(httpRequest, httpResponse, EMAIL, PASSWORD, PLATFORM, Partner.YAZINO);
    }

    @Test
    public void registerGuestShouldReturnSessionDetailsWhenSuccessful() throws IOException {
        givenAuthServiceWillReturnProfileForPlayer();
        givenLoginNewlyRegisteredUserWillReturnSession();
        givenGameIsAvailable();

        MobileLoginController.LoginInfo expectedSessionDetails = expectedSessionDetails();

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL, PARTNER_ID);

        ArgumentCaptor<GuestRegistrationController.GuestRegistrationResponse> registrationResponse = ArgumentCaptor.forClass(GuestRegistrationController.GuestRegistrationResponse.class);
        verify(responseWriter).write(same(httpResponse), eq(200), registrationResponse.capture());
        assertTrue(registrationResponse.getValue().isSuccessful());
        assertThat(registrationResponse.getValue().getLoginInfo(), equalTo(expectedSessionDetails));
    }

    @Test
    public void successFullRegistrationResponseIsSerializable() {
        MobileLoginController.LoginInfo loginInfo = new MobileLoginController.LoginInfo();
        loginInfo.setAvailability(AVAILABLE);
        loginInfo.setPlayerId(PLAYER_ID);
        loginInfo.setName(DISPLAY_NAME);
        loginInfo.setSuccess(true);
        loginInfo.setSession(SESSION_KEY);
        loginInfo.setNewPlayer(true);
        GuestRegistrationController.GuestRegistrationResponse response = new GuestRegistrationController.GuestRegistrationResponse(loginInfo);

        JsonHelper jsonHelper = new JsonHelper();
        String serialized = jsonHelper.serialize(response);
        GuestRegistrationController.GuestRegistrationResponse deserialized = jsonHelper.deserialize(GuestRegistrationController.GuestRegistrationResponse.class, serialized);

        assertThat(deserialized, equalTo(response));
    }

    @Test
    public void errorRegistrationResponseIsSerializable() {
        Set<BusinessError> errors = new HashSet<>();
        errors.add(new BusinessError(100, "message for code 100"));
        errors.add(new BusinessError(200, "message for code 200"));
        GuestRegistrationController.GuestRegistrationResponse response = new GuestRegistrationController.GuestRegistrationResponse(errors);

        JsonHelper jsonHelper = new JsonHelper();
        String serialized = jsonHelper.serialize(response);
        GuestRegistrationController.GuestRegistrationResponse deserialized = jsonHelper.deserialize(GuestRegistrationController.GuestRegistrationResponse.class, serialized);

        assertThat(deserialized, equalTo(response));
    }

    @Test
    public void registerGuestShouldReturnAllBusinessErrors() throws IOException {
        Set<ParameterisedMessage> sampleErrors = new HashSet<>();
        sampleErrors.add(new ParameterisedMessage("Display name is offensive"));
        sampleErrors.add(new ParameterisedMessage("E-mail already registered"));
        sampleErrors.add(new ParameterisedMessage("some other error"));
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(),
                anyString(), any(Platform.class), anyString(), anyString()))
                .thenReturn(new PlayerProfileRegistrationResponse(sampleErrors));

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL, PARTNER_ID);

        ArgumentCaptor<GuestRegistrationController.GuestRegistrationResponse> responseArgumentCaptor = ArgumentCaptor.forClass(GuestRegistrationController.GuestRegistrationResponse.class);
        verify(responseWriter).write(same(httpResponse), eq(200), responseArgumentCaptor.capture());
        GuestRegistrationController.GuestRegistrationResponse guestRegistrationResponse = responseArgumentCaptor.getValue();
        assertFalse(guestRegistrationResponse.isSuccessful());
        assertThat(guestRegistrationResponse.getErrors(), hasItem(new BusinessError(ERROR_CODE_INVALID_DISPLAY_NAME, "Display name is offensive")));
        assertThat(guestRegistrationResponse.getErrors(), hasItem(new BusinessError(ERROR_CODE_NON_UNIQUE_EMAIL, "E-mail already registered")));
        assertThat(guestRegistrationResponse.getErrors(), hasItem(new BusinessError(ERROR_CODE_GENERIC_ERROR, "some other error")));
    }

    @Test
    public void registerGuestShouldReturnBusinessErrorWithCode300ForGenericErrors() throws IOException {
        Set<ParameterisedMessage> sampleErrors = new HashSet<>();
        sampleErrors.add(new ParameterisedMessage("some other error"));
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(), anyString(), any(Platform.class), anyString(), anyString())).thenReturn(new PlayerProfileRegistrationResponse(sampleErrors));

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL, PARTNER_ID);

        ArgumentCaptor<GuestRegistrationController.GuestRegistrationResponse> registrationResponse = ArgumentCaptor.forClass(GuestRegistrationController.GuestRegistrationResponse.class);
        verify(responseWriter).write(same(httpResponse), eq(200), registrationResponse.capture());
        assertFalse(registrationResponse.getValue().isSuccessful());
        assertThat(registrationResponse.getValue().getErrors(), hasItem(new BusinessError(ERROR_CODE_GENERIC_ERROR, "some other error")));
    }


    @Test
    public void registerGuestShouldReturnBusinessErrorWithCode100ForAllDisplayNameErrors() throws IOException {
        Set<ParameterisedMessage> sampleErrors = new HashSet<>();
        sampleErrors.add(new ParameterisedMessage("Display name is offensive"));
        sampleErrors.add(new ParameterisedMessage("Display name must be entered"));
        sampleErrors.add(new ParameterisedMessage("Display name must be 4-8 characters long"));
        sampleErrors.add(new ParameterisedMessage("Display name must be alphanumeric"));
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(), anyString(), any(Platform.class), anyString(), anyString())).thenReturn(new PlayerProfileRegistrationResponse(sampleErrors));

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL, PARTNER_ID);

        ArgumentCaptor<GuestRegistrationController.GuestRegistrationResponse> registrationResponse = ArgumentCaptor.forClass(GuestRegistrationController.GuestRegistrationResponse.class);
        verify(responseWriter).write(same(httpResponse), eq(200), registrationResponse.capture());
        assertFalse(registrationResponse.getValue().isSuccessful());
        assertThat(registrationResponse.getValue().getErrors(), hasItem(new BusinessError(ERROR_CODE_INVALID_DISPLAY_NAME, "Display name is offensive")));
        assertThat(registrationResponse.getValue().getErrors(), hasItem(new BusinessError(ERROR_CODE_INVALID_DISPLAY_NAME, "Display name must be entered")));
        assertThat(registrationResponse.getValue().getErrors(), hasItem(new BusinessError(ERROR_CODE_INVALID_DISPLAY_NAME, "Display name must be 4-8 characters long")));
        assertThat(registrationResponse.getValue().getErrors(), hasItem(new BusinessError(ERROR_CODE_INVALID_DISPLAY_NAME, "Display name must be alphanumeric")));
    }

    @Test
    public void registerGuestShouldReturnBusinessErrorWithCode200ForNonUniqueEmail() throws IOException {
        Set<ParameterisedMessage> sampleErrors = new HashSet<>();
        sampleErrors.add(new ParameterisedMessage("E-mail already registered"));
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(), anyString(), any(Platform.class), anyString(), anyString())).thenReturn(new PlayerProfileRegistrationResponse(sampleErrors));

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL, PARTNER_ID);

        ArgumentCaptor<GuestRegistrationController.GuestRegistrationResponse> registrationResponse = ArgumentCaptor.forClass(GuestRegistrationController.GuestRegistrationResponse.class);
        verify(responseWriter).write(same(httpResponse), eq(200), registrationResponse.capture());
        assertFalse(registrationResponse.getValue().isSuccessful());
        assertThat(registrationResponse.getValue().getErrors(), hasItem(new BusinessError(ERROR_CODE_NON_UNIQUE_EMAIL, "E-mail already registered")));
    }

    @Test
    public void registerGuestShouldReturn400WithErrorMessageWhenDisplayNameIsMissing() throws IOException {
        underTest.registerGuest(httpRequest, httpResponse, null, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL, PARTNER_ID);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpServletResponse.SC_BAD_REQUEST), eq("parameter 'displayName' is missing"));
    }

    @Test
    public void registerGuestShouldReturnOkAndDefaultToYazinoWhenPartnerIdIsMissing() throws IOException {
        ArgumentCaptor<PlayerProfile> profile = ArgumentCaptor.forClass(PlayerProfile.class);
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(), anyString(), any(Platform.class), anyString(), anyString())).thenReturn(new PlayerProfileRegistrationResponse());

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL, null);

        verify(authenticationService).registerYazinoUser(eq(EMAIL), eq(PASSWORD), profile.capture(), eq(REMOTE_ADDRESS), eq(REFERRER), eq(PLATFORM), eq(AVATAR_URL), eq(GAME_TYPE));
        assertThat(profile.getValue().getPartnerId(), equalTo(Partner.YAZINO));
    }

    @Test
    public void registerGuestShouldReturn400WithErrorMessageWhenPartnerIdIsInvalid() throws IOException {
        ArgumentCaptor<PlayerProfile> profile = ArgumentCaptor.forClass(PlayerProfile.class);
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(), anyString(), any(Platform.class), anyString(), anyString())).thenReturn(new PlayerProfileRegistrationResponse());

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL, "YOUR_MUM");

        verify(authenticationService).registerYazinoUser(eq(EMAIL), eq(PASSWORD), profile.capture(), eq(REMOTE_ADDRESS), eq(REFERRER), eq(PLATFORM), eq(AVATAR_URL), eq(GAME_TYPE));
        assertThat(profile.getValue().getPartnerId(), equalTo(Partner.YAZINO));


    }

    @Test
    public void registerGuestShouldReturn400WithErrorMessageWhenEmailIsMissing() throws IOException {
        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, null, PASSWORD, PLATFORM.name(), GAME_TYPE, AVATAR_URL, PARTNER_ID);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpServletResponse.SC_BAD_REQUEST), eq("parameter 'emailAddress' is missing"));
    }

    @Test
    public void registerGuestShouldReturn400WithErrorMessageWhenPasswordIsMissing() throws IOException {
        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, null, PLATFORM.name(), GAME_TYPE, AVATAR_URL, PARTNER_ID);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpServletResponse.SC_BAD_REQUEST), eq("parameter 'password' is missing"));
    }

    @Test
    public void registerGuestShouldReturn400WithErrorMessageWhenPlatformIsMissing() throws IOException {
        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, null, GAME_TYPE, AVATAR_URL, PARTNER_ID);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpServletResponse.SC_BAD_REQUEST), eq("parameter 'platform' is missing"));
    }

    @Test
    public void registerGuestShouldReturn400WithErrorMessageWhenPlatformIsInvalid() throws IOException {
        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, "invalid platform", GAME_TYPE, AVATAR_URL, PARTNER_ID);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpServletResponse.SC_BAD_REQUEST), eq("'invalid platform' is not a valid platform"));
    }

    @Test
    public void registerGuestShouldReturn400WithErrorMessageWhenGameTypeIsMissing() throws IOException {
        when(authenticationService.registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(), anyString(), any(Platform.class), anyString(), anyString())).thenReturn(new PlayerProfileRegistrationResponse());

        underTest.registerGuest(httpRequest, httpResponse, DISPLAY_NAME, EMAIL, PASSWORD, PLATFORM.name(), null, AVATAR_URL, PARTNER_ID);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpServletResponse.SC_BAD_REQUEST), eq("parameter 'gameType' is missing"));
    }

    private void givenGameIsAvailable() {
        when(gameAvailabilityService.getAvailabilityOfGameType(GAME_TYPE)).thenReturn(new GameAvailability(AVAILABLE));
    }

    private void givenLoginNewlyRegisteredUserWillReturnSession() {
        LobbySession lobbySession = new LobbySession(SESSION_ID, PLAYER_ID, DISPLAY_NAME, SESSION_KEY,
                Partner.YAZINO, "aPictureUrl", "anEmailAddress", null, true, PLATFORM, A_AUTH_PROVIDER);
        when(yazinoWebLoginService.loginNewlyRegisteredUser(httpRequest, httpResponse, EMAIL, PASSWORD, PLATFORM, Partner.YAZINO)).thenReturn(new YazinoWebLoginService.NewlyRegisteredUserLoginResult(LoginResult.NEW_USER, lobbySession));
    }

    private void givenAuthServiceWillReturnProfileForPlayer() {
        when(authenticationService
                .registerYazinoUser(anyString(), anyString(), any(PlayerProfile.class), anyString(),
                        anyString(), any(Platform.class), anyString(), anyString()))
                .thenReturn(new PlayerProfileRegistrationResponse(PLAYER_ID));
    }

    private String sessionReferenceWith(BigDecimal playerId, String sessionKey, Platform platform) {
        return new LobbySessionReference(new LobbySession(SESSION_ID, playerId, "aPlayerName", sessionKey, Partner.YAZINO,
                "aPictureUrl", "anEmail", null, false, platform, A_AUTH_PROVIDER)).encode();
    }

    private MobileLoginController.LoginInfo expectedSessionDetails() {
        MobileLoginController.LoginInfo expectedSessionDetails = new MobileLoginController.LoginInfo();
        expectedSessionDetails.setSuccess(true);
        expectedSessionDetails.setName(DISPLAY_NAME);
        expectedSessionDetails.setPlayerId(PLAYER_ID);
        expectedSessionDetails.setNewPlayer(NEW_PLAYER_FLAG);
        expectedSessionDetails.setAvailability(AVAILABLE);
        expectedSessionDetails.setSession(sessionReferenceWith(PLAYER_ID, SESSION_KEY, PLATFORM));
        return expectedSessionDetails;
    }

}
