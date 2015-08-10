package com.yazino.platform.android;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yazino.platform.Platform;
import com.yazino.platform.event.message.EventMessageType;
import com.yazino.platform.event.message.PlatformEvent;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessagingDeviceRegistrationEvent implements PlatformEvent, Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("player_id")
    private BigDecimal playerId;

    @JsonProperty("game_type")
    private String gameType;

    @JsonProperty("registration_id")
    private String registrationId;

    @JsonProperty("platform")
    private Platform platform;

    // optional; it's the "package name" in Android
    @JsonProperty("appId")
    private String appId;

    // optional
    @JsonProperty("deviceId")
    private String deviceId;

    private MessagingDeviceRegistrationEvent() {
    }

    public MessagingDeviceRegistrationEvent(BigDecimal playerId,
                                            String gameType,
                                            String registrationId,
                                            Platform platform) {

        checkNotNull(playerId);
        checkNotNull(gameType);
        checkNotNull(registrationId);
        checkNotNull(platform);
        this.playerId = playerId;
        this.gameType = gameType;
        this.registrationId = registrationId;
        this.platform = platform;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.MESSAGING_DEVICE_REGISTRATION;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(BigDecimal playerId) {
        this.playerId = playerId;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(final Platform platform) {
        this.platform = platform;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(final String appId) {
        this.appId = appId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(final String deviceId) {
        this.deviceId = deviceId;
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
        final MessagingDeviceRegistrationEvent rhs = (MessagingDeviceRegistrationEvent) obj;
        return new EqualsBuilder()
                .append(gameType, rhs.gameType)
                .append(registrationId, rhs.registrationId)
                .append(platform, rhs.platform)
                .append(appId, rhs.appId)
                .append(deviceId, rhs.deviceId)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(gameType)
                .append(registrationId)
                .append(platform)
                .append(appId)
                .append(deviceId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "PlayerEvent{"
                + "playerId='" + playerId + '\''
                + ", gameType='" + gameType + '\''
                + ", registrationId='" + registrationId + '\''
                + ", platform='" + platform + '\''
                + ", appId='" + appId + '\''
                + ", deviceId='" + deviceId + '\''
                + '}';
    }

}
