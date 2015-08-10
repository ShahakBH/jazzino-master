package com.yazino.platform.gamehost.wallet;

import com.yazino.platform.gamehost.GamePlayerService;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.repository.community.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yazino.game.api.GamePlayer;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class GigaspacePlayerService implements GamePlayerService {
    private final PlayerRepository playerRepository;

    public GigaspacePlayerService() {
        this.playerRepository = null;
    }

    @Autowired
    public GigaspacePlayerService(final PlayerRepository playerRepository) {
        notNull(playerRepository, "playerRepository may not be null");

        this.playerRepository = playerRepository;
    }

    public GamePlayer getPlayer(final BigDecimal playerId) {
        final Player player = playerRepository.findById(playerId);
        return new GamePlayer(playerId, null, player.getName());
    }
}
