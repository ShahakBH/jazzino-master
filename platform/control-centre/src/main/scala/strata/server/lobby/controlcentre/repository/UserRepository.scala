package strata.server.lobby.controlcentre.repository

import strata.server.lobby.controlcentre.model.User
import scala._
import com.yazino.platform.model.PagedData

trait UserRepository {

    def findById(userName: String): Option[User]

    def findAll(page: Int = 0, pageSize: Int = 20): PagedData[User]

    def save(user: User): User

    def findAllRoles: Set[String]

    def delete(userName: String): Unit

}
