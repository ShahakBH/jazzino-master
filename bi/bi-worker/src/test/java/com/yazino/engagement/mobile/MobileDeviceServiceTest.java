package com.yazino.engagement.mobile;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static com.yazino.platform.Platform.ANDROID;
import static org.mockito.Mockito.*;

public class MobileDeviceServiceTest {

    private static final String GAME_TYPE = "GAME_TYPE";
    private static final String APP_ID = "com.yazino.TestApp";

    private static final long ROW_ID_1 = 1L;
    private static final long ROW_ID_2 = 2L;
    private static final BigDecimal PLAYER_1 = BigDecimal.valueOf(-1L);
    private static final BigDecimal PLAYER_2 = BigDecimal.valueOf(-2L);
    private static final String DEVICE_ID_1 = "TEST_DEVICE_ID_1";
    private static final String DEVICE_ID_2 = "TEST_DEVICE_ID_2";
    private static final String PUSH_TOKEN_1 = "TEST_PUSH_TOKEN_1";
    private static final String PUSH_TOKEN_2 = "TEST_PUSH_TOKEN_2";
    private static final MobileDevice DEVICE_1 = createDevice(ROW_ID_1, PLAYER_1, DEVICE_ID_1, PUSH_TOKEN_1);
    private static final MobileDevice DEVICE_2 = createDevice(ROW_ID_2, PLAYER_1, DEVICE_ID_2, PUSH_TOKEN_2);

    private MobileDeviceService underTest;

    private MobileDeviceDao mobileDeviceDao = mock(MobileDeviceDao.class);
    private MobileDeviceHistoryDao mobileDeviceHistoryDao = mock(MobileDeviceHistoryDao.class);

    @Before
    public void setUp() throws Exception {
        underTest = new MobileDeviceService(mobileDeviceDao, mobileDeviceHistoryDao);
    }

    @Test
    public void registerNewDeviceShouldRecordAddedEvent() {
        when(mobileDeviceDao.create(eq(DEVICE_1)))
                .thenReturn(DEVICE_1);

        underTest.register(PLAYER_1, GAME_TYPE, ANDROID, APP_ID, DEVICE_ID_1, PUSH_TOKEN_1);

        verify(mobileDeviceHistoryDao).recordEvent(ROW_ID_1, MobileDeviceEvent.ADDED, "device_id=" + DEVICE_ID_1 + " push_token=" + PUSH_TOKEN_1);
    }

    @Test
    public void registerExistingDeviceWithDifferentPushTokenShouldRecordUpdatedEvent() {
        when(mobileDeviceDao.find(PLAYER_1, GAME_TYPE, ANDROID))
                .thenReturn(ImmutableList.of(DEVICE_1));

        underTest.register(PLAYER_1, GAME_TYPE, ANDROID, APP_ID, DEVICE_ID_1, PUSH_TOKEN_2);

        verify(mobileDeviceHistoryDao).recordEvent(ROW_ID_1, MobileDeviceEvent.UPDATED_PUSH_TOKEN, "push_token=" + PUSH_TOKEN_2);
    }

    @Test
    public void registerExistingUnmodifiedDeviceShouldNotRecordAnyEvents() {
        when(mobileDeviceDao.find(PLAYER_1, GAME_TYPE, ANDROID))
                .thenReturn(ImmutableList.of(DEVICE_1));

        underTest.register(PLAYER_1, GAME_TYPE, ANDROID, APP_ID, DEVICE_ID_1, PUSH_TOKEN_1);

        verifyZeroInteractions(mobileDeviceHistoryDao);
    }

    @Test
    public void registerSameDeviceIdAsDifferentPlayerShouldRecordDeactivatedEventsForExistingDevices() {
        when(mobileDeviceDao.findByDeviceIdExcludingPlayerId(GAME_TYPE, ANDROID, DEVICE_ID_1, PLAYER_2))
                .thenReturn(ImmutableList.of(DEVICE_1));
        MobileDevice newDevice = createDevice(ROW_ID_2, PLAYER_2, DEVICE_ID_1, PUSH_TOKEN_2);
        when(mobileDeviceDao.create(eq(newDevice)))
                .thenReturn(newDevice);

        underTest.register(PLAYER_2, GAME_TYPE, ANDROID, APP_ID, DEVICE_ID_1, PUSH_TOKEN_2);

        verify(mobileDeviceHistoryDao).recordEvent(ROW_ID_2, MobileDeviceEvent.ADDED, "device_id=" + DEVICE_ID_1 + " push_token=" + PUSH_TOKEN_2);
        verify(mobileDeviceHistoryDao).recordEvent(ROW_ID_1, MobileDeviceEvent.DEACTIVATED_REGISTERED_BY_DIFFERENT_PLAYER, "id="+ ROW_ID_2 +" player_id="+ PLAYER_2);
    }

    @Test
    public void deregisterShouldRecordDeactivatedEvent() {
        when(mobileDeviceDao.findByPushToken(ANDROID, PUSH_TOKEN_1))
                .thenReturn(ImmutableList.of(DEVICE_1));

        underTest.deregisterToken(ANDROID, PUSH_TOKEN_1);

        verify(mobileDeviceHistoryDao).recordEvent(ROW_ID_1, MobileDeviceEvent.DEACTIVATED_DEREGISTERED, "");
    }

    @Test
    public void replacingPushTokenShouldRecordUpdatedEvent() {
        when(mobileDeviceDao.findByPushToken(PLAYER_1, ANDROID, PUSH_TOKEN_1))
                .thenReturn(ImmutableList.of(DEVICE_1));

        underTest.replacePushTokenWith(PLAYER_1, ANDROID, PUSH_TOKEN_1, PUSH_TOKEN_2);

        verify(mobileDeviceHistoryDao).recordEvent(ROW_ID_1, MobileDeviceEvent.UPDATED_PUSH_TOKEN, "push_token=" + PUSH_TOKEN_2);
    }

    // legacy data

    @Test
    public void registerNewDeviceWithDeviceIdShouldRecordDeactivatedEventsForExistingDevicesWithNoDeviceId() {
        MobileDevice oldDevice = createDevice(ROW_ID_1, PLAYER_1, null, PUSH_TOKEN_1);
        when(mobileDeviceDao.find(PLAYER_1, GAME_TYPE, ANDROID))
                .thenReturn(ImmutableList.of(oldDevice));
        when(mobileDeviceDao.create(eq(DEVICE_2)))
                .thenReturn(DEVICE_2);

        underTest.register(PLAYER_1, GAME_TYPE, ANDROID, APP_ID, DEVICE_ID_2, PUSH_TOKEN_2);

        verify(mobileDeviceHistoryDao).recordEvent(ROW_ID_2, MobileDeviceEvent.ADDED, "device_id=" + DEVICE_ID_2 + " push_token=" + PUSH_TOKEN_2);
        verify(mobileDeviceHistoryDao).recordEvent(ROW_ID_1, MobileDeviceEvent.DEACTIVATED_NO_DEVICE_ID, "id="+ ROW_ID_2);
    }

    @Test
    public void registerExistingDeviceAddingMissingDeviceIdShouldRecordUpdatedDeviceIdEvent() {
        MobileDevice oldDevice = createDevice(ROW_ID_1, PLAYER_1, null, PUSH_TOKEN_1);
        when(mobileDeviceDao.find(PLAYER_1, GAME_TYPE, ANDROID))
                .thenReturn(ImmutableList.of(oldDevice));

        underTest.register(PLAYER_1, GAME_TYPE, ANDROID, APP_ID, DEVICE_ID_1, PUSH_TOKEN_1);

        verify(mobileDeviceHistoryDao).recordEvent(ROW_ID_1, MobileDeviceEvent.SET_DEVICE_ID, "device_id=" + DEVICE_ID_1);
    }

    private static MobileDevice createDevice(long rowId, BigDecimal playerId, String deviceId, String pushToken) {
        return MobileDevice.builder()
                .withId(rowId)
                .withPlayerId(playerId)
                .withGameType(GAME_TYPE)
                .withPlatform(ANDROID)
                .withAppId(APP_ID)
                .withDeviceId(deviceId)
                .withPushToken(pushToken)
                .build();
    }

}
