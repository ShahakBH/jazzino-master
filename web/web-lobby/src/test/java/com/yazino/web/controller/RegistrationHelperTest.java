package com.yazino.web.controller;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.player.GuestStatus;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileRegistrationResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.web.form.MobileRegistrationForm;
import com.yazino.web.service.YazinoWebLoginService;
import com.yazino.web.session.ReferrerSessionCache;
import com.yazino.web.util.CookieHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.HashMap;

import static com.yazino.web.service.YazinoWebLoginService.NewlyRegisteredUserLoginResult;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RegistrationHelperTest {
    public static final String SLOTS = "SLOTS";
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private HttpServletResponse httpResponse;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private YazinoWebLoginService yazinoWebLoginService;
    @Mock
    private ReferrerSessionCache referrerSessionCache;
    @Mock
    private RegistrationPerIpLimit registrationPerIpLimit;
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private RegistrationHelper underTest;

    private final MobileRegistrationForm form = populatedForm();
    private final PlayerProfileRegistrationResponse response = new PlayerProfileRegistrationResponse(BigDecimal.ONE);
    private final BindingResult bindingResult = new MapBindingResult(new HashMap(), "registration");

    @Before
    public void setup() {
        underTest = new RegistrationHelper(
                authenticationService, yazinoWebLoginService, new CookieHelper(), referrerSessionCache, registrationPerIpLimit);

        when(httpRequest.getRemoteAddr()).thenReturn("aRemoteAddress");
        when(authenticationService.registerYazinoUser(any(String.class), any(String.class),
                any(PlayerProfile.class), any(String.class), anyString(), any(Platform.class), any(String.class), anyString())).thenReturn(response);
        when(yazinoWebLoginService.login(any(HttpServletRequest.class), any(HttpServletResponse.class),
                any(String.class), any(String.class), eq(Platform.WEB), any(Partner.class))).thenReturn(null);
        when(yazinoWebLoginService.loginNewlyRegisteredUser(any(HttpServletRequest.class), any(HttpServletResponse.class),
                any(String.class), any(String.class), eq(Platform.WEB), eq(Partner.YAZINO))).thenReturn(new NewlyRegisteredUserLoginResult(null, null));
        when(yazinoConfiguration.getString("strata.lobby.partnerid", "YAZINO")).thenReturn(Partner.YAZINO.name());
    }

    @Test
    public void returnsFailureWhenValidationFails() throws Exception {
        form.setEmail(null);
        RegistrationResult registrationResult = underTest.register(form, bindingResult, httpRequest, httpResponse, Platform.WEB, SLOTS, Partner.YAZINO);
        assertEquals(RegistrationResult.FAILURE, registrationResult);
    }

    @Test
    public void returnsFailureWhenServiceCallFails() throws Exception {
        PlayerProfileRegistrationResponse response = new PlayerProfileRegistrationResponse(new ParameterisedMessage("Test"));
        when(authenticationService.registerYazinoUser(any(String.class), any(String.class), any(PlayerProfile.class),
                any(String.class), anyString(), any(Platform.class), any(String.class), anyString())).thenReturn(response);
        RegistrationResult registrationResult = underTest.register(form, bindingResult, httpRequest, httpResponse, Platform.WEB, SLOTS, Partner.YAZINO);
        assertEquals(RegistrationResult.FAILURE, registrationResult);
    }

    @Test
    public void ensureProfileHasEmailAddress() throws Exception {
        String email = "a@b.com";
        form.setEmail(email);

        PlayerProfile profile = processAndCaptureProfile();
        assertEquals(email, profile.getEmailAddress());
    }

    @Test
    public void profileShouldHaveNON_GUESTStatus() throws Exception {
        PlayerProfile profile = processAndCaptureProfile();
        assertThat(profile.getGuestStatus(), equalTo(GuestStatus.NON_GUEST));
    }


    private PlayerProfile processAndCaptureProfile() {
        underTest.register(form, bindingResult, httpRequest, httpResponse, Platform.WEB, SLOTS, Partner.YAZINO);

        ArgumentCaptor<PlayerProfile> userProfileCaptor = ArgumentCaptor.forClass(PlayerProfile.class);
        verify(authenticationService).registerYazinoUser(any(String.class), any(String.class), userProfileCaptor.capture(),
                any(String.class), anyString(), any(Platform.class), any(String.class), any(String.class));
        return userProfileCaptor.getValue();
    }


    @Test
    public void ensureEmailAddressIsPassedToRegistration() throws Exception {
        String email = "a@test.com";
        form.setEmail(email);

        underTest.register(form, bindingResult, httpRequest, httpResponse, Platform.WEB, SLOTS, Partner.YAZINO);

        verify(authenticationService).registerYazinoUser(eq(email), any(String.class), any(PlayerProfile.class),
                any(String.class), anyString(), any(Platform.class), any(String.class), anyString());
    }

    @Test
    public void registerShouldPassEmailOptInToRegistration() {
        underTest.register(form, bindingResult, httpRequest, httpResponse, Platform.WEB, SLOTS, Partner.YAZINO);

        verify(authenticationService).registerYazinoUser(any(String.class), any(String.class), any(PlayerProfile.class),
                any(String.class), anyString(), any(Platform.class), any(String.class), anyString());
    }

    @Test
    public void ensurePasswordIsPassedToRegistration() throws Exception {
        String password = "UK12345";
        form.setPassword(password);
        underTest.register(form, bindingResult, httpRequest, httpResponse, Platform.WEB, SLOTS, Partner.YAZINO);

        verify(authenticationService).registerYazinoUser(any(String.class), eq(password), any(PlayerProfile.class),
                any(String.class), anyString(), any(Platform.class), any(String.class), anyString());
    }

    @Test
    public void shouldStopRegistrationIfLimitForIpWasReached() {
        when(registrationPerIpLimit.hasReachedLimit(httpRequest)).thenReturn(true);
        final RegistrationResult registrationResult = underTest.register(form, bindingResult, httpRequest, httpResponse, Platform.WEB, null, Partner.YAZINO);
        assertEquals(RegistrationResult.FAILURE, registrationResult);
        assertEquals(1, bindingResult.getErrorCount());
        verifyZeroInteractions(authenticationService);
    }

    @Test
    public void shouldRecordNewRegistrationForLimitsPurpose() {
        underTest.register(form, bindingResult, httpRequest, httpResponse, Platform.WEB, SLOTS, Partner.YAZINO);
        verify(registrationPerIpLimit).recordRegistration(httpRequest);
    }

    private static MobileRegistrationForm populatedForm() {
        MobileRegistrationForm form = new MobileRegistrationForm();
        form.setTermsAndConditions(true);
        form.setDisplayName("Test");
        form.setEmail("a@b.com");
        form.setPassword("foobar");
        return form;
    }


}
