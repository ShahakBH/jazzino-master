package strata.server.lobby.controlcentre.repository.mapper

import strata.server.lobby.controlcentre.model.User
import java.sql.ResultSet
import org.springframework.jdbc.core.{JdbcTemplate, RowMapper}
import scala.collection.JavaConversions._

object UserMapper {
    private val SQL_SELECT_ROLES = "SELECT ROLE FROM OPERATIONS_USER_ROLE WHERE USERNAME=?"
}

class UserMapper(val jdbcTemplate: JdbcTemplate) extends RowMapper[User] {

    def mapRow(rs: ResultSet, rowNum: Int): User = {
        val userName = rs.getString("USERNAME")
        var roles = jdbcTemplate.queryForList(UserMapper.SQL_SELECT_ROLES, classOf[java.lang.String], userName)

        new User(userName,
            rs.getString("PASSWORD"),
            rs.getString("REAL_NAME"),
            Set() ++ roles)
    }

}
