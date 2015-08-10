package com.yazino.web.api;

import com.yazino.platform.table.GameTypeInformation;
import com.yazino.web.data.GameTypeRepository;
import org.junit.Before;
import org.junit.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import com.yazino.game.api.GameType;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InvitationSentRecordValidatorTest {

    private final GameTypeRepository gameTypeRepository = mock(GameTypeRepository.class);
    private final InvitationSentRecordValidator validator = new InvitationSentRecordValidator(gameTypeRepository);

    @Before
    public void setup() {
        Map<String, GameTypeInformation> gameTypes = new HashMap<String, GameTypeInformation>();
        gameTypes.put("SLOTS", new GameTypeInformation(new GameType("SLOTS", "SLOTS", Collections.<String>emptySet()), true));
        when(gameTypeRepository.getGameTypes()).thenReturn(gameTypes);
    }

    @Test
    public void shouldSupportInvitationRequestClass() throws Exception {
        assertTrue(validator.supports(InvitationSentRecord.class));
    }

    @Test
    public void shouldNotSupportOnAnyOtherClass() throws Exception {
        assertFalse(validator.supports(BigDecimal.class));
        assertFalse(validator.supports(Object.class));
    }

    @Test
    public void shouldAddErrorWhenNullGame() throws Exception {
        InvitationSentRecord record = InvitationSentRecord.toInvitationSentRecord(null, "IOS", "a@b.com");
        Errors errors = new BeanPropertyBindingResult(record, "record");
        validator.validate(record, errors);
        FieldError fieldError = errors.getFieldError("gameType");
        assertEquals("gameType must be present", fieldError.getDefaultMessage());
        assertEquals("empty", fieldError.getCode());
    }

    @Test
    public void shouldAddErrorWhenEmptyGame() throws Exception {
        InvitationSentRecord record = InvitationSentRecord.toInvitationSentRecord("  ", "IOS", "a@b.com");
        Errors errors = new BeanPropertyBindingResult(record, "record");

        validator.validate(record, errors);
        FieldError fieldError = errors.getFieldError("gameType");
        assertEquals("gameType must be present", fieldError.getDefaultMessage());
        assertEquals("empty", fieldError.getCode());
    }

    @Test
    public void shouldAddErrorWhenUnknownGame() throws Exception {
        InvitationSentRecord record = InvitationSentRecord.toInvitationSentRecord("BLACKJACK", "IOS", "a@b.com");
        Errors errors = new BeanPropertyBindingResult(record, "record");
        validator.validate(record, errors);
        FieldError fieldError = errors.getFieldError("gameType");
        assertEquals("gameType is not supported", fieldError.getDefaultMessage());
        assertEquals("unsupported", fieldError.getCode());
    }

    @Test
    public void shouldAddErrorWhenNullPlatform() throws Exception {
        InvitationSentRecord record = InvitationSentRecord.toInvitationSentRecord("SLOTS", null, "a@b.com");
        Errors errors = new BeanPropertyBindingResult(record, "record");
        validator.validate(record, errors);
        FieldError fieldError = errors.getFieldError("platform");
        assertEquals("platform must be present", fieldError.getDefaultMessage());
        assertEquals("empty", fieldError.getCode());
    }

    @Test
    public void shouldAddErrorWhenEmptyPlatform() throws Exception {
        InvitationSentRecord record = InvitationSentRecord.toInvitationSentRecord("SLOTS", "   ", "a@b.com");
        Errors errors = new BeanPropertyBindingResult(record, "record");
        validator.validate(record, errors);
        FieldError fieldError = errors.getFieldError("platform");
        assertEquals("platform must be present", fieldError.getDefaultMessage());
        assertEquals("empty", fieldError.getCode());
    }

    @Test
    public void shouldAddErrorWhenInvalidPlatform() throws Exception {
        InvitationSentRecord record = InvitationSentRecord.toInvitationSentRecord("SLOTS", "PLAYSTATION", "a@b.com");
        Errors errors = new BeanPropertyBindingResult(record, "record");
        validator.validate(record, errors);
        FieldError fieldError = errors.getFieldError("platform");
        assertEquals("platform is not supported", fieldError.getDefaultMessage());
        assertEquals("unsupported", fieldError.getCode());
    }

    @Test
    public void shouldAddErrorWhenNullSourceIds() throws Exception {
        InvitationSentRecord record = InvitationSentRecord.toInvitationSentRecord("SLOTS", "IOS", null);
        Errors errors = new BeanPropertyBindingResult(record, "record");
        validator.validate(record, errors);
        FieldError fieldError = errors.getFieldError("sourceIds");
        assertEquals("sourceIds must be present", fieldError.getDefaultMessage());
        assertEquals("empty", fieldError.getCode());
    }

    @Test
    public void shouldAddErrorWhenEmptySourceIds() throws Exception {
        InvitationSentRecord record = InvitationSentRecord.toInvitationSentRecord("SLOTS", "IOS", "");
        Errors errors = new BeanPropertyBindingResult(record, "record");
        validator.validate(record, errors);
        FieldError fieldError = errors.getFieldError("sourceIds");
        assertEquals("sourceIds must be present", fieldError.getDefaultMessage());
        assertEquals("empty", fieldError.getCode());
    }

    @Test
    public void shouldNotHaveAnyErrorsWhenAllFieldsCorrectAndPresent() throws Exception {
        InvitationSentRecord record = InvitationSentRecord.toInvitationSentRecord("SLOTS", "IOS", "a@b.com,b@c.com");
        Errors errors = new BeanPropertyBindingResult(record, "record");
        validator.validate(record, errors);
        assertEquals(0, errors.getErrorCount());
    }

    @Test
    public void shouldAddErrorWhenSourceIDIsBlank() {
        InvitationSentRecord record = InvitationSentRecord.toInvitationSentRecord("SLOTS", "IOS", ",1002222223");
        Errors errors = new BeanPropertyBindingResult(record, "record");
        validator.validate(record, errors);
        FieldError fieldError = errors.getFieldError("sourceIds");
        assertEquals("sourceIds cannot be blank", fieldError.getDefaultMessage());
        assertEquals("nullOrBlankSourceId", fieldError.getCode());
    }
}
