package com.yazino.engagement.mobile;

import com.google.common.collect.ImmutableMap;
import com.yazino.engagement.PlayerTarget;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static com.yazino.engagement.mobile.JdbcParams.buildParams;
import static com.yazino.platform.Platform.ANDROID;
import static com.yazino.platform.Platform.IOS;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("MobileDeviceServiceIntegrationTest-context.xml")
@TransactionConfiguration
@Transactional
@DirtiesContext
public class MobileDeviceCampaignDaoIntegrationTest {

    private static final Long CAMPAIGN_RUN_ID = -1L;
    private static final Map<String, Object> NO_PARAMS = buildParams();
    private static final BigDecimal PLAYER_1 = BigDecimal.valueOf(-1L);
    private static final BigDecimal PLAYER_2 = BigDecimal.valueOf(-2L);
    private static final String PUSH_TOKEN_1 = "TEST_PUSH_TOKEN_1";
    private static final String PUSH_TOKEN_2 = "TEST_PUSH_TOKEN_2";
    private static final String SLOTS = "SLOTS";
    private static final String APP_ID_1 = "TEST_APP_ID_1";
    private static final String APP_ID_2 = "TEST_APP_ID_2";
    public static final Long FROM_TIME = now().minusHours(1).getMillis();
    public static final Long TO_TIME = now().plusHours(1).getMillis();

    private MobileDeviceCampaignDao underTest;

    @Autowired
    private MobileDeviceService mobileDeviceDao;

    @Autowired
    NamedParameterJdbcTemplate externalDwNamedJdbcTemplate;

    @Before
    public void setUp() throws Exception {
        externalDwNamedJdbcTemplate.update("DELETE FROM mobile_device_history h USING mobile_device d WHERE h.id = d.id AND d.player_id < 0", NO_PARAMS);
        externalDwNamedJdbcTemplate.update("DELETE FROM mobile_device WHERE player_id < 0", NO_PARAMS);
        externalDwNamedJdbcTemplate.update("DELETE FROM segment_selection WHERE campaign_run_id < 0", NO_PARAMS);
        underTest = new MobileDeviceCampaignDao(externalDwNamedJdbcTemplate);
    }

    @Test
    public void getEligibleTargetsShouldGetTargetsForThatPlatform() {
        mobileDeviceDao.register(PLAYER_1, SLOTS, ANDROID, APP_ID_2, null, "TEST_ANDROID_TOKEN");
        mobileDeviceDao.register(PLAYER_1, SLOTS, IOS, APP_ID_1, null, PUSH_TOKEN_1);
        mobileDeviceDao.register(PLAYER_2, SLOTS, IOS, APP_ID_1, null, "TEST_IOS_TOKEN");
        associatePlayerWithCampaign(CAMPAIGN_RUN_ID, PLAYER_1);
        associatePlayerWithCampaign(CAMPAIGN_RUN_ID, PLAYER_2);

        final List<PlayerTarget> targets = underTest.getEligiblePlayerTargets(CAMPAIGN_RUN_ID, IOS, null, null);
        assertThat(targets.size(), is(2));
        Map<String, String> customData = ImmutableMap.of("PROGRESSIVE", "100");
        assertThat(targets, hasItem(new PlayerTarget(SLOTS, "", PLAYER_1, PUSH_TOKEN_1, APP_ID_1, customData)));
        assertThat(targets, hasItem(new PlayerTarget(SLOTS, "", PLAYER_2, "TEST_IOS_TOKEN", APP_ID_1, customData)));
    }

    @Test
    public void getEligibleTargetsShouldGetTargetsForThatPlatformWithinTheTimesGiven() {
        mobileDeviceDao.register(PLAYER_1, SLOTS, ANDROID, APP_ID_2, null, "TEST_ANDROID_TOKEN");
        mobileDeviceDao.register(PLAYER_1, SLOTS, IOS, APP_ID_1, null, PUSH_TOKEN_1);
        mobileDeviceDao.register(PLAYER_2, SLOTS, IOS, APP_ID_1, null, "TEST_IOS_TOKEN");
        associatePlayerWithCampaign(CAMPAIGN_RUN_ID, PLAYER_1, now());
        associatePlayerWithCampaign(CAMPAIGN_RUN_ID, PLAYER_2, now().minusHours(2));

        final List<PlayerTarget> targets = underTest.getEligiblePlayerTargets(CAMPAIGN_RUN_ID, IOS, FROM_TIME, TO_TIME);
        assertThat(targets.size(), is(1));
        Map<String, String> customData = ImmutableMap.of("PROGRESSIVE", "100");
        assertThat(targets, hasItem(new PlayerTarget(SLOTS, "", PLAYER_1, PUSH_TOKEN_1, APP_ID_1, customData)));

    }

    @Test
    public void getEligibleTargetsShouldNotReturnInactiveUsers() {
        mobileDeviceDao.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, null, PUSH_TOKEN_1);
        mobileDeviceDao.register(PLAYER_1, SLOTS, IOS, APP_ID_1, null, PUSH_TOKEN_2);
        associatePlayerWithCampaign(CAMPAIGN_RUN_ID, PLAYER_1);

        mobileDeviceDao.deregisterToken(ANDROID, PUSH_TOKEN_1);
        final List<PlayerTarget> targets = underTest.getEligiblePlayerTargets(CAMPAIGN_RUN_ID, ANDROID, null, null);
        final List<PlayerTarget> iosTargets = underTest.getEligiblePlayerTargets(CAMPAIGN_RUN_ID, IOS, null, null);
        assertThat(targets.size(), is(0));
        assertThat(iosTargets.size(), is(1));
    }

    private void associatePlayerWithCampaign(final Long campaignRunId, final BigDecimal playerId) {
        externalDwNamedJdbcTemplate.update(
                "INSERT INTO SEGMENT_SELECTION(CAMPAIGN_RUN_ID, PLAYER_ID, PROGRESSIVE_BONUS) VALUES (:campaignRunId, :playerId, 100)",
                buildParams("campaignRunId", campaignRunId, "playerId", playerId));
    }

    private void associatePlayerWithCampaign(final Long campaignRunId, final BigDecimal playerId, DateTime validFromTime) {
        externalDwNamedJdbcTemplate.update(
                "INSERT INTO SEGMENT_SELECTION(CAMPAIGN_RUN_ID, PLAYER_ID, PROGRESSIVE_BONUS, VALID_FROM) VALUES (:campaignRunId, :playerId, 100, :validFrom)",
                buildParams("campaignRunId", campaignRunId, "playerId", playerId, "validFrom", new Timestamp(validFromTime.getMillis())));
    }

}
