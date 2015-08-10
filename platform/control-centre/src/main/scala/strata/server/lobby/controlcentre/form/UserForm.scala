package strata.server.lobby.controlcentre.form

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import org.apache.commons.lang3.ObjectUtils.defaultIfNull
import scala.collection.JavaConversions._
import scala.beans.BeanProperty
import strata.server.lobby.controlcentre.model.User
import java.util
import util.Collections.emptyList

class UserForm(@BeanProperty var userName: String,
               @BeanProperty var password: String,
               @BeanProperty var realName: String,
               @BeanProperty var roles: util.List[String]) {

    @BeanProperty var isNew: Boolean = true
    @BeanProperty var confirmPassword: String = null

    def this() {
        this(null, null, null, new util.ArrayList[String]())
    }

    def this(user: User) {
        this(user.userName, user.password, user.realName, new util.ArrayList(user.roles))
        isNew = false
    }

    def toUser: User =
        new User(userName, password, realName, defaultIfNull(roles, emptyList).toSet)

    override def toString: String = new ToStringBuilder(this)
        .append(userName)
        .append(password)
        .append(realName)
        .append(roles)
        .append(isNew)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: UserForm => other.getClass == getClass &&
            new EqualsBuilder()
                .append(userName, other.userName)
                .append(password, other.password)
                .append(realName, other.realName)
                .append(roles, other.roles)
                .append(isNew, other.isNew)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(userName)
        .append(password)
        .append(realName)
        .append(roles)
        .append(isNew)
        .hashCode

}
