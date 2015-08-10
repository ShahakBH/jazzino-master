package com.yazino.web.api;

import org.junit.Test;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.junit.Assert.*;

public class FindPlayersFormValidatorTest {

    private final Errors errors = new MapBindingResult(new HashMap<Object, Object>(), "players");
    private final FindPlayersFormValidator validator = new FindPlayersFormValidator();

    @Test
    public void shouldOnlySupportFindPlayersForm() throws Exception {
        assertFalse(validator.supports(BigDecimal.class));
        assertFalse(validator.supports(Integer.class));
        assertTrue(validator.supports(FindPlayersForm.class));
    }

    @Test
    public void shouldRejectFormIfProviderIsUnknown() throws Exception {
        FindPlayersForm form = new FindPlayersForm();
        form.setProvider("unknown");
        form.setProviderIds("2,3,4");
        validator.validate(form, errors);
        assertTrue(errors.hasFieldErrors("provider"));
        assertEquals("unsupported", errors.getFieldError("provider").getCode());
    }

    @Test
    public void shouldRejectFormIfProviderIsKnownButEmptyIds() throws Exception {
        FindPlayersForm form = new FindPlayersForm();
        form.setProvider("unknown");
        validator.validate(form, errors);
        assertTrue(errors.hasFieldErrors("providerIds"));
        assertEquals("empty", errors.getFieldError("providerIds").getCode());
    }

    @Test
    public void shouldntCareAboutProviderCase() throws Exception {
        FindPlayersForm form = new FindPlayersForm();
        form.setProvider("YAZINO");
        form.setProviderIds("2,3,4");
        validator.validate(form, errors);
        assertFalse(errors.hasErrors());
    }

    @Test
    public void shouldAllowFormIfProviderIsKnownAndIdsExist() throws Exception {
        FindPlayersForm form = new FindPlayersForm();
        form.setProvider("yazino");
        form.setProviderIds("2,3,4");
        validator.validate(form, errors);
        assertFalse(errors.hasErrors());
    }
}
