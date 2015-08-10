package strata.server.lobby.controlcentre.repository.mapper

import strata.server.lobby.controlcentre.model.RecurringTournamentDefinition
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import org.joda.time.DateTime
import java.math.{BigDecimal => JavaBigDecimal}
import scala._
import scala.Int
import java.util
import com.yazino.platform.tournament.DayPeriod
import org.apache.commons.lang3.StringUtils

class RecurringTournamentDefinitionMapper extends RowMapper[RecurringTournamentDefinition] {

    def mapRow(rs: ResultSet, rowNum: Int): RecurringTournamentDefinition =
        new RecurringTournamentDefinition(asBigDecimal(rs.getBigDecimal("DEFINITION_ID")),
            rs.getString("TOURNAMENT_NAME"),
            rs.getString("TOURNAMENT_DESCRIPTION"),
            rs.getString("PARTNER_ID"),
            asDateTime(rs.getTimestamp("INITIAL_SIGNUP_TIME")),
            rs.getLong("SIGNUP_PERIOD"),
            rs.getLong("FREQUENCY"),
            asBigDecimal(rs.getBigDecimal("TOURNAMENT_VARIATION_TEMPLATE_ID")),
            rs.getBoolean("ENABLED"),
            asListOfDayPeriods(rs.getString("EXCLUSION_PERIODS")))

    private def asListOfDayPeriods(dayPeriods: String): Seq[DayPeriod] = {
        if (StringUtils.isNotBlank(dayPeriods)) {
            dayPeriods.split(",").map(new DayPeriod(_))
        } else {
            Seq()
        }
    }

    private def asDateTime(value: util.Date) = if (value != null) new DateTime(value.getTime) else null

    private def asBigDecimal(value: JavaBigDecimal) = if (value != null) BigDecimal(value) else null

}
