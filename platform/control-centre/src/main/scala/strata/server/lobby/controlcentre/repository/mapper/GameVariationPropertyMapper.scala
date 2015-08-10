package strata.server.lobby.controlcentre.repository.mapper

import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import strata.server.lobby.controlcentre.model.GameVariationProperty

class GameVariationPropertyMapper extends RowMapper[GameVariationProperty] {

    def mapRow(rs: ResultSet, rowNum: Int): GameVariationProperty = new GameVariationProperty(
        rs.getBigDecimal("GAME_VARIATION_TEMPLATE_PROPERTY_ID"),
        rs.getString("NAME"),
        rs.getString("VALUE"))

}
