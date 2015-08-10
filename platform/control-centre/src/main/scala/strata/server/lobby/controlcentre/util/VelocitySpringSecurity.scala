package strata.server.lobby.controlcentre.util

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.context.SecurityContextHolder
import collection.JavaConversions._
import collection.mutable

class VelocitySpringSecurity {

    def principal: String =
        currentPrincipal map {_.getUsername} getOrElse "guest"

    def anyGranted(roles: String): Boolean =
        !(currentRoles & roles.split(" ").toSet).isEmpty

    private def currentPrincipal = {
        val principal = SecurityContextHolder.getContext.getAuthentication.getPrincipal
        if (principal.isInstanceOf[UserDetails]) {
            Some(principal.asInstanceOf[UserDetails])
        } else {
            None
        }
    }

    private def currentRoles = {
        var roles = new mutable.HashSet[String]
        currentPrincipal.foreach { _.getAuthorities.foreach { grantedAuthority => roles += grantedAuthority.getAuthority} }
        roles
    }

}
