package helper;

import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.promotions.PromotionBuilder;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import strata.server.lobby.api.promotion.BuyChipsPromotion;
import strata.server.lobby.api.promotion.ControlGroupFunctionType;
import strata.server.lobby.api.promotion.PromotionConfiguration;

import java.math.BigDecimal;
import java.util.*;

import static com.yazino.platform.Platform.*;
import static strata.server.lobby.api.promotion.BuyChipsPromotion.*;
import static strata.server.lobby.api.promotion.DailyAwardPromotion.*;

public class BuyChipsPromotionTestBuilder implements PromotionBuilder<BuyChipsPromotion> {

    private static final String ROLLOVER_HEADER_VALUE = "rollover header";
    private static final String ROLLOVER_TEXT_VALUE = "rollover text";
    private static final String PAYMENT_METHODS_VALUE = PaymentPreferences.PaymentMethod.CREDITCARD.name()
            + ',' + PaymentPreferences.PaymentMethod.PAYPAL.name();
    private static final String IN_GAME_NOTIFICATION_MSG_VALUE = "ingame message";
    private static final String IN_GAME_NOTIFICATION_HEADER_VALUE = "ingame header";
    public static final String CHIP_DEFAULT_PACKAGE_VALUE = "10000";
    public static final int SEED = 37;
    public static final int CONTROL_GROUP_PERCENTAGE = 10;
    public static final int PRIORITY = 34;
    public static final List<Platform> ALL_PLATFORMS = Arrays.asList(WEB, IOS, ANDROID);
    public static final DateTime START_DATE = new DateTime(2011, 6, 30, 12, 30, 59, 0);
    public static final DateTime END_DATE = new DateTime(2011, 7, 31, 12, 30, 59, 0);
    public static final String MAIN_IMAGE_VALUE = "MAIN_IMAGE_VALUE";
    public static final String MAIN_IMAGE_LINK_VALUE = "MAIN_IMAGE_LINK_VALU";
    public static final String SECONDARY_IMAGE_VALUE = "SECONDARY_IMAGE_VALUE";
    public static final String SECONDARY_IMAGE_LINK_VALUE = "SECONDARY_IMAGE_LINK_VALUE";
    public static final String REWARD_CHIPS_VALUE = "30";
    public static final String MAX_REWARDS_VALUE = "3";

    private BuyChipsPromotion promo;

    private final Map<Platform, Map<BigDecimal, BigDecimal>> platformToDefaultToOverriddenChipAmounts = new HashMap<Platform, Map<BigDecimal, BigDecimal>>();

    public BuyChipsPromotionTestBuilder() {
        promo = new BuyChipsPromotion();
        promo.setName(UUID.randomUUID().toString());
        promo.setPlatforms(ALL_PLATFORMS);
        promo.setStartDate(START_DATE);
        promo.setEndDate(END_DATE);
        promo.setAllPlayers(true);
        promo.setPriority(PRIORITY);
        promo.setSeed(SEED);
        promo.setControlGroupPercentage(CONTROL_GROUP_PERCENTAGE);
        promo.setControlGroupFunction(ControlGroupFunctionType.EXTERNAL_ID);

        PromotionConfiguration config = promo.getConfiguration();
        config.addConfigurationItem(MAIN_IMAGE_KEY, MAIN_IMAGE_VALUE);
        config.addConfigurationItem(MAIN_IMAGE_LINK_KEY, MAIN_IMAGE_LINK_VALUE);
        config.addConfigurationItem(SECONDARY_IMAGE_KEY, SECONDARY_IMAGE_VALUE);
        config.addConfigurationItem(SECONDARY_IMAGE_LINK_KEY, SECONDARY_IMAGE_LINK_VALUE);
        config.addConfigurationItem(MAX_REWARDS_KEY, MAX_REWARDS_VALUE);

        config.addConfigurationItem(PAYMENT_METHODS_KEY, PAYMENT_METHODS_VALUE);
        config.addConfigurationItem(IN_GAME_NOTIFICATION_HEADER_KEY, IN_GAME_NOTIFICATION_HEADER_VALUE);
        config.addConfigurationItem(IN_GAME_NOTIFICATION_MSG_KEY, IN_GAME_NOTIFICATION_MSG_VALUE);
        config.addConfigurationItem(ROLLOVER_HEADER_KEY, ROLLOVER_HEADER_VALUE);
        config.addConfigurationItem(ROLLOVER_TEXT_KEY, ROLLOVER_TEXT_VALUE);

        withChips(CHIP_DEFAULT_PACKAGE_VALUE, "1", WEB);
    }

    public BuyChipsPromotion build() {
        return (BuyChipsPromotion) promo;
    }

    public BuyChipsPromotionTestBuilder withPlatforms(final List<Platform> platforms) {
        promo.setPlatforms(platforms);
        return this;
    }

    public BuyChipsPromotionTestBuilder withStartDate(final DateTime startDAte) {
        promo.setStartDate(startDAte);
        return this;
    }

    public BuyChipsPromotionTestBuilder withEndDate(final DateTime endDAte) {
        promo.setEndDate(endDAte);
        return this;
    }

    public BuyChipsPromotionTestBuilder withId(final Long id) {
        promo.setId(id);
        return this;
    }

    public BuyChipsPromotionTestBuilder withAllPlayers(boolean allPlayers) {
        promo.setAllPlayers(allPlayers);
        return this;
    }

    public BuyChipsPromotionTestBuilder withConfiguration(final PromotionConfiguration config) {
        promo.setConfiguration(config);
        return this;
    }

    public BuyChipsPromotionTestBuilder withName(final String name) {
        promo.setName(name);
        return this;
    }

    public BuyChipsPromotionTestBuilder withMaxRewards(Integer maxRewards) {
        if (maxRewards == null) {
            promo.getConfiguration().getConfiguration().remove(MAX_REWARDS_KEY);
        } else {
            promo.getConfiguration().addConfigurationItem(MAX_REWARDS_KEY, maxRewards.toString());
        }
        return this;
    }

    public BuyChipsPromotionTestBuilder withPriority(Integer priority) {
        promo.setPriority(priority);
        return this;
    }

    public BuyChipsPromotionTestBuilder withChips(String defaultPackage, String promoChips, Platform client) {
        if (StringUtils.isBlank(promoChips)) {
            promo.getConfiguration().removeChipAmountOverrideForPlatformAndPackage(client, new BigDecimal(defaultPackage));
        } else {
            promo.getConfiguration().overrideChipAmountForPlatformAndPackage(client, new BigDecimal(defaultPackage), new BigDecimal(promoChips));
        }
        return this;
    }

    public BuyChipsPromotionTestBuilder withPaymentMethod(PaymentPreferences.PaymentMethod paymentMethod) {
        promo.addConfigurationItem(PAYMENT_METHODS_KEY, paymentMethod.name());
        return this;
    }

    public BuyChipsPromotionTestBuilder withInGameHeader(String header) {
        promo.addConfigurationItem(IN_GAME_NOTIFICATION_HEADER_KEY, header);
        return this;
    }

    public BuyChipsPromotionTestBuilder withInGameMessage(String msg) {
        promo.addConfigurationItem(IN_GAME_NOTIFICATION_MSG_KEY, msg);
        return this;
    }

    public BuyChipsPromotionTestBuilder withRollOverHeaderValue(String value) {
        promo.addConfigurationItem(ROLLOVER_HEADER_KEY, value);
        return this;
    }

    public BuyChipsPromotionTestBuilder withRollOverTextValue(String value) {
        promo.addConfigurationItem(ROLLOVER_TEXT_KEY, value);
        return this;
    }

    public BuyChipsPromotionTestBuilder withControlGroupFunction(final ControlGroupFunctionType controlGroupFunction) {
        promo.setControlGroupFunction(controlGroupFunction);
        return this;
    }

    public BuyChipsPromotionTestBuilder withControlGroupPercentage(Integer controlGroupPercentage) {
        promo.setControlGroupPercentage(controlGroupPercentage);
        return this;
    }

    public BuyChipsPromotionTestBuilder withSeed(int seed) {
        promo.setSeed(seed);
        return this;
    }

    public BuyChipsPromotionTestBuilder withDefaultChipsForPlatformAndPackage(BigDecimal defaultChipAmount, Platform web) {
        promo.getConfiguration().removeChipAmountOverrideForPlatformAndPackage(web, defaultChipAmount);
        return this;
    }
}
