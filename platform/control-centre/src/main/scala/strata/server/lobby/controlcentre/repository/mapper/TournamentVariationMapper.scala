package strata.server.lobby.controlcentre.repository.mapper

import org.springframework.jdbc.core.RowMapper
import strata.server.lobby.controlcentre.model.{Allocator, TournamentVariation}
import java.sql.ResultSet
import com.yazino.platform.tournament.TournamentType
import java.math.{BigDecimal => JavaBigDecimal}
import scala._
import scala.Int

class TournamentVariationMapper extends RowMapper[TournamentVariation] {

    def mapRow(rs: ResultSet, rowNum: Int): TournamentVariation =
        new TournamentVariation(asBigDecimal(rs.getBigDecimal("TOURNAMENT_VARIATION_TEMPLATE_ID")),
            TournamentType.valueOf(rs.getString("TOURNAMENT_TYPE")),
            rs.getString("NAME"),
            asBigDecimal(rs.getBigDecimal("ENTRY_FEE")),
            asBigDecimal(rs.getBigDecimal("SERVICE_FEE")),
            asBigDecimal(rs.getBigDecimal("STARTING_CHIPS")),
            rs.getInt("MIN_PLAYERS"),
            rs.getInt("MAX_PLAYERS"),
            rs.getString("GAME_TYPE"),
            rs.getInt("EXPIRY_DELAY"),
            asBigDecimal(rs.getBigDecimal("PRIZE_POOL")),
            Allocator.valueOf(rs.getString("ALLOCATOR")),
            List(),
            List())

    private def asBigDecimal(value: JavaBigDecimal) = if (value != null) BigDecimal(value) else null

}
