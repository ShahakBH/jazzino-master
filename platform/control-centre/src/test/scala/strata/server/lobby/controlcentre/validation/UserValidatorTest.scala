package strata.server.lobby.controlcentre.validation

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import org.junit.Test
import org.springframework.validation.Errors
import strata.server.lobby.controlcentre.form.UserForm
import scala.collection.JavaConversions._
import org.mockito.Mockito._

class UserValidatorTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val errors = mock[Errors]

    private val underTest: UserValidator = new UserValidator()

    @Test def userFormShouldBeSupported() {
        underTest.supports(classOf[UserForm]) should equal(true)
    }

    @Test def validationOfAUserFormWithANullPasswordShouldPass() {
        underTest.validate(aUser(), errors)

        verifyZeroInteractions(errors)
    }

    @Test def validationOfANewUserFormWithANullPasswordShouldFail() {
        val user: UserForm = aUser()
        user.isNew = true
        underTest.validate(user, errors)

        verify(errors).rejectValue("password", "user.password.missing", "Password is required")
    }

    @Test def validationOfAUserFormWithANonNullPasswordAndAnEqualConfirmPasswordShouldPass() {
        val user = aUser()
        user.password = "aNewPassword"
        user.confirmPassword = "aNewPassword"

        underTest.validate(user, errors)

        verifyZeroInteractions(errors)
    }

    @Test def validationOfAUserFormWithANonNullPasswordAndANullConfirmPasswordShouldFail() {
        val user = aUser()
        user.password = "aNewPassword"

        underTest.validate(user, errors)

        verify(errors).rejectValue("password", "user.password.mismatch", "Passwords do not match")
    }

    @Test def validationOfAUserFormWithANonNullPasswordAndAMismatchingConfirmPasswordShouldFail() {
        val user = aUser()
        user.password = "aNewPassword"
        user.confirmPassword = "anotherNewPassword"

        underTest.validate(user, errors)

        verify(errors).rejectValue("password", "user.password.mismatch", "Passwords do not match")
    }

    private def aUser() = {
        val form = new UserForm("aUserName", null, "aRealName", List("role1", "role2"))
        form.isNew = false
        form
    }

}
