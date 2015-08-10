package com.yazino.engagement.campaign.application;

import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.payment.PaymentMethodsFactory;
import com.yazino.promotions.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.*;
import strata.server.operations.promotion.model.ChipPackage;
import strata.server.operations.promotion.service.PaymentOptionsToChipPackageTransformer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notNull;
import static org.joda.time.DateTime.now;
import static strata.server.lobby.api.promotion.PromotionType.*;

@Service
public class PromotionCreationService {

    private static final Logger LOG = LoggerFactory.getLogger(PromotionCreationService.class);

    private static final int CONTROL_GROUP_PERCENTAGE = 0;
    private static final ControlGroupFunctionType CONTROL_GROUP_FUNCTION = ControlGroupFunctionType.PLAYER_ID;
    private static final BigDecimal PERCENTAGE_DIVISOR = new BigDecimal("100");
    private static final String IMAGES_GLOSS_DAILY_POPUP_MAIN_IMAGE_PNG = "images/gloss/dailyPopupMainImage.png";
    private static final String IMAGES_GLOSS_DAILY_POPUP_SECONDARY_IMAGE_PNG = "images/gloss/dailyPopupSecondaryImage.png";
    private static final int SCALE_FOR_CHIPS = 0;

    private final PromotionDao jdbcPromotionDAO;
    private final PromotionFormDefinitionDao<BuyChipsForm> buyChipsPromotionDefinitionDao;
    private final PromotionFormDefinitionDao<DailyAwardForm> dailyAwardPromotionDefinitionDao;
    private final PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer;
    private final PromotionDefinitionDao promotionDefinitionDao;
    private final PromotionFormDefinitionDao<GiftingForm> giftingPromotionDefinitionDao;

    @Autowired
    public PromotionCreationService(final PromotionDao jdbcPromotionDAO,
                                    final PromotionFormDefinitionDao<BuyChipsForm> buyChipsPromotionDefinitionDao,
                                    final PromotionFormDefinitionDao<DailyAwardForm> dailyAwardPromotionDefinitionDao,
                                    final PaymentOptionsToChipPackageTransformer paymentOptionsToChipPackageTransformer,
                                    final PromotionDefinitionDao promotionDefinitionDao,
                                    final PromotionFormDefinitionDao<GiftingForm> giftingPromotionDefinitionDao) {
        this.jdbcPromotionDAO = jdbcPromotionDAO;
        this.buyChipsPromotionDefinitionDao = buyChipsPromotionDefinitionDao;
        this.dailyAwardPromotionDefinitionDao = dailyAwardPromotionDefinitionDao;
        this.paymentOptionsToChipPackageTransformer = paymentOptionsToChipPackageTransformer;
        this.promotionDefinitionDao = promotionDefinitionDao;
        this.giftingPromotionDefinitionDao = giftingPromotionDefinitionDao;
    }


    public Long createPromotionForCampaign(final Long campaignId, final List<BigDecimal> playerIds) {
        LOG.info(
                "creating promotion for {} players on campaign {}", playerIds == null ? "all" : playerIds.size(),
                campaignId);

        final PromotionType promotionDefinitionType = promotionDefinitionDao.getPromotionDefinitionType(campaignId);
        if (promotionDefinitionType == BUY_CHIPS) {
            LOG.debug("creating Buy Chips Promo");
            return createBuyChipPromotionForCampaign(campaignId, playerIds);

        } else if (promotionDefinitionType == DAILY_AWARD) {
            LOG.debug("creating DailyAward Promo");

            final DailyAwardForm dailyAwardForm = dailyAwardPromotionDefinitionDao.getForm(campaignId);

            final DailyAwardPromotion promotion = new DailyAwardPromotionBuilder()
                    .withName(dailyAwardForm.getName())
                    .withMaxRewards(dailyAwardForm.getMaxRewards())
                    .withStartDate(new DateTime())
                    .withEndDate(new DateTime().plusHours(dailyAwardForm.getValidForHours()))
                    .withPriority(dailyAwardForm.getPriority())
                    .withPlatforms(dailyAwardForm.getPlatforms())
                    .withControlGroupFunction(CONTROL_GROUP_FUNCTION)
                    .withControlGroupPercentage(CONTROL_GROUP_PERCENTAGE)
                    .withMainImage(IMAGES_GLOSS_DAILY_POPUP_MAIN_IMAGE_PNG)
                    .withSecImage(IMAGES_GLOSS_DAILY_POPUP_SECONDARY_IMAGE_PNG)
                    .withReward(dailyAwardForm.getTopUpAmount())
                    .withAllPlayers(dailyAwardForm.isAllPlayers())
                    .build();

            final Long promoId = jdbcPromotionDAO.create(promotion);
            if (playerIds != null) {
                addPlayersToPromotionForCampaign(campaignId, promoId, new LinkedHashSet<>(playerIds));
            }

            return promoId;
        } else if (promotionDefinitionType == GIFTING) {
            //load up promo from stored form, convert it into a promo
            //add promo config
            final GiftingForm giftingForm = giftingPromotionDefinitionDao.getForm(campaignId);
            //why have this nasty giftingForm/promotion thing?
            final GiftingPromotion giftingPromotion = new GiftingPromotion(
                    now(),
                    now().plusHours(giftingForm.getValidForHours()),
                    giftingForm.getReward(),
                    giftingForm.isAllPlayers(),
                    giftingForm.getName(),
                    giftingForm.getTitle(),
                    giftingForm.getDescription(),
                    giftingForm.getGameType());


            final Long promoId = jdbcPromotionDAO.create(giftingPromotion);//goes to strataprod

            if (playerIds != null) {
                addPlayersToPromotionForCampaign(campaignId, promoId, new LinkedHashSet<>(playerIds));
            }
            //send the gifts! no need. this is done from seg_sel and camp
            return promoId;
        } else {
            throw new UnsupportedOperationException("Unknown promo definition :" + promotionDefinitionType.name());
        }
    }

    public boolean isAGiftingPromo(Long promoId) {
        if (promoId != null) {
            return jdbcPromotionDAO.findById(promoId).getPromotionType() == GIFTING;
        } else {
            return false;
        }
    }

    public void addPlayersToPromotionForCampaign(final Long campaignId,
                                                 final Long promoId,
                                                 final Set<BigDecimal> playerIds) {
        LOG.info("Adding players to promotion {} for {} players on campaign {}", promoId, playerIds.size(), campaignId);

        if (!playerIds.isEmpty()) {
            jdbcPromotionDAO.addPlayersTo(promoId, playerIds);
        }
        jdbcPromotionDAO.updatePlayerCountInPromotion(promoId);
    }

    private Long createBuyChipPromotionForCampaign(final Long campaignId, final List<BigDecimal> playerIds) {
        final BuyChipsForm buyChipsForm = buyChipsPromotionDefinitionDao.getForm(campaignId);

        List<PaymentPreferences.PaymentMethod> paymentMethodsForAllPlatforms = new ArrayList<>();

        for (Platform platform : buyChipsForm.getPlatforms()) {
            final PaymentPreferences.PaymentMethod[] paymentMethodsForPlatform = PaymentMethodsFactory.getPaymentMethodsForPlatform(
                    platform);
            paymentMethodsForAllPlatforms.addAll(asList(paymentMethodsForPlatform));
        }

        BuyChipsPromotion buyChipsPromotion = new BuyChipsPromotionBuilder()
                .withName(buyChipsForm.getName())
                .withInGameHeader(buyChipsForm.getInGameNotificationHeader())
                .withInGameMessage(buyChipsForm.getInGameNotificationMsg())
                .withRollOverHeaderValue(buyChipsForm.getRolloverHeader())
                .withRollOverTextValue(buyChipsForm.getRolloverText())
                .withMaxRewards(buyChipsForm.getMaxRewards())
                .withStartDate(new DateTime())
                .withEndDate(new DateTime().plusHours(buyChipsForm.getValidForHours()))
                .withPriority(buyChipsForm.getPriority())
                .withPlatforms(buyChipsForm.getPlatforms())
                .withControlGroupFunction(CONTROL_GROUP_FUNCTION)
                .withControlGroupPercentage(CONTROL_GROUP_PERCENTAGE)
                .withAllPlayers(buyChipsForm.isAllPlayers())
                .withPaymentMethods(paymentMethodsForAllPlatforms).build();

        Map<Platform, List<ChipPackage>> defaultPackages = paymentOptionsToChipPackageTransformer.getDefaultPackages();

        Map<Integer, BigDecimal> chipsPackagePercentages = buyChipsForm.getChipsPackagePercentages();

        addChipsPackageBasedOnPercentages(buyChipsPromotion, defaultPackages, chipsPackagePercentages);

        final Long promoId = jdbcPromotionDAO.create(buyChipsPromotion);
        if (playerIds != null) {
            addPlayersToPromotionForCampaign(campaignId, promoId, new LinkedHashSet<>(playerIds));
        }

        return promoId;
    }

    protected void addChipsPackageBasedOnPercentages(BuyChipsPromotion buyChipsPromotion,
                                                     Map<Platform, List<ChipPackage>> defaultPackages,
                                                     Map<Integer, BigDecimal> chipsPackagePercentages) {
        notNull(defaultPackages);
        List<Platform> buyChipsPromotionPlatforms = buyChipsPromotion.getPlatforms();

        for (Platform platform : buyChipsPromotionPlatforms) {
            notNull(
                    defaultPackages.get(platform),
                    "FATAL ERROR - Default package is not available for platform " + platform);
            List<ChipPackage> defaultChipPackageForPlatform = new ArrayList<>(defaultPackages.get(platform));
            if (isValid(defaultChipPackageForPlatform, chipsPackagePercentages)) {
                sortChipPackagesBasedOnDefaultChips(defaultChipPackageForPlatform);
                setChipsForPlatformInBuyChipsPromotion(
                        buyChipsPromotion, defaultChipPackageForPlatform, chipsPackagePercentages, platform);
            } else {
                LOG.error("Default chip packages or chipPackagePercentages for {} is not valid", platform);
            }
        }
    }

    private boolean isValid(List<ChipPackage> defaultChipPackageForPlatform,
                            Map<Integer, BigDecimal> chipsPackagePercentages) {
        if ((defaultChipPackageForPlatform != null && chipsPackagePercentages != null)
                && (defaultChipPackageForPlatform.size() == chipsPackagePercentages.size())) {
            return true;
        } else {
            LOG.error(
                    "Invalid defaultChipPackage {} and chipsPackagePercentages {}",
                    defaultChipPackageForPlatform,
                    chipsPackagePercentages);
        }
        return false;
    }

    private void setChipsForPlatformInBuyChipsPromotion(BuyChipsPromotion buyChipsPromotion,
                                                        List<ChipPackage> chipPackagesForPlatform,
                                                        Map<Integer, BigDecimal> chipsPackagePercentages,
                                                        Platform platform) {
        Integer packageIndex = 1;
        for (ChipPackage chipPackage : chipPackagesForPlatform) {
            BigDecimal chipsToOverride = BigDecimal.ZERO;
            BigDecimal chipsPackagePercentage = chipsPackagePercentages.get(packageIndex);
            if (chipPackage.getDefaultChips() != null && chipsPackagePercentage != null) {
                BigDecimal percentageToMultiply = chipsPackagePercentage.divide(PERCENTAGE_DIVISOR);
                BigDecimal extraChips = chipPackage.getDefaultChips().multiply(percentageToMultiply);
                chipsToOverride = chipPackage.getDefaultChips().add(extraChips);
            }
            // Remove trailing zeros and scale or else promotion system won't like it
            buyChipsPromotion.getConfiguration().overrideChipAmountForPlatformAndPackage(
                    platform, chipPackage.getDefaultChips(),
                    chipsToOverride.stripTrailingZeros().setScale(
                            SCALE_FOR_CHIPS,
                            RoundingMode.HALF_UP)
            );
            packageIndex++;
        }
    }

    protected void sortChipPackagesBasedOnDefaultChips(List<ChipPackage> chipPackagesForPlatform) {
        Comparator<ChipPackage> chipsPackageComparator = new Comparator<ChipPackage>() {
            @Override
            public int compare(ChipPackage o1, ChipPackage o2) {
                return o1.getDefaultChips().compareTo(o2.getDefaultChips());
            }
        };
        Collections.sort(chipPackagesForPlatform, chipsPackageComparator);
    }

}
