package com.yazino.mobile.yaps.message;

import com.yazino.platform.messaging.Message;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PushMessage implements Message {

    private static final long serialVersionUID = 7277550261152339130L;
    private final String gameType;
    private final BigDecimal playerId;
    private int expiryDateSecondsSinceEpoch = 0;
    private String alert = null;
    private String sound = null;
    private Integer badge = null;

    @JsonCreator
    public PushMessage(@JsonProperty("gameType") final String gameType,
                       @JsonProperty("playerId") final BigDecimal playerId) {
        notNull(gameType, "gameType was null");
        notBlank(gameType.trim(), "gameType was blank");
        notNull(playerId, "playerId was null");
        this.gameType = gameType;
        this.playerId = playerId;
    }

    public String getGameType() {
        return gameType;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    @JsonProperty("expiryDateSecondsSinceEpoch")
    public int getExpiryDateSecondsSinceEpoch() {
        return expiryDateSecondsSinceEpoch;
    }

    public void setExpiryDateSecondsSinceEpoch(final int expiryDateSecondsSinceEpoch) {
        this.expiryDateSecondsSinceEpoch = expiryDateSecondsSinceEpoch;
    }

    public String getAlert() {
        return alert;
    }

    public void setAlert(final String alert) {
        this.alert = alert;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(final String sound) {
        this.sound = sound;
    }

    public Integer getBadge() {
        return badge;
    }

    public void setBadge(final Integer badge) {
        this.badge = badge;
    }

    @Override
    public int getVersion() {
        return Message.VERSION;
    }

    @Override
    public Object getMessageType() {
        return "PushMessage";
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof PushMessage)) {
            return false;
        }
        final PushMessage other = (PushMessage) obj;
        final EqualsBuilder builder = new EqualsBuilder();
        builder.append(this.gameType, other.gameType);
        builder.append(this.expiryDateSecondsSinceEpoch, other.expiryDateSecondsSinceEpoch);
        builder.append(this.alert, other.alert);
        builder.append(this.sound, other.sound);
        builder.append(this.badge, other.badge);
        return builder.isEquals()
                && BigDecimals.equalByComparison(playerId, other.playerId);
    }


    @Override
    public int hashCode() {
        final HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(this.gameType);
        builder.append(BigDecimals.strip(this.playerId));
        builder.append(this.expiryDateSecondsSinceEpoch);
        builder.append(this.alert);
        builder.append(this.sound);
        builder.append(this.badge);
        return builder.toHashCode();
    }
}
