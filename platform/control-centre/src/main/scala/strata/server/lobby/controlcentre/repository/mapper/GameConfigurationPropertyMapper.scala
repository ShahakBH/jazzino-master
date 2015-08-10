package strata.server.lobby.controlcentre.repository.mapper

import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import com.yazino.platform.table.GameConfigurationProperty

class GameConfigurationPropertyMapper extends RowMapper[GameConfigurationProperty] {

    def mapRow(rs: ResultSet, rowNum: Int): GameConfigurationProperty = new GameConfigurationProperty(
        rs.getBigDecimal("GAME_PROPERTY_ID"),
        rs.getString("GAME_ID"),
        rs.getString("PROPERTY_NAME"),
        rs.getString("PROPERTY_VALUE"))
}
