package com.yazino.platform.processor.table;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.gamehost.GamePlayerService;
import com.yazino.platform.gamehost.postprocessing.Postprocessor;
import com.yazino.platform.gamehost.preprocessing.CommandPreprocessor;
import com.yazino.platform.gamehost.preprocessing.EventPreprocessor;
import com.yazino.platform.gamehost.wallet.TableBoundGamePlayerWalletFactory;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.chat.ChatRepository;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.service.audit.Auditor;
import com.yazino.platform.service.community.GigaSpaceLocationService;
import com.yazino.platform.service.community.LocationService;
import com.yazino.platform.session.Location;
import com.yazino.platform.session.LocationChangeType;
import com.yazino.platform.table.PlayerInformation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import com.yazino.game.api.time.SettableTimeSource;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class ProcessingTestContext {
    Table table;
    private com.yazino.game.api.Command command;
    private BigDecimal playerId = BigDecimal.ONE;
    private com.yazino.game.api.GamePlayer player = new com.yazino.game.api.GamePlayer(playerId, null, "name");
    private String auditLabel = "auditLabel";

    private final GameHost gameHost;
    private final DocumentDispatcher documentDispatcher;
    private final ChatRepository chatRepository;
    private final PlayerRepository playerRepository;
    private final LocationService locationService;
    private final com.yazino.game.api.GameStatus gameStatus;
    private final com.yazino.game.api.GamePlayerWallet gamePlayerWallet;
    private final GameCompletePublisher gameCompletePublisher;
    private final ArgumentCaptor<Document> documentCaptor;
    private final List<HostDocument> documentsToSend = new ArrayList<>();
    private final DestinationFactory destinationFactory;
    private final Destination destination;

    private com.yazino.game.api.ExecutionResult executionResult;
    private com.yazino.game.api.GameStatus newGameStatus;
    private SettableTimeSource timeSource = new SettableTimeSource(0);
    private Auditor auditor;
    private com.yazino.game.api.ScheduledEvent event;
    private GameRepository gameRepository;
    private GamePlayerService tablePlayerService;
    private com.yazino.game.api.GameRules gameRules;

    @SuppressWarnings({"unchecked"})
    public ProcessingTestContext() {
        gameHost = mock(GameHost.class);
        documentDispatcher = mock(DocumentDispatcher.class);
        final TableBoundGamePlayerWalletFactory gamePlayerWalletFactory = mock(TableBoundGamePlayerWalletFactory.class);
        tablePlayerService = mock(GamePlayerService.class);
        gameStatus = mock(com.yazino.game.api.GameStatus.class);
        newGameStatus = mock(com.yazino.game.api.GameStatus.class);
        gamePlayerWallet = mock(com.yazino.game.api.GamePlayerWallet.class);
        auditor = mock(Auditor.class);
        gameCompletePublisher = mock(GameCompletePublisher.class);
        locationService = mock(GigaSpaceLocationService.class);
        gameRepository = mock(GameRepository.class);
        destinationFactory = mock(DestinationFactory.class);
        destination = mock(Destination.class);
        chatRepository = mock(ChatRepository.class);
        playerRepository = mock(PlayerRepository.class);
        gameRules = mock(com.yazino.game.api.GameRules.class);

        when(destinationFactory.player(Mockito.any(BigDecimal.class))).thenReturn(destination);
        when(destinationFactory.players(Mockito.any(Set.class))).thenReturn(destination);
        when(destinationFactory.observers()).thenReturn(destination);

        documentCaptor = ArgumentCaptor.forClass(Document.class);

        when(gamePlayerWalletFactory.forPlayer(player)).thenReturn(gamePlayerWallet);

        when(gameRepository.getGameRules(anyString())).thenReturn(gameRules);

        table = new Table();
        table.setTableId(BigDecimal.TEN);
        table.setGameId(1L);
        table.setCurrentGame(gameStatus);
        table.setGameTypeId("GAME_TYPE");
        table.setIncrement(0L);
        table.setClient(new Client("client", 5, "file", "getGameType", new HashMap<String, String>()));
        table.addPlayerToTable(new PlayerInformation(
                player.getId(), player.getName(), player.getId(), BigDecimal.TEN, BigDecimal.ZERO));

        command = new com.yazino.game.api.Command(player, BigDecimal.TEN, 1L, "uuid", "ACommand");
        event = new com.yazino.game.api.ScheduledEvent(0, table.getGameId(), "", "", new HashMap<String, String>(), true);
        executionResult = new com.yazino.game.api.ExecutionResult.Builder(gameRules, newGameStatus).scheduledEvents(Arrays.asList(event)).build();
        when(gameRules.getNumberOfSeatsTaken(newGameStatus)).thenReturn(1);
    }

    public ChatRepository getChatRepository() {
        return chatRepository;
    }

    public PlayerRepository getPlayerRepository() {
        return playerRepository;
    }

    public LocationService getLocationService() {
        return locationService;
    }

    public Auditor getAuditor() {
        return auditor;
    }

    public GameCompletePublisher getGameCompletePublisher() {
        return gameCompletePublisher;
    }

    public GameRepository getGameRepository() {
        return gameRepository;
    }

    public GamePlayerService getTablePlayerService() {
        return tablePlayerService;
    }

    public com.yazino.game.api.GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setCommand(com.yazino.game.api.Command command) {
        this.command = command;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public Table getTable() {
        return table;
    }

    public com.yazino.game.api.GamePlayer getPlayer() {
        return player;
    }

    public com.yazino.game.api.GameStatus getNewGameStatus() {
        return newGameStatus;
    }

    public boolean preprocess(CommandPreprocessor preprocessor) throws com.yazino.game.api.GameException {
        return preprocessor.preProcess(gameRules, command, BigDecimal.ZERO, table, auditLabel, documentsToSend);
    }

    public void postprocess(Postprocessor postprocessor) throws com.yazino.game.api.GameException {
        when(gamePlayerWallet.getBalance()).thenReturn(BigDecimal.ZERO);

        postprocessor.postProcess(executionResult, command, table, auditLabel, documentsToSend, null);
    }

    public void gameIsFinished() {
        when(gameRules.isComplete(gameStatus)).thenReturn(true);
    }

    public void gameCanBeClosed() {
        when(gameRules.canBeClosed(gameStatus)).thenReturn(true);
    }

    public void newGameIsFinished() {
        when(gameRules.isComplete(newGameStatus)).thenReturn(true);
    }

    public void newGameCanBeClosed() {
        when(gameRules.canBeClosed(newGameStatus)).thenReturn(true);
    }

    public Document getSinglePlayerDocument() {
        verify(destinationFactory).player(playerId);
        final HostDocument hostDocument = lastQueuedDocument();
        hostDocument.send(documentDispatcher);
        verify(destination).send(documentCaptor.capture(), eq(documentDispatcher));
        return documentCaptor.getValue();
    }

    public Document getTablePlayerDocument() {
        verify(destinationFactory).players(new HashSet<>(Arrays.asList(playerId)));
        final HostDocument hostDocument = lastQueuedDocument();
        hostDocument.send(documentDispatcher);
        verify(destination).send(documentCaptor.capture(), eq(documentDispatcher));
        return documentCaptor.getValue();
    }

    public DestinationFactory getDestinationFactory() {
        return destinationFactory;
    }

    public Document getObserverDocument() {
        verify(destinationFactory).observers();
        final HostDocument hostDocument = lastQueuedDocument();
        hostDocument.send(documentDispatcher);
        verify(destination).send(documentCaptor.capture(), eq(documentDispatcher));
        return documentCaptor.getValue();
    }

    private HostDocument lastQueuedDocument() {
        if (documentsToSend.size() > 0) {
            return documentsToSend.get(documentsToSend.size() - 1);
        }
        return null;
    }

    public void verifyNoHostInteractions() {
        verifyNoMoreInteractions(gameHost);
    }

    public void playerIsLeaving() {
        when(gameRules.getPlayerInformation(gameStatus)).thenReturn(Arrays.asList(new com.yazino.game.api.PlayerAtTableInformation(player, Collections.<String, String>emptyMap())));
        when(gameRules.isAPlayer(gameStatus, player)).thenReturn(true);
        when(gameRules.getPlayerInformation(newGameStatus)).thenReturn(Collections.<com.yazino.game.api.PlayerAtTableInformation>emptyList());
        when(gameRules.isAPlayer(newGameStatus, player)).thenReturn(false);
    }

    public void playerIsPlaying() {
        when(gameRules.getPlayerInformation(gameStatus)).thenReturn(Arrays.asList(new com.yazino.game.api.PlayerAtTableInformation(player, Collections.<String, String>emptyMap())));
        when(gameRules.isAPlayer(gameStatus, player)).thenReturn(true);
        when(gameRules.getPlayerInformation(newGameStatus)).thenReturn(Arrays.asList(new com.yazino.game.api.PlayerAtTableInformation(player, Collections.<String, String>emptyMap())));
        when(gameRules.isAPlayer(newGameStatus, player)).thenReturn(true);
    }

    public void setExecutionResult(com.yazino.game.api.ExecutionResult executionResult) {
        this.executionResult = executionResult;
    }

    public List<com.yazino.game.api.ScheduledEvent> getTableEvents() {
        return table.getPendingEvents(timeSource);
    }

    public void verifyTableAudited() {
        verify(auditor).audit(auditLabel, table, gameRules);
    }

    public void moveTime(long millis) {
        timeSource.addMillis(millis);
    }

    public void updateGameIsFinished() {
        when(gameRules.isComplete(newGameStatus)).thenReturn(true);
    }

    public void updateGameAvailableForPlayersJoining() {
        when(gameRules.isAvailableForPlayerJoining(newGameStatus)).thenReturn(true);
    }

    public void updateGameNotAvailableForPlayersJoining() {
        when(gameRules.isAvailableForPlayerJoining(newGameStatus)).thenReturn(false);
    }

    public void newStatusHasChanges() {

        final com.yazino.game.api.ObservableStatus observableStatus = mock(com.yazino.game.api.ObservableStatus.class);
        when(gameRules.getObservableStatus(eq(newGameStatus), any(com.yazino.game.api.ObservableContext.class))).thenReturn(observableStatus);
        when(observableStatus.getObservableChanges()).thenReturn(Arrays.asList(new com.yazino.game.api.ObservableChange()));
    }

    public boolean preProcessEvent(EventPreprocessor underTest) {
        return underTest.preprocess(event, table);
    }

    public void setEvent(com.yazino.game.api.ScheduledEvent scheduledEvent) {
        this.event = scheduledEvent;
    }

    public void verifyPlayerLeavesLocation() {
        verify(locationService).notify(eq(playerId), any(BigDecimal.class), eq(LocationChangeType.REMOVE), any(Location.class));
    }

    public void verifyPlayerRemainsInCache() {
        assertThat(table.cachedPlayer(playerId), is(equalTo(player)));
    }

    public void setGameEnabled(boolean enabled) {
        when(gameRepository.isGameAvailable(isA(String.class))).thenReturn(enabled);
    }

    public void verifyGameAvailablityIsChecked() {
        verify(gameRepository).isGameAvailable(isA(String.class));
    }

    public void eventIsNopEvent() {
        setEvent(com.yazino.game.api.ScheduledEvent.noProcessingEvent(table.getGameId()));
    }

    public void verifyGameCompleteSent() {
        verify(gameCompletePublisher).publishCompletedGame(gameStatus, table.getGameTypeId(), table.getTableId(), table.getClientId());
    }

    public void setGetStatusCommand() {
        setCommand(new com.yazino.game.api.Command(player, BigDecimal.TEN, 1L, "uuid", com.yazino.game.api.Command.CommandType.InitialGetStatus.getCode()));
    }

    public TimeSource getTimeSource() {
        return timeSource;
    }

    public void verifyAvaialbeForPlayersJoining() {
        verify(gameRules).isAvailableForPlayerJoining(newGameStatus);
    }

    public com.yazino.game.api.GameRules getGameRules() {
        return gameRules;
    }
}
