package com.yazino.platform.tournament;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

public class TournamentMonitorView implements Serializable {
    private static final long serialVersionUID = -6776209845746838087L;

    private final BigDecimal id;
    private final String name;
    private final String gameType;
    private final String templateName;
    private final TournamentStatus status;
    private final String monitoringMessage;

    public TournamentMonitorView(final BigDecimal id,
                                 final String name,
                                 final String gameType,
                                 final String templateName,
                                 final TournamentStatus status,
                                 final String monitoringMessage) {
        notNull(id, "id may not be null");

        this.id = id;
        this.name = name;
        this.gameType = gameType;
        this.templateName = templateName;
        this.status = status;
        this.monitoringMessage = monitoringMessage;
    }


    public BigDecimal getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGameType() {
        return gameType;
    }

    public String getTemplateName() {
        return templateName;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public String getMonitoringMessage() {
        return monitoringMessage;
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
        final TournamentMonitorView rhs = (TournamentMonitorView) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(name, rhs.name)
                .append(gameType, rhs.gameType)
                .append(templateName, rhs.templateName)
                .append(status, rhs.status)
                .append(monitoringMessage, rhs.monitoringMessage)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(id)
                .append(name)
                .append(gameType)
                .append(templateName)
                .append(status)
                .append(monitoringMessage)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(id)
                .append(name)
                .append(gameType)
                .append(templateName)
                .append(status)
                .append(monitoringMessage)
                .toString();
    }
}
