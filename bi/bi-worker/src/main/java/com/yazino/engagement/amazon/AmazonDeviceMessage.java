package com.yazino.engagement.amazon;

import com.yazino.platform.messaging.Message;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AmazonDeviceMessage implements Message {

    private static final long serialVersionUID = 953111219377168811L;

    private final BigDecimal playerId;
    private final String registrationId;
    private final String title;
    private final String ticker;
    private final String message;
    private final String action;
    private final Long campaignRunId;

    @JsonCreator
    public AmazonDeviceMessage(@JsonProperty("playerId") final BigDecimal playerId,
                               @JsonProperty("registrationId") final String registrationId,
                               @JsonProperty("title") final String title,
                               @JsonProperty("ticker") final String ticker,
                               @JsonProperty("message")final String message,
                               @JsonProperty("action") final String action,
                               @JsonProperty("campaignRunId") final Long campaignRunId) {
        Validate.notNull(registrationId);
        this.playerId = playerId;
        this.registrationId = registrationId;
        this.title = title;
        this.ticker = ticker;
        this.message = message;
        this.action = action;
        this.campaignRunId = campaignRunId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public String getTitle() {
        return title;
    }

    public String getTicker() {
        return ticker;
    }

    public String getMessage() {
        return message;
    }

    public String getAction() {
        return action;
    }

    public Long getCampaignRunId() {
        return campaignRunId;
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
        AmazonDeviceMessage rhs = (AmazonDeviceMessage) obj;
        return new EqualsBuilder()
                .append(this.playerId, rhs.playerId)
                .append(this.registrationId, rhs.registrationId)
                .append(this.title, rhs.title)
                .append(this.ticker, rhs.ticker)
                .append(this.message, rhs.message)
                .append(this.action, rhs.action)
                .append(this.campaignRunId, rhs.campaignRunId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(playerId)
                .append(registrationId)
                .append(title)
                .append(ticker)
                .append(message)
                .append(action)
                .append(campaignRunId)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("playerId", playerId)
                .append("registrationId", registrationId)
                .append("title", title)
                .append("ticker", ticker)
                .append("message", message)
                .append("action", action)
                .append("campaignRunId", campaignRunId)
                .toString();
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public Object getMessageType() {
        return "amazon-device-message";
    }
}
