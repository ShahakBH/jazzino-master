package com.yazino.platform.model.session;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.yazino.platform.audit.message.SessionKey;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

@SpaceClass
public class SessionKeyPersistenceRequest implements Serializable {
    private static final long serialVersionUID = -8604354244308591067L;

    private String spaceId;
    private SessionKey sessionKey;

    public SessionKeyPersistenceRequest() {
    }

    public SessionKeyPersistenceRequest(final SessionKey sessionKey) {
        this.sessionKey = sessionKey;
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    public SessionKey getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(final SessionKey sessionKey) {
        this.sessionKey = sessionKey;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SessionKeyPersistenceRequest that = (SessionKeyPersistenceRequest) o;
        return new EqualsBuilder()
                .append(sessionKey, that.sessionKey)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(121, 4537)
                .append(sessionKey)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(sessionKey)
                .toString();
    }
}
