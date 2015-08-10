package strata.server.lobby.api.promotion;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static strata.server.lobby.api.promotion.DailyAwardPromotion.*;


public class DailyAwardConfigTest {
    public static final String MAIN_IMAGE_VALUE = "MAIN_IMAGE_VALUE";
    public static final String MAIN_IMAGE_LINK_VALUE = "MAIN_IMAGE_LINK_VALU";
    public static final String SECONDARY_IMAGE_VALUE = "SECONDARY_IMAGE_VALUE";
    public static final String SECONDARY_IMAGE_LINK_VALUE = "SECONDARY_IMAGE_LINK_VALUE";
    public static final String REWARD_CHIPS_VALUE = "30";
    public static final String MAX_REWARDS_VALUE = "3";

    @Test(expected = NullPointerException.class)
    public void cannotCreateDailyPromotionWithNullConfiguration() {
        new DailyAwardConfig(null);
    }

    @Test
    public void shouldCreateFromPromotionConfiguration() {
        // GIVEN a Promotion that is a daily award
        PromotionConfiguration config = createGoodConfiguration();
        // AND matching daily award
        DailyAwardConfig expected = new DailyAwardConfig();
        expected.setMainImage(MAIN_IMAGE_VALUE);
        expected.setMainImageLink(MAIN_IMAGE_LINK_VALUE);
        expected.setSecondaryImage(SECONDARY_IMAGE_VALUE);
        expected.setSecondaryImageLink(SECONDARY_IMAGE_LINK_VALUE);
        expected.setRewardChips(Integer.parseInt(REWARD_CHIPS_VALUE));
        expected.setMaxRewards(Integer.parseInt(MAX_REWARDS_VALUE));

        // WHEN creating daily award config
        DailyAwardConfig dailyAwardConfig = new DailyAwardConfig(config);

        // THEN daily award config should match configuration values
        assertThat(dailyAwardConfig, is(expected));
    }

    private PromotionConfiguration createGoodConfiguration() {
        PromotionConfiguration config = new PromotionConfiguration();
        config.addConfigurationItem(MAIN_IMAGE_KEY, MAIN_IMAGE_VALUE);
        config.addConfigurationItem(MAIN_IMAGE_LINK_KEY, MAIN_IMAGE_LINK_VALUE);
        config.addConfigurationItem(SECONDARY_IMAGE_KEY, SECONDARY_IMAGE_VALUE);
        config.addConfigurationItem(SECONDARY_IMAGE_LINK_KEY, SECONDARY_IMAGE_LINK_VALUE);
        config.addConfigurationItem(REWARD_CHIPS_KEY, REWARD_CHIPS_VALUE);
        config.addConfigurationItem(MAX_REWARDS_KEY, MAX_REWARDS_VALUE);
        return config;
    }
}
