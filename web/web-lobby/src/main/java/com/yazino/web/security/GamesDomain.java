package com.yazino.web.security;

import com.yazino.platform.table.GameTypeInformation;
import com.yazino.web.data.GameTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

@Repository
public class GamesDomain implements Domain {
    private final GameTypeRepository gameTypeRepository;

    @Autowired
    public GamesDomain(final GameTypeRepository gameTypeRepository) {
        checkNotNull(gameTypeRepository);
        this.gameTypeRepository = gameTypeRepository;
    }

    @Override
    public boolean includesUrl(final String url) {
        Map<String, GameTypeInformation> gamesInfo = gameTypeRepository.getGameTypes();

        for (String gameType : gamesInfo.keySet()) {
            if (isUrlForGame(url, gameType, gamesInfo)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUrlForGame(final String url,
                                 final String gameType,
                                 final Map<String, GameTypeInformation> gamesInfo) {
        if (url.equalsIgnoreCase(getGameTypeAsUrl(gameType))) {
            return true;
        } else {
            final Set<String> pseudonyms = gamesInfo.get(gameType).getGameType().getPseudonyms();
            for (String pseudonym : pseudonyms) {
                if (url.equalsIgnoreCase(getGameTypeAsUrl(pseudonym))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getGameTypeAsUrl(final String gameType) {
        return "/" + gameType;
    }
}
