package com.yazino.engagement.mobile;

import com.yazino.platform.Platform;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.math.BigDecimal;

class MobileDevice {

    private final Long id;
    private final BigDecimal playerId;
    private final String gameType;
    private final Platform platform;
    private final String appId;
    private final String deviceId;
    private final boolean active;
    private final String pushToken;

    public MobileDevice(Long id, BigDecimal playerId, String gameType, Platform platform, String appId, String deviceId, String pushToken, boolean active) {
        // id can be null for an unsaved object
        Validate.notNull(playerId, "playerId was null");
        Validate.notBlank(gameType, "gameType was blank");
        Validate.notNull(platform, "platform was null");
        if (appId != null) { Validate.notBlank(appId, "appId was non-null but blank"); }
        if (deviceId != null) { Validate.notBlank(deviceId, "deviceId was non-null but blank"); }
        Validate.notBlank(pushToken, "pushToken was blank");

        this.id = id;
        this.playerId = playerId;
        this.gameType = gameType;
        this.platform = platform;
        this.appId = appId;
        this.deviceId = deviceId;
        this.active = active;
        this.pushToken = pushToken;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getGameType() {
        return gameType;
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getAppId() {
        return appId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public boolean isActive() {
        return active;
    }

    public String getPushToken() {
        return pushToken;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        MobileDevice rhs = (MobileDevice) obj;
        // NOTE id not included to allow comparing saved and unsaved instances
        return BigDecimals.equalByComparison(this.playerId, rhs.playerId) && new EqualsBuilder()
                .append(this.gameType, rhs.gameType)
                .append(this.platform, rhs.platform)
                .append(this.appId, rhs.appId)
                .append(this.deviceId, rhs.deviceId)
                .append(this.active, rhs.active)
                .append(this.pushToken, rhs.pushToken)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(gameType)
                .append(platform)
                .append(appId)
                .append(deviceId)
                .append(active)
                .append(pushToken)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this).toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(MobileDevice copy) {
        return new Builder(copy);
    }

    public static class Builder {

        private Long id;
        private BigDecimal playerId;
        private String gameType;
        private Platform platform;
        private String appId;
        private String deviceId;
        private String pushToken;
        private boolean active = true;

        public Builder() {
        }

        public Builder(MobileDevice copy) {
            this.id = copy.getId();
            this.playerId = copy.getPlayerId();
            this.gameType = copy.getGameType();
            this.platform = copy.getPlatform();
            this.appId = copy.getAppId();
            this.deviceId = copy.getDeviceId();
            this.active = copy.isActive();
            this.pushToken = copy.getPushToken();
        }

        public Builder withId(Long rowId) {
            this.id = rowId;
            return this;
        }

        public Builder withPlayerId(BigDecimal playerId) {
            this.playerId = playerId;
            return this;
        }

        public Builder withGameType(String gameType) {
            this.gameType = gameType;
            return this;
        }

        public Builder withPlatform(Platform platform) {
            this.platform = platform;
            return this;
        }

        public Builder withAppId(String appId) {
            this.appId = appId;
            return this;
        }

        public Builder withDeviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder withPushToken(String pushToken) {
            this.pushToken = pushToken;
            return this;
        }

        public Builder withActive(boolean active) {
            this.active = active;
            return this;
        }

        public MobileDevice build() {
            return new MobileDevice(id, playerId, gameType, platform, appId, deviceId, pushToken, active);
        }

    }

}
