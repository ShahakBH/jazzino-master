package com.yazino.engagement.mobile;

import com.yazino.platform.Platform;
import org.hamcrest.Matchers;
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
import java.util.List;
import java.util.Map;

import static com.yazino.engagement.mobile.JdbcParams.buildParams;
import static com.yazino.platform.Platform.ANDROID;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration
@Transactional
@DirtiesContext
public class MobileDeviceServiceIntegrationTest {

    public static final String NEW_DEVICE_ID = "TEST_DEVICE_ID";
    private static final String DEVICE_ID = NEW_DEVICE_ID;
    private static final Map<String, Object> NO_PARAMS = buildParams();
    private static final BigDecimal PLAYER_1 = BigDecimal.valueOf(-1L);
    private static final BigDecimal PLAYER_2 = BigDecimal.valueOf(-2L);
    private static final String PUSH_TOKEN_1 = "TEST_PUSH_TOKEN_1";
    private static final String PUSH_TOKEN_2 = "TEST_PUSH_TOKEN_2";
    private static final String PUSH_TOKEN_3 = "TEST_PUSH_TOKEN_3";
    private static final String SLOTS = "SLOTS";
    private static final String APP_ID_1 = "TEST_APP_ID_1";
    public static final String NEW_TOKEN = "TEST_TOKEN_3";

    private MobileDeviceService underTest;

    @Autowired
    private NamedParameterJdbcTemplate externalDwNamedJdbcTemplate;

    @Autowired
    private MobileDeviceDao mobileDeviceDao;

    @Autowired
    private MobileDeviceHistoryDao mobileDeviceHistoryDao;

    @Before
    public void setUp() throws Exception {
        externalDwNamedJdbcTemplate.update("DELETE FROM mobile_device_history h USING mobile_device d WHERE h.id = d.id AND d.player_id < 0", NO_PARAMS);
        externalDwNamedJdbcTemplate.update("DELETE FROM mobile_device WHERE player_id < 0", NO_PARAMS);
        underTest = new MobileDeviceService(mobileDeviceDao, mobileDeviceHistoryDao);
    }

    @Test
    public void registerShouldAddNewDevice() {
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, DEVICE_ID, PUSH_TOKEN_1);

        Map device = loadSingleDevice(PLAYER_1, SLOTS, ANDROID);
        assertThat(((String) device.get("push_token")), is(PUSH_TOKEN_1));
        assertThat(((String) device.get("device_id")), is(DEVICE_ID));
        assertThat(((Boolean) device.get("active")), is(true));
    }

    @Test
    public void registerDuplicateTokenShouldResultInSingleDevice() {
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, null, PUSH_TOKEN_1);
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, null, PUSH_TOKEN_1);
        List<Map<String, Object>> devices = loadDevices(PLAYER_1, SLOTS, ANDROID);
        assertThat(devices.size(), is(1));

        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, "token", PUSH_TOKEN_1);
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, "token", PUSH_TOKEN_1);
        devices = loadDevices(PLAYER_1, SLOTS, ANDROID);
        assertThat(devices.size(), is(1));
    }

    @Test
    public void registrationWithDeviceIdShouldClearRegistrationsWithNoDeviceId() {
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, null, PUSH_TOKEN_2);
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, null, PUSH_TOKEN_3);

        List<Map<String, Object>> devices = loadDevices(PLAYER_1, SLOTS, ANDROID);
        assertThat(devices.size(), is(2));

        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, NEW_DEVICE_ID, NEW_TOKEN);

        devices = loadDevices(PLAYER_1, SLOTS, ANDROID);
        assertThat(devices.size(), is(1));
        assertThat((String) devices.get(0).get("device_id"), is(NEW_DEVICE_ID));
        assertThat((String) devices.get(0).get("push_token"), is(NEW_TOKEN));
    }

    @Test
    public void registrationWithDeviceIdShouldKeepPreviousRegistrationsWithDeviceId() {
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, "TEST_DEVICE_ID_1", PUSH_TOKEN_1);
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, null, PUSH_TOKEN_2);

        List<Map<String, Object>> devices = loadDevices(PLAYER_1, SLOTS, ANDROID);
        assertThat(devices.size(), is(2));

        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, "TEST_DEVICE_ID_2", PUSH_TOKEN_3);

        devices = loadDevices(PLAYER_1, SLOTS, ANDROID);
        assertThat(devices.size(), is(2));
        assertThat(devices, hasItem(buildParams("device_id", "TEST_DEVICE_ID_1", "push_token", PUSH_TOKEN_1)));
        assertThat(devices, hasItem(buildParams("device_id", "TEST_DEVICE_ID_2", "push_token", PUSH_TOKEN_3)));
    }

    @Test
    public void registrationShouldPopulateMissingDeviceIdForKnownToken() {
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, null, PUSH_TOKEN_3);
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, "TEST_DEVICE_ID_2", PUSH_TOKEN_3);

        List<Map<String, Object>> devices = loadDevices(PLAYER_1, SLOTS, ANDROID);
        assertThat(devices.size(), is(1));
        assertThat(devices, hasItem(buildParams("device_id", "TEST_DEVICE_ID_2", "push_token", PUSH_TOKEN_3)));
    }

    @Test
    public void registrationShouldRemoveExistingDevicesWithTheSameDeviceIdButDifferentPlayer() {
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, DEVICE_ID, PUSH_TOKEN_1);
        underTest.register(PLAYER_2, SLOTS, ANDROID, APP_ID_1, DEVICE_ID, PUSH_TOKEN_2);

        List<Map<String, Object>> devices = loadDevices(PLAYER_1, SLOTS, ANDROID);
        assertThat(devices, is(empty()));
    }

    @Test
    public void registrationShouldReplacePushTokenForTheSameDeviceId() {
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, DEVICE_ID, PUSH_TOKEN_2);
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, DEVICE_ID, PUSH_TOKEN_3);

        List<Map<String, Object>> devices = loadDevices(PLAYER_1, SLOTS, ANDROID);
        assertThat(devices.size(), is(1));
        assertThat(devices, hasItem(buildParams("device_id", DEVICE_ID, "push_token", PUSH_TOKEN_3)));
    }

    @Test
    public void replacePushTokenShouldReplaceOldPushToken() {
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, DEVICE_ID, PUSH_TOKEN_2);
        underTest.replacePushTokenWith(PLAYER_1, ANDROID, PUSH_TOKEN_2, PUSH_TOKEN_3);
        assertThat(loadDevices(PLAYER_1, SLOTS, ANDROID), hasItem(buildParams("device_id", DEVICE_ID, "push_token", PUSH_TOKEN_3)));
    }

    @Test
    public void replacePushTokenShouldDeleteOldPushTokenIfTheNewOneAlreadyExists() {
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, null, PUSH_TOKEN_2);
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, null, PUSH_TOKEN_3);
        underTest.replacePushTokenWith(PLAYER_1, ANDROID, PUSH_TOKEN_2, PUSH_TOKEN_3);
        List<Map<String, Object>> devices = loadDevices(PLAYER_1, SLOTS, ANDROID);
        assertThat(devices.size(), is(1));
        assertThat((String) devices.get(0).get("push_token"), is(PUSH_TOKEN_3));
    }

    @Test
    public void deregisterTokenShouldMarkTokenAsInactive() {
        underTest.register(PLAYER_1, SLOTS, ANDROID, APP_ID_1, null, PUSH_TOKEN_2);
        underTest.register(PLAYER_2, SLOTS, ANDROID, APP_ID_1, "blah2", PUSH_TOKEN_2);
        underTest.register(PLAYER_2, SLOTS, ANDROID, APP_ID_1, "blah", PUSH_TOKEN_3);
        assertThat(loadDevices(PLAYER_2, SLOTS, ANDROID).size(), is(2));

        underTest.deregisterToken(ANDROID, PUSH_TOKEN_2);

        assertThat(loadDevices(PLAYER_1, SLOTS, ANDROID).size(), is(0));
        assertThat(loadDevices(PLAYER_2, SLOTS, ANDROID).size(), is(1));
    }

    private Map<String, Object> loadSingleDevice(BigDecimal playerId, String gameType, Platform platform) {
        final Map<String, Object> paramMap = buildParams("playerId", playerId, "gameType", gameType, "platform", platform.name());
        return externalDwNamedJdbcTemplate.queryForMap(
                "select push_token, device_id, active from MOBILE_DEVICE where player_id = :playerId and game_type = :gameType and platform = :platform",
                paramMap);
    }

    private List<Map<String, Object>> loadDevices(BigDecimal playerId, String gameType, Platform platform) {
        return externalDwNamedJdbcTemplate.queryForList(
                "select push_token, device_id from MOBILE_DEVICE where player_id = :playerId and game_type = :gameType and platform = :platform and active=true",
                buildParams("playerId", playerId, "gameType", gameType, "platform", platform.name()));
    }

}
