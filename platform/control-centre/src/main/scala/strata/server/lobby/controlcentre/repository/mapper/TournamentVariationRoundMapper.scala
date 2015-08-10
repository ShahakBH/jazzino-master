package strata.server.lobby.controlcentre.repository.mapper

import strata.server.lobby.controlcentre.model.TournamentVariationRound
import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.math.{BigDecimal => JavaBigDecimal}

class TournamentVariationRoundMapper extends RowMapper[TournamentVariationRound] {
    def mapRow(rs: ResultSet, rowNum: Int): TournamentVariationRound =
        new TournamentVariationRound(
            asBigDecimal(rs.getBigDecimal("TOURNAMENT_VARIATION_ROUND_ID")),
            rs.getInt("ROUND_NUMBER"),
            rs.getInt("ROUND_END_INTERVAL"),
            rs.getInt("ROUND_LENGTH"),
            asBigDecimal(rs.getBigDecimal("GAME_VARIATION_TEMPLATE_ID")),
            rs.getString("CLIENT_PROPERTIES_ID"),
            asBigDecimal(rs.getBigDecimal("MINIMUM_BALANCE")),
            rs.getString("DESCRIPTION"))

    private def asBigDecimal(value: JavaBigDecimal) = if (value != null) BigDecimal(value) else null

}
