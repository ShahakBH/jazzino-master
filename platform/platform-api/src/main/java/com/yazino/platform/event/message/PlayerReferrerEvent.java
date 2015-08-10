package com.yazino.platform.event.message;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerReferrerEvent implements PlatformEvent {

    public static final String INVITE = "INVITE";

    @JsonProperty("id")
    private BigDecimal playerId;

    @JsonProperty("ref")
    private String ref;

    @JsonProperty("plt")
    private String platform;

    @JsonProperty("gt")
    private String gameType;

    private PlayerReferrerEvent() {
    }

    public PlayerReferrerEvent(final BigDecimal playerId,
                               final String ref,
                               final String platform,
                               final String gameType) {
        notNull(playerId, "playerId may not be null");
        this.playerId = playerId;
        this.ref = ref;
        this.platform = platform;
        this.gameType = gameType;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.REGISTRATION_REFERRER;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(BigDecimal playerId) {
        this.playerId = playerId;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(final String platform) {
        this.platform = platform;
    }

    public String getGameType() {
        return this.gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    @Override

    public String toString() {
        return "PlayerReferrerEvent{"
                + "playerId='" + playerId + '\''
                + ", platform=" + platform + '\''
                + ", gameType=" + gameType + '\''
                + ", referrer='" + ref + '\''
                + "}";
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
        PlayerReferrerEvent rhs = (PlayerReferrerEvent) obj;
        return new EqualsBuilder()
                .append(this.ref, rhs.ref)
                .append(this.platform, rhs.platform)
                .append(this.gameType, rhs.gameType)
                .isEquals()
                && BigDecimals.equalByComparison(playerId, rhs.playerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(playerId))
                .append(ref)
                .append(platform)
                .append(gameType)
                .toHashCode();
    }
}
