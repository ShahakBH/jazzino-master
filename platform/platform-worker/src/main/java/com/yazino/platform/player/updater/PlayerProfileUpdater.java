package com.yazino.platform.player.updater;

import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.PlayerProfileUpdateResponse;

public interface PlayerProfileUpdater {

    PlayerProfileUpdateResponse update(PlayerProfile playerProfile,
                                       String password, final String avatarUrl);

    boolean accepts(String provider);

}
