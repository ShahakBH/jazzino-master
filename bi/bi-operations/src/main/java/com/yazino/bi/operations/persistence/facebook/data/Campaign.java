package com.yazino.bi.operations.persistence.facebook.data;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.restfb.Facebook;

/**
 * Data describing a Facebook ad campaign
 */
public class Campaign {
    @Facebook("account_id")
    private String accountId;

    @Facebook("id")
    private String campaignId;

    @Facebook
    private String name;

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("accountId", accountId).append("campaignId", campaignId)
                .append("name", name).toString();
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Campaign)) {
            return false;
        }
        final Campaign castOther = (Campaign) other;
        return new EqualsBuilder().append(accountId, castOther.accountId)
                .append(campaignId, castOther.campaignId).append(name, castOther.name).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(accountId).append(campaignId).append(name).toHashCode();
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(final String accountId) {
        this.accountId = accountId;
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
