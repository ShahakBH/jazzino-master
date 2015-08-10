package strata.server.lobby.controlcentre.repository

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.transaction.TransactionConfiguration
import org.springframework.test.context.ContextConfiguration
import org.junit.runner.RunWith
import org.scalatest.junit.AssertionsForJUnit
import org.springframework.beans.factory.annotation.Autowired
import org.scalatest.matchers.ShouldMatchers
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.junit.{After, Before, Test}
import strata.server.lobby.controlcentre.model.User
import strata.server.lobby.controlcentre.repository.JDBCUserRepositoryIntegrationTest._

object JDBCUserRepositoryIntegrationTest {
    val TEST_DESC = "JDBCUserRepositoryIntegrationTest"
    val TEST_USERNAME = "aTestUserName"
}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration
@TransactionConfiguration
class JDBCUserRepositoryIntegrationTest extends AssertionsForJUnit with ShouldMatchers {

    @Autowired private var underTest: UserRepository = null
    @Autowired private val jdbcTemplate: JdbcTemplate = null

    @After def cleanUpDatabase() {
        jdbcTemplate.update("DELETE FROM OPERATIONS_USER_ROLE WHERE USERNAME=?", TEST_USERNAME)
        jdbcTemplate.update("DELETE FROM OPERATIONS_ROLE WHERE DESCRIPTION=?", TEST_DESC)
        jdbcTemplate.update("DELETE FROM OPERATIONS_USER WHERE USERNAME=?", TEST_USERNAME)
    }

    @Before def createTestRoles() {
        jdbcTemplate.update("INSERT INTO OPERATIONS_ROLE (ROLE,DESCRIPTION) VALUES ('role1',?)", TEST_DESC)
        jdbcTemplate.update("INSERT INTO OPERATIONS_ROLE (ROLE,DESCRIPTION) VALUES ('role2',?)", TEST_DESC)
        jdbcTemplate.update("INSERT INTO OPERATIONS_ROLE (ROLE,DESCRIPTION) VALUES ('role3',?)", TEST_DESC)
    }

    @Transactional
    @Test def findingANonExistentUserReturnsNone() {
        underTest.findById("aNonexistentUser") should equal(None)
    }

    @Transactional
    @Test def savingANewUserCreatesItInTheRepository() {
        val user = underTest.save(aNewUser())

        val userMap = jdbcTemplate.queryForMap("SELECT * FROM OPERATIONS_USER WHERE USERNAME=?", user.userName)
        userMap.get("PASSWORD") should equal(user.password)
        userMap.get("REAL_NAME") should equal(user.realName)
        userMap.get("USERNAME") should equal(user.userName)
    }

    @Transactional
    @Test def updatingAnExistingUserUpdatesItInTheRepository() {
        val originalUser = underTest.save(aNewUser())
        val modifiedUser = new User(TEST_USERNAME, "aNewPassword", "aNewRealName", Set("role2", "role3"))

        underTest.save(modifiedUser)

        modifiedUser.userName should equal(originalUser.userName)
        val userMap = jdbcTemplate.queryForMap("SELECT * FROM OPERATIONS_USER WHERE USERNAME=?", modifiedUser.userName)
        userMap.get("PASSWORD") should equal(modifiedUser.password)
        userMap.get("REAL_NAME") should equal(modifiedUser.realName)
        userMap.get("USERNAME") should equal(modifiedUser.userName)
    }

    @Transactional
    @Test def deletingAnExistingUserRemovesItFromTheRepository() {
        val originalUser = underTest.save(aNewUser())

        underTest.delete(TEST_USERNAME)

        underTest.findById(TEST_USERNAME) should equal(None)
    }

    @Transactional
    @Test def findingAUserReturnsTheUser() {
        val user = underTest.save(aNewUser())

        underTest.findById(TEST_USERNAME).getOrElse(None) should equal(user)
    }

    @Transactional
    @Test def findingAllUsersReturnsAKnownUser() {
        val user = underTest.save(aNewUser())

        val allUsers = underTest.findAll(0, Integer.MAX_VALUE)

        allUsers.getTotalSize should (be >= 1)
        allUsers.getData should contain(user)
    }

    @Transactional
    @Test def findingAllRolesReturnsAllRoles() {
        val roles = underTest.findAllRoles

        roles should contain("role1")
        roles should contain("role2")
        roles should contain("role3")
    }

    private def aNewUser() = new User(TEST_USERNAME, "aPassword", "aRealName", Set("role1", "role2"))

}
