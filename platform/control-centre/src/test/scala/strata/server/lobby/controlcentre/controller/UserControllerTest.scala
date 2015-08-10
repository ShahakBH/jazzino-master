package strata.server.lobby.controlcentre.controller

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.mock.MockitoSugar
import javax.servlet.http.HttpServletResponse
import org.junit.{Test, Before}
import strata.server.lobby.controlcentre.repository.JDBCUserRepository
import strata.server.lobby.controlcentre.model.User
import org.mockito.Mockito._
import com.yazino.platform.model.PagedData
import scala.collection.JavaConversions._
import strata.server.lobby.controlcentre.form.UserForm
import java.util
import org.springframework.security.authentication.encoding.PasswordEncoder
import strata.server.lobby.controlcentre.validation.UserValidator
import org.springframework.validation.BindingResult

class UserControllerTest extends AssertionsForJUnit with ShouldMatchers with MockitoSugar {

    private val response = mock[HttpServletResponse]
    private val userRepository = mock[JDBCUserRepository]
    private val passwordEncoder = mock[PasswordEncoder]
    private val userValidator = mock[UserValidator]
    private val bindingResult = mock[BindingResult]

    private var underTest: UserController = null

    @Before def setUp() {
        when(userRepository.findAllRoles).thenReturn(Set("role1", "role2"))
        when(passwordEncoder.encodePassword("aNewPassword", null)).thenReturn("aHashedPassword")

        underTest = new UserController(userRepository, passwordEncoder, userValidator)
    }

    @Test def listingReturnsAllUsers() {
        val expectedData = new PagedData[User](0, 2, 2, List(aUser("user1"), aUser("user2")))
        when(userRepository.findAll(0, 20)).thenReturn(expectedData)

        val model = underTest.list

        model.getModel.get("users") should equal(expectedData)
    }

    @Test def listingAPageRequestTheAppropriatePage() {
        val expectedData = new PagedData[User](0, 2, 2, List(aUser("user1"), aUser("user2")))
        when(userRepository.findAll(3, 20)).thenReturn(expectedData)

        underTest.listAtPage(4)

        verify(userRepository).findAll(3, 20)
    }

    @Test def listingUsersTheListView() {
        when(userRepository.findAll(0, 20)).thenReturn(new PagedData[User](0, 2, 2, List(aUser("user1"), aUser("user2"))))

        val model = underTest.list

        model.getViewName should equal("admin/user/list")
    }

    @Test def showingANonExistentUserReturnsAFileNotFoundStatus() {
        when(userRepository.findById("user1")).thenReturn(None)

        val model = underTest.show("user1", response)

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
        model should equal(null)
    }

    @Test def showingAnExistingUserReturnsTheUser() {
        when(userRepository.findById("user1")).thenReturn(Some(aUser("user1")))

        val model = underTest.show("user1", response)

        model.getModel.get("user") should equal(new UserForm(aUser("user1")))
        verifyZeroInteractions(response)
    }

    @Test def showingAnExistingUserIncludesAllMappedRolesInTheModel() {
        when(userRepository.findById("user1")).thenReturn(Some(aUser("user1")))

        val model = underTest.show("user1", response)

        model.getModel.get("roles") should equal(mapOfRoles)
    }

    @Test def showingAnExistingUserUsesTheShowView() {
        when(userRepository.findById("user1")).thenReturn(Some(aUser("user1")))

        val model = underTest.show("user1", response)

        model.getViewName should equal("admin/user/show")
    }

    @Test def creatingAUserUsesTheCreateView() {
        val model = underTest.create()

        model.getViewName should equal("admin/user/create")
    }

    @Test def creatingAUserAddsAnEmptyUserFormToTheModel() {
        val model = underTest.create()

        model.getModel.get("user") should equal(new UserForm())
    }

    @Test def creatingAUserIncludesAllMappedRolesInTheModel() {
        val model = underTest.create()

        model.getModel.get("roles") should equal(mapOfRoles)
    }

    @Test def editingAUserUsesTheEditView() {
        when(userRepository.findById("user1")).thenReturn(Some(aUser("user1")))

        val model = underTest.edit("user1", response)

        model.getViewName should equal("admin/user/edit")
    }

    @Test def editAnExistingUserAddsTheAppropriateUserFormToTheModel() {
        when(userRepository.findById("user1")).thenReturn(Some(aUser("user1")))

        val model = underTest.edit("user1", response)

        model.getModel.get("user") should equal(new UserForm(aUser("user1")))
    }

    @Test def editingAnExistingUserIncludesAllMappedRolesInTheModel() {
        when(userRepository.findById("user1")).thenReturn(Some(aUser("user1")))

        val model = underTest.edit("user1", response)

        model.getModel.get("roles") should equal(mapOfRoles)
    }

    @Test def editingANonExistentUserReturnsANotFoundError() {
        when(userRepository.findById("user1")).thenReturn(None)

        val model = underTest.edit("user1", response)

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND)
        model should equal(null)
    }

    @Test def deletingAUserDeletesTheUserViaTheRepository() {
        underTest.delete("user1")

        verify(userRepository).delete("user1")
    }

    @Test def deletingAUserAddsAMessageToTheModel() {
        val model = underTest.delete("user1")

        model.getModel.get("message") should equal("User user1 has been deleted.")
    }

    @Test def savingAUserRedirectsTheUserToTheShowView() {
        val userForm = new UserForm(aUser("user1"))
        val savedUser = new User(userForm.userName, "aHashedPassword", userForm.realName, userForm.roles.toSet)
        when(userRepository.save(savedUser)).thenReturn(aUser("user1"))
        userForm.password = "aNewPassword"

        val model = underTest.save(userForm, bindingResult)

        model.getViewName should equal("redirect:/admin/user/show/user1")
    }

    @Test def savingAnExistingUserSavesTheUserToTheRepository() {
        val userForm = new UserForm(aUser("user1"))
        val savedUser = new User(userForm.userName, "aHashedPassword", userForm.realName, userForm.roles.toSet)
        when(userRepository.save(savedUser)).thenReturn(aUser("user1"))
        userForm.password = "aNewPassword"

        underTest.save(userForm, bindingResult)

        verify(userRepository).save(savedUser)
    }

    @Test def savingAnExistingUserWithNoPasswordChangeSavesTheUserWithTheOriginalPassword() {
        val userForm = new UserForm(aUser("user1"))
        when(userRepository.findById("user1")).thenReturn(Some(userForm.toUser))
        val savedUser = new User(userForm.userName, "aPassword", userForm.realName, userForm.roles.toSet)
        when(userRepository.save(savedUser)).thenReturn(aUser("user1"))
        userForm.password = null

        underTest.save(userForm, bindingResult)

        verify(userRepository).save(savedUser)
    }

    @Test def savingANewUserSavesTheUserToTheRepository() {
        val userForm = new UserForm(aUser("user1"))
        userForm.isNew = true
        val savedUser = new User(userForm.userName, "aHashedPassword", userForm.realName, userForm.roles.toSet)
        when(userRepository.save(savedUser)).thenReturn(aUser("user1"))
        userForm.password = "aNewPassword"

        underTest.save(userForm, bindingResult)

        verify(userRepository).save(savedUser)
    }

    @Test def savingANewUserThatFailsValidationRedirectsTheUserToTheCreatePage() {
        val form = new UserForm(aUser("user1"))
        form.isNew = true
        when(bindingResult.hasErrors).thenReturn(true)

        val model = underTest.save(form, bindingResult)

        verify(userValidator).validate(form, bindingResult)
        model.getViewName should equal("admin/user/create")
        model.getModel.get("user") should equal(form)
        model.getModel.get("roles") should equal(mapOfRoles)
    }

    @Test def savingAnExistingUserThatFailsValidationRedirectsTheUserToTheCreatePage() {
        val form = new UserForm(aUser("user1"))
        when(bindingResult.hasErrors).thenReturn(true)

        val model = underTest.save(form, bindingResult)

        verify(userValidator).validate(form, bindingResult)
        model.getViewName should equal("admin/user/edit")
        model.getModel.get("user") should equal(form)
        model.getModel.get("roles") should equal(mapOfRoles)
    }

    @Test def deletingAUserDirectsTheUserToTheListView() {
        val expectedData = new PagedData[User](0, 2, 2, List(aUser("user1"), aUser("user2")))
        when(userRepository.findAll(0, 20)).thenReturn(expectedData)

        val model = underTest.delete("user3")

        model.getViewName should equal("admin/user/list")
        model.getModel.get("users") should equal(expectedData)
    }

    private def mapOfRoles: util.HashMap[String, String] = {
        val javaMap = new util.HashMap[String, String]()
        javaMap.putAll(Map("role1" -> "Role1", "role2" -> "Role2"))
        javaMap
    }

    private def aUser(userName: String) = new User(userName, "aPassword", "aRealName", Set("role1", "role2"))
}
