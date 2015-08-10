package com.yazino.web.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.Validate.notBlank;

public class  FacebookConfiguration {

    private final String apiKey;
    private final String appName;
    private final String applicationId;

    public FacebookConfiguration(final String apiKey, final String appName, final String applicationId) {
        notBlank(apiKey, "API Key may not be null/empty");
        notBlank(appName, "App Name may not be null/empty");
        notBlank(applicationId, "applicationId is null");

        this.apiKey = apiKey;
        this.appName = appName;
        this.applicationId = applicationId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getAppName() {
        return appName;
    }

    public String getApplicationId() {
        return applicationId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final FacebookConfiguration rhs = (FacebookConfiguration) obj;
        return new EqualsBuilder()
                .append(apiKey, rhs.apiKey)
                .append(appName, rhs.appName)
                .append(applicationId, rhs.applicationId)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(apiKey)
                .append(appName)
                .append(applicationId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
