package com.yazino.web.session;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

import static org.apache.commons.lang3.Validate.notNull;

public class LobbySessionCreationResponse implements Serializable {
    private static final long serialVersionUID = 4299954039660263029L;
    private LobbySession lobbySession;
    private ReferralResult referralResult;
    private final boolean newPlayer;

    public LobbySessionCreationResponse(final LobbySession lobbySession,
                                        final boolean newPlayer,
                                        final ReferralResult referralResult) {
        notNull(lobbySession, "lobbySession not null");
        this.lobbySession = lobbySession;
        this.referralResult = referralResult;
        this.newPlayer = newPlayer;
    }

    public boolean isNewPlayer() {
        return newPlayer;
    }

    public LobbySession getLobbySession() {
        return lobbySession;
    }

    public ReferralResult getReferralResult() {
        return referralResult;
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
        final LobbySessionCreationResponse rhs = (LobbySessionCreationResponse) obj;
        return new EqualsBuilder()
                .append(lobbySession, rhs.lobbySession)
                .append(newPlayer, rhs.newPlayer)
                .append(referralResult, rhs.referralResult)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(lobbySession)
                .append(newPlayer)
                .append(referralResult)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(lobbySession)
                .append(newPlayer)
                .append(referralResult)
                .toString();
    }
}
