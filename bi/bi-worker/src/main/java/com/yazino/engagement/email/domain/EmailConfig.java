package com.yazino.engagement.email.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class EmailConfig {

    private final String encryptValue;
    private final String randomValue;

    private final String uidKey;
    private final SynchronizationType synchronizationType;

    public EmailConfig(String encryptValue, String randomValue, String uidKey, SynchronizationType synchronizationType) {
        this.encryptValue = encryptValue;
        this.randomValue = randomValue;
        this.uidKey = uidKey;
        this.synchronizationType = synchronizationType;
    }

    public String getEncryptValue() {
        return encryptValue;
    }

    public String getUidKey() {
        return uidKey;
    }

    public String getRandomValue() {
        return randomValue;
    }

    public SynchronizationType getSynchronizationType() {
        return synchronizationType;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(this.encryptValue)
                .append(this.randomValue)
                .append(this.uidKey)
                .append(this.synchronizationType).toHashCode();
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
        final EmailConfig other = (EmailConfig) obj;
        return new EqualsBuilder().append(this.encryptValue, other.encryptValue)
                .append(this.randomValue, other.randomValue)
                .append(this.uidKey, other.uidKey)
                .append(this.synchronizationType, other.synchronizationType).isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("encryptValue").append(encryptValue)
                .append("randomValue").append(randomValue)
                .append("uidKey").append(uidKey)
                .append("synchronizationType").append(synchronizationType)
                .toString();
    }
}

