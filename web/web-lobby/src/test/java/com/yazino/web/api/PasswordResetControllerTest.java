package com.yazino.web.api;

import com.yazino.platform.player.ResetPasswordResponse;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.web.domain.email.EmailBuilder;
import com.yazino.web.form.ResetPasswordForm;
import com.yazino.web.service.QuietPlayerEmailer;
import com.yazino.web.util.SpringErrorResponseFormatter;
import com.yazino.web.util.WebApiResponses;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PasswordResetControllerTest {
    @Mock
    private PlayerProfileService service;
    @Mock
    private QuietPlayerEmailer emailer;
    @Mock
    private WebApiResponses webApiResponses;
    @Mock
    private HttpServletResponse response;

    private final BindingResult bindingResult = new MapBindingResult(new HashMap<>(), "record");

    private PasswordResetController underTest;

    @Before
    public void setUp() {
        underTest = new PasswordResetController(service, emailer, webApiResponses, new SpringErrorResponseFormatter());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnSuccessMessageOnSuccess() throws Exception {
        when(service.resetPassword(anyString())).thenReturn(new ResetPasswordResponse(BigDecimal.TEN, "Matt", "foo"));
        when(emailer.quietlySendEmail(any(EmailBuilder.class))).thenReturn(true);

        underTest.resetPassword(response, toForm("a@b.com"), bindingResult);

        assertThat((List<Map<String, Object>>) successJsonFromResponse().get("globalMessages"),
                hasItems(message("success", "Your new password has been emailed to you.")));
    }

    @Test
    public void shouldSetResponseCodeTo400OnEmptyEmailAddress() throws Exception {
        underTest.resetPassword(response, toForm("   "), bindingResult);

        verify(webApiResponses).write(eq(response), eq(HttpServletResponse.SC_BAD_REQUEST), any(Map.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnFailureOnEmptyEmailAddress() throws Exception {
        underTest.resetPassword(response, toForm("   "), bindingResult);

        assertThat(fieldErrors("email"), hasItems(message("empty", "Value is required")));
    }

    @Test
    public void shouldReturnResultWithFieldErrorOnEmptyEmailAddress() throws Exception {
        underTest.resetPassword(response, toForm("   "), bindingResult);
        assertTrue(bindingResult.hasFieldErrors("email"));
        FieldError fieldError = bindingResult.getFieldError("email");
        assertEquals("empty", fieldError.getCode());
        assertEquals("Value is required", fieldError.getDefaultMessage());
    }

    @Test
    public void shouldSetResponseCodeTo400OnInvalidEmailAddress() throws Exception {
        underTest.resetPassword(response, toForm("adsdsa.com"), bindingResult);

        verify(webApiResponses).write(eq(response), eq(HttpServletResponse.SC_BAD_REQUEST), any(Map.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnFailureOnInvalidEmailAddress() throws Exception {
        underTest.resetPassword(response, toForm("adsdsa.com"), bindingResult);

        assertThat(fieldErrors("email"), hasItems(message("invalid", "Value is invalid")));
    }

    @Test
    public void shouldReturnResultWithFieldErrorOnInvalidEmailAddress() throws Exception {
        underTest.resetPassword(response, toForm("adsdsa.com"), bindingResult);
        assertTrue(bindingResult.hasFieldErrors("email"));
        FieldError fieldError = bindingResult.getFieldError("email");
        assertEquals("invalid", fieldError.getCode());
        assertEquals("Value is invalid", fieldError.getDefaultMessage());
    }

    @Test
    public void shouldSetResponseCodeTo500OnUnknownEmailAddress() throws Exception {
        when(service.resetPassword(anyString())).thenReturn(new ResetPasswordResponse());

        underTest.resetPassword(response, toForm("a@b.com"), bindingResult);

        verify(webApiResponses).write(eq(response), eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), any(Map.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnFailureOnUnknownEmailAddress() throws Exception {
        when(service.resetPassword(anyString())).thenReturn(new ResetPasswordResponse());

        underTest.resetPassword(response, toForm("a@b.com"), bindingResult);

        verify(webApiResponses).write(eq(response), eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), any(Map.class));
    }

    @Test
    public void shouldReturnResultWithGlobalErrorOnUnknownEmailAddress() throws Exception {
        when(service.resetPassword(anyString())).thenReturn(new ResetPasswordResponse());
        underTest.resetPassword(response, toForm("a@b.com"), bindingResult);

        assertTrue(bindingResult.hasGlobalErrors());
        ObjectError error = bindingResult.getGlobalError();
        assertEquals("failure", error.getCode());
        assertEquals("There was a problem resetting your password, please contact customer services", error.getDefaultMessage());
    }

    @Test
    public void shouldSetResponseCodeTo500OnEmailerFailure() throws Exception {
        when(service.resetPassword(anyString())).thenReturn(new ResetPasswordResponse(BigDecimal.TEN, "Matt", "foo"));
        when(emailer.quietlySendEmail(any(EmailBuilder.class))).thenReturn(false);

        underTest.resetPassword(response, toForm("a@b.com"), bindingResult);

        verify(webApiResponses).write(eq(response), eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), any(Map.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReturnFailureViewOnEmailerFailure() throws Exception {
        when(service.resetPassword(anyString())).thenReturn(new ResetPasswordResponse(BigDecimal.TEN, "Matt", "foo"));
        when(emailer.quietlySendEmail(any(EmailBuilder.class))).thenReturn(false);

        underTest.resetPassword(response, toForm("a@b.com"), bindingResult);

        assertThat((List<Map<String, Object>>) jsonFromResponse().get("globalMessages"),
                hasItems(message("failure", "There was a problem resetting your password, please contact customer services")));
    }

    @Test
    public void shouldReturnResultWithGlobalErrorOnEmailerFailure() throws Exception {
        when(service.resetPassword(anyString())).thenReturn(new ResetPasswordResponse(BigDecimal.TEN, "Matt", "foo"));
        when(emailer.quietlySendEmail(any(EmailBuilder.class))).thenReturn(false);
        underTest.resetPassword(response, toForm("a@b.com"), bindingResult);
        assertTrue(bindingResult.hasGlobalErrors());

        ObjectError globalError = bindingResult.getGlobalError();
        assertEquals("failure", globalError.getCode());
        assertEquals("There was a problem resetting your password, please contact customer services", globalError.getDefaultMessage());
    }

    private Map<String, Object> message(final String code,
                                        final String message) {
        final Map<String, Object> json = new HashMap<>();
        json.put("code", code);
        json.put("message", message);
        return json;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> jsonFromResponse() throws IOException {
        final ArgumentCaptor<Map> jsonCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).write(eq(response), anyInt(), jsonCaptor.capture());
        return jsonCaptor.getValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> successJsonFromResponse() throws IOException {
        final ArgumentCaptor<Map> jsonCaptor = ArgumentCaptor.forClass(Map.class);
        verify(webApiResponses).writeOk(eq(response), jsonCaptor.capture());
        return jsonCaptor.getValue();
    }

    private static ResetPasswordForm toForm(String email) {
        ResetPasswordForm form = new ResetPasswordForm();
        form.setEmail(email);
        return form;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fieldErrors(final String fieldName) throws IOException {
        final Map<String, List<Map<String, Object>>> fieldErrors = (Map<String, List<Map<String, Object>>>) jsonFromResponse().get("fieldErrors");
        return fieldErrors.get(fieldName);
    }

}
