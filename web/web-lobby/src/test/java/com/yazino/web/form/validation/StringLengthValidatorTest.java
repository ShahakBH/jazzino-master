package com.yazino.web.form.validation;

import org.junit.Test;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests the {@link com.yazino.web.form.validation.StringLengthValidator} class.
 */
public class StringLengthValidatorTest {

	private final Errors errors = new MapBindingResult(new HashMap(), "");

	@Test
	public void onlySupportsStringClass() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(3, 5);
		assertFalse(validator.supports(Integer.class));
		assertFalse(validator.supports(Map.class));
		assertTrue(validator.supports(String.class));
	}

	@Test (expected = IllegalArgumentException.class)
	public void minimumLength_lessThanZero_throwsException() throws Exception {
        new StringLengthValidator(-1, 5);
	}
	
	@Test
	public void minimumLength_zero_null_passes() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(0, 5);
		validator.validate(null, errors);
		assertEquals(0, errors.getErrorCount());
	}
	
	@Test
	public void minimumLength_zero_emptyString_passes() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(0, 5);
		validator.validate("", errors);
		assertEquals(0, errors.getErrorCount());
	}
	
	@Test
	public void minimumLength_zero_blankString_passes() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(0, 5);
		validator.validate("    ", errors);
		assertEquals(0, errors.getErrorCount());
	}
	
	@Test
	public void minimumLength_zero_anyString_passes() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(0, 8);
		validator.validate("FooBar", errors);
		assertEquals(0, errors.getErrorCount());
	}
	
	@Test
	public void minimumLength_nonZero_null_fails() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(1, 5);
		validator.validate(null, errors);
		assertEquals(1, errors.getErrorCount());
	}
	
	@Test
	public void minimumLength_nonZero_emptyString_fails() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(1, 5);
		validator.validate("", errors);
		assertEquals(1, errors.getErrorCount());
	}

	@Test
	public void minimumLength_nonZero_blankString_fails() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(1, 5);
		validator.validate("    ", errors);
		assertEquals(1, errors.getErrorCount());
	}

	@Test
	public void minimumLength_nonZero_onLimitString_passes() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(1, 5);
		validator.validate("F", errors);
		assertEquals(0, errors.getErrorCount());
	}
	
	@Test
	public void minimumLength_nonZero_overLimitString_passes() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(1, 5);
		validator.validate("Foo", errors);
		assertEquals(0, errors.getErrorCount());
	}

	@Test (expected = IllegalArgumentException.class)
	public void maximumLength_lessThanZero_throwsException() throws Exception {
        new StringLengthValidator(1, -1);
	}

	@Test
	public void maximumLength_zero_null_passes() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(0, 0);
		validator.validate(null, errors);
		assertEquals(0, errors.getErrorCount());
	}
	
	@Test
	public void maximumLength_zero_emptyString_passes() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(0, 0);
		validator.validate("", errors);
		assertEquals(0, errors.getErrorCount());

	}
	
	@Test
	public void maximumLength_zero_blankString_passes() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(0, 0);
		validator.validate("    ", errors);
		assertEquals(0, errors.getErrorCount());
	}

	@Test
	public void maximumLength_zero_anyString_fails() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(0, 0);
		validator.validate("f", errors);
		assertEquals(1, errors.getErrorCount());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void maximumLength_lessThanMinimumLength_throwsException() throws Exception {
        new StringLengthValidator(5, 3);
	}
	
	@Test
	public void maximumLength_nonZero_stringInRange_passes() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(3, 5);
		validator.validate("foo", errors);
		assertEquals(0, errors.getErrorCount());
		validator.validate("fooo", errors);
		assertEquals(0, errors.getErrorCount());
		validator.validate("foooo", errors);
		assertEquals(0, errors.getErrorCount());
	}
	
	@Test
	public void maximumLength_nonZero_stringOutOfRange_fails() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(3, 5);
		validator.validate("fo", errors);
		assertEquals(1, errors.getErrorCount());
		validator.validate("fooo0oooo", errors);
		assertEquals(2, errors.getErrorCount());
	}

	@Test
	public void minimumLength_nonZero_stringHasBlanks_fails() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(3, 5);
		validator.validate("fo    ", errors);
		assertEquals(1, errors.getErrorCount());
	}

	@Test
	public void maximumLength_nonZero_stringHasBlanks_passes() throws Exception {
        StringLengthValidator validator = new StringLengthValidator(3, 100);
		validator.validate("fo00             ", errors);
		assertEquals(0, errors.getErrorCount());
	}
}
