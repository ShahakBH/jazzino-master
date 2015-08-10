package com.yazino.web.controller;

import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.platform.worker.message.VerificationType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VerificationControllerTest {

    public static final String EMAIL_ADDRESS = "an-email-address";
    public static final String VERIFICATION_IDENTIFIER = "a-verification-identifier";
    public static final String PLAYED = "p";
    public static final String NOT_PLAYED = "n";

    @Mock
    private PlayerProfileService playerProfileService;
    @Mock
    private HttpServletResponse response;

    private VerificationController underTest;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        underTest = new VerificationController(playerProfileService);
    }

    @Test(expected = NullPointerException.class)
    public void theControllerCannotBeCreatedWithANullPlayerProfileService() {
        new VerificationController(null);
    }

    @Test
    public void verificationShouldReturnBadRequestIfEmailAddressIsNull() throws IOException {
        final ModelAndView result = underTest.verifyPlayer(null, PLAYED, VERIFICATION_IDENTIFIER, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void verificationShouldReturnBadRequestIfPlayedStatusIsNull() throws IOException {
        final ModelAndView result = underTest.verifyPlayer(EMAIL_ADDRESS, null, VERIFICATION_IDENTIFIER, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void verificationShouldReturnBadRequestIfVerificationIdentifierIsNull() throws IOException {
        final ModelAndView result = underTest.verifyPlayer(EMAIL_ADDRESS, PLAYED, null, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void verificationShouldReturnBadRequestIfVerificationTypeIsInvalid() throws IOException {
        final ModelAndView result = underTest.verifyPlayer(EMAIL_ADDRESS, "non-existent-verification-type",
                VERIFICATION_IDENTIFIER, response);

        verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(result, is(nullValue()));
    }

    @Test
    public void verificationShouldInvokePlayerProfileService() throws IOException {
        underTest.verifyPlayer(EMAIL_ADDRESS, PLAYED, VERIFICATION_IDENTIFIER, response);

        verify(playerProfileService).verify(EMAIL_ADDRESS, VERIFICATION_IDENTIFIER, VerificationType.PLAYED);
    }

    @Test
    public void verificationControllerShouldSetSuccessFlagIntoModel() throws IOException {
        when(playerProfileService.verify(EMAIL_ADDRESS, VERIFICATION_IDENTIFIER, VerificationType.PLAYED))
                .thenReturn(true);
        final ModelAndView result = underTest.verifyPlayer(EMAIL_ADDRESS, PLAYED, VERIFICATION_IDENTIFIER, response);

        assertThat(result.getModel(), hasKey("verificationResult"));
    }

    @Test
    public void verificationSuccessShouldBeAddedToModel() throws IOException {
        when(playerProfileService.verify(EMAIL_ADDRESS, VERIFICATION_IDENTIFIER, VerificationType.PLAYED))
                .thenReturn(true);
        final ModelAndView result = underTest.verifyPlayer(EMAIL_ADDRESS, PLAYED, VERIFICATION_IDENTIFIER, response);

        assertThat(result.getModel(), hasKey("verificationResult"));
        assertThat((Boolean) result.getModel().get("verificationResult"), is(equalTo(true)));
    }

    @Test
    public void verificationFailureShouldBeAddedToModel() throws IOException {
        when(playerProfileService.verify(EMAIL_ADDRESS, VERIFICATION_IDENTIFIER, VerificationType.PLAYED))
                .thenReturn(false);
        final ModelAndView result = underTest.verifyPlayer(EMAIL_ADDRESS, PLAYED, VERIFICATION_IDENTIFIER, response);

        assertThat(result.getModel(), hasKey("verificationResult"));
        assertThat((Boolean) result.getModel().get("verificationResult"), is(equalTo(false)));
    }

    @Test
    public void verificationControllerShouldReturnVerificationView() throws IOException {
        final ModelAndView result = underTest.verifyPlayer(EMAIL_ADDRESS, PLAYED, VERIFICATION_IDENTIFIER, response);

        assertThat(result.getViewName(), is(equalTo("verification")));
    }

}
