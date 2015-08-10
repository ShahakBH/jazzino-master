package com.yazino.bi.operations.security;

import com.yazino.platform.Platform;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * User access data
 */
public class UserAccess {
    private String userName;
    private String password;
    private String realName;
    private Set<UserRole> roles = new HashSet<UserRole>();
    private Set<Platform> platforms = new HashSet<Platform>();

    public Set<Platform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(final Set<Platform> platforms) {
        this.platforms = platforms;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("userName", userName).append("password", password)
                .append("realName", realName).append("roles", roles).toString();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        if (password == null) {
            return "";
        } else {
            return password;
        }
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(final String realName) {
        this.realName = realName;
    }

    public Set<UserRole> getRoles() {
        if (roles == null) {
            return new HashSet<UserRole>();
        } else {
            return roles;
        }
    }

    public void setRoles(final Set<UserRole> roles) {
        this.roles = roles;
    }
}
