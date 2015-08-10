package strata.server.lobby.promotion.service;

import com.google.common.base.Optional;
import com.yazino.bi.payment.PaymentOption;
import com.yazino.bi.payment.PromotionPaymentOption;
import com.yazino.bi.payment.persistence.JDBCPaymentOptionDAO;
import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.messaging.publisher.QueuePublishingService;
import com.yazino.platform.payment.PlatformPaymentMethods;
import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import com.yazino.platform.reference.Currency;
import com.yazino.promotion.PromoRewardEvent;
import com.yazino.promotions.PromotionDao;
import com.yazino.promotions.PromotionPlayerReward;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import strata.server.lobby.api.promotion.BuyChipsPromotionService;
import strata.server.lobby.api.promotion.InGameMessage;
import strata.server.lobby.api.promotion.Promotion;
import strata.server.lobby.api.promotion.PromotionConfiguration;
import strata.server.lobby.api.promotion.util.PromotionPriorityDateComparator;
import strata.server.lobby.promotion.tools.JsonHelper;

import java.math.BigDecimal;
import java.util.*;

import static com.yazino.platform.Platform.FACEBOOK_CANVAS;
import static org.apache.commons.lang3.Validate.notNull;
import static strata.server.lobby.api.promotion.BuyChipsPromotion.*;
import static strata.server.lobby.api.promotion.PromotionType.BUY_CHIPS;

@Service
public class RemotingBuyChipsPromotionService implements BuyChipsPromotionService {
    /*
    Defaults to WEB usage. CHIP_AMOUNT_FORMAT_KEY usage illustrates.
     */
    private static final Logger LOG = LoggerFactory.getLogger(RemotingBuyChipsPromotionService.class);

    public static final String PLAYER_REWARD_DETAILS = "method=%s, defaultChips=%s, promoChips=%s";

    private final PromotionDao promotionDao;
    private final JDBCPaymentOptionDAO paymentOptionDAO;
    private final PromotionControlGroupService controlGroupService;
    private final PlayerProfileService playerProfileService;
    private final QueuePublishingService<PromoRewardEvent> promotionRewardEventService;

    @Autowired
    public RemotingBuyChipsPromotionService(
            @Qualifier("mysqlPromotionDao") final PromotionDao promotionDao,
            final JDBCPaymentOptionDAO paymentOptionDAO,
            @Qualifier("promotionControlGroupService") final PromotionControlGroupService controlGroupService,
            @Qualifier("playerProfileService") final PlayerProfileService playerProfileService,
            @Qualifier("promoRewardEventQueuePublishingService")
            final QueuePublishingService<PromoRewardEvent> promotionRewardEventService) {
        notNull(promotionDao, "promotionDao is null");
        notNull(paymentOptionDAO, "paymentOptionDAO is null");
        notNull(controlGroupService, "controlGroupService is null");
        notNull(playerProfileService, "playerProfileService is null");
        notNull(promotionRewardEventService, "promoRewardEventService is null");
        this.promotionRewardEventService = promotionRewardEventService;
        this.promotionDao = promotionDao;
        this.paymentOptionDAO = paymentOptionDAO;
        this.controlGroupService = controlGroupService;
        this.playerProfileService = playerProfileService;
    }

    @Override
    public Map<Currency, List<PaymentOption>> getBuyChipsPaymentOptionsFor(
            final BigDecimal playerId,
            final Platform platform) {
        notNull(playerId, "playerId is null");
        notNull(platform, "platform is null");

        LOG.debug("Getting buy chip payment options for player[{}]", playerId);

        final Map<PaymentPreferences.PaymentMethod, Promotion> applicablePromotions
                = promotionDao.getBuyChipsPromotions(playerId, platform, new DateTime());

        if (applicablePromotions == null || applicablePromotions.isEmpty()) {
            return paymentOptionDAO.findByPlatformWithCurrencyKey(platform);
        } else {
            return promotionalPaymentOptionsForPlayerForClient(playerId, applicablePromotions, platform,
                                                               profileFor(playerId));
        }
    }

    private Map<Currency, List<PaymentOption>> promotionalPaymentOptionsForPlayerForClient(
            final BigDecimal playerId,
            final Map<PaymentPreferences.PaymentMethod, Promotion> applicablePromotions,
            final Platform platform, final PlayerProfile playerProfile) {
        LOG.debug("Applicable promotions for player [{}] are [{}]", playerId, applicablePromotions);

        final Map<Currency, List<PaymentOption>> paymentOptions
                = new HashMap<Currency, List<PaymentOption>>();

        // deep copy default payment options and add additional promo details if applicable
        for (PaymentOption defaultOption : paymentOptionDAO.findByPlatform(platform)) {
            final Currency acceptedCurrency = Currency.valueOf(defaultOption.getRealMoneyCurrency());
            List<PaymentOption> existingOptions = paymentOptions.get(acceptedCurrency);
            if (existingOptions == null) {
                existingOptions = new ArrayList<PaymentOption>();
                paymentOptions.put(acceptedCurrency, existingOptions);
            }
            final PaymentOption promotionalOption = createPaymentOptionWithPromotionDetails(
                    applicablePromotions, defaultOption, platform, playerProfile);
            existingOptions.add(promotionalOption);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Payment options for player [%s] are [%s]", playerId,
                    new JsonHelper().serialize(paymentOptions)));
        }

        return paymentOptions;
    }

    private PaymentOption createPaymentOptionWithPromotionDetails(
            final Map<PaymentPreferences.PaymentMethod, Promotion> applicationPromotions,
            final PaymentOption paymentOption,
            final Platform client, final PlayerProfile playerProfile) {
        final PaymentOption paymentOptionWithPromo = new PaymentOption(paymentOption);
        for (PaymentPreferences.PaymentMethod paymentMethod : applicationPromotions.keySet()) {
            final Promotion promotion = applicationPromotions.get(paymentMethod);
            if (promotion == null) {
                LOG.warn("No promotion application for payment method {}", paymentMethod);
                continue;
            }

            paymentOptionWithPromo.addPromotionPaymentOption(promotionPaymentOptionFor(
                    paymentMethod, promotion, client, paymentOption, playerProfile, paymentOption));
        }
        return paymentOptionWithPromo;
    }

    private PlayerProfile profileFor(final BigDecimal playerId) {
        final PlayerProfile playerProfile = playerProfileService.findByPlayerId(playerId);
        if (playerProfile == null) {
            throw new IllegalArgumentException("Invalid player ID: " + playerId);
        }
        return playerProfile;
    }

    @Override
    public PaymentOption getPaymentOptionFor(final BigDecimal playerId,
                                             final Long promotionId,
                                             final PaymentPreferences.PaymentMethod paymentMethod,
                                             final String paymentOptionId) {
        final Promotion promotion = promotionDao.findById(promotionId);
        if (promotion == null) {
            return null;
        }

        final Platform platform = PlatformPaymentMethods.getPlatformForPaymentMethod(paymentMethod);

        final Optional<PaymentOption> defaultPaymentOption = paymentOptionDAO.findByIdAndPlatform(paymentOptionId, platform);
        if (!defaultPaymentOption.isPresent()) {
            return null;
        }

        final PlayerProfile playerProfile = profileFor(playerId);
        final String[] methods = promotion.getConfiguration().getConfigurationValue(PAYMENT_METHODS_KEY).split(",");
        for (String method : methods) {
            if (paymentMethod.name().equals(method)) {
                final PaymentOption paymentOption = new PaymentOption(defaultPaymentOption.get());
                paymentOption.addPromotionPaymentOption(promotionPaymentOptionFor(
                        paymentMethod, promotion, platform, defaultPaymentOption.get(),
                        playerProfile, paymentOption));
                return paymentOption;
            }
        }
        return null;
    }

    private PromotionPaymentOption promotionPaymentOptionFor(final PaymentPreferences.PaymentMethod paymentMethod,
                                                             final Promotion promotion,
                                                             final Platform defaultClientForMethod,
                                                             final PaymentOption defaultPaymentOption,
                                                             final PlayerProfile playerProfile,
                                                             final PaymentOption paymentOption) {
        final BigDecimal promoChips = promotion.getConfiguration().getConfigurationValueAsBigDecimal(
                String.format(CHIP_AMOUNT_FORMAT_KEY, defaultClientForMethod,
                        defaultPaymentOption.getNumChipsPerPurchase().toString()));

        PromotionPaymentOption promotionPaymentOption;
        if (inControlGroup(promotion, playerProfile) || promoChips == null) {
            promotionPaymentOption = new PromotionPaymentOption(paymentMethod, promotion.getId(),
                    paymentOption.getNumChipsPerPurchase(), "", "");
        } else {
            final String rolloverHeader = promotion.getConfiguration().getConfigurationValue(ROLLOVER_HEADER_KEY);
            final String rolloverText = promotion.getConfiguration().getConfigurationValue(ROLLOVER_TEXT_KEY);
            promotionPaymentOption = new PromotionPaymentOption(paymentMethod, promotion.getId(),
                    promoChips, rolloverHeader, rolloverText);
        }
        return promotionPaymentOption;
    }

    private boolean inControlGroup(final Promotion promotion, final PlayerProfile playerProfile) {
        return controlGroupService.isControlGroupMember(playerProfile, promotion);
    }

    @Override
    public Boolean hasPromotion(final BigDecimal playerId, final Platform platform) {
        notNull(playerId, "playerId is null");
        notNull(platform, "platform is null");
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Getting buy chip payment options for player[%s]", playerId));
        }

        final Map<PaymentPreferences.PaymentMethod, Promotion> applicablePromotions
                = promotionDao.getBuyChipsPromotions(playerId, platform, new DateTime());
        return !(applicablePromotions == null || applicablePromotions.isEmpty());
    }

    @Override
    public PaymentOption getDefaultPaymentOptionFor(final String paymentOptionId, Platform platform) {
        final Optional<PaymentOption> paymentOption = paymentOptionDAO.findByIdAndPlatform(paymentOptionId, platform);
        if (paymentOption.isPresent()) {
            return paymentOption.get();
        }
        return null;
    }

    @Override
    public PaymentOption getDefaultFacebookPaymentOptionFor(final String paymentOptionId) {
        final Optional<PaymentOption> paymentOption = paymentOptionDAO.findByIdAndPlatform(paymentOptionId, FACEBOOK_CANVAS);
        if (paymentOption.isPresent()) {
            return paymentOption.get();
        }
        return null;
    }

    @Override
    public void logPlayerReward(final BigDecimal playerId,
                                final Long promotionId,
                                final PaymentPreferences.PaymentMethod paymentMethod,
                                final String paymentOptionId,
                                final DateTime awardDate) {
        final PaymentOption paymentOption = getPaymentOptionFor(playerId, promotionId, paymentMethod, paymentOptionId);
        if (paymentOption != null) {
            final PlayerProfile playerProfile = profileFor(playerId);
            final Promotion promotion = promotionDao.findById(promotionId);
            final String details = String.format(PLAYER_REWARD_DETAILS, paymentMethod.name(),
                    paymentOption.getNumChipsPerPurchase(), paymentOption.getNumChipsPerPurchase(paymentMethod.name()));
            promotionRewardEventService.send(new PromoRewardEvent(playerId, promotionId, awardDate));
            promotionDao.addLastReward(new PromotionPlayerReward(promotionId,
                    playerId, inControlGroup(promotion, playerProfile), awardDate, details));
        }
    }

    @Override
    public void logPlayerReward(BigDecimal playerId,
                                Long promotionId,
                                BigDecimal defaultChips,
                                BigDecimal promoChips,
                                PaymentPreferences.PaymentMethod paymentMethod,
                                DateTime awardDate) {
        final Promotion promotion = promotionDao.findById(promotionId);
        final String details = String.format(PLAYER_REWARD_DETAILS, paymentMethod.name(), defaultChips, promoChips);
        final PlayerProfile playerProfile = profileFor(playerId);

        if (promotion == null) {
            LOG.info("Player {} purchased with promotion that can no longer be found {}", playerId, promotionId);
            return;
        }
        final boolean isControlGroup = inControlGroup(promotion, playerProfile);
        promotionRewardEventService.send(new PromoRewardEvent(playerId, promotionId, awardDate));
        promotionDao.addLastReward(new PromotionPlayerReward(promotionId, playerId, isControlGroup, awardDate, details));
    }

    @Override
    public InGameMessage getInGameMessageFor(final BigDecimal playerId) {
        return getInGameMessage(playerId, Platform.WEB);
    }

    @Override
    public InGameMessage getInGameMessageFor(final BigDecimal playerId, Platform platform) {
        return getInGameMessage(playerId, platform);
    }

    public InGameMessage getInGameMessage(final BigDecimal playerId, Platform platform) {
        final PlayerProfile playerProfile = profileFor(playerId);
        final List<Promotion> promotions = promotionDao.findPromotionsFor(playerId,
                BUY_CHIPS,
                platform, new DateTime());
        if (CollectionUtils.isNotEmpty(promotions)) {
            Collections.sort(promotions, new PromotionPriorityDateComparator());
            final Promotion promotion = promotions.get(0);
            if (!inControlGroup(promotion, playerProfile)) {
                final PromotionConfiguration cfg = promotion.getConfiguration();
                return new InGameMessage(cfg.getConfigurationValue(IN_GAME_NOTIFICATION_HEADER_KEY),
                        cfg.getConfigurationValue(IN_GAME_NOTIFICATION_MSG_KEY));
            }
        }

        return null;
    }
}
