package com.yazino.engagement.facebook;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.DateTime;

// Container wrapping facebook request details
public class FacebookAppRequestEnvelope {

    private String title;
    private String description;
    private String externalId;
    private String message;
    private String gameType;
    private String data;
    private DateTime expires;


    public FacebookAppRequestEnvelope(final String title,
                                      final String description,
                                      final String externalId,
                                      final String gameType,
                                      final String message,
                                      final String data,
                                      final DateTime expires) {
        this.title = title;
        this.description = description;
        this.externalId = externalId;
        this.message = message;
        this.gameType = gameType;
        this.data = data;
        this.expires = expires;
    }

    public String getMessage() {
        return message;
    }

    public String getGameType() {
        return gameType;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getData() {
        return data;
    }

    public DateTime getExpires() {
        return expires;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
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
        final FacebookAppRequestEnvelope rhs = (FacebookAppRequestEnvelope) obj;
        return new EqualsBuilder()
                .append(title, rhs.title)
                .append(description, rhs.description)
                .append(externalId, rhs.externalId)
                .append(message, rhs.message)
                .append(gameType, rhs.gameType)
                .append(data, rhs.data)
                .append(expires, rhs.expires)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(title)
                .append(description)
                .append(externalId)
                .append(message)
                .append(gameType)
                .append(data)
                .append(expires)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("title", title)
                .append("description", description)
                .append("externalId", externalId)
                .append("message", message)
                .append("gameType", gameType)
                .append("data", data)
                .append("expires", expires)
                .build();
    }

}
