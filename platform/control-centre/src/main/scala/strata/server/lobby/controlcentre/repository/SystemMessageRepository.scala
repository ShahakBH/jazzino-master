package strata.server.lobby.controlcentre.repository

import strata.server.lobby.controlcentre.model.SystemMessage
import scala._
import com.yazino.platform.model.PagedData

trait SystemMessageRepository {

    def findById(id: BigDecimal): Option[SystemMessage]

    def findAll(page: Int = 0, pageSize: Int = 20): PagedData[SystemMessage]

    def save(systemMessage: SystemMessage): SystemMessage

    def delete(id: BigDecimal): Unit
}
