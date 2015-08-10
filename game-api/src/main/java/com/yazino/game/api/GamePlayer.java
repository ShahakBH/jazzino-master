package com.yazino.game.api;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.document.DocumentAccessors;
import com.yazino.game.api.document.DocumentBuilder;
import com.yazino.game.api.document.Documentable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

public final class GamePlayer implements Serializable, Documentable {
    private static final long serialVersionUID = 7578338369078478484L;

    private final BigDecimal playerId;
    private final BigDecimal sessionId;
    private final String name;
    private final String auditIdentifier;

    public GamePlayer(final BigDecimal playerId,
                      final BigDecimal sessionId,
                      final String name) {
        notNull(playerId, "Player ID may not be null");

        this.playerId = playerId;
        this.sessionId = sessionId;
        this.name = name;
        this.auditIdentifier = buildAuditString(name, playerId);
    }

    public GamePlayer(final Map<String, Object> document) {
        notNull(document, "document may not be null");

        this.playerId = DocumentAccessors.bigDecimalFor(document, "playerId");
        this.sessionId = DocumentAccessors.bigDecimalFor(document, "sessionId");
        this.name = DocumentAccessors.stringFor(document, "name");
        this.auditIdentifier = DocumentAccessors.stringFor(document, "auditIdentifier");
    }

    public static GamePlayer fromDocument(final Map<String, Object> document) {
        if (document == null || document.isEmpty()) {
            return null;
        }
        return new GamePlayer(document);
    }

    @Override
    public Map<String, Object> toDocument() {
        return new DocumentBuilder()
                .withBigDecimal("playerId", playerId)
                .withBigDecimal("sessionId", sessionId)
                .withString("name", name)
                .withString("auditIdentifier", auditIdentifier)
                .toDocument();
    }

    public BigDecimal getId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getSessionId() {
        return sessionId;
    }

    public String toAuditString() {
        return auditIdentifier;
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
        final GamePlayer rhs = (GamePlayer) obj;
        return new EqualsBuilder()
                .append(playerId, rhs.playerId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 23)
                .append(playerId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(name)
                .toString();
    }

    private static String buildAuditString(final String name,
                                           final BigDecimal id) {
        final StringBuilder builder = new StringBuilder();
        if (name == null) {
            builder.append("UNKNOWN");
        } else {
            builder.append(name);
        }
        builder.append(" (").append(id).append(")");
        return builder.toString();
    }

}
