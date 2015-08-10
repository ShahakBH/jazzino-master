package com.yazino.engagement.facebook;

import com.restfb.DefaultWebRequestor;
import com.restfb.WebRequestor;
import com.yazino.engagement.campaign.AccessTokenException;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import strata.server.lobby.api.facebook.FacebookAppConfiguration;
import strata.server.lobby.api.facebook.FacebookConfiguration;

import java.io.IOException;
import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
@DirtiesContext
public class FacebookAccessTokenServiceCacheIntegrationTest {

    public static final String BLACKJACK_SECRET = "BLACKJACK_SECRET";
    public static final String BLACKJACK_CLIENT_ID = "BLACKJACK_CLIENT_ID";
    public static final String BLACKJACK = "BLACKJACK";
    public static final String FACEBOOK_ACCESS_TOKEN_URL = "https://graph.facebook.com/oauth/access_token?client_id=%s&client_secret="
            + "%s&grant_type=client_credentials";
    public static final String BLACKJACK_ACCESS_TOKEN = "BLACKJACK_ACCESS_TOKEN";

    @Autowired()
    private FacebookAccessTokenService underTest;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private DefaultWebRequestor defaultWebRequestor;

    @Before
    public void setup() {
        final ArrayList<FacebookAppConfiguration> applicationConfigs = new ArrayList<FacebookAppConfiguration>();

        final FacebookAppConfiguration facebookAppConfiguration = new FacebookAppConfiguration();
        facebookAppConfiguration.setSecretKey(BLACKJACK_SECRET);
        facebookAppConfiguration.setApplicationId(BLACKJACK_CLIENT_ID);
        facebookAppConfiguration.setGameType(BLACKJACK);
        applicationConfigs.add(facebookAppConfiguration);

        FacebookConfiguration facebookConfiguration = new FacebookConfiguration();
        facebookConfiguration.setApplicationConfigs(applicationConfigs);
    }

//    @Ignore("http://issues.london.yazino.com/browse/WEB-3578")
    @Test
    public void configuredServiceShouldCacheResultsFromFetchApplicationAccessToken() throws AccessTokenException, IOException {
        WebRequestor.Response blackjackResponse = new WebRequestor.Response(200, "access_token=" + BLACKJACK_ACCESS_TOKEN);
        when(defaultWebRequestor.executeGet(
                String.format(FACEBOOK_ACCESS_TOKEN_URL, BLACKJACK_CLIENT_ID, BLACKJACK_SECRET))).thenReturn(blackjackResponse);

        Cache fbAppAccessTokenDetectionCache = cacheManager.getCache("facebookApplicationAccessTokenCache");
        fbAppAccessTokenDetectionCache.removeAll();
        fbAppAccessTokenDetectionCache.setStatisticsEnabled(true);

        for (int i= 0; i < 4; i++) {
            assertThat(underTest.fetchApplicationAccessToken(BLACKJACK), is(BLACKJACK_ACCESS_TOKEN));
        }
        assertThat(fbAppAccessTokenDetectionCache.getStatistics().getCacheHits(), is(3l));
        assertThat(fbAppAccessTokenDetectionCache.getStatistics().getCacheMisses(), is(1l));
        assertThat(fbAppAccessTokenDetectionCache.getStatistics().getMemoryStoreObjectCount(), is(1l));

//        for (int i= 0; i < 4; i++) {
//            assertThat(underTest.fetchApplicationAccessToken(BLACKJACK_CLIENT_ID, BLACKJACK_SECRET), is(BLACKJACK_ACCESS_TOKEN));
//        }
//        assertThat(fbAppAccessTokenDetectionCache.getStatistics().getCacheHits(), is(6l));
//        assertThat(fbAppAccessTokenDetectionCache.getStatistics().getCacheMisses(), is(2l));
//        assertThat(fbAppAccessTokenDetectionCache.getStatistics().getMemoryStoreObjectCount(), is(2l));
    }

//    @Test
//    public void configuredServiceShouldNotCacheResultsFromFetchApplicationAccessToken() throws AccessTokenException, IOException {
//        WebRequestor.Response blackjackResponse = new WebRequestor.Response(200, "access_token=" + BLACKJACK_ACCESS_TOKEN);
//        when(defaultWebRequestor.executeGet(
//                String.format(FACEBOOK_ACCESS_TOKEN_URL, BLACKJACK_CLIENT_ID, BLACKJACK_SECRET))).thenReturn(blackjackResponse);
//
//        Cache fbAppAccessTokenDetectionCache = cacheManager.getCache("facebookApplicationAccessTokenCache");
//        fbAppAccessTokenDetectionCache.removeAll();
//        fbAppAccessTokenDetectionCache.setStatisticsEnabled(true);
//
//        for (int i= 0; i < 4; i++) {
//            assertThat(underTest.fetchApplicationAccessToken(BLACKJACK), is(BLACKJACK_ACCESS_TOKEN));
//        }
//        assertThat(fbAppAccessTokenDetectionCache.getStatistics().getCacheHits(), is(0l));
//        assertThat(fbAppAccessTokenDetectionCache.getStatistics().getCacheMisses(), is(0l));
//        assertThat(fbAppAccessTokenDetectionCache.getStatistics().getMemoryStoreObjectCount(), is(0l));
//
//        for (int i= 0; i < 4; i++) {
//            assertThat(underTest.fetchApplicationAccessToken(BLACKJACK_CLIENT_ID, BLACKJACK_SECRET), is(BLACKJACK_ACCESS_TOKEN));
//        }
//        assertThat(fbAppAccessTokenDetectionCache.getStatistics().getCacheHits(), is(0l));
//        assertThat(fbAppAccessTokenDetectionCache.getStatistics().getCacheMisses(), is(0l));
//        assertThat(fbAppAccessTokenDetectionCache.getStatistics().getMemoryStoreObjectCount(), is(0l));
//    }
}
