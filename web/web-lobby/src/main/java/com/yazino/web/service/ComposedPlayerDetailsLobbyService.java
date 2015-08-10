package com.yazino.web.service;

import com.yazino.platform.community.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

@Service("playerDetailsLobbyService")
public class ComposedPlayerDetailsLobbyService implements PlayerDetailsLobbyService {
    private final PlayerService playerService;

    /*
     CGLib constructor
      */
    ComposedPlayerDetailsLobbyService() {
        playerService = null;
    }

    @Autowired
    public ComposedPlayerDetailsLobbyService(@Qualifier("playerService") final PlayerService playerService) {
        this.playerService = playerService;
    }

    public Set<BigDecimal> getFriends(final BigDecimal playerId) {
        return playerService.getFriends(playerId);
    }

}
