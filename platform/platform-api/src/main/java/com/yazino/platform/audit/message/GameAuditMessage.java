package com.yazino.platform.audit.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


@JsonIgnoreProperties(ignoreUnknown = true)
public class GameAuditMessage extends GameAudit implements AuditMessage {
    private static final long serialVersionUID = 9022944648975236757L;

    public GameAuditMessage() {
    }

    public GameAuditMessage(final GameAudit gameAudit) {
        super(gameAudit.getAuditLabel(),
                gameAudit.getHostname(),
                gameAudit.getTimeStamp(),
                gameAudit.getTableId(),
                gameAudit.getGameId(),
                gameAudit.getIncrement(),
                gameAudit.getObservableStatusXml(),
                gameAudit.getInternalStatusXml(),
                gameAudit.getPlayerIds());
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public AuditMessageType getMessageType() {
        return AuditMessageType.GAME_AUDIT;
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
        final GameAuditMessage rhs = (GameAuditMessage) obj;
        return new EqualsBuilder()
                .append(getVersion(), rhs.getVersion())
                .appendSuper(super.equals(obj))
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(11, 17)
                .append(getVersion())
                .append(getMessageType())
                .appendSuper(super.hashCode())
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(getVersion())
                .append(getMessageType())
                .appendSuper(super.toString())
                .toString();
    }

}
