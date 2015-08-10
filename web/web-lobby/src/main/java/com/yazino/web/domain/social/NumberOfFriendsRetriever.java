package com.yazino.web.domain.social;

import com.yazino.web.util.PlayerFriendsCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Qualifier("playerInformationRetriever")
public class NumberOfFriendsRetriever implements PlayerInformationRetriever {

    private static final Logger LOG = LoggerFactory.getLogger(NumberOfFriendsRetriever.class);

    private final PlayerFriendsCache playerFriendsCache;

    @Autowired
    public NumberOfFriendsRetriever(PlayerFriendsCache playerFriendsCache) {
        this.playerFriendsCache = playerFriendsCache;
    }

    @Override
    public PlayerInformationType getType() {
        return PlayerInformationType.NUMBER_OF_FRIENDS;
    }

    @Override
    public Object retrieveInformation(BigDecimal playerId, String gameType) {
        try {
            return playerFriendsCache.getFriendIds(playerId).size();
        } catch (Exception e) {
            LOG.warn("Failed to retrieve number of friends for player {}: {}", playerId, e.getMessage());
            return 0;
        }
    }
}
