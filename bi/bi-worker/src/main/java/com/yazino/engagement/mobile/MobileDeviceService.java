package com.yazino.engagement.mobile;

import com.google.common.base.Predicate;
import com.yazino.platform.Platform;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.google.common.collect.Collections2.filter;

@Service
public class MobileDeviceService {

    private static final Logger LOG = LoggerFactory.getLogger(MobileDeviceService.class);

    private final MobileDeviceDao mobileDeviceDao;
    private final MobileDeviceHistoryDao mobileDeviceHistoryDao;

    @Autowired
    public MobileDeviceService(MobileDeviceDao mobileDeviceDao, MobileDeviceHistoryDao mobileDeviceHistoryDao) {
        this.mobileDeviceDao = mobileDeviceDao;
        this.mobileDeviceHistoryDao = mobileDeviceHistoryDao;
    }

    public void register(final BigDecimal playerId,
                         final String gameType,
                         final Platform platform,
                         final String appId,
                         final String deviceId,
                         final String pushToken) {
        MobileDevice request = MobileDevice.builder()
                .withPlayerId(playerId)
                .withGameType(gameType)
                .withPlatform(platform)
                .withAppId(appId)
                .withDeviceId(deviceId)
                .withPushToken(pushToken)
                .build();

        List<MobileDevice> existingDevices = mobileDeviceDao.find(playerId, gameType, platform);
        if (existingDevices.contains(request)) {
            LOG.debug("Ignoring registration for existing device: {}", request);
            return;
        }

        if (deviceId != null) {
            registerWithDeviceId(request, existingDevices);
        } else {
            // old clients do not send the deviceId
            LOG.debug("Adding device: {}", request);
            MobileDevice savedDevice = mobileDeviceDao.create(request);
            mobileDeviceHistoryDao.recordEvent(savedDevice.getId(), MobileDeviceEvent.ADDED_NO_DEVICE_ID,
                    "push_token=" + request.getPushToken());
        }
    }

    private void registerWithDeviceId(final MobileDevice request, List<MobileDevice> existingDevices) {
        Collection<MobileDevice> existingDevicesWithSamePushToken = filter(existingDevices, new Predicate<MobileDevice>() {
            @Override
            public boolean apply(MobileDevice candidate) {
                return Objects.equals(candidate.getPushToken(), request.getPushToken());
            }
        });
        Collection<MobileDevice> existingDevicesWithSameDeviceId = filter(existingDevices, new Predicate<MobileDevice>() {
            @Override
            public boolean apply(MobileDevice candidate) {
                return Objects.equals(candidate.getDeviceId(), request.getDeviceId());
            }
        });

        if (!existingDevicesWithSamePushToken.isEmpty()) {
            for (MobileDevice existingDevice : existingDevicesWithSamePushToken) {
                LOG.debug("Updating device: {}; setting deviceId: {}", existingDevice, request.getDeviceId());
                mobileDeviceDao.update(MobileDevice.builder(existingDevice).withDeviceId(request.getDeviceId()).build());
                mobileDeviceHistoryDao.recordEvent(existingDevice.getId(), MobileDeviceEvent.SET_DEVICE_ID,
                        "device_id=" + request.getDeviceId());
            }
        } else if (!existingDevicesWithSameDeviceId.isEmpty()) {
            for (MobileDevice existingDevice : existingDevicesWithSameDeviceId) {
                LOG.debug("Updating device: {}; setting pushToken: {}", existingDevice, request.getPushToken());
                mobileDeviceDao.update(MobileDevice.builder(existingDevice).withPushToken(request.getPushToken()).build());
                mobileDeviceHistoryDao.recordEvent(existingDevice.getId(), MobileDeviceEvent.UPDATED_PUSH_TOKEN,
                        "push_token=" + request.getPushToken());
            }
        } else {
            LOG.debug("Adding device: {}", request);
            MobileDevice savedDevice = mobileDeviceDao.create(request);
            mobileDeviceHistoryDao.recordEvent(savedDevice.getId(), MobileDeviceEvent.ADDED,
                    "device_id=" + request.getDeviceId() + " push_token=" + request.getPushToken());

            for (MobileDevice device : existingDevices) {
                if (device.getDeviceId() == null) {
                    deactivate(device, MobileDeviceEvent.DEACTIVATED_NO_DEVICE_ID, "id=" + savedDevice.getId());
                }
            }

            List<MobileDevice> otherPlayerDevicesWithSameId = mobileDeviceDao.findByDeviceIdExcludingPlayerId(
                    request.getGameType(), request.getPlatform(), request.getDeviceId(), request.getPlayerId());
            for (MobileDevice device : otherPlayerDevicesWithSameId) {
                deactivate(device, MobileDeviceEvent.DEACTIVATED_REGISTERED_BY_DIFFERENT_PLAYER, "id=" + savedDevice.getId()
                        + " player_id=" + savedDevice.getPlayerId());
            }
        }
    }

    //Just used by the GCM Sender if the token has changed
    public void replacePushTokenWith(BigDecimal playerId, Platform platform, String oldPushToken, String newPushToken) {
        Validate.notNull(playerId, "playerId was null");
        Validate.notNull(platform, "platform was null");
        Validate.notBlank(oldPushToken, "oldPushToken was blank");
        Validate.notBlank(newPushToken, "newPushToken was blank");

        List<MobileDevice> devicesWithOldToken = mobileDeviceDao.findByPushToken(playerId, platform, oldPushToken);
        List<MobileDevice> devicesWithNewToken = mobileDeviceDao.findByPushToken(playerId, platform, newPushToken);
        if (devicesWithNewToken.isEmpty()) {
            for (MobileDevice device : devicesWithOldToken) {
                LOG.debug("Updating device {}: setting pushToken: {}", device, newPushToken);
                mobileDeviceDao.update(MobileDevice.builder(device).withPushToken(newPushToken).build());
                mobileDeviceHistoryDao.recordEvent(device.getId(), MobileDeviceEvent.UPDATED_PUSH_TOKEN,
                        "push_token=" + newPushToken);
            }
        } else {
            for (MobileDevice device : devicesWithOldToken) {
                deactivate(device, MobileDeviceEvent.DEACTIVATED_DEREGISTERED, "");
            }
        }
    }

    public void deregisterToken(Platform platform, final String pushToken) {
        Validate.notNull(platform, "platform was null");
        Validate.notBlank(pushToken, "pushToken was blank");

        List<MobileDevice> devices = mobileDeviceDao.findByPushToken(platform, pushToken);
        for (MobileDevice device : devices) {
            deactivate(device, MobileDeviceEvent.DEACTIVATED_DEREGISTERED, "");
        }
    }

    private void deactivate(MobileDevice device, MobileDeviceEvent event, String eventDetails) {
        LOG.debug("Deactivating device: {}; event: {}", device, event);
        mobileDeviceDao.update(MobileDevice.builder(device).withActive(false).build());
        mobileDeviceHistoryDao.recordEvent(device.getId(), event, eventDetails);
    }

}
