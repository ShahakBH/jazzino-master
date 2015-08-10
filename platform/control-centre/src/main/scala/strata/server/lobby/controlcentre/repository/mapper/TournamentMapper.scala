package strata.server.lobby.controlcentre.repository.mapper

import strata.server.lobby.controlcentre.model.Tournament
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import scala._
import scala.Int
import com.yazino.platform.tournament.TournamentStatus
import java.math.{BigDecimal => JavaBigDecimal}
import org.joda.time.DateTime
import java.util
import com.yazino.platform.util.BigDecimals

class TournamentMapper extends RowMapper[Tournament] {

    def mapRow(rs: ResultSet, rowNum: Int): Tournament = new Tournament(
        asBigDecimal(BigDecimals.strip(rs.getBigDecimal("TOURNAMENT_ID"))),
        rs.getString("TOURNAMENT_NAME"),
        asBigDecimal(rs.getBigDecimal("TOURNAMENT_VARIATION_TEMPLATE_ID")),
        asDateTime(rs.getTimestamp("TOURNAMENT_SIGNUP_START_TS")),
        asDateTime(rs.getTimestamp("TOURNAMENT_SIGNUP_END_TS")),
        asDateTime(rs.getTimestamp("TOURNAMENT_START_TS")),
        TournamentStatus.getById(rs.getString("TOURNAMENT_STATUS")),
        rs.getString("PARTNER_ID"),
        rs.getString("TOURNAMENT_DESCRIPTION")
    )

    private def asDateTime(value: util.Date) = if (value != null) new DateTime(value.getTime) else null

    private def asBigDecimal(value: JavaBigDecimal) = if (value != null) BigDecimal(value) else null

}
