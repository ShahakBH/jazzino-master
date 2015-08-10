package strata.server.lobby.controlcentre.repository

import strata.server.lobby.controlcentre.model.Tournament
import com.yazino.platform.model.PagedData

trait TournamentRepository {

    def findById(id: BigDecimal): Option[Tournament]

    def findAll(page: Int = 0, pageSize: Int = 20): PagedData[Tournament]

}
