package strata.server.lobby.api.facebook;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class FacebookAppConfiguration implements Serializable {
    private static final long serialVersionUID = -7947491863508436801L;

    private String apiKey;
    private String secretKey;
    private String applicationId;
    private String fanPageId;
    private String gameType;
    private String appName;
    private boolean redirecting;
    private String redirectUrl;
    private String openGraphObjectPrefix;
    private boolean canvasActionsAllowed;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(final String applicationId) {
        this.applicationId = applicationId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(final String secretKey) {
        this.secretKey = secretKey;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(final String appName) {
        this.appName = appName;
    }

    public String getFanPageId() {
        return fanPageId;
    }

    public void setFanPageId(final String fanPageId) {
        this.fanPageId = fanPageId;
    }

    public boolean isRedirecting() {
        return redirecting;
    }

    public void setRedirecting(final boolean redirecting) {
        this.redirecting = redirecting;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(final String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getOpenGraphObjectPrefix() {
        return openGraphObjectPrefix;
    }

    public void setOpenGraphObjectPrefix(final String openGraphObjectPrefix) {
        this.openGraphObjectPrefix = openGraphObjectPrefix;
    }

    public boolean isCanvasActionsAllowed() {
        return canvasActionsAllowed;
    }

    public void setCanvasActionsAllowed(final boolean canvasActionsAllowed) {
        this.canvasActionsAllowed = canvasActionsAllowed;
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
        final FacebookAppConfiguration rhs = (FacebookAppConfiguration) obj;
        return new EqualsBuilder()
                .append(apiKey, rhs.apiKey)
                .append(secretKey, rhs.secretKey)
                .append(applicationId, rhs.applicationId)
                .append(fanPageId, rhs.fanPageId)
                .append(gameType, rhs.gameType)
                .append(appName, rhs.appName)
                .append(redirecting, rhs.redirecting)
                .append(redirectUrl, rhs.redirectUrl)
                .append(openGraphObjectPrefix, rhs.openGraphObjectPrefix)
                .append(canvasActionsAllowed, rhs.canvasActionsAllowed)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(apiKey)
                .append(secretKey)
                .append(applicationId)
                .append(fanPageId)
                .append(gameType)
                .append(appName)
                .append(redirecting)
                .append(redirectUrl)
                .append(openGraphObjectPrefix)
                .append(canvasActionsAllowed)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(apiKey)
                .append(secretKey)
                .append(applicationId)
                .append(fanPageId)
                .append(gameType)
                .append(appName)
                .append(redirecting)
                .append(redirectUrl)
                .append(openGraphObjectPrefix)
                .append(canvasActionsAllowed)
                .toString();
    }
}
