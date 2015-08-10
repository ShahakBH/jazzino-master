package com.yazino.web.controller.guestplay;

import com.google.common.collect.Sets;
import com.yazino.configuration.YazinoConfiguration;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.PlayerInformationHolder;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileServiceResponse;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.controller.TangoPlayerInformationProvider;
import com.yazino.web.domain.facebook.FacebookUserInformationProvider;
import com.yazino.web.session.LobbySession;
import com.yazino.web.session.LobbySessionCache;
import com.yazino.web.util.WebApiResponses;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;

import static com.yazino.web.controller.guestplay.BusinessError.ERROR_CODE_YAZINO_USER_ALREADY_HAS_AN_ACCOUNT;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CANVAS;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.LOOSE;

public class GuestConversionControllerTest {
    private static final String NEW_PASSWORD = "password";
    private static final String NEW_EMAIL_ADDRESS = "notGuest@eample.com";
    private static final String NEW_DISPLAY_NAME = "display name";
    private static final BigDecimal PLAYER_ID = BigDecimal.TEN;
    private static final String GAME_TYPE = "game-type";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String FACEBOOK_ID = "facebook-id";
    private static final String REMOTE_IP_ADDRESS = "1.2.3.4";
    private static final String TANGO_PARTNER_ID = "TANGO";
    private static final String ENCRYPTED_DATA = "scrambledeggs";
    private final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    private final HttpServletResponse httpResponse = mock(HttpServletResponse.class);
    private final PrintWriter printWriter = mock(PrintWriter.class);
    private LobbySessionCache lobbySessionCache = mock(LobbySessionCache.class);
    private PlayerProfileService playerProfileService = mock(PlayerProfileService.class);
    private WebApiResponses responseWriter = mock(WebApiResponses.class);
    private FacebookUserInformationProvider facebookUserInformationProvider = mock(FacebookUserInformationProvider.class);
    private FacebookConfiguration facebookConfiguration = mock(FacebookConfiguration.class);
    private GuestConversionController underTest;
    private TangoPlayerInformationProvider tangoPlayerInformationProvider = mock(TangoPlayerInformationProvider.class);
    private YazinoConfiguration yazinoConfiguration = mock(YazinoConfiguration.class);

    @Before
    public void setUp() {
        underTest = new GuestConversionController(lobbySessionCache,
                playerProfileService,
                facebookUserInformationProvider,
                facebookConfiguration,
                responseWriter,
                tangoPlayerInformationProvider, yazinoConfiguration);
        when(facebookConfiguration.getAppConfigFor(GAME_TYPE, CANVAS, LOOSE)).thenReturn(new FacebookAppConfiguration());
        when(httpRequest.getRemoteAddr()).thenReturn(REMOTE_IP_ADDRESS);
    }

    @Test
    public void convertGuestToYazinoAccountShouldDoSo() throws IOException {
        givenActiveSession();
        when(playerProfileService.convertGuestToYazinoAccount(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD, NEW_DISPLAY_NAME)).thenReturn(new PlayerProfileServiceResponse(true));

        underTest.convertGuestToYazinoAccount(httpRequest, httpResponse, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, NEW_PASSWORD);

        verify(playerProfileService).convertGuestToYazinoAccount(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD, NEW_DISPLAY_NAME);
        verify(responseWriter).writeOk(same(httpResponse),
                eq(GuestConversionController.ConversionResponse.createSuccessfulConversionResponse(NEW_DISPLAY_NAME)));
    }

    @Test
    public void convertGuestToYazinoAccountShouldReturn400WithErrorMessageWhenDisplayNameIsMissing() throws IOException {
        underTest.convertGuestToYazinoAccount(httpRequest, httpResponse, null, NEW_EMAIL_ADDRESS, NEW_PASSWORD);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpStatus.SC_BAD_REQUEST), eq("parameter 'displayName' is missing"));
    }

    @Test
    public void convertGuestToYazinoAccountShouldReturn400WithErrorMessageWhenEmailAddressIsMissing() throws IOException {
        underTest.convertGuestToYazinoAccount(httpRequest, httpResponse, NEW_DISPLAY_NAME, null, NEW_PASSWORD);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpStatus.SC_BAD_REQUEST), eq("parameter 'emailAddress' is missing"));
    }

    @Test
    public void convertGuestToYazinoAccountShouldReturn400WithErrorMessageWhenPasswordIsMissing() throws IOException {
        underTest.convertGuestToYazinoAccount(httpRequest, httpResponse, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, null);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpStatus.SC_BAD_REQUEST), eq("parameter 'password' is missing"));
    }

    @Test
    public void convertGuestToYazinoAccountShouldReturn403WhenNoSession() throws IOException {
        underTest.convertGuestToYazinoAccount(httpRequest, httpResponse, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, NEW_PASSWORD);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpStatus.SC_FORBIDDEN), eq("no session"));
    }

    @Test
    public void convertGuestToYazinoAccountShouldFailIfYazinoUserAlreadyRegisteredGivenEmailAddress() throws IOException {
        givenActiveSession();

        Set<ParameterisedMessage> errors = new HashSet<>();
        errors.add(new ParameterisedMessage("Cannot convert to Yazino account. E-mail already registered"));
        PlayerProfileServiceResponse profileServiceResponse = new PlayerProfileServiceResponse(errors, false);
        when(playerProfileService.convertGuestToYazinoAccount(PLAYER_ID, NEW_EMAIL_ADDRESS, NEW_PASSWORD, NEW_DISPLAY_NAME)).thenReturn(profileServiceResponse);

        underTest.convertGuestToYazinoAccount(httpRequest, httpResponse, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS, NEW_PASSWORD);

        HashSet<BusinessError> businessErrors = Sets.newHashSet(new BusinessError(ERROR_CODE_YAZINO_USER_ALREADY_HAS_AN_ACCOUNT, "Cannot convert to Yazino account. E-mail already registered"));
        GuestConversionController.ConversionResponse response = new GuestConversionController.ConversionResponse(businessErrors);
        verify(responseWriter).writeOk(same(httpResponse), eq(response));
    }

    @Test
    public void convertGuestToFacebookAccountShouldDelegateToPlayerProfileService() throws IOException {
        PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setExternalId(FACEBOOK_ID);
        playerProfile.setDisplayName(NEW_DISPLAY_NAME);
        playerProfile.setEmailAddress(NEW_EMAIL_ADDRESS);
        PlayerInformationHolder playerInformationHolder = new PlayerInformationHolder();
        playerInformationHolder.setPlayerProfile(playerProfile);
        when(facebookUserInformationProvider.getUserInformationHolder(eq(ACCESS_TOKEN), anyString(), eq(REMOTE_IP_ADDRESS), anyBoolean())).thenReturn(playerInformationHolder);
        givenActiveSession();
        when(playerProfileService.convertGuestToFacebookAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS)).thenReturn(new PlayerProfileServiceResponse(true));

        underTest.convertGuestToFacebookAccount(httpRequest, httpResponse, GAME_TYPE, ACCESS_TOKEN);

        verify(playerProfileService).convertGuestToFacebookAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS);
        verify(responseWriter).writeOk(same(httpResponse), eq(GuestConversionController.ConversionResponse.createSuccessfulConversionResponse(NEW_DISPLAY_NAME)));
    }

    @Test
    public void convertGuestToFacebookAccountShouldReturnBadRequestIfPlayerProfileServiceReturnsNullInformationHolder() throws IOException {
        when(facebookUserInformationProvider.getUserInformationHolder(eq(ACCESS_TOKEN), anyString(), eq(REMOTE_IP_ADDRESS), anyBoolean())).thenReturn(null);
        givenActiveSession();
        when(playerProfileService.convertGuestToFacebookAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS)).thenReturn(new PlayerProfileServiceResponse(true));

        underTest.convertGuestToFacebookAccount(httpRequest, httpResponse, GAME_TYPE, ACCESS_TOKEN);

        verify(responseWriter).writeError(same(httpResponse), eq(SC_BAD_REQUEST), eq("unable to load user with access-token"));
    }

    @Test
    public void convertGuestToFacebookAccountShouldReturnBadRequestIfPlayerProfileServiceReturnsNullProfile() throws IOException {
        PlayerInformationHolder playerInformationHolder = new PlayerInformationHolder();
        playerInformationHolder.setPlayerProfile(null);
        when(facebookUserInformationProvider.getUserInformationHolder(eq(ACCESS_TOKEN), anyString(), eq(REMOTE_IP_ADDRESS), anyBoolean())).thenReturn(playerInformationHolder);
        givenActiveSession();
        when(playerProfileService.convertGuestToFacebookAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS)).thenReturn(new PlayerProfileServiceResponse(true));

        underTest.convertGuestToFacebookAccount(httpRequest, httpResponse, GAME_TYPE, ACCESS_TOKEN);

        verify(responseWriter).writeError(same(httpResponse), eq(SC_BAD_REQUEST), eq("unable to load user with access-token"));
    }

    @Test
    @Ignore
    public void convertGuestToFacebookAccountShouldFailIfFacebookUserAlreadyAssociatedWithAccount() throws IOException {
        PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setExternalId(FACEBOOK_ID);
        playerProfile.setDisplayName(NEW_DISPLAY_NAME);
        playerProfile.setEmailAddress(NEW_EMAIL_ADDRESS);
        PlayerInformationHolder playerInformationHolder = new PlayerInformationHolder();
        playerInformationHolder.setPlayerProfile(playerProfile);
        when(facebookUserInformationProvider.getUserInformationHolder(eq(ACCESS_TOKEN), anyString(), eq(REMOTE_IP_ADDRESS), anyBoolean())).thenReturn(playerInformationHolder);
        givenActiveSession();
        when(playerProfileService.convertGuestToFacebookAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS)).thenReturn(new PlayerProfileServiceResponse(true));

        underTest.convertGuestToFacebookAccount(httpRequest, httpResponse, GAME_TYPE, ACCESS_TOKEN);

        verify(playerProfileService).convertGuestToFacebookAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS);
        verify(responseWriter).writeOk(same(httpResponse), eq(GuestConversionController.ConversionResponse.createSuccessfulConversionResponse(NEW_DISPLAY_NAME)));
    }

    private void givenActiveSession() {
        when(lobbySessionCache.getActiveSession(httpRequest)).thenReturn(new LobbySession(null, PLAYER_ID, null, null, null, null, null, null, true, Platform.ANDROID, AuthProvider.YAZINO));
    }

    @Test
    public void convertGuestToFacebookAccountShouldReturn400WithErrorMessageWhenGameTypeIsMissing() throws IOException {
        underTest.convertGuestToFacebookAccount(httpRequest, httpResponse, null, ACCESS_TOKEN);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpStatus.SC_BAD_REQUEST), eq("parameter 'gameType' is missing"));
    }

    @Test
    public void convertGuestToFacebookAccountShouldReturn400WithErrorMessageWhenAccessTokenIsMissing() throws IOException {
        underTest.convertGuestToFacebookAccount(httpRequest, httpResponse, GAME_TYPE, null);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpStatus.SC_BAD_REQUEST), eq("parameter 'accessToken' is missing"));
    }

    @Test
    public void convertGuestToFacebookAccountShouldReturn400WithErrorMessageWhenFacebookLookupFails() throws IOException {
        givenActiveSession();
        when(facebookUserInformationProvider.getUserInformationHolder(eq(ACCESS_TOKEN), anyString(), anyString(), anyBoolean())).thenReturn(null);
        underTest.convertGuestToFacebookAccount(httpRequest, httpResponse, GAME_TYPE, ACCESS_TOKEN);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpStatus.SC_BAD_REQUEST), eq("unable to load user with access-token"));
    }

    @Test
    public void convertTangoGuestToExternalAccountShouldDelegateToPlayerProfileService() throws IOException, GeneralSecurityException {
        PlayerProfile playerProfile = new PlayerProfile();
        playerProfile.setExternalId(FACEBOOK_ID);
        playerProfile.setDisplayName(NEW_DISPLAY_NAME);
        playerProfile.setEmailAddress(NEW_EMAIL_ADDRESS);
        playerProfile.setPartnerId(Partner.TANGO);
        PlayerInformationHolder playerInformationHolder = new PlayerInformationHolder();
        playerInformationHolder.setPlayerProfile(playerProfile);
        when(tangoPlayerInformationProvider.getPlayerInformationFromEncryptedData(ENCRYPTED_DATA)).thenReturn(playerInformationHolder);
        givenActiveSession();
        when(playerProfileService.convertGuestToExternalAccount(PLAYER_ID,
                FACEBOOK_ID,
                NEW_DISPLAY_NAME,
                NEW_EMAIL_ADDRESS,
                TANGO_PARTNER_ID)).thenReturn(new PlayerProfileServiceResponse(true));

        underTest.convertGuestToTangoAccount(httpRequest, httpResponse, GAME_TYPE, ENCRYPTED_DATA);

        verify(playerProfileService).convertGuestToExternalAccount(PLAYER_ID,
                FACEBOOK_ID,
                NEW_DISPLAY_NAME,
                NEW_EMAIL_ADDRESS,
                TANGO_PARTNER_ID);

        verify(responseWriter).writeOk(same(httpResponse),
                eq(GuestConversionController.ConversionResponse.createSuccessfulConversionResponse(NEW_DISPLAY_NAME)));
    }

    @Test
    public void convertGuestToTangoAccountShouldReturn400WithErrorMessageWhenGameTypeIsMissing() throws IOException {
        underTest.convertGuestToTangoAccount(httpRequest, httpResponse, null, ENCRYPTED_DATA);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpStatus.SC_BAD_REQUEST), eq("parameter 'gameType' is missing"));
    }

    @Test
    public void convertGuestToTangoAccountShouldReturn400WithErrorMessageWhenencryptedDataIsMissing() throws IOException {
        underTest.convertGuestToTangoAccount(httpRequest, httpResponse, GAME_TYPE, null);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpStatus.SC_BAD_REQUEST), eq("parameter 'encryptedData' is missing"));
    }

    @Test
    public void convertGuestToTangoAccountShouldReturn400WithErrorMessageWhenFacebookLookupFails() throws IOException, GeneralSecurityException {
        givenActiveSession();
        when(tangoPlayerInformationProvider.getPlayerInformationFromEncryptedData(ENCRYPTED_DATA)).thenReturn(null);
        underTest.convertGuestToTangoAccount(httpRequest, httpResponse, GAME_TYPE, ENCRYPTED_DATA);

        verify(responseWriter).writeError(same(httpResponse), eq(HttpStatus.SC_BAD_REQUEST), eq("unable to load user with access-token"));
    }

    @Test
    public void convertGuestToTangoAccountShouldReturnBadRequestIfPlayerProfileServiceReturnsNullInformationHolder() throws IOException, GeneralSecurityException {
        when(tangoPlayerInformationProvider.getPlayerInformationFromEncryptedData(ENCRYPTED_DATA)).thenReturn(null);
        givenActiveSession();
        when(playerProfileService.convertGuestToFacebookAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS)).thenReturn(new PlayerProfileServiceResponse(true));

        underTest.convertGuestToTangoAccount(httpRequest, httpResponse, GAME_TYPE, ENCRYPTED_DATA);

        verify(responseWriter).writeError(same(httpResponse), eq(SC_BAD_REQUEST), eq("unable to load user with access-token"));
    }

    @Test
    public void convertGuestToTangoAccountShouldReturnBadRequestIfPlayerProfileServiceReturnsNullProfile() throws IOException, GeneralSecurityException {
        PlayerInformationHolder playerInformationHolder = new PlayerInformationHolder();
        playerInformationHolder.setPlayerProfile(null);
        when(tangoPlayerInformationProvider.getPlayerInformationFromEncryptedData(ENCRYPTED_DATA)).thenReturn(playerInformationHolder);
        givenActiveSession();
        when(playerProfileService.convertGuestToFacebookAccount(PLAYER_ID, FACEBOOK_ID, NEW_DISPLAY_NAME, NEW_EMAIL_ADDRESS)).thenReturn(new PlayerProfileServiceResponse(true));

        underTest.convertGuestToTangoAccount(httpRequest, httpResponse, GAME_TYPE, ENCRYPTED_DATA);

        verify(responseWriter).writeError(same(httpResponse), eq(SC_BAD_REQUEST), eq("unable to load user with access-token"));
    }


    @Ignore
    @Test
    public void addTestsForBusinessErrorHandlingAfterRefactoringYazinoPlayerValidator() {

    }
}
