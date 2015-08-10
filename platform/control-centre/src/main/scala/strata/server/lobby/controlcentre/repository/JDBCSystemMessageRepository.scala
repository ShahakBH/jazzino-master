package strata.server.lobby.controlcentre.repository

import mapper.SystemMessageMapper
import org.springframework.stereotype.Repository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.{PreparedStatementCreator, JdbcTemplate}
import org.apache.commons.lang3.Validate.notNull
import strata.server.lobby.controlcentre.model.SystemMessage
import strata.server.lobby.controlcentre.repository.JDBCSystemMessageRepository._
import com.yazino.platform.model.PagedData
import java.sql.{PreparedStatement, Timestamp, Statement, Connection}
import org.joda.time.DateTime
import org.springframework.transaction.annotation.Transactional

object JDBCSystemMessageRepository {
    private val SQL_SELECT = "SELECT * FROM SYSTEM_MESSAGE WHERE SYSTEM_MESSAGE_ID=?"

    private val SQL_SELECT_ALL = "SELECT * FROM SYSTEM_MESSAGE"

    private val SQL_INSERT = "INSERT INTO SYSTEM_MESSAGE (MESSAGE,VALID_FROM,VALID_TO) VALUES (?,?,?)"

    private val SQL_UPDATE = "UPDATE SYSTEM_MESSAGE SET MESSAGE=?,VALID_FROM=?,VALID_TO=? WHERE SYSTEM_MESSAGE_ID=?"

    private val SQL_DELETE = "DELETE FROM SYSTEM_MESSAGE WHERE SYSTEM_MESSAGE_ID=?"
}

@Repository
class JDBCSystemMessageRepository @Autowired() (val jdbcTemplate: JdbcTemplate) extends SystemMessageRepository with MySQLPaging[SystemMessage] {

    private val systemMessageMapper = new SystemMessageMapper()

    def findById(id: BigDecimal): Option[SystemMessage] = {
        notNull(id, "id may not be null")

        val systemMessages = jdbcTemplate.query(SQL_SELECT, systemMessageMapper, id.underlying())
        if (systemMessages != null && !systemMessages.isEmpty) {
            Some(systemMessages.get(0))
        } else {
            None
        }
    }

    @Transactional(readOnly = true)
    def findAll(page: Int = 0, pageSize: Int = 20): PagedData[SystemMessage]
            = selectWithPaging(jdbcTemplate, SQL_SELECT_ALL, systemMessageMapper, page, pageSize)

    def save(systemMessage: SystemMessage): SystemMessage = {
        notNull(systemMessage, "systemMessage may not be null")

        if (systemMessage.id == null) {
            val systemMessageId = jdbcTemplate.execute(new PreparedStatementCreator {
                def createPreparedStatement(conn: Connection): PreparedStatement = {
                    val stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)
                    stmt.setString(1, systemMessage.message)
                    stmt.setTimestamp(2, asTimestamp(systemMessage.validFrom))
                    stmt.setTimestamp(3, asTimestamp(systemMessage.validTo))
                    stmt
                }
            }, new PrimaryKeyLoader())
            systemMessage.withId(BigDecimal(systemMessageId))

        } else {
            jdbcTemplate.update(SQL_UPDATE, systemMessage.message,
                    asTimestamp(systemMessage.validFrom),
                    asTimestamp(systemMessage.validTo),
                    systemMessage.id.underlying())
            systemMessage
        }
    }

    def delete(id: BigDecimal) {
        notNull(id, "id may not be null")

        jdbcTemplate.update(SQL_DELETE, id.underlying())
    }

    private def asTimestamp(dateTime: DateTime) =
        if (dateTime == null) {
            null
        } else {
            new Timestamp(dateTime.getMillis)
        }

}
