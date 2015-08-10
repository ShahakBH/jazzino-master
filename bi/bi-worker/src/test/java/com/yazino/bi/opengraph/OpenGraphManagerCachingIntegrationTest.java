package com.yazino.bi.opengraph;

import com.yazino.platform.opengraph.OpenGraphAction;
import com.yazino.platform.opengraph.OpenGraphObject;
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
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class OpenGraphManagerCachingIntegrationTest {

    private final static OpenGraphAction ACTION_1 = new OpenGraphAction("spin", new OpenGraphObject("wheel", "http://sample.url1"));
    private final static String GAME_TYPE_1 = "SLOTS";
    private final static BigInteger PLAYER_1_ID = BigInteger.ONE;
    private final static AccessTokenStore.AccessToken SAMPLE_ACCESS_TOKEN = new AccessTokenStore.AccessToken("sample-access-token");

    @Autowired
    private OpenGraphManager underTest;

    @Autowired
    private OpenGraphHttpInvoker httpInvoker;

    @Autowired
    private AccessTokenStore accessTokenStore;

    @Autowired
    private FacebookConfiguration facebookConfiguration;

    @Autowired
    private CacheManager ehCacheManager;

    private Cache permissionCache;
    private FacebookAppConfiguration appConfig = mock(FacebookAppConfiguration.class);

    @Before
    public void setUp() {
        when(accessTokenStore.findByKey(any(AccessTokenStore.Key.class))).thenReturn(SAMPLE_ACCESS_TOKEN);
        permissionCache = ehCacheManager.getCache("openGraphPermissionCache");
        permissionCache.setStatisticsEnabled(true);
        permissionCache.clearStatistics();
        permissionCache.removeAll();
        when(appConfig.getAppName()).thenReturn("wheelDeal");
        when(facebookConfiguration.getAppConfigFor(anyString(),
                eq(FacebookConfiguration.ApplicationType.CANVAS),
                eq(FacebookConfiguration.MatchType.STRICT))).thenReturn(appConfig);
    }

    @Test
    public void shouldCachePermissions() throws IOException {
        assertEquals(0, permissionCache.getStatistics().getObjectCount());

        for (int i = 0; i < 5; i++) {
            underTest.publishAction(ACTION_1, PLAYER_1_ID, GAME_TYPE_1);
        }

        assertTrue(permissionCache.isStatisticsEnabled());
        assertEquals(1, permissionCache.getStatistics().getCacheMisses());
        assertEquals(1, permissionCache.getStatistics().getObjectCount());
        assertEquals(4, permissionCache.getStatistics().getCacheHits());
    }

}
