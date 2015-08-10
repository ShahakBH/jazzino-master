package com.yazino.web.domain.email;

import com.yazino.platform.player.service.PlayerProfileService;

/**
 * Describes a class which is able to build {@link EmailRequest}'s.
 */
public interface EmailBuilder {

    /**
     * Build the request. May use the profile service to retrieve details about the player.
     * @param profileService, will never be null.
     * @return an EmailRequest, never null
     */
    EmailRequest buildRequest(PlayerProfileService profileService);
}
