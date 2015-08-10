package com.yazino.platform.service.table;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.model.table.Countdown;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.plugin.GamePluginManager;
import com.yazino.platform.repository.table.*;
import com.yazino.platform.table.GameTypeInformation;
import com.yazino.platform.table.TableConfigurationUpdateService;
import com.yazino.platform.util.JsonHelper;
import com.yazino.platform.util.Visitor;
import org.openspaces.remoting.RemotingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Set;

import static com.yazino.platform.model.table.TableControlMessageType.SHUTDOWN;
import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingTableConfigurationUpdateService implements TableConfigurationUpdateService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingTableConfigurationUpdateService.class);

    private final JsonHelper jsonHelper = new JsonHelper();
    private final CountdownRepository countdownRepository;
    private final GameRepository gameRepository;
    private final TableRepository tableRepository;
    private final GameVariationRepository gameTemplateRepository;
    private final DocumentDispatcher documentDispatcher;
    private final GameConfigurationRepository gameConfigurationRepository;
    private final GamePluginManager gamePluginManager;

    @Autowired
    public GigaspaceRemotingTableConfigurationUpdateService(
            final GameRepository gameRepository,
            final CountdownRepository countdownRepository,
            final TableRepository tableRepository,
            final GameConfigurationRepository gameConfigurationRepository,
            final GameVariationRepository gameTemplateRepository,
            final GamePluginManager gamePluginManager,
            @Qualifier("documentDispatcher") final DocumentDispatcher documentDispatcher) {
        notNull(gameRepository, "gameRepository may not be null");
        notNull(countdownRepository, "countdownRepository may not be null");
        notNull(tableRepository, "tableRepository may not be null");
        notNull(gameConfigurationRepository, "gameConfigurationRepository may not be null");
        notNull(gameTemplateRepository, "gameTemplateRepository may not be null");
        notNull(gamePluginManager, "gamePluginManager may not be null");
        notNull(documentDispatcher, "documentDispatcher may not be null");

        this.gameRepository = gameRepository;
        this.countdownRepository = countdownRepository;
        this.tableRepository = tableRepository;
        this.gameConfigurationRepository = gameConfigurationRepository;
        this.gameTemplateRepository = gameTemplateRepository;
        this.gamePluginManager = gamePluginManager;
        this.documentDispatcher = documentDispatcher;
    }

    @Override
    public void setAvailabilityFor(final String gameType, final boolean available) {
        notNull(gameType, "gameType may not be null");
        gameRepository.setGameAvailable(gameType, available);
    }

    @Override
    public void asyncSetAvailabilityFor(final String gameType, final boolean available) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #setAvailabilityFor will be invoked
    }

    @Override
    public void disableAndShutdownAllGames() {
        for (GameTypeInformation gameTypeInformation : gameRepository.getAvailableGameTypes()) {
            gameRepository.setGameAvailable(gameTypeInformation.getId(), false);
        }

        tableRepository.visitAllLocalTables(new Visitor<Table>() {
            @Override
            public void visit(final Table table) {
                tableRepository.sendControlMessage(table.getTableId(), SHUTDOWN);
            }
        });
    }

    @Override
    public void asyncDisableAndShutdownAllGames() {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #disableAndShutdownAllGames will be invoked
    }

    @Override
    public void publishCountdownForAllGames(final Long countdownTimeout) {
        final Set<BigDecimal> playerIds = tableRepository.findAllLocalPlayers();
        publishCountdownForPlayers("ALL", countdownTimeout, playerIds);
    }

    @Override
    public void asyncPublishCountdownForAllGames(final Long countdown) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #publishCountdownForAllGames will be invoked
    }

    @Override
    public void publishCountdownForGameType(final Long countdownTimeout, final String gameType) {
        notNull(gameType, "gameType may not be null");
        final Set<BigDecimal> playerIds = tableRepository.findAllLocalPlayersForGameType(gameType);
        publishCountdownForPlayers(gameType, countdownTimeout, playerIds);
    }

    @Override
    public void asyncPublishCountdownForGameType(final Long countdownTimeout, final String gameType) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #publishCountdownForGameType will be invoked
    }

    private void publishCountdownForPlayers(final String countdownId,
                                            final Long countdownTimeout,
                                            final Set<BigDecimal> playerIds) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Publishing Countdown [%s]", countdownTimeout));
        }
        final Countdown countdown = new Countdown(countdownId, countdownTimeout);
        countdownRepository.publishIntoSpace(countdown);

        final String documentType = DocumentType.COUNTDOWN.name();
        final String serializedCountdown = jsonHelper.serialize(countdown);
        final Document document = new Document(documentType, serializedCountdown, new HashMap<String, String>());

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Dispatching Countdown [%s] to players [%s] with payload %s",
                    serializedCountdown, playerIds, document));
        }
        documentDispatcher.dispatch(document, playerIds);
    }

    @Override
    public void stopCountdown(final String countdownId) {
        final Countdown countdown = new Countdown(countdownId, 0L);
        final Document document = new Document(DocumentType.COUNTDOWN.name(),
                jsonHelper.serialize(countdown), new HashMap<String, String>());
        countdownRepository.removeCountdownFromSpace(countdown);
        documentDispatcher.dispatch(document);
    }

    @Override
    public void asyncStopCountdown(final String countdownId) {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #stopCountdown will be invoked
    }

    @Override
    public void publishGame(final String filename) throws IOException {
        gamePluginManager.publishPlugin(filename);
    }

    /* called by the control centre to save game configuration changes */
    @Override
    public void refreshGameConfigurations() {
        gameConfigurationRepository.refreshAll();
    }

    @Override
    public void asyncRefreshGameConfigurations() {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #refreshGameConfigurations will be invoked
    }

    @Override
    public void refreshTemplates() {
        gameTemplateRepository.refreshAll();
    }

    @Override
    public void asyncRefreshTemplates() {
        // as per http://www.gigaspaces.com/wiki/display/XAP7/Executor+Based+Remoting, the
        // implementation is empty and #refreshTemplates will be invoked
    }
}
