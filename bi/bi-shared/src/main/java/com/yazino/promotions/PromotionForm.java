package com.yazino.promotions;

import com.yazino.platform.Platform;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import strata.server.lobby.api.promotion.PromotionType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class PromotionForm {
    private Long promotionDefinitionId;
    private List<Platform> platforms;
    private Long campaignId;
    private String name;
    private Integer maxRewards = 1;
    private Integer validForHours = 24;
    private Integer priority = 1;
    private PromotionType promoType;
    private boolean allPlayers;

    public abstract String getPromoKey();

    public boolean isAllPlayers() {
        return allPlayers;
    }

    public void setAllPlayers(final boolean allPlayers) {
        this.allPlayers = allPlayers;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Integer getMaxRewards() {
        return maxRewards;
    }

    public void setMaxRewards(final Integer maxRewards) {
        this.maxRewards = maxRewards;
    }

    public Integer getValidForHours() {
        return validForHours;
    }

    public void setValidForHours(final Integer validForHours) {
        this.validForHours = validForHours;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(final Long campaignId) {
        this.campaignId = campaignId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public List<Platform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(final List<Platform> platforms) {
        this.platforms = platforms;
    }

    public Long getPromotionDefinitionId() {
        return promotionDefinitionId;
    }

    public void setPromotionDefinitionId(final Long promotionDefinitionId) {
        this.promotionDefinitionId = promotionDefinitionId;
    }

    public PromotionType getPromoType() {
        return promoType;
    }

    protected void setPromoType(final PromotionType promoType) {
        this.promoType = promoType;
    }

    public Map<String, String> toStringMap() {
        Map<String, String> promotionFormMap = new LinkedHashMap<String, String>();
        promotionFormMap.put(getPromoKey() + "name", getValueAsStringWithNullCheck(getName()));
        promotionFormMap.put(getPromoKey() + "maxRewards", getValueAsStringWithNullCheck(getMaxRewards()));
        promotionFormMap.put(getPromoKey() + "validForHours", getValueAsStringWithNullCheck(getValidForHours()));
        promotionFormMap.put(getPromoKey() + "priority", getValueAsStringWithNullCheck(getPriority()));
        promotionFormMap.put(getPromoKey() + "platforms", getValueAsStringWithNullCheck(getPlatforms()));
        promotionFormMap.put(getPromoKey() + "campaignId", getValueAsStringWithNullCheck(getCampaignId()));
        promotionFormMap.put(getPromoKey() + "allPlayers", getValueAsStringWithNullCheck(isAllPlayers()));


        return promotionFormMap;
    }

    protected String getValueAsStringWithNullCheck(Object object) {
        if (object == null) {
            return "";
        } else {
            return object.toString();
        }
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
        PromotionForm rhs = (PromotionForm) obj;
        return new EqualsBuilder()
                .append(this.promotionDefinitionId, rhs.promotionDefinitionId)
                .append(this.platforms, rhs.platforms)
                .append(this.campaignId, rhs.campaignId)
                .append(this.name, rhs.name)
                .append(this.maxRewards, rhs.maxRewards)
                .append(this.validForHours, rhs.validForHours)
                .append(this.priority, rhs.priority)
                .append(this.promoType, rhs.promoType)
                .append(this.allPlayers, rhs.allPlayers)
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
                .append(allPlayers)
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
                .append("allPlayers", allPlayers)
                .toString();
    }
}
