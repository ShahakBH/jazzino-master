package com.yazino.web.service;

import com.googlecode.ehcache.annotations.Cacheable;
import com.yazino.platform.table.GameConfiguration;
import com.yazino.platform.table.TableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

@Repository("gameConfigurationRepository")
public class GameConfigurationRepository {

    private final TableService tableService;

    private static final Comparator<GameConfiguration> BY_ORDER = new Comparator<GameConfiguration>() {
        @Override
        public int compare(final GameConfiguration a, final GameConfiguration b) {
            if (a.getOrder() > b.getOrder()) {
                return 1;
            }
            if (a.getOrder() < b.getOrder()) {
                return -1;
            }
            return 0;
        }
    };

    //cglib
    protected GameConfigurationRepository() {
        this.tableService = null;
    }

    @Autowired
    public GameConfigurationRepository(final TableService tableService) {
        notNull(tableService, "tableService is null");
        this.tableService = tableService;
    }

    @Cacheable(cacheName = "gameConfigurationAliasCache", refreshInterval = 1L)
    public GameConfiguration find(final String gameIdOrAlias) {
        return findByAlias(gameIdOrAlias);
    }

    @Cacheable(cacheName = "gameConfigurationCache", refreshInterval = 1L)
    public List<GameConfiguration> findAll() {
        return retrieveAllFromTableService();
    }

    private GameConfiguration findByAlias(final String gameIdOrAlias) {
        final Collection<GameConfiguration> gameConfigurations = findAll();
        for (GameConfiguration gameConfiguration : gameConfigurations) {
            if (gameConfiguration.getGameId().equalsIgnoreCase(gameIdOrAlias)
                    || gameConfiguration.getShortName().equalsIgnoreCase(gameIdOrAlias)) {
                return gameConfiguration;
            }
            for (final String alias : gameConfiguration.getAliases()) {
                if (alias.equalsIgnoreCase(gameIdOrAlias)) {
                    return gameConfiguration;
                }
            }
        }
        return null;
    }

    private List<GameConfiguration> retrieveAllFromTableService() {
        final Collection<GameConfiguration> gameConfigurations = tableService.getGameConfigurations();
        final List<GameConfiguration> result = new ArrayList<GameConfiguration>(gameConfigurations);
        Collections.sort(result, BY_ORDER);
        return result;
    }
}
