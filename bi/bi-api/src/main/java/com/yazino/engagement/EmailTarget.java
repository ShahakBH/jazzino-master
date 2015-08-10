package com.yazino.engagement;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class EmailTarget {

    private final String emailAddress;
    private final String displayName;
    private final Map<String, Object> content;


    public Map<String, Object> getContent() {
        return content;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public EmailTarget(@JsonProperty("emailAddress") String emailAddress,
                       @JsonProperty("displayName") String displayName,
                       @JsonProperty("content") final Map<String, Object> content) {
        this.emailAddress = emailAddress;
        this.displayName = displayName;
        this.content = content;
    }

//    public EmailTarget(@JsonProperty("emailAddress") String emailAddress,
//                       @JsonProperty("displayName") String displayName,
//                       final Map<String, String> content) {
//        this.emailAddress = emailAddress;
//        this.displayName = displayName;
//
//        this.content = content;
//    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.emailAddress)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EmailTarget other = (EmailTarget) obj;
        return new EqualsBuilder()
                .append(this.emailAddress, other.emailAddress)
                .append(this.displayName, other.displayName)
                .isEquals();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("emailAddress", emailAddress)
                .append("displayName", displayName)
                .toString();
    }
}
