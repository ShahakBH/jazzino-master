package strata.server.lobby.promotion.service;

import com.yazino.platform.Platform;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.promotion.PromoRewardEvent;
import com.yazino.promotions.PromotionDao;
import com.yazino.promotions.PromotionPlayerReward;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import strata.server.lobby.api.promotion.*;
import strata.server.lobby.api.promotion.domain.PlayerPromotionStatus;
import strata.server.lobby.api.promotion.domain.builder.PlayerPromotionStatusBuilder;
import strata.server.lobby.api.promotion.message.TopUpRequest;
import strata.server.lobby.promotion.persistence.PlayerPromotionStatusDao;

import java.math.BigDecimal;
import java.util.List;

import static com.yazino.platform.Platform.ANDROID;
import static com.yazino.platform.Platform.IOS;
import static org.apache.commons.lang3.Validate.notNull;
import static strata.server.lobby.api.promotion.DailyAwardPromotion.REWARD_CHIPS_KEY;
import static strata.server.lobby.api.promotion.PromotionType.BUY_CHIPS;
import static strata.server.lobby.api.promotion.TopUpStatus.*;

@Service
public class ProgressiveDailyAwardService implements DailyAwardPromotionService {
    private static final Logger LOG = LoggerFactory.getLogger(ProgressiveDailyAwardService.class);

    public static final String TRANSACTION_TYPE = "Progressive";
    public static final String TRANSACTION_REFERENCE_TEMPLATE = "Day %s";
    public static final String REWARD_DETAIL_TEMPLATE = "reward=%s";
    public static final String MAIN_IMAGE_CONFIG_KEY = "main.image";
    public static final String MAIN_IMAGE_LINK_CONFIG_KEY = "main.image.link";
    public static final String SECONDARY_IMAGE_LINK_CONFIG_KEY = "secondary.image.link";
    public static final String SECONDARY_IMAGE_CONFIG_KEY = "secondary.image";
    public static final String IOS_IMAGE_KEY = "ios.image";
    public static final String ANDROID_IMAGE_KEY = "android.image";
    private static final DateTimeZone NEW_YORK = DateTimeZone.forID("America/New_York");

    private final PromotionDao promotionDao;
    private final PlayerPromotionStatusDao playerPromotionStatusDao;
    private final com.yazino.platform.community.PlayerService playerService;
    private final PromotionControlGroupService promotionControlGroupService;
    private final PlayerProfileService playerProfileService;
    private final String assetUrl;
    private final String mainImage;
    private final String secondaryImage;
    private final String secondaryImageLink;
    private final QueuePublishingService<PromoRewardEvent> promotionRewardEventService;


    @Autowired
    public ProgressiveDailyAwardService(final PromotionDao promotionDao,
                                        final PlayerPromotionStatusDao playerPromotionStatusDao,
                                        @Qualifier("playerService") final PlayerService playerService,
                                        @Qualifier("promotionControlGroupService")
                                        final PromotionControlGroupService promotionPlayerService,
                                        @Value("${strata.server.lobby.promotion.content}") final String assetUrl,
                                        @Value("${progressive.main.image}") final String mainImage,
                                        @Value("${progressive.main.secondaryImage}") final String secondaryImage,
                                        @Value("${progressive.secondaryImage.link}") final String secondaryImageLink,
                                        @Qualifier("playerProfileService") final PlayerProfileService playerProfileService,
                                        @Qualifier("promoRewardEventQueuePublishingService")
                                        final QueuePublishingService<PromoRewardEvent> promotionRewardEventService) {
        notNull(playerProfileService, "playerProfileService is null");
        notNull(assetUrl, "assetUrl is null");
        notNull(promotionRewardEventService, "promotionRewardEventService is null");
        this.promotionRewardEventService = promotionRewardEventService;
        this.promotionDao = promotionDao;
        this.playerPromotionStatusDao = playerPromotionStatusDao;
        this.playerService = playerService;
        this.promotionControlGroupService = promotionPlayerService;
        this.assetUrl = assetUrl;
        this.mainImage = mainImage;
        this.secondaryImage = secondaryImage;
        this.secondaryImageLink = secondaryImageLink;

        this.playerProfileService = playerProfileService;
    }

    @Override
    @Transactional
    public void awardDailyTopUp(final TopUpRequest topUpRequest) {
        try {
            LOG.info("Awarding progressive and daily topup only: {}", topUpRequest);

            final PlayerPromotionStatus playerPromotionStatus = playerPromotionStatusDao.get(
                    topUpRequest.getPlayerId());

            if (playerHasBeenToppedUpToday(playerPromotionStatus, topUpRequest.getRequestDate())) {
                return;
            }

            processProgressiveDailyAwardPromotion(playerPromotionStatus, topUpRequest.getRequestDate(), topUpRequest.getSessionId());
            addOtherDailyAwards(playerPromotionStatus.getPlayerId(), topUpRequest.getSessionId(),
                    topUpRequest.getRequestDate(), topUpRequest.getPlatform());
        } catch (Exception ex) {
            LOG.error("Something broke processing the topup only awards: {}", topUpRequest, ex);
        }
    }

    @Override
    public TopUpResult getTopUpResult(BigDecimal playerId, Platform platform) {
        final PlayerPromotionStatus playerPromotionStatus = playerPromotionStatusDao.get(playerId);
        if (playerPromotionStatus.getLastTopup() == null) {
            LOG.debug("Player has never been topped up, player: {}", playerId);
            return new TopUpResult(playerId, NEVER_CREDITED, null);
        }
        if (playerPromotionStatus.isTopUpAcknowledged()) {
            LOG.debug("Top up result already been displayed for player {}", playerId);
            return new TopUpResult(playerId, ACKNOWLEDGED, playerPromotionStatus.getLastTopup());
        }
        return buildTopUpResult(playerId, playerPromotionStatus, platform);
    }

    private TopUpResult buildTopUpResult(BigDecimal playerId, PlayerPromotionStatus playerPromotionStatus, Platform platform) {
        LOG.debug("Getting last top up result for player {} on {}", playerId, platform.name());
        final FindPromotionsLinkedToPromotionPlayerRewards findPromotionsForPromotionPlayerRewards =
                new FindPromotionsLinkedToPromotionPlayerRewards(playerId, playerPromotionStatus).invoke();

        TopUpResult topUpResult;
        switch (platform) {
            case WEB:
            case FACEBOOK_CANVAS:
                topUpResult = new WebTopUpResult(playerId, CREDITED, playerPromotionStatus.getLastTopup());
                break;
            case IOS:
            case ANDROID:
            case AMAZON:
                topUpResult = new MobileTopUpResult(playerId, CREDITED, playerPromotionStatus.getLastTopup());
                break;
            default:
                LOG.error("creating top up result for unknown platform. player[{}], platform[{}]", playerId, platform.name());
                return new TopUpResult(playerId, NEVER_CREDITED, playerPromotionStatus.getLastTopup());
        }
        updateResultWithProgressiveDetails(
                findPromotionsForPromotionPlayerRewards.getProgressiveDailyAwardPromotion(),
                topUpResult);
        updateResultWithDailyAwardDetails(playerId, findPromotionsForPromotionPlayerRewards.getDailyAwardPromotion(), topUpResult);
        updateResultWithBuyChipsDetails(playerId, playerPromotionStatus, topUpResult, platform);
        return topUpResult;
    }

    private void updateResultWithDailyAwardDetails(BigDecimal playerId, DailyAwardPromotion dailyAwardPromotion, TopUpResult topUpResult) {
        if (dailyAwardPromotion != null && !isPlayerInControlGroup(playerId, dailyAwardPromotion)) {
            final BigDecimal totalTopUpAmount = topUpResult.getTotalTopUpAmount().add(
                    dailyAwardPromotion.getTopUpAmount());
            topUpResult.setTotalTopUpAmount(totalTopUpAmount);
            if (topUpResult instanceof WebTopUpResult) {
                overrideDefaultImagesAndLinks((WebTopUpResult) topUpResult, dailyAwardPromotion);
            }
            // as of 2012/11/07, the mobile image is not read from daily promotions - i know odd..
        }
    }

    private boolean isPlayerInControlGroup(BigDecimal playerId, Promotion promotion) {
        return promotionControlGroupService.isControlGroupMember(playerProfileService.findByPlayerId(playerId), promotion);
    }

    private void updateResultWithBuyChipsDetails(BigDecimal playerId,
                                                 PlayerPromotionStatus playerPromotionStatus,
                                                 TopUpResult topUpResult,
                                                 Platform platform) {
        final List<Promotion> buyChipPromotions = promotionDao.findPromotionsByTypeOrderByPriority(
                playerId, BUY_CHIPS, platform, playerPromotionStatus.getLastTopup());
        if (buyChipPromotions.size() > 0) {
            final Promotion chipPromotion = buyChipPromotions.get(0);
            if (!isPlayerInControlGroup(playerId, chipPromotion)) {
                if (topUpResult instanceof WebTopUpResult) {
                    overrideDefaultImagesAndLinks((WebTopUpResult) topUpResult, chipPromotion);
                } else if (topUpResult instanceof MobileTopUpResult) {
                    overrideMobileImage(platform, (MobileTopUpResult) topUpResult, chipPromotion);
                }
            }
        }
    }

    private void overrideMobileImage(Platform platform, MobileTopUpResult topUpResult, Promotion chipPromotion) {
        final PromotionConfiguration promotionConfiguration = chipPromotion.getConfiguration();
        String imageKey = null;
        if (platform == ANDROID) {
            imageKey = ANDROID_IMAGE_KEY;
        } else if (platform == IOS) {
            imageKey = IOS_IMAGE_KEY;
        }
        final String image = promotionConfiguration.getConfigurationValue(imageKey);
        if (StringUtils.isNotBlank(image)) {
            topUpResult.setImageUrl(determineImageUrl(image));
        }
    }

    private void updateResultWithProgressiveDetails(ProgressiveDailyAwardPromotion progressiveDailyAwardPromotion,
                                                    TopUpResult topUpResult) {
        if (progressiveDailyAwardPromotion != null) {
            final ProgressiveAwardEnum progressiveAwardEnum = ProgressiveAwardEnum.valueOf(
                    progressiveDailyAwardPromotion.getPromotionType());
            topUpResult.setConsecutiveDaysPlayed(progressiveAwardEnum.getConsecutiveDaysPlayed());
            topUpResult.setTotalTopUpAmount(progressiveDailyAwardPromotion.getAmount());
            if (topUpResult instanceof WebTopUpResult) {
                WebTopUpResult webTopUpResult = (WebTopUpResult) topUpResult;
                webTopUpResult.setPromotionValueList(promotionDao.getProgressiveAwardPromotionValueList());
                webTopUpResult.setMainImage(determineImageUrl(mainImage));
                webTopUpResult.setSecondaryImage(determineImageUrl(secondaryImage));
                webTopUpResult.setSecondaryImageLink(secondaryImageLink);
            }
            // as of 2012/11/07, the mobile image is not read from progressive promotions - i know odd..
        }
    }

    private boolean playerHasBeenToppedUpToday(PlayerPromotionStatus playerPromotionStatus, DateTime currentTime) {
        if (playerPromotionStatus.getLastTopup() != null) {
            if (isLastTopUpToday(playerPromotionStatus.getLastTopup(), currentTime)) {
                LOG.info("Player, playerId={}, has already been topped up today ({})",
                        playerPromotionStatus.getPlayerId(), currentTime);
                return true;
            }
        }
        return false;
    }

    private void processProgressiveDailyAwardPromotion(PlayerPromotionStatus playerPromotionStatus,
                                                       DateTime currentTime,
                                                       final BigDecimal sessionId)
            throws WalletServiceException {
        final BigDecimal playerId = playerPromotionStatus.getPlayerId();
        if (playerPlayedYesterdayOrHasNeverPlayed(playerPromotionStatus, currentTime)) {
            applyProgressiveAward(playerId, sessionId, playerPromotionStatus, currentTime);
        } else {
            applyProgressiveAward(playerId, sessionId, new PlayerPromotionStatusBuilder(playerPromotionStatus).withConsecutiveDaysPlayed(0).build(),
                    currentTime);
        }
    }

    private boolean playerPlayedYesterdayOrHasNeverPlayed(final PlayerPromotionStatus playerPromotionStatus,
                                                          final DateTime currentTime) {
        return playerPromotionStatus.getLastPlayed() == null
                || playerPromotionStatus.getLastPlayed().plusDays(1).dayOfYear().get() >= currentTime.getDayOfYear();
    }

    public void applyProgressiveAward(final BigDecimal playerId,
                                      final BigDecimal sessionId,
                                      final PlayerPromotionStatus playerPromotionStatus,
                                      final DateTime currentTime) throws WalletServiceException {
        LOG.info("doing progressive topup. playerId={} status={}", playerId, playerPromotionStatus);

        final ProgressiveAwardEnum progressiveAwardEnum = ProgressiveAwardEnum
                .getProgressiveAwardEnumForConsecutiveDaysPlayed(playerPromotionStatus.getConsecutiveDaysPlayed());
        final List<ProgressiveDailyAwardPromotion> progressiveDailyAwardPromotionList =
                promotionDao.getProgressiveDailyAwardPromotion(playerId,
                        currentTime,
                        progressiveAwardEnum);

        final ProgressiveDailyAwardPromotion progressiveDailyAwardPromotion = progressiveDailyAwardPromotionList.get(0);

        final BigDecimal amount = progressiveDailyAwardPromotion.getAmount();

        playerService.postTransaction(playerId,
                sessionId,
                amount,
                TRANSACTION_TYPE,
                String.format(TRANSACTION_REFERENCE_TEMPLATE, progressiveAwardEnum));

        playerPromotionStatusDao.save(new PlayerPromotionStatusBuilder(playerPromotionStatus)
                .withLastTopupDate(currentTime)
                .withTopUpAcknowledged(false)
                .build());

        final PromotionPlayerReward promotionPlayerReward =
                new PromotionPlayerReward(progressiveDailyAwardPromotion.getPromoId(),
                        playerId,
                        false,
                        currentTime,
                        String.format(REWARD_DETAIL_TEMPLATE, amount));

        promotionDao.addLastReward(promotionPlayerReward);
        promotionRewardEventService.send(new PromoRewardEvent(playerId, progressiveDailyAwardPromotion.getPromoId(), currentTime));

        LOG.info("Progressive topup complete. playerId={} topup amount={}", playerId, amount);
    }

    private void addOtherDailyAwards(final BigDecimal playerId,
                                     final BigDecimal sessionId,
                                     final DateTime currentTime,
                                     final Platform platform)
            throws WalletServiceException {
        final List<Promotion> dailyAwardPromotions = promotionDao.findPromotionsByTypeOrderByPriority(
                playerId,
                PromotionType.DAILY_AWARD,
                platform,
                currentTime
        );

        if (dailyAwardPromotions != null) {
            if (!dailyAwardPromotions.isEmpty()) {
                final Promotion dailyAwardPromotion = dailyAwardPromotions.get(0);

                if (!promotionControlGroupService.isControlGroupMember(
                        playerProfileService.findByPlayerId(playerId),
                        dailyAwardPromotion)) {
                    final PromotionConfiguration dailyAwardPromotionConfiguration =
                            dailyAwardPromotion.getConfiguration();
                    final BigDecimal topUpAmount = new BigDecimal(
                            dailyAwardPromotionConfiguration.getConfigurationValueAsInteger(REWARD_CHIPS_KEY));
                    playerService.postTransaction(playerId,
                            sessionId,
                            topUpAmount,
                            "TopUp",
                            "Other Daily Award Promotion");

                    final PromotionPlayerReward promotionPlayerReward =
                            new PromotionPlayerReward(dailyAwardPromotion.getId(),
                                    playerId,
                                    false,
                                    currentTime,
                                    String.format(REWARD_DETAIL_TEMPLATE, topUpAmount));

                    promotionDao.addLastReward(promotionPlayerReward);
                    promotionRewardEventService.send(new PromoRewardEvent(playerId, dailyAwardPromotion.getId(), currentTime));

                }
            }
        }
    }

    private boolean isLastTopUpToday(final DateTime lastTopUp, final DateTime currentTime) {
        if (lastTopUp == null) {
            return false;
        }
        final DateTime lastTopUpNYDay = new DateTime(lastTopUp, NEW_YORK).withTimeAtStartOfDay();
        final DateTime currentNYDay = new DateTime(currentTime, NEW_YORK).withTimeAtStartOfDay();

        return lastTopUpNYDay.equals(currentNYDay);
    }

    private String determineImageUrl(final String imageUrl) {
        if (!imageUrl.contains("http")) {
            return String.format("%s/%s", assetUrl, imageUrl);
        }
        return imageUrl;
    }

    private WebTopUpResult overrideDefaultImagesAndLinks(final WebTopUpResult webTopUpResult,
                                                         final Promotion promotion) {
        overridePrimaryImageAndLink(webTopUpResult, promotion);
        overrideSecondaryImageAndLink(webTopUpResult, promotion);

        return webTopUpResult;
    }

    private WebTopUpResult overridePrimaryImageAndLink(final WebTopUpResult webTopUpResult,
                                                       final Promotion promotion) {
        final PromotionConfiguration promotionConfiguration = promotion.getConfiguration();
        final String otherPrimaryImage = promotionConfiguration
                .getConfigurationValue(MAIN_IMAGE_CONFIG_KEY);

        if (StringUtils.isNotBlank(otherPrimaryImage)) {
            webTopUpResult.setMainImage(determineImageUrl(otherPrimaryImage));
            final String otherMainImageLink = promotionConfiguration.getConfigurationValue(
                    MAIN_IMAGE_LINK_CONFIG_KEY);
            webTopUpResult.setMainImageLink(otherMainImageLink);
        }

        return webTopUpResult;
    }

    private WebTopUpResult overrideSecondaryImageAndLink(final WebTopUpResult webTopUpResult,
                                                         final Promotion promotion) {
        final String otherPromotionSecondaryImage = promotion
                .getConfiguration()
                .getConfigurationValue(SECONDARY_IMAGE_CONFIG_KEY);

        if (StringUtils.isNotBlank(otherPromotionSecondaryImage)) {
            webTopUpResult.setSecondaryImage(determineImageUrl(otherPromotionSecondaryImage));
            final String overrideSecondaryImageLink = promotion.getConfiguration().getConfigurationValue(
                    SECONDARY_IMAGE_LINK_CONFIG_KEY);
            webTopUpResult.setSecondaryImageLink(overrideSecondaryImageLink);
        }

        return webTopUpResult;
    }

    private class FindPromotionsLinkedToPromotionPlayerRewards {
        private BigDecimal playerId;
        private PlayerPromotionStatus playerPromotionStatus;
        private DailyAwardPromotion dailyAwardPromotion;
        private ProgressiveDailyAwardPromotion progressiveDailyAwardPromotion;

        public FindPromotionsLinkedToPromotionPlayerRewards(final BigDecimal playerId,
                                                            final PlayerPromotionStatus playerPromotionStatus) {
            this.playerId = playerId;
            this.playerPromotionStatus = playerPromotionStatus;
        }

        public DailyAwardPromotion getDailyAwardPromotion() {
            return dailyAwardPromotion;
        }

        public ProgressiveDailyAwardPromotion getProgressiveDailyAwardPromotion() {
            return progressiveDailyAwardPromotion;
        }

        public FindPromotionsLinkedToPromotionPlayerRewards invoke() {
            final List<PromotionPlayerReward> promotionPlayerRewards = promotionDao.findPromotionPlayerRewards(
                    playerId, playerPromotionStatus.getLastTopup());
            dailyAwardPromotion = null;
            progressiveDailyAwardPromotion = null;
            for (PromotionPlayerReward promotionPlayerReward : promotionPlayerRewards) {
                final Promotion promotion = promotionDao.findById(promotionPlayerReward.getPromoId());
                if (promotion.getPromotionType() == PromotionType.DAILY_AWARD) {
                    dailyAwardPromotion = (DailyAwardPromotion) promotion;
                } else if (promotion instanceof ProgressiveDailyAwardPromotion) {
                    progressiveDailyAwardPromotion = (ProgressiveDailyAwardPromotion) promotion;
                }
            }
            return this;
        }
    }
}
