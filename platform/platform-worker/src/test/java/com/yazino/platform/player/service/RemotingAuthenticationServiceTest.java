package com.yazino.platform.player.service;

import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RemotingAuthenticationServiceTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(200);
    private static final String REF = "ref";
    public static final String SLOTS = "SLOTS";

    @Mock
    private YazinoAuthenticationService yazinoAuthenticationService;
    @Mock
    private ExternalAuthenticationService externalAuthenticationService;

    private RemotingAuthenticationService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        underTest = new RemotingAuthenticationService(yazinoAuthenticationService, externalAuthenticationService);
    }

    @Test
    public void registrationDelegatesToTheYazinoAuthService() {
        final PlayerProfileRegistrationResponse expectedResult = new PlayerProfileRegistrationResponse(PLAYER_ID);
        when(yazinoAuthenticationService.register("anEmail", "aPassword", aPlayerProfile(), "aRemoteAddress", REF, Platform.FACEBOOK_CANVAS, "anAvatar", SLOTS))
                .thenReturn(expectedResult);

        final PlayerProfileRegistrationResponse actualResponse = underTest.registerYazinoUser(
                "anEmail", "aPassword", aPlayerProfile(), "aRemoteAddress", REF, Platform.FACEBOOK_CANVAS, "anAvatar", SLOTS);

        assertThat(actualResponse, is(equalTo(expectedResult)));
        verify(yazinoAuthenticationService).register("anEmail", "aPassword", aPlayerProfile(), "aRemoteAddress", REF, Platform.FACEBOOK_CANVAS, "anAvatar", SLOTS);
    }

    @Test
    public void loginYazinoUserDelegatesToTheYazinoAuthService() {
        final PlayerProfileLoginResponse expectedResult = new PlayerProfileLoginResponse(LoginResult.BLOCKED);
        when(yazinoAuthenticationService.login("anEmail", "aPassword")).thenReturn(expectedResult);

        final PlayerProfileLoginResponse actualResponse = underTest.loginYazinoUser(
                "anEmail", "aPassword");

        assertThat(actualResponse, is(equalTo(expectedResult)));
        verify(yazinoAuthenticationService).login("anEmail", "aPassword");
    }

    @Test
    public void authenticateYazinoUserDelegatesToTheYazinoAuthService() {
        final PlayerProfileAuthenticationResponse expectedResult = new PlayerProfileAuthenticationResponse(PLAYER_ID);
        when(yazinoAuthenticationService.authenticate("anEmail", "aPassword")).thenReturn(expectedResult);

        final PlayerProfileAuthenticationResponse actualResponse = underTest.authenticateYazinoUser(
                "anEmail", "aPassword");

        assertThat(actualResponse, is(equalTo(expectedResult)));
        verify(yazinoAuthenticationService).authenticate("anEmail", "aPassword");
    }

    @Test
    public void loginExternalUserDelegatesToTheExternalAuthService() {
        final PlayerProfileLoginResponse expectedResult = new PlayerProfileLoginResponse(LoginResult.BLOCKED);
        when(externalAuthenticationService.login("aRemoteAddress", Partner.YAZINO, aPlayerInformationHolder(), REF, Platform.FACEBOOK_CANVAS, SLOTS))
                .thenReturn(expectedResult);

        final PlayerProfileLoginResponse actualResponse = underTest.loginExternalUser(
                "aRemoteAddress", Partner.YAZINO, aPlayerInformationHolder(), REF, Platform.FACEBOOK_CANVAS, SLOTS);

        assertThat(actualResponse, is(equalTo(expectedResult)));
    }

    @Test
    public void authenticateExternalUserDelegatesToTheExternalAuthService() {
        final PlayerProfileAuthenticationResponse expectedResult = new PlayerProfileAuthenticationResponse(PLAYER_ID);
        when(externalAuthenticationService.authenticate("aProvider", "anExternalId")).thenReturn(expectedResult);

        final PlayerProfileAuthenticationResponse actualResponse = underTest.authenticateExternalUser(
                "aProvider", "anExternalId");

        assertThat(actualResponse, is(equalTo(expectedResult)));
        verify(externalAuthenticationService).authenticate("aProvider", "anExternalId");
    }

    @Test
    public void loginExternalUserShouldPassThroughGameTypeToExternalAuthenticationService() {
        final PlayerProfileLoginResponse expectedResult = new PlayerProfileLoginResponse(LoginResult.BLOCKED);
        when(externalAuthenticationService.login("aRemoteAddress", Partner.YAZINO, aPlayerInformationHolder(), REF, Platform.ANDROID, SLOTS))
                .thenReturn(expectedResult);

        final PlayerProfileLoginResponse actualResponse = underTest.loginExternalUser(
                "aRemoteAddress", Partner.YAZINO, aPlayerInformationHolder(), REF, Platform.ANDROID, SLOTS);

        assertThat(actualResponse, is(equalTo(expectedResult)));
    }

    private PlayerInformationHolder aPlayerInformationHolder() {
        return new PlayerInformationHolder();
    }

    private PlayerProfile aPlayerProfile() {
        return PlayerProfile.withPlayerId(PLAYER_ID)
                .withDisplayName("aDisplayName")
                .asProfile();
    }
}
