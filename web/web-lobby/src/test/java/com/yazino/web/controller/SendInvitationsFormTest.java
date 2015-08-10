package com.yazino.web.controller;

import com.yazino.web.form.SendInvitationsForm;
import org.junit.Test;
import org.springframework.validation.ObjectError;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SendInvitationsFormTest {

	@Test
	public void testValidation() {
		SendInvitationsForm form = new SendInvitationsForm();
		List<ObjectError> actual = form.validate("anyName");
		assertEquals(1, actual.size());
		form.setSentTo("example@example.com");
		actual = form.validate("anyName");
		assertEquals(0, actual.size());
		form.setSentTo("example at example.com");
		actual = form.validate("anyName");
		assertEquals(1, actual.size());
	}
}
