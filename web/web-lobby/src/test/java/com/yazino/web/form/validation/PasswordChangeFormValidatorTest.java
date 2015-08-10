package com.yazino.web.form.validation;

import com.yazino.platform.player.PasswordChangeRequest;
import com.yazino.platform.player.PlayerProfileAuthenticationResponse;
import com.yazino.platform.player.service.AuthenticationService;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.form.PasswordChangeFormTestBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.validation.Validator;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PasswordChangeFormValidatorTest extends AbstractValidatorTest {

    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(1);

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private PlayerProfileService playerProfileService;

    private PasswordChangeFormValidator underTest;

    @Before
    public void initTest() {
        MockitoAnnotations.initMocks(this);

        underTest = new PasswordChangeFormValidator(authenticationService, playerProfileService);
    }

    @Override
    protected Validator getUnderTest() {
        return underTest;
    }

    @Override
    protected Class getSupportedClass() {
        return PasswordChangeRequest.class;
    }

    @Test
    public void shouldErrorIfCurrentPasswordBlank() {
        PasswordChangeRequest passwordChangeForm = new PasswordChangeFormTestBuilder().withCurrentPassword(null).build();
        setupYazinoLogin(passwordChangeForm);
        assertErrorCodeEmpty("currentPassword", passwordChangeForm);
    }

    @Test
    public void shouldErrorIfNewPasswordDoesNotMatch() {
        PasswordChangeRequest passwordChangeForm = new PasswordChangeFormTestBuilder().withConfirmNewPassword("A").build();
        setupYazinoLogin(passwordChangeForm);
        assertErrorCodeValuesDonNotMatch("newPassword", passwordChangeForm);
    }

    @Test
    public void shouldErrorIfNewPasswordBlank() {
        PasswordChangeRequest passwordChangeForm = new PasswordChangeFormTestBuilder().withNewPassword("   ").build();
        setupYazinoLogin(passwordChangeForm);
        assertErrorCodeEmpty("newPassword", passwordChangeForm);
    }

    @Test
    public void shouldErrorIfNewPasswordTooLong() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("5318008");
        }
        PasswordChangeRequest passwordChangeForm = new PasswordChangeFormTestBuilder().withNewPassword(sb.toString()).build();
        setupYazinoLogin(passwordChangeForm);

        getUnderTest().validate(passwordChangeForm, errors);

        verify(errors).rejectValue("newPassword", ValidationTools.ERROR_CODE_LENGTH, "Must be between 5 and 20 characters");
    }

    @Test
    public void shouldErrorIfConfirmNewPasswordBlank() {
        PasswordChangeRequest passwordChangeForm = new PasswordChangeFormTestBuilder().withConfirmNewPassword("   ").build();
        setupYazinoLogin(passwordChangeForm);
        assertErrorCodeValuesDonNotMatch("newPassword", passwordChangeForm);
    }

    private void setupYazinoLogin(PasswordChangeRequest passwordChangeForm) {
        when(authenticationService.authenticateYazinoUser("anEmail", passwordChangeForm.getCurrentPassword()))
                .thenReturn(new PlayerProfileAuthenticationResponse(PLAYER_ID));
        when(playerProfileService.findLoginEmailByPlayerId(PLAYER_ID)).thenReturn("anEmail");
    }

    @Test
    public void shouldNotErrorIfCurrentPasswordMatch() {
        PasswordChangeRequest passwordChangeForm = new PasswordChangeFormTestBuilder().withConfirmNewPassword(PasswordChangeFormTestBuilder.NEW_PASSWORD).build();
        setupYazinoLogin(passwordChangeForm);
        assertNoErrors(passwordChangeForm);

        verify(authenticationService).authenticateYazinoUser("anEmail", "currentPassword");
    }

    @Test
    public void shouldErrorIfCurrentPasswordDoesNotMatch() {
        final PasswordChangeRequest passwordChangeForm = new PasswordChangeFormTestBuilder().withConfirmNewPassword(PasswordChangeFormTestBuilder.NEW_PASSWORD).build();
        passwordChangeForm.setCurrentPassword("incorrectPassword");
        when(authenticationService.authenticateYazinoUser("anEmail", passwordChangeForm.getCurrentPassword()))
                .thenReturn(new PlayerProfileAuthenticationResponse());
        when(playerProfileService.findLoginEmailByPlayerId(PLAYER_ID)).thenReturn("anEmail");

        getUnderTest().validate(passwordChangeForm, errors);

        verify(errors).rejectValue("currentPassword", ValidationTools.ERROR_CODE_NON_MATCHING, "incorrect password");
    }
}
