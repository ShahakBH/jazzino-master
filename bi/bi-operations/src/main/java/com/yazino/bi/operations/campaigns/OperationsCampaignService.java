package com.yazino.bi.operations.campaigns;

import com.yazino.bi.campaign.dao.CampaignAddTargetDao;
import com.yazino.bi.campaign.dao.CampaignDefinitionDao;
import com.yazino.bi.campaign.domain.CampaignDefinition;
import com.yazino.bi.campaign.domain.CampaignSchedule;
import com.yazino.bi.operations.campaigns.controller.Campaign;
import com.yazino.bi.operations.campaigns.controller.CampaignScheduleWithName;
import com.yazino.bi.operations.campaigns.controller.CampaignScheduleWithNameDao;
import com.yazino.bi.operations.campaigns.model.CampaignPlayerUpload;
import com.yazino.engagement.campaign.domain.NotificationChannelConfigType;
import com.yazino.promotions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.PromotionType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static strata.server.lobby.api.promotion.PromotionType.*;

@Service
public class OperationsCampaignService {
    private static final Logger LOG = LoggerFactory.getLogger(OperationsCampaignService.class);
    public static final String UTF_8 = "UTF-8";

    private final CampaignDefinitionDao campaignDefinitionDao;
    private final CampaignScheduleWithNameDao campaignScheduleWithNameDao;
    private final PromotionFormDefinitionDao<BuyChipsForm> buyChipsPromotionDefinitionDao;
    private final PromotionFormDefinitionDao<DailyAwardForm> dailyAwardPromotionDefinitionDao;
    private final PromotionDefinitionDao promotionDefinitionDao;
    private final CampaignAddTargetDao campaignAddTargetDao;
    private final PromotionFormDefinitionDao<GiftingForm> giftingPromotionDefinitionDao;

    @Autowired
    public OperationsCampaignService(final CampaignDefinitionDao campaignDefinitionDao,
                                     final CampaignScheduleWithNameDao campaignScheduleWithNameDao,
                                     final PromotionFormDefinitionDao<BuyChipsForm> buyChipsPromotionDefinitionDao,
                                     final PromotionFormDefinitionDao<DailyAwardForm> dailyAwardPromotionDefinitionDao,
                                     final PromotionDefinitionDao promotionDefinitionDao,
                                     final CampaignAddTargetDao campaignAddTargetDao,
                                     final PromotionFormDefinitionDao<GiftingForm> giftingPromotionDefinitionDao) {

        this.campaignScheduleWithNameDao = campaignScheduleWithNameDao;
        this.buyChipsPromotionDefinitionDao = buyChipsPromotionDefinitionDao;
        this.dailyAwardPromotionDefinitionDao = dailyAwardPromotionDefinitionDao;
        this.promotionDefinitionDao = promotionDefinitionDao;
        this.campaignAddTargetDao = campaignAddTargetDao;
        this.campaignDefinitionDao = campaignDefinitionDao;
        this.giftingPromotionDefinitionDao = giftingPromotionDefinitionDao;
    }

    public Long save(final CampaignForm campaignForm) {
        if (campaignForm.getCampaign().isDelayNotifications()) {
            validateRunTime(campaignForm.getCampaign());
        }
        final Long campaignId = saveCampaign(campaignForm.getCampaign());
        campaignForm.getCampaign().getCampaignScheduleWithName().setCampaignId(campaignId);
        savePromoIfExists(campaignForm);

        return campaignId;
    }

    private void validateRunTime(final Campaign campaign) {
        if (campaign.getCampaignScheduleWithName().getRunHours() < 24) {
            throw new RuntimeException("cannot save Delayed Notification campaign with runtime of <24hours");
        }
    }

    public void update(final CampaignForm campaignForm) {
        updateCampaign(campaignForm.getCampaign());
        updatePromotionIfExists(campaignForm);
    }

    private void updatePromotionIfExists(final CampaignForm campaignForm) {
        if (promotionExists(campaignForm)) {
            if (promotionIs(BUY_CHIPS, campaignForm)) {
                final BuyChipsForm buyChipsForm = campaignForm.getBuyChipsForm();
                setNameCampaignIdAndPaymentMethodsForBuyChipsPromo(campaignForm, buyChipsForm);
                buyChipsPromotionDefinitionDao.update(buyChipsForm);
            }

            if (promotionIs(DAILY_AWARD, campaignForm)) {
                final DailyAwardForm dailyAwardForm = campaignForm.getDailyAwardForm();
                setDailyAwardPromoIdAndName(campaignForm, dailyAwardForm);
                dailyAwardPromotionDefinitionDao.update(dailyAwardForm);
            }

            if (promotionIs(GIFTING, campaignForm)) {
                final GiftingForm giftingForm = campaignForm.getGiftingForm();
                giftingForm.setName(campaignForm.getCampaign().getName());
                giftingForm.setCampaignId(campaignForm.getCampaign().getCampaignId());
                giftingPromotionDefinitionDao.update(giftingForm);
            }
        }
    }

    private boolean promotionIs(final PromotionType promoType, final CampaignForm campaignForm) {
        return promoType.name().equals(campaignForm.getPromotionType());
    }

    private boolean promotionExists(final CampaignForm campaignForm) {
        return campaignForm.getCampaign().isPromo();
    }

    private void setNameCampaignIdAndPaymentMethodsForBuyChipsPromo(final CampaignForm campaignForm, final BuyChipsForm buyChipsForm) {
        buyChipsForm.setName(campaignForm.getCampaign().getName());
        buyChipsForm.setCampaignId(campaignForm.getCampaign().getCampaignId());
        buyChipsForm.setPaymentMethods(CampaignHelper.getSupportedPaymentMethods(buyChipsForm.getPlatforms()));
    }

    public CampaignForm getCampaignForm(Long campaignId) {
        final Campaign campaign = getCampaign(campaignId);
        final PromotionType promotionDefinitionType = promotionDefinitionDao.getPromotionDefinitionType(campaignId);

        final PromotionForm promoForm;
        if (BUY_CHIPS.equals(promotionDefinitionType)) {
            promoForm = getBuyChipsFormForCampaignId(campaign);

        } else if (DAILY_AWARD.equals(promotionDefinitionType)) {
            promoForm = getDailyAwardFormForCampaignId(campaign);
        } else if (GIFTING.equals(promotionDefinitionType)) {
            promoForm = getGiftingFormForCampaignId(campaign);
        } else {
            promoForm = null;
        }
        return new CampaignForm(campaign, promoForm);

    }

    public Integer addPlayersToCampaign(final CampaignPlayerUpload campaignPlayerUpload) throws IOException {
        final Set<BigDecimal> playerIds = readPlayerIdCsv(campaignPlayerUpload.getFile().getInputStream());

        campaignAddTargetDao.savePlayersToCampaign(campaignPlayerUpload.getCampaignId(), playerIds);
        return campaignAddTargetDao.numberOfTargetsInCampaign(campaignPlayerUpload.getCampaignId());
    }

    private Set<BigDecimal> readPlayerIdCsv(final InputStream inputStream) throws IOException {
        final HashSet<BigDecimal> playerIds = new HashSet<>();

        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, UTF_8));
        while ((line = br.readLine()) != null) {
            BigDecimal playerId;
            try {
                playerId = BigDecimal.valueOf(Long.valueOf(line));
                playerIds.add(playerId);
            } catch (NumberFormatException nfe) {
                LOG.warn("invalid playerId {}", line);
            }
        }
        return playerIds;
    }


    private Long saveCampaign(final Campaign campaign) {
        final Long campaignId = campaignDefinitionDao.save(new CampaignDefinition(
                null,
                campaign.getName(),
                campaign.getSqlQuery(),
                campaign.getContent(),
                campaign.getChannels(),
                campaign.isPromo(),
                campaign.getChannelConfig(),
                campaign.isEnabled(),
                campaign.isDelayNotifications()));

        campaignScheduleWithNameDao.save(new CampaignSchedule(campaignId,
                campaign.getCampaignScheduleWithName().getNextRunTs(),
                campaign.getCampaignScheduleWithName().getRunHours(),
                campaign.getCampaignScheduleWithName().getRunMinutes(),
                campaign.getCampaignScheduleWithName().getEndTime()));

        return campaignId;
    }

    private void updateCampaign(final Campaign campaign) {
        campaignDefinitionDao.update(new CampaignDefinition(
                campaign.getCampaignId(),
                campaign.getName(),
                campaign.getSqlQuery(),
                campaign.getContent(),
                campaign.getChannels(),
                campaign.isPromo(),
                campaign.getChannelConfig(),
                campaign.isEnabled(),
                campaign.isDelayNotifications()));

        campaignScheduleWithNameDao.update(new CampaignSchedule(campaign.getCampaignId(), campaign.getCampaignScheduleWithName().getNextRunTs(),
                campaign.getCampaignScheduleWithName().getRunHours(), campaign.getCampaignScheduleWithName().getRunMinutes(),
                campaign.getCampaignScheduleWithName().getEndTime()));
    }

    private Campaign getCampaign(long campaignId) {
        final CampaignDefinition campaignDefinition = campaignDefinitionDao.fetchCampaign(campaignId);
        final CampaignSchedule campaignSchedule = campaignScheduleWithNameDao.getCampaignSchedule(campaignId);

        Map<NotificationChannelConfigType, String> channelConfig = campaignDefinition.getChannelConfig();
        String unopened = channelConfig.get(NotificationChannelConfigType.FILTER_OUT_120_DAY_UNOPENED);
        if ("on".equals(unopened)) {
            channelConfig.put(NotificationChannelConfigType.FILTER_OUT_120_DAY_UNOPENED, "true");
        } else {
            channelConfig.put(NotificationChannelConfigType.FILTER_OUT_120_DAY_UNOPENED, "false");
        }

        return new Campaign(
                campaignDefinition.getId(),
                campaignDefinition.getName(),
                campaignSchedule.getNextRunTs(),
                campaignSchedule.getEndTime(),
                campaignSchedule.getRunHours(),
                campaignSchedule.getRunMinutes(),
                campaignDefinition.getSegmentSelectionQuery(),
                campaignDefinition.getContent(),
                campaignDefinition.getChannels(),
                campaignDefinition.hasPromo(),
                channelConfig,
                campaignDefinition.delayNotifications());
    }

    public List<CampaignScheduleWithName> getCampaignScheduleWithNameList() {
        return campaignScheduleWithNameDao.getCampaignList(false);
    }

    private BuyChipsForm getBuyChipsFormForCampaignId(final Campaign campaign) {
        if (campaign.isPromo()) {
            return buyChipsPromotionDefinitionDao.getForm(campaign.getCampaignId());
        }
        return new BuyChipsForm();
    }


    private DailyAwardForm getDailyAwardFormForCampaignId(final Campaign campaign) {
        if (campaign.isPromo()) {
            return dailyAwardPromotionDefinitionDao.getForm(campaign.getCampaignId());
        }
        return new DailyAwardForm();
    }

    private GiftingForm getGiftingFormForCampaignId(final Campaign campaign) {
        if (campaign.isPromo()) {
            return giftingPromotionDefinitionDao.getForm(campaign.getCampaignId());
        }
        return new GiftingForm();
    }

    private void savePromoIfExists(final CampaignForm campaignForm) {
        if (promotionExists(campaignForm)) {
            final PromotionType promotionType = PromotionType.valueOf(campaignForm.getPromotionType());

            if (promotionType == BUY_CHIPS) {
                final BuyChipsForm buyChipsForm = campaignForm.getBuyChipsForm();
                saveBuyChipsPromo(campaignForm, buyChipsForm);
            } else if (promotionType == DAILY_AWARD) {
                final DailyAwardForm dailyAwardForm = campaignForm.getDailyAwardForm();
                saveDailyAwardPromo(campaignForm, dailyAwardForm);
            } else if (promotionType == GIFTING) {
                final GiftingForm giftingForm = campaignForm.getGiftingForm();
                saveGiftingPromo(campaignForm, giftingForm);
            } else {
                throw new UnsupportedOperationException("Unknown promotion type");
            }
        }
    }

    private void saveGiftingPromo(final CampaignForm campaignForm, final GiftingForm giftingForm) {
        giftingForm.setName(campaignForm.getCampaign().getName());
        giftingForm.setCampaignId(campaignForm.getCampaign().getCampaignId());
        giftingPromotionDefinitionDao.save(giftingForm);
    }

    private void saveDailyAwardPromo(final CampaignForm campaignForm, final DailyAwardForm dailyAwardForm) {
        setDailyAwardPromoIdAndName(campaignForm, dailyAwardForm);
        dailyAwardPromotionDefinitionDao.save(dailyAwardForm);
    }

    private void setDailyAwardPromoIdAndName(final CampaignForm campaignForm, final DailyAwardForm dailyAwardForm) {
        dailyAwardForm.setCampaignId(campaignForm.getCampaign().getCampaignId());
        dailyAwardForm.setName(campaignForm.getCampaign().getName());
    }

    private void saveBuyChipsPromo(final CampaignForm campaignForm, final BuyChipsForm buyChipsForm) {
        buyChipsForm.setCampaignId(campaignForm.getCampaign().getCampaignId());
        buyChipsForm.setName(campaignForm.getCampaign().getName());
        buyChipsForm.setPaymentMethods(CampaignHelper.getSupportedPaymentMethods(buyChipsForm.getPlatforms()));
        buyChipsPromotionDefinitionDao.save(buyChipsForm);
    }

    public void disable(final long campaignId) {
        campaignDefinitionDao.setEnabledStatus(campaignId, false);
    }
}
