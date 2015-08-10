package strata.server.lobby.controlcentre.repository.mapper

import strata.server.lobby.controlcentre.model.SystemMessage
import java.sql.ResultSet
import org.springframework.jdbc.core.RowMapper
import java.util
import org.joda.time.DateTime

class SystemMessageMapper extends RowMapper[SystemMessage] {

    def mapRow(rs: ResultSet, rowNum: Int): SystemMessage =
        new SystemMessage(BigDecimal(rs.getBigDecimal("SYSTEM_MESSAGE_ID").toPlainString),
            rs.getString("MESSAGE"),
            asDateTime(rs.getTimestamp("VALID_FROM")),
            asDateTime(rs.getTimestamp("VALID_TO")))

    private def asDateTime(date: util.Date) =
        if (date == null) {
            null
        } else {
            new DateTime(date.getTime)
        }

}
