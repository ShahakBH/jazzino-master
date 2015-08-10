package com.yazino.platform.invitation.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.joda.time.DateTime;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailInvitationRequestedMessage implements InvitationMessage {

    private String callToActionUrl;
    private BigDecimal issuingPlayerId;
    private String issuingPlayerName;
    private String recipientEmail;
    private String customisedMessage;
    private DateTime requestTime;
    private String source;
    private String gameType;

    public EmailInvitationRequestedMessage() {
    }

    public EmailInvitationRequestedMessage(BigDecimal issuingPlayerId,
                                           String issuingPlayerName,
                                           String recipientEmail,
                                           String customisedMessage,
                                           String callToActionUrl,
                                           DateTime requestTime,
                                           String source,
                                           String gameType) {
        this.issuingPlayerId = issuingPlayerId;
        this.issuingPlayerName = issuingPlayerName;
        this.recipientEmail = recipientEmail;
        this.customisedMessage = customisedMessage;
        this.callToActionUrl = callToActionUrl;
        this.requestTime = requestTime;
        this.source = source;
        this.gameType = gameType;
    }

    public String getCallToActionUrl() {
        return callToActionUrl;
    }

    public void setCallToActionUrl(String callToActionUrl) {
        this.callToActionUrl = callToActionUrl;
    }

    public String getCustomisedMessage() {
        return customisedMessage;
    }

    public void setCustomisedMessage(String customisedMessage) {
        this.customisedMessage = customisedMessage;
    }

    public BigDecimal getIssuingPlayerId() {
        return issuingPlayerId;
    }

    public void setIssuingPlayerId(BigDecimal issuingPlayerId) {
        this.issuingPlayerId = issuingPlayerId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public String getRecipientEmailAddress() {
        return recipientEmail;
    }

    public void setRecipientEmailAddress(String recipientEmailAddress) {
        this.recipientEmail = recipientEmailAddress;
    }

    public DateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(DateTime requestTime) {
        this.requestTime = requestTime;
    }

    public String getIssuingPlayerName() {
        return issuingPlayerName;
    }

    public void setIssuingPlayerName(String issuingPlayerName) {
        this.issuingPlayerName = issuingPlayerName;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public InvitationMessageType getMessageType() {
        return InvitationMessageType.EMAIL_INVITATION_REQUESTED;
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
        final EmailInvitationRequestedMessage rhs = (EmailInvitationRequestedMessage) obj;
        return new EqualsBuilder()
                .append(callToActionUrl, rhs.callToActionUrl)
                .append(customisedMessage, rhs.customisedMessage)
                .append(issuingPlayerId, rhs.issuingPlayerId)
                .append(issuingPlayerName, rhs.issuingPlayerName)
                .append(source, rhs.source)
                .append(gameType, rhs.gameType)
                .append(recipientEmail, rhs.recipientEmail)
                .append(requestTime, rhs.requestTime)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(callToActionUrl)
                .append(customisedMessage)
                .append(issuingPlayerId)
                .append(issuingPlayerName)
                .append(source)
                .append(gameType)
                .append(recipientEmail)
                .append(requestTime)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    public boolean isValid() {
        return callToActionUrl != null
                && issuingPlayerId != null
                && issuingPlayerName != null
                && recipientEmail != null
                && requestTime != null;
    }
}
