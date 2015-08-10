package strata.server.lobby.controlcentre.repository.mapper

import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import strata.server.lobby.controlcentre.model.GameVariation
class GameVariationMapper extends RowMapper[GameVariation] {

    def mapRow(rs: ResultSet, rowNum: Int): GameVariation = new GameVariation(
        rs.getBigDecimal("GAME_VARIATION_TEMPLATE_ID"),
        rs.getString("GAME_TYPE"),
        rs.getString("NAME"),
        List())

}
