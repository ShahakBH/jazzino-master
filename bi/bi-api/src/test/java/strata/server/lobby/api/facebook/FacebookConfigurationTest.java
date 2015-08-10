package strata.server.lobby.api.facebook;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CANVAS;
import static strata.server.lobby.api.facebook.FacebookConfiguration.ApplicationType.CONNECT;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.LOOSE;
import static strata.server.lobby.api.facebook.FacebookConfiguration.MatchType.STRICT;

public class FacebookConfigurationTest {
    private static final String BLACKJACK = "blackjack";
    private static final String SLOTS = "slots";
    private static final String ROULETTE = "roulette";
    private static final String WEBSITE = "website";
    private static final String BLACKJACK_PREFIX = "bj";
    private static final String SLOTS_PREFIX = "wd";

    private FacebookConfiguration facebookConfiguration;

    @Before
    public void setup() {
        facebookConfiguration = new FacebookConfiguration();
        facebookConfiguration.setApplicationConfigs(Arrays.asList(
                appConfigFor(SLOTS, null), appConfigFor(ROULETTE, null)));
        facebookConfiguration.setConnectAppConfiguration(appConfigFor(WEBSITE, null));
    }

    @Test
    public void configurationIsConfiguredWhenOneOrMoreApplicationConfigsExist() {
        assertThat(facebookConfiguration.isConfigured(), is(true));
    }

    @Test
    public void configurationIsNotConfiguredWhenOneZeroApplicationConfigsExist() {
        facebookConfiguration.setApplicationConfigs(null);

        assertThat(facebookConfiguration.isConfigured(), is(false));
    }

    @Test
    public void getAppConfigForANullGameTypeShouldReturnNullWhenStrict() {
        assertThat(facebookConfiguration.getAppConfigFor(null, CANVAS, STRICT), is(nullValue()));
    }

    @Test
    public void getAppConfigForANullGameTypeShouldReturnTheDefaultWhenLoose() {
        assertThat(facebookConfiguration.getAppConfigFor(null, CANVAS, LOOSE).getGameType(), is(equalTo(SLOTS)));
    }

    @Test
    public void getAppConfigForGameTypeShouldReturnNullIfGameTypeDoesNotExistAndStrict() {
        assertThat(facebookConfiguration.getAppConfigFor(BLACKJACK, CANVAS, STRICT), is(nullValue()));
    }

    @Test
    public void getAppConfigForUnknownGameTypeShouldReturnDefaultConfigWhenStrictIsFalse() {
        assertThat(facebookConfiguration.getAppConfigFor(BLACKJACK, CANVAS, LOOSE).getGameType(),
                is(equalTo(SLOTS)));
    }

    @Test
    public void getAppConfigForKnownGameTypeShouldReturnMatchingConfig() {
        assertThat(facebookConfiguration.getAppConfigFor(ROULETTE, CANVAS, STRICT).getGameType(),
                is(equalTo(ROULETTE)));
    }

    @Test
    public void getAppConfigForKnownGameTypeShouldReturnMatchingConfigForConnectWhenSeparateConfigIsFalse() {
        assertThat(facebookConfiguration.getAppConfigFor(ROULETTE, CONNECT, STRICT).getGameType(),
                is(equalTo(ROULETTE)));
    }

    @Test
    public void getAppConfigForKnownGameTypeShouldReturnTheConnectConfigurationWhenSeparateConfigIsTrueAndStrict() {
        facebookConfiguration.setUsingSeparateConnectApplication(true);

        assertThat(facebookConfiguration.getAppConfigFor(ROULETTE, CONNECT, STRICT).getGameType(),
                is(equalTo(WEBSITE)));
    }

    @Test
    public void getAppConfigForKnownGameTypeShouldReturnTheConnectConfigurationWhenSeparateConfigIsTrueAndLoose() {
        facebookConfiguration.setUsingSeparateConnectApplication(true);

        assertThat(facebookConfiguration.getAppConfigFor(ROULETTE, CONNECT, LOOSE).getGameType(),
                is(equalTo(WEBSITE)));
    }

    @Test
    public void getAppConfigForKnownGameTypeShouldReturnNullWhenSeparateConfigIsTrue() {
        facebookConfiguration.setUsingSeparateConnectApplication(true);
        facebookConfiguration.setConnectAppConfiguration(null);

        assertThat(facebookConfiguration.getAppConfigFor(ROULETTE, CONNECT, LOOSE), is(nullValue()));
    }

    @Test
    public void getAppConfigForOpenGraphObjectPrefixShouldReturnNullIfPrefixDoesNotExist() {
        // given at least one configuration
        assertTrue(facebookConfiguration.isConfigured());

        // when getting configuration for unknown game type, then null is return rather than first in list
        assertNull(facebookConfiguration.getAppConfigForOpenGraphObjectPrefix(BLACKJACK_PREFIX));
    }

    @Test
    public void getAppConfigForOpenGraphObjectPrefixShouldReturnCorrectConfig() {
        facebookConfiguration.setApplicationConfigs(Arrays.asList(
                appConfigFor(SLOTS, SLOTS_PREFIX),
                appConfigFor(BLACKJACK, BLACKJACK_PREFIX)));

        FacebookAppConfiguration blackjackConfig = facebookConfiguration.getAppConfigForOpenGraphObjectPrefix(BLACKJACK_PREFIX);
        assertThat(blackjackConfig.getGameType(), is(BLACKJACK));

        FacebookAppConfiguration slotsConfig = facebookConfiguration.getAppConfigForOpenGraphObjectPrefix(SLOTS_PREFIX);
        assertThat(slotsConfig.getGameType(), is(SLOTS));
    }

    private FacebookAppConfiguration appConfigFor(final String gameType, final String prefix) {
        final FacebookAppConfiguration facebookAppConfiguration = new FacebookAppConfiguration();
        facebookAppConfiguration.setGameType(gameType);
        facebookAppConfiguration.setOpenGraphObjectPrefix(prefix);
        return facebookAppConfiguration;
    }
}
