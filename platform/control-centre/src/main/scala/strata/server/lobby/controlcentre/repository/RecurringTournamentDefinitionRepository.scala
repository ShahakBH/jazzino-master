package strata.server.lobby.controlcentre.repository

import strata.server.lobby.controlcentre.model.RecurringTournamentDefinition
import com.yazino.platform.model.PagedData

trait RecurringTournamentDefinitionRepository {

    def findById(id: BigDecimal): Option[RecurringTournamentDefinition]

    def findAll(page: Int = 0, pageSize: Int = 20): PagedData[RecurringTournamentDefinition]

    def save(definition: RecurringTournamentDefinition): RecurringTournamentDefinition

}
