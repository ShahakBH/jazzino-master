package com.yazino.host.table.player;

import com.yazino.host.community.StandalonePlayerSource;
import com.yazino.model.StandalonePlayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yazino.game.api.GamePlayer;
import com.yazino.platform.gamehost.GamePlayerService;

import java.math.BigDecimal;

@Component
public class StandalonePlayerService implements GamePlayerService {
    private final StandalonePlayerSource standalonePlayerRepository;

    @Autowired
    public StandalonePlayerService(final StandalonePlayerSource standalonePlayerRepository) {
        this.standalonePlayerRepository = standalonePlayerRepository;
    }

    @Override
    public GamePlayer getPlayer(final BigDecimal playerId) {
        final StandalonePlayer player = standalonePlayerRepository.findById(playerId);
        return new GamePlayer(player.getPlayerId(), null, player.getName());
    }
}
