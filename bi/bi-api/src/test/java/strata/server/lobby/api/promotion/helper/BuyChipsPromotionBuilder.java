package strata.server.lobby.api.promotion.helper;

import com.yazino.platform.Platform;
import com.yazino.platform.community.PaymentPreferences;
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
import static strata.server.lobby.api.promotion.helper.DailyAwardPromotionBuilder.*;

public class BuyChipsPromotionBuilder implements PromotionBuilder<BuyChipsPromotion> {

    private static final String ROLLOVER_HEADER_VALUE = "rollover header";
    private static final String ROLLOVER_TEXT_VALUE = "rollover text";
    private static final String PAYMENT_METHODS_VALUE = PaymentPreferences.PaymentMethod
            .CREDITCARD
            .name() + ',' + PaymentPreferences.PaymentMethod.PAYPAL.name();
    private static final String IN_GAME_NOTIFICATION_MSG_VALUE = "ingame message";
    private static final String IN_GAME_NOTIFICATION_HEADER_VALUE = "ingame header";
    public static final String CHIP_DEFAULT_PACKAGE_VALUE = "10000";
    public static final int SEED = 37;
    public static final int CONTROL_GROUP_PERCENTAGE = 10;
    public static final int PRIORITY = 34;
    public static final List<Platform> ALL_PLATFORMS = Arrays.asList(WEB, IOS, ANDROID);
    public static final DateTime START_DATE = new DateTime(2011, 6, 30, 12, 30, 59, 0);
    public static final DateTime END_DATE = new DateTime(2011, 7, 31, 12, 30, 59, 0);

    private BuyChipsPromotion promo;

    private final Map<Platform, Map<BigDecimal, BigDecimal>> platformToDefaultToOverriddenChipAmounts = new HashMap<Platform, Map<BigDecimal, BigDecimal>>();

    public BuyChipsPromotionBuilder() {
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

    public BuyChipsPromotion getPromotion() {
        return (BuyChipsPromotion) promo;
    }

    public BuyChipsPromotionBuilder withPlatforms(final List<Platform> platforms) {
        promo.setPlatforms(platforms);
        return this;
    }

    public BuyChipsPromotionBuilder withStartDate(final DateTime startDAte) {
        promo.setStartDate(startDAte);
        return this;
    }

    public BuyChipsPromotionBuilder withEndDate(final DateTime endDAte) {
        promo.setEndDate(endDAte);
        return this;
    }

    public BuyChipsPromotionBuilder withId(final Long id) {
        promo.setId(id);
        return this;
    }

    public BuyChipsPromotionBuilder withAllPlayers(boolean allPlayers) {
        promo.setAllPlayers(allPlayers);
        return this;
    }

    public BuyChipsPromotionBuilder withConfiguration(final PromotionConfiguration config) {
        promo.setConfiguration(config);
        return this;
    }

    public BuyChipsPromotionBuilder withName(final String name) {
        promo.setName(name);
        return this;
    }

    public BuyChipsPromotionBuilder withMaxRewards(Integer maxRewards) {
        if (maxRewards == null) {
            promo.getConfiguration().getConfiguration().remove(MAX_REWARDS_KEY);
        } else {
            promo.getConfiguration().addConfigurationItem(MAX_REWARDS_KEY, maxRewards.toString());
        }
        return this;
    }

    public BuyChipsPromotionBuilder withPriority(Integer priority) {
        promo.setPriority(priority);
        return this;
    }

    public BuyChipsPromotionBuilder withChips(String defaultPackage, String promoChips, Platform client) {
        if (StringUtils.isBlank(promoChips)) {
            promo.getConfiguration().removeChipAmountOverrideForPlatformAndPackage(client, new BigDecimal(defaultPackage));
            //OverridegetConfiguration().remove(String.format(CHIP_AMOUNT_FORMAT_KEY, client, defaultPackage));
        } else {
            promo.getConfiguration().overrideChipAmountForPlatformAndPackage(client, new BigDecimal(defaultPackage), new BigDecimal(promoChips));
//            promo.addConfigurationItem(String.format(CHIP_AMOUNT_FORMAT_KEY, client, defaultPackage), promoChips);
        }
        return this;
    }

    public BuyChipsPromotionBuilder withPaymentMethod(PaymentPreferences.PaymentMethod paymentMethod) {
        promo.addConfigurationItem(PAYMENT_METHODS_KEY, paymentMethod.name());
        return this;
    }

    public BuyChipsPromotionBuilder withInGameHeader(String header) {
        promo.addConfigurationItem(IN_GAME_NOTIFICATION_HEADER_KEY, header);
        return this;
    }

    public BuyChipsPromotionBuilder withInGameMessage(String msg) {
        promo.addConfigurationItem(IN_GAME_NOTIFICATION_MSG_KEY, msg);
        return this;
    }

    public BuyChipsPromotionBuilder withControlGroupFunction(final ControlGroupFunctionType controlGroupFunction) {
        promo.setControlGroupFunction(controlGroupFunction);
        return this;
    }

    public BuyChipsPromotionBuilder withControlGroupPercentage(Integer controlGroupPercentage) {
        promo.setControlGroupPercentage(controlGroupPercentage);
        return this;
    }

    public BuyChipsPromotionBuilder withSeed(int seed) {
        promo.setSeed(seed);
        return this;
    }

    public BuyChipsPromotionBuilder withDefaultChipsForPlatformAndPackage(BigDecimal defaultChipAmount, Platform web) {
        promo.getConfiguration().removeChipAmountOverrideForPlatformAndPackage(web, defaultChipAmount);
        return this;
    }
}
