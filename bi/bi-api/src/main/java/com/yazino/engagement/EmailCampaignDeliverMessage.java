package com.yazino.engagement;

import com.yazino.platform.messaging.Message;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailCampaignDeliverMessage implements Message {

    private static final long serialVersionUID = 1755981796182147814L;
    @JsonProperty("id")
    private Long campaignRunId;
    @JsonProperty("uploadId")
    private Long uploadId;
    @JsonProperty("templateId")
    private String templateId;
    @JsonProperty("filter")
    private String filter120days;

    protected EmailCampaignDeliverMessage() {
    }

    public EmailCampaignDeliverMessage(final Long campaignRunId, final Long uploadId, final String templateId, final String filter120days) {
        notNull(campaignRunId, "campaignRunId may not be null");
        notNull(uploadId, "uploadId Cannot be null");
        notNull(templateId, "template not be null");

        this.campaignRunId = campaignRunId;
        this.uploadId = uploadId;
        this.templateId = templateId;
        this.filter120days = filter120days;
    }

    public Long getCampaignRunId() {
        return campaignRunId;
    }

    public Long getUploadId() {
        return uploadId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getFilter120days() {
        return filter120days;
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
        final EmailCampaignDeliverMessage rhs = (EmailCampaignDeliverMessage) obj;
        return new EqualsBuilder()
                .append(campaignRunId, rhs.campaignRunId)
                .append(uploadId, rhs.uploadId)
                .append(templateId, rhs.templateId)
                .append(filter120days, rhs.filter120days)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(campaignRunId)
                .append(uploadId)
                .append(templateId)
                .append(filter120days)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    @Override
    public int getVersion() {
        return Message.VERSION;
    }

    @Override
    public Object getMessageType() {
        return "EmailCampaignDeliverMessage";
    }

}
