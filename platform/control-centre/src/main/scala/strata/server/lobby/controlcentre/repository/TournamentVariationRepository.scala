package strata.server.lobby.controlcentre.repository

import strata.server.lobby.controlcentre.model.TournamentVariation
import scala._
import com.yazino.platform.model.PagedData

trait TournamentVariationRepository {

    def findById(id: BigDecimal): Option[TournamentVariation]

    def findAll(page: Int = 0, pageSize: Int = 20): PagedData[TournamentVariation]

    def save(variation: TournamentVariation): TournamentVariation

    def delete(id: BigDecimal): Unit

    def list(): Map[BigDecimal, String]
}
