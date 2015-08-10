package strata.server.lobby.controlcentre.repository.mapper

import strata.server.lobby.controlcentre.model.TournamentVariationPayout
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.math.{BigDecimal => JavaBigDecimal}
import scala._
import scala.Int

class TournamentVariationPayoutMapper extends RowMapper[TournamentVariationPayout] {

    def mapRow(rs: ResultSet, rowNum: Int): TournamentVariationPayout =
        new TournamentVariationPayout(
            asBigDecimal(rs.getBigDecimal("TOURNAMENT_VARIATION_PAYOUT_ID")),
            rs.getInt("RANK"),
            asBigDecimal(rs.getBigDecimal("PAYOUT")))

    private def asBigDecimal(value: JavaBigDecimal) = if (value != null) BigDecimal(value) else null

}
