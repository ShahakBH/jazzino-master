package com.yazino.web.util;

import com.yazino.configuration.YazinoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.yazino.web.util.MobilePlatformSniffer.MobilePlatform;
import static com.yazino.web.util.MobilePlatformSniffer.MobilePlatform.ANDROID;
import static com.yazino.web.util.MobilePlatformSniffer.MobilePlatform.IOS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

/**
 * Redirects as per http://issues.london.yazino.com/browse/WEB-3512
 */
@RunWith(MockitoJUnitRunner.class)
public class LandingUrlRegistryTest {
    @Mock
    private YazinoConfiguration yazinoConfiguration;

    private LandingUrlRegistry underTest;

    @Before
    public void setUp() {
        when(yazinoConfiguration.getString("web.landing-url.IOS.BLACKJACK", null)).thenReturn("http://hastrk2.com/serve?action=click&publisher_id=11118&site_id=3574&campaign_id=239220");
        when(yazinoConfiguration.getString("web.landing-url.IOS.HIGH_STAKES", null)).thenReturn("http://hastrk3.com/serve?action=click&publisher_id=11120&site_id=3578&campaign_id=239224");
        when(yazinoConfiguration.getString("web.landing-url.IOS.SLOTS", null)).thenReturn("http://hastrk1.com/serve?action=click&publisher_id=11116&site_id=3482&campaign_id=239252");
        when(yazinoConfiguration.getString("web.landing-url.ANDROID.TEXAS_HOLDEM", null)).thenReturn("https://play.google.com/store/apps/details?id=air.com.yazino.android.poker.texholdem");
        when(yazinoConfiguration.getString("web.landing-url.ANDROID.SLOTS", null)).thenReturn("market://details?id=air.com.yazino.android.slots&referrer=publisher_id%3D11122%26offer_id%3D243156");

        underTest = new LandingUrlRegistry(yazinoConfiguration);
    }

    @Test
    public void shouldRedirectAsPer_WEB_3512_and_WEB_3704() {
        assertGameUrlForPlatformIs(ANDROID, "BLACKJACK", null);
        assertGameUrlForPlatformIs(ANDROID, "HIGH_STAKES", null);
        assertGameUrlForPlatformIs(ANDROID, "ROULETTE", null);
        assertGameUrlForPlatformIs(ANDROID, "SLOTS", "market://details?id=air.com.yazino.android.slots&referrer=publisher_id%3D11122%26offer_id%3D243156");
        assertGameUrlForPlatformIs(ANDROID, "TEXAS_HOLDEM", "https://play.google.com/store/apps/details?id=air.com.yazino.android.poker.texholdem");

        assertGameUrlForPlatformIs(IOS, "BLACKJACK", "http://hastrk2.com/serve?action=click&publisher_id=11118&site_id=3574&campaign_id=239220");
        assertGameUrlForPlatformIs(IOS, "HIGH_STAKES", "http://hastrk3.com/serve?action=click&publisher_id=11120&site_id=3578&campaign_id=239224");
        assertGameUrlForPlatformIs(IOS, "ROULETTE", null);
        assertGameUrlForPlatformIs(IOS, "SLOTS", "http://hastrk1.com/serve?action=click&publisher_id=11116&site_id=3482&campaign_id=239252");
        assertGameUrlForPlatformIs(IOS, "TEXAS_HOLDEM", null);
    }

    private void assertGameUrlForPlatformIs(MobilePlatform platform, String gameId, String expectedUrl) {
        assertThat(underTest.getLandingUrlForGameAndPlatform(gameId, platform), equalTo(expectedUrl));
    }
}
