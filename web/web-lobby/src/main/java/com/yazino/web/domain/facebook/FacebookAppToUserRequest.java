package com.yazino.web.domain.facebook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FacebookAppToUserRequest {
    private String id;
    private String data;
    private String message;
    @JsonProperty(value = "created_time")
    private DateTime createdTime;

    public FacebookAppToUserRequest() {
    }

    public FacebookAppToUserRequest(final String id,
                                    final String data,
                                    final String message,
                                    final DateTime createdTime) {
        this.id = id;
        this.data = data;
        this.message = message;
        this.createdTime = createdTime;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
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
        final FacebookAppToUserRequest rhs = (FacebookAppToUserRequest) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(data, rhs.data)
                .append(message, rhs.message)
                .append(createdTime, rhs.createdTime)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(id)
                .append(data)
                .append(message)
                .append(createdTime)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(id)
                .append(data)
                .append(message)
                .append(createdTime)
                .toString();
    }

}
