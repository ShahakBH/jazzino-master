package com.yazino.bi.operations.security;

import com.yazino.platform.Platform;

import java.util.Set;

/**
 * Information about the current user, used in the operations menus
 */
public class UserInformationCommand {
    private Set<String> roles;
    private Set<Platform> platforms;
    private String username;
    private String version;

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(final Set<String> roles) {
        this.roles = roles;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public Set<Platform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(final Set<Platform> platforms) {
        this.platforms = platforms;
    }
}
