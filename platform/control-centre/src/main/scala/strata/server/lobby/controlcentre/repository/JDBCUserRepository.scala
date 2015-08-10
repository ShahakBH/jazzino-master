package strata.server.lobby.controlcentre.repository

import mapper.UserMapper
import org.springframework.stereotype.Repository
import strata.server.lobby.controlcentre.model.User
import org.springframework.beans.factory.annotation.Autowired
import com.yazino.platform.model.PagedData
import org.apache.commons.lang3.Validate.notNull
import org.springframework.jdbc.core.{PreparedStatementCreator, JdbcTemplate}
import java.sql.{PreparedStatement, Connection}
import scala.collection.JavaConversions._
import strata.server.lobby.controlcentre.repository.JDBCUserRepository._
import org.springframework.transaction.annotation.Transactional

object JDBCUserRepository {
    private val SQL_INSERT_OR_UPDATE = """
        INSERT INTO OPERATIONS_USER (USERNAME,PASSWORD,REAL_NAME) VALUES (?,?,?) ON DUPLICATE KEY UPDATE PASSWORD=VALUES(PASSWORD), REAL_NAME=VALUES(REAL_NAME)
        """
    private val SQL_SELECT = "SELECT * FROM OPERATIONS_USER WHERE USERNAME=?"

    private val SQL_SELECT_ALL = "SELECT * FROM OPERATIONS_USER"

    private val SQL_DELETE = "DELETE FROM OPERATIONS_USER WHERE USERNAME=?"

    private val SQL_ADD_ROLE = "INSERT INTO OPERATIONS_USER_ROLE (USERNAME,ROLE) VALUES (?, ?)"

    private val SQL_DELETE_ROLES = "DELETE FROM OPERATIONS_USER_ROLE WHERE USERNAME=?"

    private val SQL_FIND_ALL_ROLES = "SELECT ROLE FROM OPERATIONS_ROLE"
}

@Repository
class JDBCUserRepository @Autowired() (val jdbcTemplate: JdbcTemplate) extends UserRepository with MySQLPaging[User] {

    private val userMapper = new UserMapper(jdbcTemplate)

    def findById(userName: String): Option[User] = {
        notNull(userName, "userName may not be null")

        val users = jdbcTemplate.query(SQL_SELECT, userMapper, userName)
        if (users != null && !users.isEmpty) {
            Some(users.get(0))
        } else {
            None
        }
    }

    @Transactional(readOnly = true)
    def findAll(page: Int = 0,  pageSize: Int = 20) : PagedData[User]
            = selectWithPaging(jdbcTemplate, SQL_SELECT_ALL, userMapper, page, pageSize)

    def save(user: User): User = {
        notNull(user, "user may not be null")

        jdbcTemplate.update(new PreparedStatementCreator {
            def createPreparedStatement(conn: Connection): PreparedStatement = {
                val stmt = conn.prepareStatement(SQL_INSERT_OR_UPDATE)
                stmt.setString(1, user.userName)
                stmt.setString(2, user.password)
                stmt.setString(3, user.realName)
                stmt
            }
        })

        updateRolesFor(user)
        user
    }

    def findAllRoles: Set[String] =
        Set[String]() ++ jdbcTemplate.queryForList(SQL_FIND_ALL_ROLES, classOf[String])

    private def updateRolesFor(user: User) {
        jdbcTemplate.update(SQL_DELETE_ROLES, user.userName)

        user.roles.foreach(jdbcTemplate.update(SQL_ADD_ROLE, user.userName, _))
    }

    def delete(userName: String) {
        notNull(userName, "userName may not be null")

        jdbcTemplate.update(SQL_DELETE_ROLES, userName)
        jdbcTemplate.update(SQL_DELETE, userName)
    }
}
