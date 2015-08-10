package com.yazino.web.controller;

import com.yazino.email.EmailException;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.ResetPasswordResponse;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.email.EmailBuilder;
import com.yazino.web.domain.email.ResetPasswordEmailBuilder;
import com.yazino.web.form.ResetPasswordForm;
import com.yazino.web.service.QuietPlayerEmailer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ResetPasswordControllerTest {
    private static final String EMAIL = "email@email.com";
    private static final String NEW_PASSWORD = "theNewPassword";
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(101);
    private static final String FIRSTNAME = "firstname";

    private final PlayerProfileService playerProfileService = mock(PlayerProfileService.class);
    private final ResetPasswordForm form = mock(ResetPasswordForm.class);
    private final BindingResult bindingResult = mock(BindingResult.class);
    private final ModelMap modelMap = mock(ModelMap.class);
    private boolean partial = false;
    private final QuietPlayerEmailer emailer = mock(QuietPlayerEmailer.class);
    private final PlayerProfile playerProfile = mock(PlayerProfile.class);

    private final ResetPasswordController underTest = new ResetPasswordController(playerProfileService, emailer);

    @Before
    public void setUp() {
        when(form.getEmail()).thenReturn(EMAIL);
        when(playerProfileService.resetPassword(EMAIL)).thenReturn(new ResetPasswordResponse(
                PLAYER_ID, FIRSTNAME, NEW_PASSWORD));
        when(playerProfileService.findByPlayerId(PLAYER_ID)).thenReturn(playerProfile);
        when(playerProfile.getFirstName()).thenReturn("firstname");
        when(emailer.quietlySendEmail(any(EmailBuilder.class))).thenReturn(true);

    }

    @Test
    public void successfullyUsesUserServiceToResetUserPassword() {

        String goToView = underTest.processSubmit(form, bindingResult, modelMap, null, partial);

        assertEquals("partials/loginPanel", goToView);
        verify(playerProfileService).resetPassword(eq(EMAIL));
    }

    @Test
    public void failureInEmailShouldTellUserToGoToCustomerServices() throws EmailException {
        doThrow(new RuntimeException("EEK")).when(emailer).quietlySendEmail(any(EmailBuilder.class));
        assertThat(underTest.processSubmit(form, bindingResult, modelMap, null, partial), is(equalTo(ResetPasswordController.RESET_PASSWORD_VIEW)));
        verify(bindingResult).addError(new ObjectError(ResetPasswordController.RESET_PASSWORD_VIEW,"There was a problem resetting your password, please contact customer services"));
    }

    @Test
    public void partialIsAddedToTheModel() {
        partial = true;
        underTest.processSubmit(form, bindingResult, modelMap, null, partial);
        verify(modelMap).addAttribute(eq("partial"), eq(true));
    }

    @Test
    public void anEmailIsSentToThePlayerWhenThePasswordIsSuccessfullyReset() throws EmailException {
        underTest.processSubmit(form, bindingResult, modelMap, null, partial);

        ArgumentCaptor<ResetPasswordEmailBuilder> captor = ArgumentCaptor.forClass(ResetPasswordEmailBuilder.class);
        verify(emailer).quietlySendEmail(captor.capture());

        ResetPasswordEmailBuilder builder = captor.getValue();
        assertEquals(EMAIL, builder.getEmail());
        assertEquals(NEW_PASSWORD, builder.getPassword());
        assertEquals("firstname", builder.getName());

    }

    @Test
    public void failsToUseUserServiceToResetUserPasswordBecauseUserServiceFails() {
        reset(playerProfileService);
        when(playerProfileService.resetPassword(EMAIL)).thenReturn(new ResetPasswordResponse());

        String goToView = underTest.processSubmit(form, bindingResult, modelMap, null, partial);

        assertEquals("resetPassword", goToView);
    }
}
