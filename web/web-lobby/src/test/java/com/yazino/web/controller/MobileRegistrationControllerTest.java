package com.yazino.web.controller;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.platform.Platform;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileRegistrationResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.web.data.GameTypeRepository;
import com.yazino.web.form.MobileRegistrationForm;
import com.yazino.web.service.YazinoWebLoginService;
import com.yazino.web.session.PlatformReportingHelper;
import com.yazino.web.session.ReferrerSessionCache;
import com.yazino.web.util.CookieHelper;
import com.yazino.web.util.WebApiResponses;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

/**
 * Tests the {@link com.yazino.web.controller.MobileRegistrationController} class.
 */
@RunWith(MockitoJUnitRunner.class)
public class MobileRegistrationControllerTest {

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
    private GameTypeRepository gameTypeRepository;
    @Mock
    private YazinoConfiguration yazinoConfiguration;
    @Mock
    private WebApiResponses webApiResponses;

    private final MobileRegistrationForm form = populatedForm();
    private final PlayerProfileRegistrationResponse response = new PlayerProfileRegistrationResponse(BigDecimal.TEN);
    private final Map attributes = new HashMap();
    private final BindingResult bindingResult = new MapBindingResult(attributes, "registration");

    private MobileRegistrationController controller;

    @Before
    public void setup() {
        controller = new MobileRegistrationController(new RegistrationHelper(
                authenticationService, yazinoWebLoginService, new CookieHelper(), referrerSessionCache, registrationPerIpLimit), gameTypeRepository, webApiResponses);
        when(authenticationService.registerYazinoUser(any(String.class), any(String.class),
                any(PlayerProfile.class), any(String.class), anyString(), any(Platform.class), any(String.class))).thenReturn(response);
    }

    @Test
    public void shouldAllowProcessingOfFormWithoutErringOnNoAvatar() throws Exception {
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.yazino.com/public/mobileRegistration"));
        controller.processSubmit(form, bindingResult, httpRequest, httpResponse, "");
        assertFalse(bindingResult.hasErrors());
    }

    @Test
    public void mobileRegPageShouldReturnJsonBasedOnBindingResult() throws IOException {
        ModelMap model = Mockito.mock(ModelMap.class);
        controller.viewMobileRegistration(model, bindingResult, httpRequest, httpResponse);
        verify(webApiResponses).writeOk(httpResponse, singletonMap("registered", true));
    }

    @Test
    public void shouldReturnErrorCodeWhenAttemptingToPostWithInvalidPlatform() throws Exception {
        controller.register(form, bindingResult, httpRequest, httpResponse, "FOO", "SLOTS");
        verify(httpResponse).sendError(anyInt());
    }

    @Test
    public void shouldReturnErrorCodeWhenAttemptingToPostWithInvalidGame() throws Exception {
        controller.register(form, bindingResult, httpRequest, httpResponse, "IOS", "SLOTS2");
        verify(httpResponse).sendError(anyInt());
    }

    @Test
    public void shouldAddRequestAttributeWithCorrectStartPageURLForIOSWheelDeal() throws Exception {
        String userAgent = "Wheel Deal 3.0 rv:3.0-beta1 (iPhone; iPhone OS 5.1.1; en_GB)";
        when(httpRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(userAgent);
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.yazino.com/public/mobileRegistration"));
        controller.processSubmit(form, bindingResult, httpRequest, httpResponse, "IOS");
        verify(httpRequest).setAttribute(PlatformReportingHelper.REQUEST_URL, "http://www.yazino.com/public/registration/IOS/SLOTS");
    }

    @Test
    public void shouldAddRequestAttributeWithCorrectStartPageURLForIOSBlackjack() throws Exception {
        String userAgent = "Blackjack 2.0 rv:2.0-beta1 (iPhone; iPhone OS 5.1.1; en_GB)";
        when(httpRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(userAgent);
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.yazino.com/public/mobileRegistration"));
        controller.processSubmit(form, bindingResult, httpRequest, httpResponse, null);
        verify(httpRequest).setAttribute(PlatformReportingHelper.REQUEST_URL, "http://www.yazino.com/public/registration/IOS/BLACKJACK");
    }

    @Test
    public void shouldAddRequestAttributeWithCorrectStartPageURLForIOSHighStakes() throws Exception {
        String userAgent = "High Stakes 1.0 (beta-2) rv:1.0 (iPhone; iPhone OS 5.1.1; en_GB)";
        when(httpRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(userAgent);
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.yazino.com/public/mobileRegistration"));
        controller.processSubmit(form, bindingResult, httpRequest, httpResponse, "IOS");
        verify(httpRequest).setAttribute(PlatformReportingHelper.REQUEST_URL, "http://www.yazino.com/public/registration/IOS/HIGH_STAKES");
    }

    @Test
    public void shouldAddRequestAttributeWithCorrectStartPageURLForAndroidTexasHoldem() throws Exception {
        String userAgent = "Mozilla/5.0 (Android; U; en-GB) AppleWebKit/533.19.4 (KHTML, like Gecko) AdobeAIR/3.3";
        when(httpRequest.getHeader(HttpHeaders.USER_AGENT)).thenReturn(userAgent);
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://www.yazino.com/public/mobileRegistration"));
        controller.processSubmit(form, bindingResult, httpRequest, httpResponse, "ANDROID");
        verify(httpRequest).setAttribute(PlatformReportingHelper.REQUEST_URL, "http://www.yazino.com/public/registration/ANDROID/TEXAS_HOLDEM");
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
