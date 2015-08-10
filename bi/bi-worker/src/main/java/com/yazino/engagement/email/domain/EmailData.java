package com.yazino.engagement.email.domain;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import java.util.Map;

public class EmailData {

    private final String emailAddress;
    private final DateTime sendDate;
    private final Map<String, String> dynamicKeys;

    public EmailData(String emailAddress, DateTime sendDate, Map<String, String> dynamicKeys) {
        if (StringUtils.isEmpty(emailAddress)) {
            throw new IllegalArgumentException();
        }

        this.emailAddress = emailAddress;
        this.sendDate = sendDate;
        this.dynamicKeys = dynamicKeys;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public DateTime getSendDate() {
        return sendDate;
    }

    public Map<String, String> getDynamicKeys() {
        return dynamicKeys;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.emailAddress)
                .append(this.sendDate)
                .append(this.dynamicKeys)
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
        final EmailData other = (EmailData) obj;
        return new EqualsBuilder().append(this.emailAddress, other.emailAddress)
                .append(this.sendDate, other.sendDate)
                .append(this.dynamicKeys, other.dynamicKeys).isEquals();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("emailAddress").append(emailAddress)
                .append("sendDate").append(sendDate)
                .append("dynamicKeys").append(dynamicKeys)
                .toString();
    }
}
