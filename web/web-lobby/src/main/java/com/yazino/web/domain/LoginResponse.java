package com.yazino.web.domain;

import com.google.common.base.Optional;
import com.yazino.platform.player.LoginResult;
import com.yazino.web.session.LobbySession;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

import static org.apache.commons.lang3.Validate.notNull;

public class LoginResponse implements Serializable {
    private static final long serialVersionUID = -2742832154783501816L;

    private final LoginResult result;
    private final LobbySession session;

    public LoginResponse(final LoginResult result) {
        this(result, null);
    }

    public LoginResponse(final LoginResult result,
                         final LobbySession session) {
        notNull(result, "result may not be null");
        if (result == LoginResult.NEW_USER || result == LoginResult.EXISTING_USER) {
            notNull(session, "session may not be null when result is new/existing user");
        }

        this.result = result;
        this.session = session;
    }

    public LoginResult getResult() {
        return result;
    }

    public Optional<LobbySession> getSession() {
        return Optional.fromNullable(session);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final LoginResponse rhs = (LoginResponse) obj;
        return new EqualsBuilder()
                .append(result, rhs.result)
                .append(session, rhs.session)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(result)
                .append(session)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(result)
                .append(session)
                .toString();
    }
}
