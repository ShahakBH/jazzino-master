package strata.server.lobby.controlcentre.repository.mapper

import org.springframework.jdbc.core.RowMapper
import scala.collection.JavaConversions._
import java.sql.ResultSet
import com.yazino.platform.table.GameConfiguration

class GameConfigurationMapper extends RowMapper[GameConfiguration] {

    def mapRow(rs: ResultSet, rowNum: Int): GameConfiguration = {
        var dbAliases: String = rs.getString("ALIASES")
        if (dbAliases == null) {
            dbAliases = ""
        }
        val aliases = dbAliases.split(",").filter {
            _.length > 0
        }
        new GameConfiguration(rs.getString("GAME_ID"),
            rs.getString("SHORT_NAME"),
            rs.getString("DISPLAY_NAME"),
            Set(aliases: _*),
            rs.getInt("ORD"))
    }
}
