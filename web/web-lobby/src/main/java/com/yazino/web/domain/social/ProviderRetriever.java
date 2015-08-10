package com.yazino.web.domain.social;

import com.yazino.platform.player.PlayerProfile;
import com.yazino.platform.player.service.PlayerProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Qualifier("playerInformationRetriever")
public class ProviderRetriever implements PlayerInformationRetriever {
    private final PlayerProfileService playerProfileService;

    @Autowired
    public ProviderRetriever(final PlayerProfileService playerProfileService) {
        this.playerProfileService = playerProfileService;
    }

    @Override
    public PlayerInformationType getType() {
        return PlayerInformationType.PROVIDER;
    }

    @Override
    public String retrieveInformation(final BigDecimal playerId, final String gameType) {
        final PlayerProfile player = playerProfileService.findByPlayerId(playerId);
        if (player == null) {
            return null;
        }
        return player.getProviderName();
    }
}
