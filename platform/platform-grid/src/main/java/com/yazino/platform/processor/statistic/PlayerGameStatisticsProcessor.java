package com.yazino.platform.processor.statistic;

import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.statistic.PlayerGameStatistics;
import com.yazino.platform.model.statistic.PlayerStatisticEvent;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.statistic.PlayerGameStatisticConsumerRepository;
import com.yazino.platform.repository.table.ClientRepository;
import org.openspaces.core.GigaSpace;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.yazino.game.api.GamePlayer;
import com.yazino.game.api.statistic.GameStatisticConsumer;
import com.yazino.game.api.statistic.GameStatistics;
import com.yazino.game.api.statistic.StatisticEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", concurrentConsumers = 1, maxConcurrentConsumers = 5)
public class PlayerGameStatisticsProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(PlayerGameStatisticsProcessor.class);

    private static final PlayerGameStatistics TEMPLATE = new PlayerGameStatistics();

    private final GigaSpace gigaSpace;
    private final ClientRepository clientRepository;
    private final PlayerRepository playerRepository;
    private final PlayerGameStatisticConsumerRepository playerGameStatisticConsumerRepository;

    /*
      * CGLib constructor.
      */
    protected PlayerGameStatisticsProcessor() {
        this.gigaSpace = null;
        this.clientRepository = null;
        this.playerRepository = null;
        this.playerGameStatisticConsumerRepository = null;
    }

    @Autowired
    public PlayerGameStatisticsProcessor(@Qualifier("gigaSpace") final GigaSpace gigaSpace,
                                         final ClientRepository clientRepository,
                                         final PlayerRepository playerRepository,
                                         final PlayerGameStatisticConsumerRepository playerGameStatisticConsumerRepository) {
        notNull(gigaSpace, "GigaSpace may not be null");
        notNull(clientRepository, "clientRepository may not be null");
        notNull(playerRepository, "playerRepository must not be null");
        notNull(playerGameStatisticConsumerRepository, "playerGameStatisticConsumerRepository must not be null");

        this.gigaSpace = gigaSpace;
        this.clientRepository = clientRepository;
        this.playerRepository = playerRepository;
        this.playerGameStatisticConsumerRepository = playerGameStatisticConsumerRepository;
    }

    @EventTemplate
    public PlayerGameStatistics template() {
        return TEMPLATE;
    }

    private void checkForInitialisation() {
        if (gigaSpace == null
                || clientRepository == null
                || playerRepository == null
                || playerGameStatisticConsumerRepository == null) {
            throw new IllegalStateException("Class was created via CGLib constructor and is invalid for direct use");
        }
    }

    @SpaceDataEvent
    public void processRequest(final PlayerGameStatistics request) {
        LOG.debug("ProcessRequest: {}", request);

        try {
            checkForInitialisation();

            notNull(request, "Request may not be null");
            notNull(request.getPlayerId(), "Request: Player ID may not be null");

        } catch (Exception e) {
            LOG.error("Internal error encountered", e);
            return;
        }

        final Player player = playerRepository.findById(request.getPlayerId());
        if (player == null) {
            LOG.warn("Player not found {}", request.getPlayerId());
            return;
        }

        try {
            final GamePlayer gamePlayer = new GamePlayer(player.getPlayerId(), null, player.getName());
            final GameStatistics gameStatistics = new GameStatistics(request.getStatistics());
            final Collection<StatisticEvent> statisticEvents = new HashSet<StatisticEvent>();

            final Map<String, String> clientProperties = propertiesFor(request.getClientId());
            for (final GameStatisticConsumer consumer : playerGameStatisticConsumerRepository.getConsumers()) {
                if (consumer.acceptsGameType(request.getGameType())) {
                    LOG.debug("Processing statistics with consumer {}", consumer);

                    statisticEvents.addAll(consumer.consume(
                            gamePlayer, request.getTableId(), request.getGameType(),
                            clientProperties, gameStatistics));
                }
            }

            final PlayerStatisticEvent playerStatisticEvent = new PlayerStatisticEvent(
                    player.getPlayerId(), request.getGameType(), statisticEvents);
            gigaSpace.write(playerStatisticEvent);

        } catch (Throwable t) {
            LOG.warn("Failed to generate statistic event for request {}", request, t);
        }
    }

    private Map<String, String> propertiesFor(final String clientId) {
        if (clientId == null) {
            return Collections.emptyMap();
        }

        final Client client = clientRepository.findById(clientId);
        if (client != null) {
            return client.getClientProperties();
        }

        throw new IllegalArgumentException("Invalid client ID: " + clientId);
    }
}
