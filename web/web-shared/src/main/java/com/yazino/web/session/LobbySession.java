package com.yazino.web.session;

import com.yazino.platform.AuthProvider;
import com.yazino.platform.Partner;
import com.yazino.platform.Platform;
import com.yazino.platform.session.Session;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class LobbySession implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String localSessionKey;
    private final Partner partnerId;
    private final BigDecimal playerId;
    private final BigDecimal sessionId;
    private final String playerName;
    private final String pictureUrl;
    private final String email;
    private final Platform platform;
    private final AuthProvider authProvider;
    private final Set<String> tags = new HashSet<>();
    // TODO remove if only use by 'old' topup, after mobile
    private final boolean newSession;

    public LobbySession(final BigDecimal sessionId,
                        final BigDecimal playerId,
                        final String playerName,
                        final String senetSessionKey,
                        final Partner partnerId,
                        final String pictureUrl,
                        final String email,
                        final Collection<String> tags,
                        final boolean newSession,
                        final Platform platform,
                        final AuthProvider authProvider) {
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.localSessionKey = senetSessionKey;
        this.partnerId = partnerId;
        this.pictureUrl = pictureUrl;
        this.email = email;
        this.newSession = newSession;
        this.platform = platform;
        this.authProvider = authProvider;
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    public static LobbySession forSession(final Session summary,
                                          final boolean newSession,
                                          final Platform platform,
                                          final AuthProvider authProvider) {
        if (summary == null) {
            return null;
        }

        return new LobbySession(summary.getSessionId(), summary.getPlayerId(), summary.getNickname(),
                summary.getLocalSessionKey(), summary.getPartnerId(), summary.getPictureUrl(),
                summary.getEmail(), summary.getTags(), newSession, platform,
                authProvider);
    }

    public String getLocalSessionKey() {
        return localSessionKey;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public String getEmail() {
        return email;
    }

    public boolean isNewSession() {
        return newSession;
    }

    public Platform getPlatform() {
        return platform;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public Set<String> getTags() {
        return tags;
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
        final LobbySession rhs = (LobbySession) obj;
        return new EqualsBuilder()
                .append(sessionId, rhs.sessionId)
                .append(localSessionKey, rhs.localSessionKey)
                .append(partnerId, rhs.partnerId)
                .append(playerId, rhs.playerId)
                .append(playerName, rhs.playerName)
                .append(email, rhs.email)
                .append(platform, rhs.platform)
                .append(playerName, rhs.playerName)
                .append(tags, rhs.tags)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(sessionId)
                .append(localSessionKey)
                .append(partnerId)
                .append(playerId)
                .append(playerName)
                .append(email)
                .append(platform)
                .append(playerName)
                .append(tags)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(sessionId)
                .append(localSessionKey)
                .append(partnerId)
                .append(playerId)
                .append(playerName)
                .append(email)
                .append(pictureUrl)
                .append(newSession)
                .append(platform)
                .append(playerName)
                .append(tags)
                .toString();
    }

    public Partner getPartnerId() {
        return partnerId;
    }
}
