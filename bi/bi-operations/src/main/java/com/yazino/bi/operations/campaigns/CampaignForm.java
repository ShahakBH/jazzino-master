package com.yazino.bi.operations.campaigns;

import com.yazino.bi.operations.campaigns.controller.Campaign;
import com.yazino.promotions.BuyChipsForm;
import com.yazino.promotions.DailyAwardForm;
import com.yazino.promotions.GiftingForm;
import com.yazino.promotions.PromotionForm;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

import static strata.server.lobby.api.promotion.PromotionType.*;

public class CampaignForm {
    private Campaign campaign;
    private BuyChipsForm buyChipsForm;
    private DailyAwardForm dailyAwardForm;
    private String promotionType;
    private GiftingForm giftingForm;


    public CampaignForm() {
        campaign = new Campaign();
        buyChipsForm = new BuyChipsForm();
        dailyAwardForm = new DailyAwardForm();
    }

    public CampaignForm(final Campaign campaign, final PromotionForm promotionForm) {
        this.campaign = campaign;
        if (promotionForm != null) {
            this.promotionType = promotionForm.getPromoType().toString();
            if (BUY_CHIPS.equals(promotionForm.getPromoType())) {
                buyChipsForm = (BuyChipsForm) promotionForm;
            } else if (DAILY_AWARD.equals(promotionForm.getPromoType())) {
                dailyAwardForm = (DailyAwardForm) promotionForm;
            } else if (GIFTING.equals(promotionForm.getPromoType())) {
                giftingForm = (GiftingForm) promotionForm;
            }
        }
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(final Campaign campaign) {
        this.campaign = campaign;
    }

    public BuyChipsForm getBuyChipsForm() {
        return buyChipsForm;
    }

    public void setBuyChipsForm(final BuyChipsForm buyChipsForm) {
        this.buyChipsForm = buyChipsForm;
    }

    public DailyAwardForm getDailyAwardForm() {
        return dailyAwardForm;
    }

    public void setDailyAwardForm(final DailyAwardForm dailyAwardForm) {
        this.dailyAwardForm = dailyAwardForm;
    }

    public GiftingForm getGiftingForm() {
        return giftingForm;
    }

    public void setGiftingForm(final GiftingForm giftingForm) {
        this.giftingForm = giftingForm;
    }

    public String getPromotionType() {
        return promotionType;
    }

    public void setPromotionType(final String promotionType) {
        this.promotionType = promotionType;
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
        CampaignForm rhs = (CampaignForm) obj;
        return new EqualsBuilder()
                .append(this.campaign, rhs.campaign)
                .append(this.buyChipsForm, rhs.buyChipsForm)
                .append(this.dailyAwardForm, rhs.dailyAwardForm)
                .append(this.promotionType, rhs.promotionType)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(campaign)
                .append(buyChipsForm)
                .append(dailyAwardForm)
                .append(promotionType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("campaign", campaign)
                .append("buyChipsForm", buyChipsForm)
                .append("dailyAwardForm", dailyAwardForm)
                .append("promotionType", promotionType)
                .toString();
    }

    public Map<String, String> toStringMap() {
        Map<String, String> campaignFormMap = new LinkedHashMap<String, String>();
        if (campaign != null) {
            Map<String, String> campaignMap = campaign.toStringMap();
            campaignFormMap.putAll(campaignMap);

            if (campaign.isPromo() && "BUY_CHIPS".equals(promotionType)) {
                Map<String, String> buyChipsFormMap = buyChipsForm.toStringMap();
                campaignFormMap.putAll(buyChipsFormMap);
            }

            if (campaign.isPromo() && "DAILY_AWARD".equals(promotionType)) {
                Map<String, String> dailyAwardFormMap = dailyAwardForm.toStringMap();
                campaignFormMap.putAll(dailyAwardFormMap);
            }
        }
        return campaignFormMap;
    }


}
