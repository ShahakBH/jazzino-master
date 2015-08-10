package com.yazino.yaps;

import com.yazino.mobile.yaps.message.PlayerDevice;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Provides access to player device details.
 */
public interface PlayerDeviceDAO {

    void insertDeviceForPlayer(PlayerDevice playerDevice) throws Exception;

    Set<PlayerDevice> getDevicesForPlayerAndGame(BigDecimal playerId, String gameType);

    void removeDevicesForBundleAndTokens(String bundle, Set<Feedback> feedbacks);

}
