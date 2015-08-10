package com.yazino.bi.campaign.domain;

import com.yazino.platform.Platform;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import strata.server.lobby.api.promotion.PromotionType;

import java.util.List;

public class PromotionDefinition {

    private Long promotionDefinitionId;
    private List<Platform> platforms;
    private Long campaignId;
    private String name;
    private Integer maxRewards;
    private Integer validForHours;
    private Integer priority;
    private PromotionType promoType;

    public PromotionDefinition(final Long promotionDefinitionId, final List<Platform> platforms, final Long campaignId, final String name, final Integer maxRewards, final Integer validForHours, final Integer priority, final PromotionType promoType) {
        this.promotionDefinitionId = promotionDefinitionId;
        this.platforms = platforms;
        this.campaignId = campaignId;
        this.name = name;
        this.maxRewards = maxRewards;
        this.validForHours = validForHours;
        this.priority = priority;
        this.promoType = promoType;
    }

    public Long getPromotionDefinitionId() {
        return promotionDefinitionId;
    }

    public List<Platform> getPlatforms() {
        return platforms;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public String getName() {
        return name;
    }

    public Integer getMaxRewards() {
        return maxRewards;
    }

    public Integer getValidForHours() {
        return validForHours;
    }

    public Integer getPriority() {
        return priority;
    }

    public PromotionType getPromoType() {
        return promoType;
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
        PromotionDefinition rhs = (PromotionDefinition) obj;
        return new EqualsBuilder()
                .append(this.promotionDefinitionId, rhs.promotionDefinitionId)
                .append(this.platforms, rhs.platforms)
                .append(this.campaignId, rhs.campaignId)
                .append(this.name, rhs.name)
                .append(this.maxRewards, rhs.maxRewards)
                .append(this.validForHours, rhs.validForHours)
                .append(this.priority, rhs.priority)
                .append(this.promoType, rhs.promoType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(promotionDefinitionId)
                .append(platforms)
                .append(campaignId)
                .append(name)
                .append(maxRewards)
                .append(validForHours)
                .append(priority)
                .append(promoType)
                .toHashCode();
    }


    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("promotionDefinitionId", promotionDefinitionId)
                .append("platforms", platforms)
                .append("campaignId", campaignId)
                .append("name", name)
                .append("maxRewards", maxRewards)
                .append("validForHours", validForHours)
                .append("priority", priority)
                .append("promoType", promoType)
                .toString();
    }
}
