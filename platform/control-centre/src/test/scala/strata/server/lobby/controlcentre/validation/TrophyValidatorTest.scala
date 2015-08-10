package strata.server.lobby.controlcentre.validation

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.springframework.validation.Errors
import org.junit.Test
import strata.server.lobby.controlcentre.form.TrophyForm
import org.mockito.Mockito._

class TrophyValidatorTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val errors = mock[Errors]

    private val underTest = new TrophyValidator

    @Test def trophyFormShouldBeSupported() {
        underTest.supports(classOf[TrophyForm]) should equal(true)
    }

    @Test def aValidTrophyShouldPass() {
        underTest.validate(aTrophy, errors)

        verifyZeroInteractions(errors)
    }

    @Test def aTrophyWithNoNameShouldFail() {
        val message = aTrophy
        message.setName(null)
        underTest.validate(message, errors)

        verify(errors).rejectValue("name", "trophy.name.missing", "Name is required")
    }

    @Test def aTrophyWithNoGameTypeShouldFail() {
        val message = aTrophy
        message.setGameType(null)
        underTest.validate(message, errors)

        verify(errors).rejectValue("gameType", "trophy.gameType.missing", "Game Type is required")
    }

    @Test def aTrophyWithNoImageShouldFail() {
        val message = aTrophy
        message.setImage(null)
        underTest.validate(message, errors)

        verify(errors).rejectValue("image", "trophy.image.missing", "Image is required")
    }

    @Test def aTrophyWithNoMessageShouldFail() {
        val message = aTrophy
        message.setMessage(null)
        underTest.validate(message, errors)

        verify(errors).rejectValue("message", "trophy.message.missing", "Message is required")
    }

    @Test def aTrophyWithNoShortDescriptionShouldFail() {
        val message = aTrophy
        message.setShortDescription(null)
        underTest.validate(message, errors)

        verify(errors).rejectValue("shortDescription", "trophy.shortDescription.missing", "Short Description is required")
    }

    private def aTrophy =
        new TrophyForm(BigDecimal(10), "anImage", "aName", "aGameType",
            "aMessage", "aShortDescription", "aCabinetDescription")

}
