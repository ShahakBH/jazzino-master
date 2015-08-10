package com.yazino.payment.worldpay.nvp;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class NVPMessageTest {

    @Test
    public void aMessageWithNoFieldsIsNotValid() {
        assertThat(new NoFieldMessage().isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void aMessageWithNoFieldsCannotBeRendered() {
        new NoFieldMessage().toMessage();
    }

    @Test
    public void aMessageMissingMandatoryFieldsIsNotValid() {
        assertThat(new TestMessage().isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void aMessageMissingMandatoryFieldsCannotBeRendered() {
        new TestMessage().toMessage();
    }

    @Test
    public void aMessageWithValuesForMandatoryFieldsCanBeRendered() {
        final String renderedMessage = withMandatoryFields(new TestMessage())
                .toMessage();

        assertThat(renderedMessage, is(equalTo(
                "StringIn=MerchantId^12345~TimeOut^60000~TransactionType^PT~UserName^aUsername~UserPassword^aPassword~VersionUsed^1")));
    }

    @Test
    public void aMessageWithMultipleValuesCanBeRendered() {
        final String renderedMessage = withMandatoryFields(new TestMessage())
                .withValue("testNumeric", BigDecimal.valueOf(123456))
                .withValue("testAlpha", "ABCDEF")
                .withValue("testMinLength", "1234")
                .withValue("testMaxLength", "1234")
                .toMessage();

        assertThat(renderedMessage, is(equalTo(
                "StringIn=MerchantId^12345~TimeOut^60000~TransactionType^PT~UserName^aUsername~UserPassword^aPassword~VersionUsed^1~testAlpha^ABCDEF~testMaxLength^1234~testMinLength^1234~testNumeric^123456")));
    }

    @Test
    public void aMessageWithAnAcctNumberFieldHasTheMiddleDigitsObscured() {
        final NVPMessage testMessage = new TestMessage().withValue("AcctNumber", "1234567890123456");

        assertThat(testMessage.toString(), containsString("AcctNumber=1234XXXXXXXX3456"));
        assertThat(testMessage.toString(), not(containsString("1234567890123456")));
    }

    @Test
    public void aMessageWithAnAcctNumberThatIsTooShortToBeACardNumberIsNotObscured() {
        final NVPMessage testMessage = new TestMessage().withValue("AcctNumber", "12345678");

        assertThat(testMessage.toString(), containsString("AcctNumber=12345678"));
    }

    @Test
    public void aMessageWithAnAcctNumberFieldDoesNotObscureItInTheMessage() {
        final String renderedMessage = withMandatoryFields(new TestMessage())
                .withValue("AcctNumber", "1234567890123456")
                .toMessage();

        assertThat(renderedMessage, is(equalTo(
                "StringIn=AcctNumber^1234567890123456~MerchantId^12345~TimeOut^60000~TransactionType^PT~UserName^aUsername~UserPassword^aPassword~VersionUsed^1")));
    }

    @Test
    public void aRenderedMessageIsUrlEncoded() {
        final String renderedMessage = withMandatoryFields(new TestMessage())
                .withValue("testAlphanumeric", "a value & cake = something")
                .toMessage();

        assertThat(renderedMessage, is(equalTo(
                "StringIn=MerchantId^12345~TimeOut^60000~TransactionType^PT~UserName^aUsername~UserPassword^aPassword~VersionUsed^1~testAlphanumeric^a+value+%26+cake+%3D+something")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addingAnInvalidFieldValueCausesAnIllegalArgumentException() {
        new TestMessage().withValue("testNumeric", "abc");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addingANullFieldValueCausesAnIllegalArgumentException() {
        new TestMessage().withValue("testNumeric", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addingABlankFieldValueCausesAnIllegalArgumentException() {
        new TestMessage().withValue("testAlpha", "    ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addingAFieldValueThatIsTooShortCausesAnIllegalArgumentException() {
        new TestMessage().withValue("testMinLength", "12");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addingAFieldValueThatIsTooLongCausesAnIllegalArgumentException() {
        new TestMessage().withValue("testMaxLength", "123456");
    }

    @Test
    public void addingATruncatedFieldValueThatIsTooLongCausesTruncatedToMaxLength() {
        final String renderedMessage = withMandatoryFields(new TestMessage())
                .withTruncatedValue("testMaxLength", "123456")
                .toMessage();

        assertThat(renderedMessage, is(equalTo(
                "StringIn=MerchantId^12345~TimeOut^60000~TransactionType^PT~UserName^aUsername~UserPassword^aPassword~VersionUsed^1~testMaxLength^12345")));
    }

    private NVPMessage withMandatoryFields(final NVPMessage message) {
        return message.withValue("VersionUsed", 1)
                .withValue("MerchantId", "12345")
                .withValue("UserName", "aUsername")
                .withValue("UserPassword", "aPassword")
                .withValue("TransactionType", "PT")
                .withValue("TimeOut", 60000);
    }

    private class NoFieldMessage extends NVPMessage {
        private static final long serialVersionUID = 7029208457965851899L;
    }

    private class TestMessage extends NVPMessage {
        private static final long serialVersionUID = 2452225001898755259L;

        {
            defineField("testAlphanumeric", NVPType.ALPHANUMERIC, null, null, false);
            defineField("testAlpha", NVPType.ALPHA, false);
            defineField("testNumeric", NVPType.NUMERIC, false);
            defineField("testMinLength", NVPType.ALPHANUMERIC, 3, null, false);
            defineField("testMaxLength", NVPType.ALPHANUMERIC, null, 5, false);
            defineField("AcctNumber", NVPType.NUMERIC, false);
        }
    }

}
