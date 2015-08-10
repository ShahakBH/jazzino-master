package com.yazino.platform.audit.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CommandAuditMessage implements AuditMessage {
    private static final long serialVersionUID = -6000288672430160805L;

    @JsonProperty("cmds")
    private List<CommandAudit> commands;

    public CommandAuditMessage() {
    }

    public CommandAuditMessage(final List<CommandAudit> commands) {
        notNull(commands, "commands may not be null");

        this.commands = commands;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public AuditMessageType getMessageType() {
        return AuditMessageType.COMMAND_AUDIT;
    }

    public List<CommandAudit> getCommands() {
        return commands;
    }

    public void setCommands(final List<CommandAudit> commands) {
        this.commands = commands;
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
        final CommandAuditMessage rhs = (CommandAuditMessage) obj;
        return new EqualsBuilder()
                .append(commands, rhs.commands)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(commands)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(commands)
                .toString();
    }

}
