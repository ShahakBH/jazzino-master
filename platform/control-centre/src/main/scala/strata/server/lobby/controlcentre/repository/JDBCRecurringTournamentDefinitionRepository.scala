package strata.server.lobby.controlcentre.repository

import JDBCRecurringTournamentDefinitionRepository._
import mapper.RecurringTournamentDefinitionMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.{PreparedStatementCreator, JdbcTemplate}
import strata.server.lobby.controlcentre.model.RecurringTournamentDefinition
import org.apache.commons.lang3.Validate._
import scala._
import com.yazino.platform.model.PagedData
import org.springframework.stereotype.Repository
import java.sql.{PreparedStatement, Timestamp, Statement, Connection}
import scala.Int
import scala.Some
import org.joda.time.DateTime
import java.lang
import org.springframework.transaction.annotation.Transactional

object JDBCRecurringTournamentDefinitionRepository {
    private val SQL_SELECT = "SELECT * FROM RECURRING_TOURNAMENT_DEFINITION WHERE DEFINITION_ID=?"

    private val SQL_SELECT_ALL = "SELECT * FROM RECURRING_TOURNAMENT_DEFINITION"

    private val SQL_INSERT =
        """INSERT INTO RECURRING_TOURNAMENT_DEFINITION
             (TOURNAMENT_NAME,TOURNAMENT_DESCRIPTION,PARTNER_ID,INITIAL_SIGNUP_TIME,
              SIGNUP_PERIOD,FREQUENCY,TOURNAMENT_VARIATION_TEMPLATE_ID,ENABLED,EXCLUSION_PERIODS)
           VALUES (?,?,?,?,?,?,?,?,?)"""

    private val SQL_UPDATE =
        """UPDATE RECURRING_TOURNAMENT_DEFINITION
           SET TOURNAMENT_NAME=?,TOURNAMENT_DESCRIPTION=?,PARTNER_ID=?,INITIAL_SIGNUP_TIME=?,
               SIGNUP_PERIOD=?,FREQUENCY=?,TOURNAMENT_VARIATION_TEMPLATE_ID=?,ENABLED=?,
               EXCLUSION_PERIODS=?
           WHERE DEFINITION_ID=?"""
}

@Repository
class JDBCRecurringTournamentDefinitionRepository @Autowired()(val jdbcTemplate: JdbcTemplate)
    extends RecurringTournamentDefinitionRepository with MySQLPaging[RecurringTournamentDefinition] {

    private val definitionMapper = new RecurringTournamentDefinitionMapper()

    def findById(id: BigDecimal): Option[RecurringTournamentDefinition] = {
        notNull(id, "id may not be null")

        val definitions = jdbcTemplate.query(SQL_SELECT, definitionMapper, id.underlying())
        if (definitions != null && !definitions.isEmpty) {
            Some(definitions.get(0))
        } else {
            None
        }
    }

    @Transactional(readOnly = true)
    def findAll(page: Int = 0, pageSize: Int = 20): PagedData[RecurringTournamentDefinition] =
        selectWithPaging(jdbcTemplate, SQL_SELECT_ALL, definitionMapper, page, pageSize)

    def save(definition: RecurringTournamentDefinition): RecurringTournamentDefinition = {
        notNull(definition, "definition may not be null")

        if (definition.id == null) {
            val recurringTournamentDefinitionId = jdbcTemplate.execute(new PreparedStatementCreator {
                def createPreparedStatement(conn: Connection): PreparedStatement = {
                    val stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)
                    stmt.setString(1, definition.tournamentName)
                    stmt.setString(2, definition.tournamentDescription)
                    stmt.setString(3, definition.partnerId)
                    stmt.setTimestamp(4, asTimestamp(definition.initialSignupTime))
                    stmt.setLong(5, definition.signupPeriod)
                    stmt.setLong(6, definition.frequency)
                    stmt.setBigDecimal(7, definition.variationId.underlying())
                    stmt.setBoolean(8, definition.enabled)
                    stmt.setString(9, definition.exclusionPeriods.mkString(","))
                    stmt
                }
            }, new PrimaryKeyLoader())
            definition.withId(BigDecimal(recurringTournamentDefinitionId))

        } else {
            jdbcTemplate.update(SQL_UPDATE, definition.tournamentName,
                definition.tournamentDescription,
                definition.partnerId,
                asTimestamp(definition.initialSignupTime),
                new lang.Long(definition.signupPeriod),
                new lang.Long(definition.frequency),
                definition.variationId.underlying(),
                new lang.Boolean(definition.enabled),
                definition.exclusionPeriods.mkString(","),
                definition.id.underlying())
            definition
        }
    }

    private def asTimestamp(dateTime: DateTime) =
        if (dateTime == null) {
            null
        } else {
            new Timestamp(dateTime.getMillis)
        }
}
