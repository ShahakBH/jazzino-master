package com.yazino.web.payment.creditcard;

import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CreditCardFormTest {
	private Map<String, String> usStates;
	private Map<String, String> canadianStates;
	private List<String> results = new ArrayList<String>();
	private CreditCardForm underTest = CreditCardFormBuilder.valueOf().build();

    @Before
	public void setup() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
		usStates = new HashMap<>();
		usStates.put("AL", "Alabama");
		usStates.put("AK", "Alaska");
		canadianStates = new HashMap<>();
		canadianStates.put("AB", "Alberta");
		canadianStates.put("BC", "British Columbia");
	}

    @Test
    public void testErrorMessageForMissingCreditCardNumber() {
        CreditCardForm creditCardFormWithMissingCreditCard = new CreditCardFormBuilderForTest().withCreditCardNumber(null).build();
        assertFalse(creditCardFormWithMissingCreditCard.isValidForm(results));
        assertTrue("Credit Card Number must be entered.".equals(results.get(0)));
    }

    @Test
    public void testErrorMessageForMissingAllValues() {
        CreditCardForm creditCardFormWithMissingCreditCard = new CreditCardFormBuilderForTest().withCvc2(null).withCardHolderName(null).withEmailAddress(null).withCreditCardNumber(null).build();
        assertFalse(creditCardFormWithMissingCreditCard.isValidForm(results));
        assertTrue("Card Holder Name, Credit Card Number, Security Code, Email Address must be entered.".equals(results.get(0)));
    }

    @Test
    public void testErrorMessagesWhenAllValuesInvalid() {
        CreditCardForm creditCardFormWithMissingCreditCard = new CreditCardFormBuilderForTest().withCvc2("1234").withExpiryYear("2008").withEmailAddress("alison").withCreditCardNumber("4200").build();
        assertFalse(creditCardFormWithMissingCreditCard.isValidForm(results));
        assertEquals("Invalid Credit Card Number, Expiry Date, Security Code, Email Address entered.", results.get(0));
    }
    @Test
    public void testErrorMessagesWhenExpiryDateInvalid() {
        CreditCardForm creditCardFormWithMissingCreditCard = new CreditCardFormBuilderForTest().withExpiryYear("2008").build();
        assertFalse(creditCardFormWithMissingCreditCard.isValidForm(results));
        assertTrue("Invalid Expiry Date entered.".equals(results.get(0)));
    }
    @Test
    public void testErrorMessagesWhenEmailAddressInvalid() {
        CreditCardForm creditCardFormWithMissingCreditCard = new CreditCardFormBuilderForTest().withEmailAddress("alison").build();
        assertFalse(creditCardFormWithMissingCreditCard.isValidForm(results));
        assertTrue("Invalid Email Address entered.".equals(results.get(0)));
    }
    @Test
    public void testErrorMessagesWhenEmailAddressInvalidAndMissingCardHolderName() {
        CreditCardForm creditCardFormWithMissingCreditCard = new CreditCardFormBuilderForTest().withEmailAddress("alison").withCardHolderName(null).build();
        assertFalse(creditCardFormWithMissingCreditCard.isValidForm(results));
        assertTrue("Invalid Email Address entered.".equals(results.get(0)));
        assertTrue("Card Holder Name must be entered.".equals(results.get(1)));
    }

	@Test
	public void testValidForm() {
		assertTrue(createCreditCardBuilder().build().isValidForm(results));
	}

	@Test
	public void testCreditCardValidationWithInvalidLuhn() {
		assertFalse(underTest.validateCreditCardNumber("1234567890123456"));
	}

	@Test
	public void testCreditCardValidationAlphaCharacters() {
		assertFalse(underTest.validateCreditCardNumber("1234567-90D23456"));
	}

    @Test
	public void testCreditCardValidation() {
		assertTrue(underTest.validateCreditCardNumber("4200000000000000"));
	}

    @Test
	public void testCreditCardValidationWithSpaces() {
		assertTrue(underTest.validateCreditCardNumber("4200 0000 0000 0000"));
	}

    @Test
	public void testCreditCardValidationWithMultipleSpaces() {
		assertTrue(underTest.validateCreditCardNumber("4200   0000   0000    0000"));
	}

    @Test
	public void testCreditCardValidationWithWhiteSpaceChars() {
		assertTrue(underTest.validateCreditCardNumber("4200  \t 0000  \n  0000    0000"));
	}

     @Test
	public void testCreditCardValidationWithLeadingTrailingWhiteSpaceChars() {
		assertTrue(underTest.validateCreditCardNumber(" 4200  \t 0000  \n  0000    0000 \n"));
	}

	@Test
	public void testCreditCardValidationTooFewDigits() {
		assertFalse(underTest.validateCreditCardNumber("1234"));
	}

	@Test
	public void testCreditCardValidationTooManyDigits() {
		assertFalse(underTest.validateCreditCardNumber("12345678901234567"));
	}

	@Test
	public void testExpiryMonthValidationNonNumeric() {
		assertFalse(underTest.validateExpiryMonth("A2"));
	}

	@Test
	public void testExpiryMonthValidationValidCase() {
		assertTrue(underTest.validateExpiryMonth("03"));
	}

	@Test
	public void testExpiryYearValidationValidCase() {
		assertTrue(underTest.validateExpiryYear("2019"));
	}

	@Test
	public void testExpiryYearValidationTooManyDigits() {
		assertFalse(underTest.validateExpiryYear("201911"));
	}

	@Test
	public void testCVCValid3Digit() {
		assertTrue(underTest.validateCVC("123"));
	}

	@Test
	public void testCVCValid4Digit() {
		assertFalse(underTest.validateCVC("1234"));
	}

	@Test
	public void testCVCInvalidAlpha() {
		assertFalse(underTest.validateCVC("1A34"));
		assertFalse(underTest.validateCVC("B"));
	}

	@Test
	public void testInvalidString() {
		assertFalse(underTest.validateString(null));
		assertFalse(underTest.validateString(""));
	}

	@Test
	public void testValidString() {
		assertTrue(underTest.validateString("1223344432"));
		assertTrue(underTest.validateString("Whatever"));
	}



    @Test
    public void testValidationPassesWithExpireyDateInFuture() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2011, 1, 1, 1, 1, 0, 0).getMillis());

        assertTrue(createCreditCardBuilder().build().validateExpireDateInFuture("02", "2011"));
    }

    @Test
    public void testValidationFailsWithExpiryDateInPast() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2011, 1, 1, 1, 1, 0, 0).getMillis());

        assertFalse(createCreditCardBuilder().build().validateExpireDateInFuture("12", "2010"));
    }

    @Test
    public void testFormValidationFailsWithMissingExpiryMonth() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2011, 1, 1, 1, 1, 0, 0).getMillis());
        final ArrayList<String> results = new ArrayList<String>();

        final CreditCardForm creditCardForm = createCreditCardBuilder().withExpirationMonth(null).withExpirationYear("2016").build();
        assertFalse(creditCardForm.isValidForm(results));
    }

    @Test
    public void testFormValidationFailsWithMissingExpiryYear() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2011, 1, 1, 1, 1, 0, 0).getMillis());
        final CreditCardFormBuilder validCreditCardFormBuilder = createCreditCardBuilder().withExpirationMonth(null).withExpirationYear("2016");
        final ArrayList<String> results = new ArrayList<String>();

        assertFalse(validCreditCardFormBuilder.build().isValidForm(results));
    }

    @Test
    public void testValidationPassesInExpirationMonth() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2011, 1, 31, 1, 59, 0, 0).getMillis());

        assertTrue(createCreditCardBuilder().build().validateExpireDateInFuture("01", "2011"));
    }

    private CreditCardFormBuilder createCreditCardBuilder() {
        return CreditCardFormBuilder.valueOf()
                .withPaymentOptionId("option2")
                .withPromotionId(1l)
                .withCreditCardNumber("4200000000000000")
                .withCvc2("123")
                .withExpirationMonth("11")
                .withExpirationYear("2019")
                .withCardHolderName("Nick Jones")
                .withEmailAddress("somebody@somewhere.com")
                .withTermsAndServiceAgreement("true");
    }

}
