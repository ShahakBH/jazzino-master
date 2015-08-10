package strata.server.lobby.controlcentre.repository.mapper

import strata.server.lobby.controlcentre.model.GameVariation
import com.yazino.platform.model.PagedData

trait GameVariationRepository {
    def listFor(gameType: String): Map[BigDecimal, String]

    def findById(id: BigDecimal): Option[GameVariation]

    def findAll(page: Int = 0, pageSize: Int = 20): PagedData[GameVariation]

    def save(gameVariation: GameVariation): GameVariation

    def delete(gameVariationId: BigDecimal): Unit
}
