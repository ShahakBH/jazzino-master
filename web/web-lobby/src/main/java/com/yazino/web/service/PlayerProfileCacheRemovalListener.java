package com.yazino.web.service;

import com.yazino.platform.table.GameTypeInformation;
import com.yazino.platform.table.TableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@Service
public class PlayerProfileCacheRemovalListener {
    private final TableService tableService;
    private final PlayerProfileCacheRemoval playerProfileCacheRemoval;

    @Autowired
    public PlayerProfileCacheRemovalListener(final TableService tableService,
                                             final PlayerProfileCacheRemoval playerProfileCacheRemoval) {
        notNull(tableService, "tableService may not be null");
        notNull(playerProfileCacheRemoval, "playerProfileCacheRemoval may not be null");

        this.tableService = tableService;
        this.playerProfileCacheRemoval = playerProfileCacheRemoval;
    }

    public void playerUpdated(final BigDecimal playerId) {
        for (GameTypeInformation gameType : tableService.getGameTypes()) {
            playerProfileCacheRemoval.remove(playerId, gameType.getId());
        }
    }
}
