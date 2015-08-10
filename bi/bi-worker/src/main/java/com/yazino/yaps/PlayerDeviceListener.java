package com.yazino.yaps;

import com.yazino.engagement.mobile.MobileDeviceService;
import com.yazino.mobile.yaps.message.PlayerDevice;
import com.yazino.platform.Platform;
import com.yazino.platform.messaging.consumer.QueueMessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Listens for {@link PlayerDevice} objects (on a queue) and writes the player device to underlying dao.
 */
public class PlayerDeviceListener implements QueueMessageConsumer<PlayerDevice> {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerDeviceListener.class);

    private final MobileDeviceService mobileDeviceDao;

    @Autowired
    public PlayerDeviceListener(final MobileDeviceService mobileDeviceDao) {
        notNull(mobileDeviceDao, "mobileDeviceDao was null");
        this.mobileDeviceDao = mobileDeviceDao;
    }

    public void handle(final PlayerDevice playerDevice) {
        LOG.debug("Processing playerDevice [{}]", playerDevice);
        try {
            mobileDeviceDao.register(playerDevice.getPlayerId(), playerDevice.getGameType(), Platform.IOS, playerDevice.getBundle(),
                    null, playerDevice.getDeviceToken());
        } catch (Exception e) {
            LOG.error("Error processing playerDevice {}", playerDevice, e);
        }
    }

}
