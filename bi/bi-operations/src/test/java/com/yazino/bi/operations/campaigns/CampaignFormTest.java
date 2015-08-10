package com.yazino.bi.operations.campaigns;

import com.yazino.bi.operations.campaigns.controller.Campaign;
import com.yazino.promotions.BuyChipsForm;
import com.yazino.promotions.DailyAwardForm;
import org.junit.Test;
import strata.server.lobby.api.promotion.PromotionType;

import java.util.Map;

import static com.yazino.promotions.BuyChipsForm.PROMO_KEY;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

public class CampaignFormTest {


    @Test
    public void campaignFormShouldNotIncludeBuyChipsFormIfPromoNotSet() throws Exception {
        CampaignForm campaignForm = new CampaignForm(new Campaign(), new BuyChipsForm());
        Map<String, String> campaignFormMap = campaignForm.toStringMap();

        assertNull(campaignFormMap.get(PROMO_KEY));
    }

    @Test
    public void campaignFormShouldNotIncludeBuyChipsFormOrDailyAwardFormIfPromoSetButBothBuyCHipsAndDailyAwardAreNull() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setPromo(Boolean.TRUE);
        CampaignForm campaignForm = new CampaignForm(campaign, null);
        Map<String, String> campaignFormMap = campaignForm.toStringMap();

        assertNull(campaignFormMap.get(PROMO_KEY));
    }


    @Test
    public void campaignFormShouldIncludeBuyChipsFormIfPromoSetAndBuyChipsFormIsNotNull() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setPromo(Boolean.TRUE);
        CampaignForm campaignForm = new CampaignForm(campaign, new BuyChipsForm());
        Map<String, String> campaignFormMap = campaignForm.toStringMap();

        assertNotNull(campaignFormMap.get(PROMO_KEY));
    }

    @Test
    public void campaignFormShouldIncludeDailyAwardFormIfPromoSetAndDailyAwardFormPromoType() throws Exception {
        Campaign campaign = new Campaign();
        campaign.setPromo(Boolean.TRUE);
        CampaignForm campaignForm = new CampaignForm(campaign, new DailyAwardForm());
        Map<String, String> campaignFormMap = campaignForm.toStringMap();

        assertNotNull(campaignFormMap.get("DailyAward.topUpAmount"));
    }


}
