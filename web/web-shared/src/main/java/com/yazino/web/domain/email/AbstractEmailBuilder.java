package com.yazino.web.domain.email;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * An abstract {@link EmailBuilder} useful for storing both template and temporary
 * properties that may be required whilst building the request.
 */
public abstract class AbstractEmailBuilder implements EmailBuilder {

    private final Map<String, Object> templateProperties = new HashMap<>();
    private final Map<String, Object> otherProperties = new HashMap<>();
    private BigDecimal playerId;

    public AbstractEmailBuilder setTemplateProperty(final String key, final Object value) {
        templateProperties.put(key, value);
        return this;
    }

    protected String formattedEmailWithName(final String displayName, final String sender) {
        if (sender.indexOf("<") > 0) {
            final String email = sender.split("<")[1];
            final int end;
            if (email.lastIndexOf(">") > 0) {
                end = email.lastIndexOf(">");
            } else {
                end = email.length();
            }
            return String.format("%s <%s>", displayName, email.substring(0, end));

        }
        return String.format("%s <%s>", displayName, sender);
    }

    public Object getTemplateProperty(final String key) {
        return templateProperties.get(key);
    }

    public Map<String, Object> getTemplateProperties() {
        return templateProperties;
    }

    public AbstractEmailBuilder setOtherProperty(final String key, final Object value) {
        otherProperties.put(key, value);
        return this;
    }

    public Object getOtherProperty(final String key) {
        return otherProperties.get(key);
    }

    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }

    public AbstractEmailBuilder withPlayerId(final BigDecimal newPlayerId) {
        this.playerId = newPlayerId;
        return this;
    }

    public BigDecimal getPlayerId() {
        return this.playerId;
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
        final AbstractEmailBuilder rhs = (AbstractEmailBuilder) obj;
        return new EqualsBuilder()
                .append(playerId, rhs.playerId)
                .append(templateProperties, rhs.templateProperties)
                .append(otherProperties, rhs.otherProperties)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(playerId)
                .append(templateProperties)
                .append(otherProperties)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playerId)
                .append(templateProperties)
                .append(otherProperties)
                .toString();
    }
}
