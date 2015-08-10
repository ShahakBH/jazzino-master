package com.yazino.platform.player.service;

public interface PlayerProfileTestService {

    void breakLinkBetweenPlayerAndExternalIdentity(String providerName, String externalId);

    void breakLinkBetweenDeviceAndGuestAccount(String email);

}
