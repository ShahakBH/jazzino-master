package com.yazino.engagement.campaign.domain;

import com.yazino.engagement.CampaignDeliverMessage;
import com.yazino.platform.messaging.Message;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

public class DelayedCampaignDeliverMessage implements Message {

    private DateTime runAfter;
    private CampaignDeliverMessage message;

    public DelayedCampaignDeliverMessage(final DateTime runAfter, final CampaignDeliverMessage message) {
        this.runAfter = runAfter;
        this.message = message;
    }

    DelayedCampaignDeliverMessage() {
    }

    public DateTime getRunAfter() {
        return runAfter;
    }

    public void setRunAfter(final DateTime runAfter) {
        this.runAfter = runAfter;
    }

    public CampaignDeliverMessage getMessage() {
        return message;
    }

    public void setMessage(final CampaignDeliverMessage message) {
        this.message = message;
    }



    @Override
    public int getVersion() {
        return Message.VERSION;
    }

    @Override
    public Object getMessageType() {
        return "DelayedCampaignRunMessage";
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
        DelayedCampaignDeliverMessage rhs = (DelayedCampaignDeliverMessage) obj;
        return new EqualsBuilder()
                .append(this.runAfter, rhs.runAfter)
                .append(this.message, rhs.message)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(runAfter)
                .append(message)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("runAfter", runAfter)
                .append("message", message)
                .toString();
    }
}
