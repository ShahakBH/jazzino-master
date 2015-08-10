package strata.server.lobby.api.promotion.helper;

import com.yazino.platform.Platform;
import org.joda.time.DateTime;
import strata.server.lobby.api.promotion.ControlGroupFunctionType;
import strata.server.lobby.api.promotion.DailyAwardPromotion;
import strata.server.lobby.api.promotion.PromotionConfiguration;
import strata.server.lobby.api.promotion.PromotionType;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.yazino.platform.Platform.IOS;
import static com.yazino.platform.Platform.WEB;
import static strata.server.lobby.api.promotion.DailyAwardPromotion.*;

public class DailyAwardPromotionBuilder implements PromotionBuilder<DailyAwardPromotion> {

    public static final String MAIN_IMAGE_VALUE = "main image";
    public static final String MAIN_IMAGE_VALUE_FOR_DEFAULT = "default award " + MAIN_IMAGE_VALUE;
    public static final String MAIN_IMAGE_LINK_VALUE = "main image link";
    public static final String MAIN_IMAGE_LINK_VALUE_FOR_DEFAULT = "default award " + MAIN_IMAGE_LINK_VALUE;
    public static final String SECONDARY_IMAGE_VALUE = "secondary image";
    public static final String SECONDARY_IMAGE_VALUE_FOR_DEFAULT = "default award " + SECONDARY_IMAGE_VALUE;
    public static final String SECONDARY_IMAGE_LINK_VALUE = "secondary image link";
    public static final String SECONDARY_IMAGE_LINK_VALUE_FOR_DEFAULT = "default award " + SECONDARY_IMAGE_LINK_KEY;
    public static final String REWARD_CHIPS_VALUE = "4500";
    public static final String REWARD_CHIPS_VALUE_FOR_DEFAULT_AWARD = "1500";
    public static final String MAX_REWARDS_VALUE = "2";
    public static final int PRIORITY = 34;
    public static final boolean ALL_PLAYERS = true;
    public static final List<Platform> ALL_PLATFORMS = Arrays.asList(WEB, IOS);
    public static final DateTime START_DATE = new DateTime(2011, 6, 30, 12, 30, 59, 0);
    public static final DateTime END_DATE = new DateTime(2011, 7, 31, 12, 30, 59, 0);
    public static final int CONTROL_GROUP_PERCENTAGE = 10;
    public static final int SEED = 21;

    private DailyAwardPromotion promo;

    public DailyAwardPromotionBuilder() {
        promo = new DailyAwardPromotion();

        promo.setName(UUID.randomUUID().toString());
        promo.setPlatforms(ALL_PLATFORMS);
        promo.setStartDate(START_DATE);
        promo.setEndDate(END_DATE);
        promo.setAllPlayers(ALL_PLAYERS);
        promo.setPriority(PRIORITY);
        promo.setSeed(SEED);
        promo.setControlGroupPercentage(CONTROL_GROUP_PERCENTAGE);
        promo.setControlGroupFunction(ControlGroupFunctionType.EXTERNAL_ID);

        PromotionConfiguration config = promo.getConfiguration();
        config.addConfigurationItem(MAIN_IMAGE_KEY, MAIN_IMAGE_VALUE);
        config.addConfigurationItem(MAIN_IMAGE_LINK_KEY, MAIN_IMAGE_LINK_VALUE);
        config.addConfigurationItem(SECONDARY_IMAGE_KEY, SECONDARY_IMAGE_VALUE);
        config.addConfigurationItem(SECONDARY_IMAGE_LINK_KEY, SECONDARY_IMAGE_LINK_VALUE);
        config.addConfigurationItem(REWARD_CHIPS_KEY, REWARD_CHIPS_VALUE);
        config.addConfigurationItem(MAX_REWARDS_KEY, MAX_REWARDS_VALUE);

    }

    public DailyAwardPromotionBuilder withReward(Integer rewardChips) {
        if (rewardChips == null) {
            promo.getConfiguration().getConfiguration().remove(REWARD_CHIPS_KEY);
        } else {
            promo.getConfiguration().addConfigurationItem(REWARD_CHIPS_KEY, rewardChips.toString());
        }
        return this;
    }

    public DailyAwardPromotionBuilder withPlatforms(final List<Platform> platforms) {
        promo.setPlatforms(platforms);
        return this;
    }

    public DailyAwardPromotionBuilder withStartDate(final DateTime startDAte) {
        promo.setStartDate(startDAte);
        return this;
    }

    public DailyAwardPromotionBuilder withEndDate(final DateTime endDAte) {
        promo.setEndDate(endDAte);
        return this;
    }

    public DailyAwardPromotionBuilder withId(final Long id) {
        promo.setId(id);
        return this;
    }

    public DailyAwardPromotion getPromotion() {
        return promo;
    }

    public DailyAwardPromotionBuilder withAllPlayers(boolean allPlayers) {
        promo.setAllPlayers(allPlayers);
        return this;
    }

    public DailyAwardPromotionBuilder withConfiguration(final PromotionConfiguration config) {
        promo.setConfiguration(config);
        return this;
    }

    public DailyAwardPromotionBuilder withName(final String name) {
        promo.setName(name);
        return this;
    }

    public DailyAwardPromotionBuilder withMaxRewards(Integer maxRewards) {
        if (maxRewards == null) {
            promo.getConfiguration().getConfiguration().remove(MAX_REWARDS_KEY);
        } else {
            promo.getConfiguration().addConfigurationItem(MAX_REWARDS_KEY, maxRewards.toString());
        }
        return this;
    }

    public DailyAwardPromotionBuilder withPriority(Integer priority) {
        promo.setPriority(priority);
        return this;
    }

    public DailyAwardPromotionBuilder withMainImage(final String mainImageValue) {
        promo.getConfiguration().addConfigurationItem(MAIN_IMAGE_KEY, mainImageValue);
        return this;
    }

    public DailyAwardPromotionBuilder withMainImageLink(final String mainImageLink) {
        promo.getConfiguration().addConfigurationItem(MAIN_IMAGE_LINK_KEY, mainImageLink);
        return this;
    }

    public DailyAwardPromotionBuilder withSecImage(final String secImageValue) {
        promo.getConfiguration().addConfigurationItem(SECONDARY_IMAGE_KEY, secImageValue);
        return this;
    }

    public DailyAwardPromotionBuilder withSecImageLink(final String secImageLink) {
        promo.getConfiguration().addConfigurationItem(SECONDARY_IMAGE_LINK_KEY, secImageLink);
        return this;
    }

    public DailyAwardPromotionBuilder withSeed(Integer seed) {
        promo.setSeed(seed);
        return this;
    }

    public DailyAwardPromotionBuilder withControlGroupPercentage(Integer controlGroupPercentage) {
        promo.setControlGroupPercentage(controlGroupPercentage);
        return this;
    }

    public DailyAwardPromotionBuilder withControlGroupFunction(final ControlGroupFunctionType controlGroupFunction) {
        promo.setControlGroupFunction(controlGroupFunction);
        return this;
    }

    public DailyAwardPromotionBuilder withPromotionType(final PromotionType promotionType) {
        promo.setPromotionType(promotionType);
        return this;
    }
}
