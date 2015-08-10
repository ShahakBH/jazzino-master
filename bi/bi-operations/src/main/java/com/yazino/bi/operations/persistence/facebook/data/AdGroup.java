package com.yazino.bi.operations.persistence.facebook.data;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.restfb.Facebook;

/**
 * Data describing a Facebook ad group
 */
public class AdGroup {
    @Facebook("id")
    private Long id;

    @Facebook("campaign_id")
    private String campaignId;

    @Facebook("name")
    private String name;

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("campaignId", campaignId)
                .append("name", name)
                .toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof AdGroup)) {
            return false;
        }
        final AdGroup castOther = (AdGroup) other;
        return new EqualsBuilder()
                .append(id, castOther.id)
                .append(campaignId, castOther.campaignId)
                .append(name, castOther.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(campaignId)
                .append(name)
                .toHashCode();
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(final String campaignId) {
        this.campaignId = campaignId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
