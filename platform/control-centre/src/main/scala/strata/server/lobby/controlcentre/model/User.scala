package strata.server.lobby.controlcentre.model

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}


class User(val userName: String,
           val password: String,
           val realName: String,
           val roles: Iterable[String]) {

    override def toString: String = new ToStringBuilder(this)
        .append(userName)
        .append(password)
        .append(realName)
        .append(roles)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: User => other.getClass == getClass &&
            new EqualsBuilder()
                .append(userName, other.userName)
                .append(password, other.password)
                .append(realName, other.realName)
                .append(roles, other.roles)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(userName)
        .append(password)
        .append(realName)
        .append(roles)
        .hashCode
}
