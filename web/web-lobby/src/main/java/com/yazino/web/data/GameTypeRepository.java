package com.yazino.web.data;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.platform.table.TableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GameTypeRepository {

    private final TableService tableService;

    GameTypeRepository() {
        // CGLib constructor
        tableService = null;
    }

    @Autowired
    public GameTypeRepository(final TableService tableService) {
        notNull(tableService, "tableService may not be null");

        this.tableService = tableService;
    }

    @Cacheable(cacheName = "gameTypeCache")
    public Map<String, GameTypeInformation> getGameTypes() {
        final Map<String, GameTypeInformation> gameTypesToInformation = new HashMap<String, GameTypeInformation>();

        final Set<GameTypeInformation> gameTypeInformation = tableService.getGameTypes();
        if (gameTypeInformation != null) {
            for (final GameTypeInformation info : gameTypeInformation) {
                gameTypesToInformation.put(info.getGameType().getId(), info);
            }
        }

        return gameTypesToInformation;
    }

}
