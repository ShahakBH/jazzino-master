package com.yazino.platform.model.table;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.yazino.game.api.Command;

@SpaceClass(replicate = false)
public final class CommandAuditWrapper {
    private Command command;
    private String spaceId;
    private AuditContext auditContext;

    public AuditContext getAuditContext() {
        return auditContext;
    }

    public void setAuditContext(final AuditContext auditContext) {
        this.auditContext = auditContext;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(final Command command) {
        this.command = command;
    }

    @SpaceId(autoGenerate = true)
    @SpaceRouting
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    /**
     * required for gs
     */
    public CommandAuditWrapper() {
    }

    public CommandAuditWrapper(final Command command,
                               final AuditContext auditContext) {
        this.command = command;
        this.auditContext = auditContext;
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
        final CommandAuditWrapper rhs = (CommandAuditWrapper) obj;
        return new EqualsBuilder()
                .append(spaceId, rhs.spaceId)
                .append(command, rhs.command)
                .append(auditContext, rhs.auditContext)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(spaceId)
                .append(command)
                .append(auditContext)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(spaceId)
                .append(command)
                .append(auditContext)
                .toString();
    }
}
