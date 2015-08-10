package strata.server.lobby.controlcentre.repository

import mapper.TournamentMapper
import org.springframework.stereotype.Repository
import strata.server.lobby.controlcentre.model.Tournament
import org.springframework.beans.factory.annotation.Autowired
import com.yazino.platform.model.PagedData
import org.apache.commons.lang3.Validate.notNull
import org.springframework.jdbc.core.JdbcTemplate
import JDBCTournamentRepository._
import org.springframework.transaction.annotation.Transactional

object JDBCTournamentRepository {
    private val SQL_SELECT = "SELECT * FROM TOURNAMENT WHERE TOURNAMENT_ID=?"

    private val SQL_SELECT_ALL = "SELECT * FROM TOURNAMENT ORDER BY TOURNAMENT_START_TS DESC"
}

@Repository
class JDBCTournamentRepository @Autowired()(val jdbcTemplate: JdbcTemplate) extends TournamentRepository with MySQLPaging[Tournament] {

    private val tournamentMapper = new TournamentMapper()

    def findById(id: BigDecimal): Option[Tournament] = {
        notNull(id, "id may not be null")

        val tournaments = jdbcTemplate.query(SQL_SELECT, tournamentMapper, id.underlying())
        if (tournaments != null && !tournaments.isEmpty) {
            Some(tournaments.get(0))
        } else {
            None
        }
    }

    @Transactional(readOnly = true)
    def findAll(page: Int = 0, pageSize: Int = 20): PagedData[Tournament] =
        selectWithPaging(jdbcTemplate, SQL_SELECT_ALL, tournamentMapper, page, pageSize)

}
