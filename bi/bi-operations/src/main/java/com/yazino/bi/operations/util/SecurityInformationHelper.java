package com.yazino.bi.operations.util;

import com.yazino.platform.Platform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.yazino.bi.operations.persistence.UserAdministrationDao;

import java.util.HashSet;
import java.util.Set;

/**
 * Helper class access to the security information
 */
@Component
public class SecurityInformationHelper {

    private final UserAdministrationDao dao;

    @Autowired(required = true)
    public SecurityInformationHelper(final UserAdministrationDao dao) {
        this.dao = dao;
    }

    /**
     * Gets the list of the current user's access rights
     *
     * @return List of authority roles as strings
     */
    public Set<String> getAccessRoles() {
        final Set<String> authorities = new HashSet<String>();
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return authorities;
        }
        for (final GrantedAuthority role : authentication.getAuthorities()) {
            authorities.add(role.getAuthority());
        }
        return authorities;
    }

    /**
     * Gets the current user's name
     *
     * @return Username as a string
     */
    public String getCurrentUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "";
        }
        return authentication.getName();
    }

    public Set<Platform> getAccessiblePlatforms() {
        return dao.getPlatformsForUser(getCurrentUser());
    }
}
