package com.yazino.game.api;

import com.yazino.game.api.document.DocumentBuilder;
import com.yazino.game.api.document.Documentable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.yazino.game.api.document.DocumentAccessors.*;
import static org.apache.commons.lang3.Validate.notNull;

public final class ScheduledEvent implements Serializable, Documentable {
    private static final long serialVersionUID = -6791854182523009628L;

    public static final String NO_OP_EVENT = "NoPEvent";
    public static final long NOP_TIMEOUT = 500;

    private final long delayInMillis;
    private final long gameId;
    private final String eventClassName;
    private final String eventSimpleName;
    private final Map<String, String> properties;
    private final boolean clientVisible;
    private boolean noProcessingEvent;

    public ScheduledEvent(final long delayInMillis,
                          final long gameId,
                          final String eventClassName,
                          final String eventSimpleName,
                          final Map<String, String> properties,
                          final boolean isClientVisible) {
        this.delayInMillis = delayInMillis;
        this.gameId = gameId;
        this.eventClassName = eventClassName;
        this.eventSimpleName = eventSimpleName;
        this.properties = properties;
        this.clientVisible = isClientVisible;
        this.noProcessingEvent = false;
    }

    @SuppressWarnings("unchecked")
    public ScheduledEvent(final Map<String, Object> document) {
        notNull(document, "document may not be null");

        delayInMillis = longFor(document, "delayInMillis");
        gameId = longFor(document, "gameId");
        eventClassName = stringFor(document, "eventClassName");
        eventSimpleName = stringFor(document, "eventSimpleName");
        properties = (Map<String, String>) document.get("properties");
        clientVisible = booleanFor(document, "clientVisible");
        noProcessingEvent = booleanFor(document, "noProcessingEvent");
    }

    public static ScheduledEvent fromDocument(final Map<String, Object> document) {
        if (document == null || document.isEmpty()) {
            return null;
        }
        return new ScheduledEvent(document);
    }

    @Override
    public Map<String, Object> toDocument() {
        return new DocumentBuilder()
                .withLong("delayInMillis", delayInMillis)
                .withLong("gameId", gameId)
                .withString("eventClassName", eventClassName)
                .withString("eventSimpleName", eventSimpleName)
                .withPrimitiveMapOf("properties", properties)
                .withBoolean("clientVisible", clientVisible)
                .withBoolean("noProcessingEvent", noProcessingEvent)
                .toDocument();
    }

    public long getDelayInMillis() {
        return delayInMillis;
    }

    public long getGameId() {
        return gameId;
    }

    public String getEventClassName() {
        return eventClassName;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public boolean isClientVisible() {
        return clientVisible;
    }

    public String getEventSimpleName() {
        return eventSimpleName;
    }

    public boolean isNoProcessingEvent() {
        return noProcessingEvent;
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
        final ScheduledEvent rhs = (ScheduledEvent) obj;
        return new EqualsBuilder()
                .append(delayInMillis, rhs.delayInMillis)
                .append(gameId, rhs.gameId)
                .append(eventClassName, rhs.eventClassName)
                .append(eventSimpleName, rhs.eventSimpleName)
                .append(properties, rhs.properties)
                .append(clientVisible, rhs.clientVisible)
                .append(noProcessingEvent, rhs.noProcessingEvent)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(delayInMillis)
                .append(gameId)
                .append(eventClassName)
                .append(eventSimpleName)
                .append(properties)
                .append(clientVisible)
                .append(noProcessingEvent)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(delayInMillis)
                .append(gameId)
                .append(eventClassName)
                .append(eventSimpleName)
                .append(properties)
                .append(clientVisible)
                .append(noProcessingEvent)
                .toString();
    }

    public static ScheduledEvent noProcessingEvent(final long gameId) {
        final ScheduledEvent event = new ScheduledEvent(ScheduledEvent.NOP_TIMEOUT, gameId,
                null, ScheduledEvent.NO_OP_EVENT, new HashMap<String, String>(), true);
        event.noProcessingEvent = true;
        return event;
    }

}
