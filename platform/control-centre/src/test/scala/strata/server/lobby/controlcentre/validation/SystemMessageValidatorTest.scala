package strata.server.lobby.controlcentre.validation

import org.scalatest.mock.MockitoSugar
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.AssertionsForJUnit
import org.springframework.validation.Errors
import org.junit.{After, Before, Test}
import strata.server.lobby.controlcentre.form.SystemMessageForm
import org.joda.time.DateTime
import org.mockito.Mockito._
import com.yazino.test.ThreadLocalDateTimeUtils

class SystemMessageValidatorTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val errors = mock[Errors]

    private val underTest = new SystemMessageValidator

    @Before def lockSystemTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime(2012, 10, 19, 0, 0, 0, 0).getMillis)
    }

    @After def unlockSystemTime() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem()
    }

    @Test def systemMessageFormShouldBeSupported() {
        underTest.supports(classOf[SystemMessageForm]) should equal(true)
    }

    @Test def aValidSystemMessageShouldPass() {
        underTest.validate(aSystemMessage, errors)

        verifyZeroInteractions(errors)
    }

    @Test def aSystemMessageWithNoMessageShouldFail() {
        val message = aSystemMessage
        message.setMessage(null)
        underTest.validate(message, errors)

        verify(errors).rejectValue("message", "systemMessage.message.missing", "Message is required")
    }

    @Test def aSystemMessageWithNoValidFromDateShouldFail() {
        val message = aSystemMessage
        message.setValidFrom(null)
        underTest.validate(message, errors)

        verify(errors).rejectValue("validFrom", "systemMessage.validFrom.missing", "Valid From is required")
    }

    @Test def aSystemMessageWithNoValidToDateShouldFail() {
        val message = aSystemMessage
        message.setValidTo(null)
        underTest.validate(message, errors)

        verify(errors).rejectValue("validTo", "systemMessage.validTo.missing", "Valid To is required")
    }

    @Test def aSystemMessageWithAValidToDateInThePastShouldFail() {
        val message = aSystemMessage
        message.setValidFrom(new DateTime(2012, 10, 18, 22, 59, 59, 999).toDate)
        message.setValidTo(new DateTime(2012, 10, 18, 23, 59, 59, 999).toDate)
        underTest.validate(message, errors)

        verify(errors).rejectValue("validTo", "systemMessage.validTo.past", "Valid To must be in the future")
    }

    @Test def aSystemMessageWithAValidToDateBeforeTheValidFromDateShouldFail() {
        val message = aSystemMessage
        message.setValidTo(new DateTime(2012, 10, 19, 0, 0, 0, 0).toDate)
        underTest.validate(message, errors)

        verify(errors).rejectValue("validTo", "systemMessage.validTo.inconsistent",
                "Valid To must be after the Valid From date")
    }

    private def aSystemMessage =
            new SystemMessageForm(BigDecimal(10), "aMessage",
                new DateTime(2012, 10, 20, 0, 0, 0, 0).toDate,
                new DateTime(2012, 10, 20, 10, 0, 0, 0).toDate)

}
