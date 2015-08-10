package com.yazino.bi.operations.model.security;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

/**
 * Command object used to manage the user access rights
 */
public class UserManagementCommand {
    private List<UserAccess> users;
    private UserAccess activeUser;
    private String errorMessage;
    private String req;
    private String activeId;

    public UserAccess getActiveUser() {
        return activeUser;
    }

    public void setActiveUser(final UserAccess activeUser) {
        this.activeUser = activeUser;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("users", users).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof UserManagementCommand)) {
            return false;
        }
        final UserManagementCommand castOther = (UserManagementCommand) other;
        return new EqualsBuilder().append(users, castOther.users).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(users).toHashCode();
    }

    public List<UserAccess> getUsers() {
        return users;
    }

    public void setUsers(final List<UserAccess> users) {
        this.users = users;
    }

    public String getReq() {
        return req;
    }

    public void setReq(final String request) {
        this.req = request;
    }

    public String getActiveId() {
        return activeId;
    }

    public void setActiveId(final String activeId) {
        this.activeId = activeId;
    }
}
