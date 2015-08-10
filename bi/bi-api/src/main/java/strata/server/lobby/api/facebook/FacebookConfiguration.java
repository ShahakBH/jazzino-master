package strata.server.lobby.api.facebook;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CONNECT;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.LOOSE;

public class FacebookConfiguration implements Serializable {
    private static final long serialVersionUID = -7947491863508436803L;

    public enum ApplicationType {
        /**
         * An application served within the Facebook canvas, or under the same rules (e.g. mobile).
         */
        CANVAS,

        /**
         * A web application accessed via Facebook Connect.
         */
        CONNECT
    }

    public enum MatchType {
        STRICT,
        LOOSE
    }

    private final Map<String, FacebookAppConfiguration> appsByGameTypes
            = new HashMap<String, FacebookAppConfiguration>();
    private final Map<String, FacebookAppConfiguration> appsByOpenGraphPrefix
            = new HashMap<String, FacebookAppConfiguration>();
    private final Map<String, ConversionTrackingData> conversionTracking
            = new HashMap<String, ConversionTrackingData>();

    private FacebookAppConfiguration defaultAppConfiguration;
    private FacebookAppConfiguration connectAppConfiguration;
    private String appUrlRoot;
    private String loginUrl;
    private boolean reviewsEnabled;
    private boolean publishStreamEnabled;
    private boolean appsEnabled;
    private boolean usingSeparateConnectApplication;

    public boolean isConfigured() {
        return !appsByGameTypes.isEmpty();
    }

    public void setUsingSeparateConnectApplication(final boolean usingSeparateConnectApplication) {
        this.usingSeparateConnectApplication = usingSeparateConnectApplication;
    }

    public void setConnectAppConfiguration(final FacebookAppConfiguration connectAppConfiguration) {
        this.connectAppConfiguration = connectAppConfiguration;
    }

    public void setApplicationConfigs(final List<FacebookAppConfiguration> applicationConfigs) {
        updateAppConfigurations(applicationConfigs);
    }

    public String getAppUrlRoot() {
        return appUrlRoot;
    }

    public void setAppUrlRoot(final String appUrlRoot) {
        this.appUrlRoot = appUrlRoot;
    }

    public boolean isReviewsEnabled() {
        return reviewsEnabled;
    }

    public void setReviewsEnabled(final boolean reviewsEnabled) {
        this.reviewsEnabled = reviewsEnabled;
    }

    public boolean isPublishStreamEnabled() {
        return publishStreamEnabled;
    }

    public void setPublishStreamEnabled(final boolean publishStreamEnabled) {
        this.publishStreamEnabled = publishStreamEnabled;
    }

    public Map<String, ConversionTrackingData> getConversionTracking() {
        return conversionTracking;
    }

    public void setConversionTracking(final Map<String, ConversionTrackingData> conversionTracking) {
        this.conversionTracking.clear();

        if (conversionTracking != null) {
            this.conversionTracking.putAll(conversionTracking);
        }
    }

    public void setAppsEnabled(final boolean appsEnabled) {
        this.appsEnabled = appsEnabled;
    }

    public boolean isAppsEnabled() {
        return appsEnabled;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(final String loginUrl) {
        this.loginUrl = loginUrl;
    }

    /**
     * Get the appropriate application configuration.
     *
     * @param gameType        the current game type. Null is acceptable, although discouraged.
     * @param applicationType the current application type.
     * @param matchType       STRICT if an exact match is required; LOOSE if a fallback is acceptable.
     * @return the application configuration, or null if no match is found.
     */
    public FacebookAppConfiguration getAppConfigFor(final String gameType,
                                                    final ApplicationType applicationType,
                                                    final MatchType matchType) {
        if (applicationType == CONNECT && usingSeparateConnectApplication) {
            return connectAppConfiguration;
        }

        FacebookAppConfiguration appConfig = null;
        if (gameType != null) {
            appConfig = appsByGameTypes.get(gameType);
        }
        if (appConfig == null && matchType == LOOSE) {
            return defaultAppConfiguration;
        }
        return appConfig;
    }

    /**
     * Get the application config for a given OpenGraph prefix.
     *
     * @param openGraphObjectPrefix the prefix to search for.
     * @return the configuration if a match is found; otherwise null.
     */
    public FacebookAppConfiguration getAppConfigForOpenGraphObjectPrefix(final String openGraphObjectPrefix) {
        notNull(openGraphObjectPrefix, "openGraphObjectPrefix may not be null");

        return appsByOpenGraphPrefix.get(openGraphObjectPrefix);
    }

    private void updateAppConfigurations(final List<FacebookAppConfiguration> applicationConfigsToParse) {
        appsByGameTypes.clear();
        appsByOpenGraphPrefix.clear();
        defaultAppConfiguration = null;

        if (applicationConfigsToParse != null) {
            for (final FacebookAppConfiguration appConfig : applicationConfigsToParse) {
                if (defaultAppConfiguration == null) {
                    defaultAppConfiguration = appConfig;
                }
                if (appConfig.getGameType() != null) {
                    appsByGameTypes.put(appConfig.getGameType(), appConfig);
                }
                if (appConfig.getOpenGraphObjectPrefix() != null) {
                    appsByOpenGraphPrefix.put(appConfig.getOpenGraphObjectPrefix(), appConfig);
                }
            }
        }
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
        final FacebookConfiguration rhs = (FacebookConfiguration) obj;
        return new EqualsBuilder()
                .append(appsByGameTypes, rhs.appsByGameTypes)
                .append(appsByOpenGraphPrefix, rhs.appsByOpenGraphPrefix)
                .append(conversionTracking, rhs.conversionTracking)
                .append(appUrlRoot, rhs.appUrlRoot)
                .append(loginUrl, rhs.loginUrl)
                .append(reviewsEnabled, rhs.reviewsEnabled)
                .append(publishStreamEnabled, rhs.publishStreamEnabled)
                .append(appsEnabled, rhs.appsEnabled)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(appsByGameTypes)
                .append(appsByOpenGraphPrefix)
                .append(conversionTracking)
                .append(appUrlRoot)
                .append(loginUrl)
                .append(reviewsEnabled)
                .append(publishStreamEnabled)
                .append(appsEnabled)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(appsByGameTypes)
                .append(appsByOpenGraphPrefix)
                .append(conversionTracking)
                .append(appUrlRoot)
                .append(loginUrl)
                .append(reviewsEnabled)
                .append(publishStreamEnabled)
                .append(appsEnabled)
                .toString();
    }
}
