package com.yazino.validation;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmailAddressFormatValidatorTest {

    @Test
    public void isValidFormat_shouldReturnFalseForInvalidEmailFormat() {
        assertFalse(EmailAddressFormatValidator.isValidFormat("invalid"));
    }

    @Test
    public void isValidFormat_shouldAcceptPlusCharacterInLocalPartOfEmailAddress() {
        assertTrue(EmailAddressFormatValidator.isValidFormat("is+valid@example.com"));
    }


    // Just out of interest, here is how we do against the samples on Wikipedia (http://en.wikipedia.org/wiki/Email_address#Valid_email_addresses)
    @Test
    public void isValidFormat_shouldAccept() {
        assertTrue(EmailAddressFormatValidator.isValidFormat("niceandsimple@example.com")); //
        assertTrue(EmailAddressFormatValidator.isValidFormat("very.common@example.com")); //
        assertTrue(EmailAddressFormatValidator.isValidFormat("a.little.lengthy.but.fine@dept.example.com")); //
        assertTrue(EmailAddressFormatValidator.isValidFormat("disposable.style.email.with+symbol@example.com")); //
        assertTrue(EmailAddressFormatValidator.isValidFormat("\"much.more unusual\"@example.com")); //
        assertTrue(EmailAddressFormatValidator.isValidFormat("\"very.unusual.@.unusual.com\"@example.com")); //
        assertTrue(EmailAddressFormatValidator.isValidFormat("\"very.(),:;<>[]\\\".VERY.\\\"very@\\\\ \\\"very\\\".unusual\"@strange.example.com")); //
        assertTrue(EmailAddressFormatValidator.isValidFormat("!#$%&'*+-/=?^_`{}|~@example.org")); //
        assertTrue(EmailAddressFormatValidator.isValidFormat("\"()<>[]:,;@\\\\\\\"!#$%&'*+-/=?^_`{}| ~  ? ^_`{}|~.a\"@example.org")); //
        assertTrue(EmailAddressFormatValidator.isValidFormat("\"\"@example.org")); //

        // not supported
//        assertTrue(EmailAddressFormatValidator.isValidFormat("0@a")); //
//        assertTrue(EmailAddressFormatValidator.isValidFormat("postbox@com")); // (top-level domains are valid hostnames)
//        assertTrue(EmailAddressFormatValidator.isValidFormat("user@[IPv6:2001:db8:1ff::a0b:dbd0]")); //
    }

    // Just out of interest, here is how we do against the samples on Wikipedia (http://en.wikipedia.org/wiki/Email_address#Invalid_email_addresses)
    @Test
    public void isValidFormat_shouldNotAccept() {

        assertFalse(EmailAddressFormatValidator.isValidFormat("Abc.example.com")); // (an @ character must separate the local and domain parts)

        // These pass but shouldn't
//        assertFalse(EmailAddressFormatValidator.isValidFormat("Abc.@example.com")); // (character dot(.) is last in local part)
//        assertFalse(EmailAddressFormatValidator.isValidFormat("A@b@c@example.com")); // (only one @ is allowed outside quotation marks)
//        assertFalse(EmailAddressFormatValidator.isValidFormat("a\"b(c)d,e:f;g<h>i[j\\k]l@example.com")); // (none of the special characters in this local
        // part is allowed outside quotation marks)
//        assertFalse(EmailAddressFormatValidator.isValidFormat("just\"not\"right@example.com")); // (quoted strings must be dot separated,
        // or the only element making up the local-part)
//        assertFalse(EmailAddressFormatValidator.isValidFormat("this is\"not\\allowed@example.com")); // (spaces, quotes,
        // and backslashes may only exist when within quoted strings and preceded by a backslash)
//        assertFalse(EmailAddressFormatValidator.isValidFormat("this\\ still\\\"not\\\\allowed@example.com")); // (even if escaped (preceded by a backslash),
        // spaces, quotes, and backslashes must still be contained by quotes)

//        assertFalse(EmailAddressFormatValidator.isValidFormat("Abc..123@example.com")); // (character dot(.) is double)

    }

}
