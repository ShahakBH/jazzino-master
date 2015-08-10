package strata.server.lobby.promotion.service;

import com.yazino.platform.player.PlayerProfile;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import strata.server.lobby.api.promotion.ControlGroupFunctionType;
import strata.server.lobby.api.promotion.DailyAwardPromotion;
import strata.server.lobby.api.promotion.Promotion;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class PromotionControlGroupServiceCacheIntegrationTest {

    private static final PlayerProfile[] PLAYER_PROFILES;
    private static final Promotion[] PROMOTIONS;

    static {
        PlayerProfile profile1 = new PlayerProfile();
        profile1.setProviderName("facebook");
        profile1.setPlayerId(new BigDecimal("100"));

        PlayerProfile profile2 = new PlayerProfile();
        profile2.setProviderName("yazino");
        profile2.setPlayerId(new BigDecimal("200"));

        PLAYER_PROFILES = new PlayerProfile[]{profile1, profile2};

        DailyAwardPromotion promotion1 = new DailyAwardPromotion();
        promotion1.setSeed(10);
        promotion1.setControlGroupPercentage(10);
        promotion1.setControlGroupFunction(ControlGroupFunctionType.PLAYER_ID);

        DailyAwardPromotion promotion2 = new DailyAwardPromotion();
        promotion2.setSeed(20);
        promotion2.setControlGroupPercentage(20);
        promotion2.setControlGroupFunction(ControlGroupFunctionType.EXTERNAL_ID);

        PROMOTIONS = new Promotion[]{promotion1, promotion2};
    }

    @Autowired
    @Qualifier("promotionControlGroupService")
    private PromotionControlGroupService underTest;

    @Autowired
    private CacheManager cacheManager;

    @Before
    public void setUpUserService() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void configuredServiceShouldCacheResultsFromIsControlGroupMember() {
        Cache controlGroupDetectionCache = cacheManager.getCache("controlGroupDetectionCache");
        controlGroupDetectionCache.setStatisticsEnabled(true);

        // verify that the cache is clean
        assertEquals(0, controlGroupDetectionCache.getStatistics().getCacheHits());
        assertEquals(0, controlGroupDetectionCache.getStatistics().getCacheMisses());
        assertEquals(0, controlGroupDetectionCache.getStatistics().getMemoryStoreObjectCount());

        // verify that the test for control group membership is executed when not cached
        underTest.isControlGroupMember(PLAYER_PROFILES[0], PROMOTIONS[0]);
        assertEquals(0, controlGroupDetectionCache.getStatistics().getCacheHits());
        assertEquals(1, controlGroupDetectionCache.getStatistics().getCacheMisses());
        assertEquals(1, controlGroupDetectionCache.getStatistics().getMemoryStoreObjectCount());

        // verify that the test for control group membership is executed when not cached (different promotion)
        underTest.isControlGroupMember(PLAYER_PROFILES[0], PROMOTIONS[1]);
        assertEquals(0, controlGroupDetectionCache.getStatistics().getCacheHits());
        assertEquals(2, controlGroupDetectionCache.getStatistics().getCacheMisses());
        assertEquals(2, controlGroupDetectionCache.getStatistics().getMemoryStoreObjectCount());

        // verify that the test for control group membership is executed when not cached (different player)
        underTest.isControlGroupMember(PLAYER_PROFILES[1], PROMOTIONS[0]);
        assertEquals(0, controlGroupDetectionCache.getStatistics().getCacheHits());
        assertEquals(3, controlGroupDetectionCache.getStatistics().getCacheMisses());
        assertEquals(3, controlGroupDetectionCache.getStatistics().getMemoryStoreObjectCount());

        // verify that all the above were cached
        underTest.isControlGroupMember(PLAYER_PROFILES[0], PROMOTIONS[0]);
        underTest.isControlGroupMember(PLAYER_PROFILES[0], PROMOTIONS[1]);
        underTest.isControlGroupMember(PLAYER_PROFILES[1], PROMOTIONS[0]);
        assertEquals(3, controlGroupDetectionCache.getStatistics().getCacheHits());
        assertEquals(3, controlGroupDetectionCache.getStatistics().getCacheMisses());
        assertEquals(3, controlGroupDetectionCache.getStatistics().getMemoryStoreObjectCount());
    }
}
