package com.yazino.test.game;

import com.yazino.platform.gamehost.GameCommandExecutor;
import com.yazino.platform.gamehost.GameEventExecutor;
import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.gamehost.GameInitialiser;
import com.yazino.platform.gamehost.postprocessing.*;
import com.yazino.platform.gamehost.preprocessing.*;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWalletFactory;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.processor.table.GameCompletePublisher;
import com.yazino.platform.repository.chat.ChatRepository;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.service.audit.Auditor;
import com.yazino.platform.service.audit.CommonsLoggingAuditor;
import com.yazino.platform.service.community.LocationService;
import com.yazino.platform.service.statistic.PlayerStatisticEventsPublisher;
import com.yazino.platform.test.*;
import com.yazino.platform.util.JavaUUIDSource;
import com.yazino.platform.util.UUIDSource;
import com.yazino.test.statistic.InMemoryGameStatisticsPublisher;
import com.yazino.game.api.GameRules;
import senet.server.host.local.InMemoryBufferedGameHostWalletFactory;
import com.yazino.game.api.time.SettableTimeSource;
import senet.server.host.local.InMemoryNovomaticGameRequestService;

import java.util.Arrays;

public class GameHostFactory {

    public GameHost create(final InMemoryPlayerRepository playerRepository,
                           final SettableTimeSource timeSource,
                           final InMemoryWalletService walletService,
                           final GameRules gameRules,
                           final InMemoryGameStatisticsPublisher gameStatisticsPublisher,
                           final InMemoryNovomaticGameRequestService externalGameRequestService) {

        final GameRepository gameRepository = new InMemoryGameRepository(gameRules);
        final DestinationFactory destinationFactory = new DestinationFactory();


        final GameInitialiser gameInitialiser = new GameInitialiser();
        gameInitialiser.setEventPreInitialisationProcessors(Arrays.asList((EventPreprocessor)
                new TableClosedPreprocessor(destinationFactory),
                new GameDisabledPreprocessor(gameRepository, destinationFactory)));
        gameInitialiser.setCommandPreInitialisationProcessors(Arrays.asList(
                new TableClosedPreprocessor(destinationFactory),
                new TournamentPlayerValidator(destinationFactory),
                new GameDisabledPreprocessor(gameRepository, destinationFactory)));
        final ChatRepository chatRepository = new BlackholeChatRepository();
        final InMemoryNovomaticGameRequestService blah = externalGameRequestService;
        final LocationService locationService = new BlackholeLocationService();
        gameInitialiser.setPostInitialisationProcessors(Arrays.asList(
                new PlayerNotificationPostProcessor(destinationFactory, gameRepository),
                new NotifyLocationChangesPostprocessor(gameRepository,
                        playerRepository,
                        chatRepository,
                        locationService),
                new TableUpdatePostprocessor(gameRepository, timeSource)));

        final Auditor auditor = new CommonsLoggingAuditor();
        final BufferedGameHostWalletFactory bufferedGameHostWalletFactory =
                new InMemoryBufferedGameHostWalletFactory(new InMemoryGameHostWallet(walletService));

        final GameEventExecutor eventExecutor = new GameEventExecutor(gameRepository,
                auditor,
                bufferedGameHostWalletFactory, blah);
        eventExecutor.setGameInitialiser(gameInitialiser);
        final GameCommandExecutor commandExecutor = new GameCommandExecutor(gameRepository,
                auditor,
                bufferedGameHostWalletFactory,
                blah, playerRepository,
                destinationFactory);
        commandExecutor.setGameInitialiser(gameInitialiser);
        final UUIDSource uuidSource = new JavaUUIDSource();
        commandExecutor.setUuidSource(uuidSource);

        final GameHost gameHost = new GameHost(auditor,
                gameRepository,
                destinationFactory,
                playerRepository,
                bufferedGameHostWalletFactory,
                blah, uuidSource,
                eventExecutor,
                commandExecutor,
                gameInitialiser);

        gameHost.getEventExecutor().setGameInitialiser(gameInitialiser);
        gameHost.getCommandExecutor().setGameInitialiser(gameInitialiser);

        gameHost.getEventExecutor().setPreExecutionProcessors(Arrays.asList(
                new AuditPreprocessor(auditor),
                new TableReadyEventPreprocessor(gameRepository),
                new NopEventPreprocessor(),
                new EventStillValidPreprocessor()));
        final PlayerStatisticEventsPublisher playerStatisticEventsPublisher =
                new InMemoryPlayerStatisticEventsPublisher();
        final GameCompletePublisher gameCompletePublisher = new InMemoryGameCompletedPublisher();
        gameHost.getEventExecutor().setPostExecutionProcessors(Arrays.asList(
                new GameCompletePostprocessor(gameRepository, timeSource),
                new NotifyLocationChangesPostprocessor(gameRepository,
                        playerRepository,
                        chatRepository,
                        locationService),
                new PlayerNotificationPostProcessor(destinationFactory, gameRepository),
                new GameDisabledPostprocessor(gameRepository, destinationFactory),
                new TableUpdatePostprocessor(gameRepository, timeSource),
                new GameXPPublishingPostProcessor(playerStatisticEventsPublisher),
                new GameStatisticsPublishingPostProcessor(gameStatisticsPublisher),
                new TableAuditPostprocessor(auditor, gameCompletePublisher, gameRepository)));

        gameHost.getCommandExecutor().setPreExecutionProcessors(Arrays.asList(
                new AuditPreprocessor(auditor),
                new TableClosingPreprocessor(destinationFactory),
                new InitialGetStatusPreprocessor(auditor, destinationFactory),
                new AcknowledgementPreprocessor()));

        gameHost.getCommandExecutor().setPostExecutionProcessors(Arrays.asList(
                new GameCompletePostprocessor(gameRepository, timeSource),
                new NotifyLocationChangesPostprocessor(gameRepository,
                        playerRepository,
                        chatRepository,
                        locationService),
                new PlayerNotificationPostProcessor(destinationFactory, gameRepository),
                new GameDisabledPostprocessor(gameRepository, destinationFactory),
                new TableUpdatePostprocessor(gameRepository, timeSource),
                new GameXPPublishingPostProcessor(playerStatisticEventsPublisher),
                new GameStatisticsPublishingPostProcessor(gameStatisticsPublisher),
                new TableAuditPostprocessor(auditor, gameCompletePublisher, gameRepository)
        ));

        gameHost.setPlayerRemovalProcessors(Arrays.asList((PlayerRemovalProcessor)
                new NotifyLocationChangesPostprocessor(gameRepository,
                        playerRepository,
                        chatRepository,
                        locationService)));

        return gameHost;
    }
}
