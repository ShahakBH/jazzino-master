package com.yazino.platform.gamehost;

import com.yazino.game.api.GameStatus;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.gamehost.external.NovomaticGameRequestService;
import com.yazino.platform.gamehost.preprocessing.CommandPreprocessor;
import com.yazino.platform.gamehost.preprocessing.TournamentPlayerValidator;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWallet;
import com.yazino.platform.gamehost.wallet.BufferedGameHostWalletFactory;
import com.yazino.platform.messaging.destination.DestinationFactory;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.community.PlayerSessionSummary;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.table.GameRepository;
import com.yazino.platform.service.audit.Auditor;
import com.yazino.platform.service.audit.CommonsLoggingAuditor;
import com.yazino.platform.table.PlayerInformation;
import com.yazino.platform.test.game.status.MockGameStatus;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class GameCommandExecutorTest {

    private final GameRepository gameRepository = mock(GameRepository.class);
    private final Auditor auditor = new CommonsLoggingAuditor();
    private final BufferedGameHostWalletFactory walletFactory = mock(BufferedGameHostWalletFactory.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final DestinationFactory destinationFactory = new DestinationFactory();
    private final BufferedGameHostWallet gameHostWallet = mock(BufferedGameHostWallet.class);
    private final com.yazino.game.api.GameRules gameRules = mock(com.yazino.game.api.GameRules.class);
    private NovomaticGameRequestService novomaticRequestService = mock(NovomaticGameRequestService.class);

    private final GameCommandExecutor commandExecutor = new GameCommandExecutor(gameRepository, auditor, walletFactory, novomaticRequestService, playerRepository, destinationFactory);

    @Before
    public void setup() {
        when(walletFactory.create(any(BigDecimal.class))).thenReturn(gameHostWallet);
        when(walletFactory.create(any(BigDecimal.class), anyString())).thenReturn(gameHostWallet);
        when(gameRepository.getGameRules(anyString())).thenReturn(gameRules);
    }

    @Test
    public void shouldReturnErrorDocumentIfTableDoesntSupportAnonymousPlayerCommands() throws WalletServiceException {
        commandExecutor.setPreExecutionProcessors(Arrays.<CommandPreprocessor>asList(new TournamentPlayerValidator(destinationFactory)));
        PlayerInformation playerAtTable = new PlayerInformation(BigDecimal.ONE, "TestPlayer", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN);
        when(playerRepository.findSummaryByPlayerAndSession(any(BigDecimal.class), eq(BigDecimal.TEN))).thenReturn(
                new PlayerSessionSummary(playerAtTable.getPlayerId(), playerAtTable.getAccountId(), playerAtTable.getName(), playerAtTable.getSessionId()));
        when(gameHostWallet.getBalance(playerAtTable.getAccountId())).thenReturn(BigDecimal.TEN);

        Table table = new Table(new com.yazino.game.api.GameType("SLOTS", "Slots", Collections.<String>emptySet()), BigDecimal.ONE, "DefaultSlots", false);
        table.setTableId(BigDecimal.ONE);
        table.setCurrentGame(statusWithPhase(MockGameStatus.MockGamePhase.Playing));
        table.setGameId(100L);
        com.yazino.game.api.GamePlayer gamePlayer = new com.yazino.game.api.GamePlayer(playerAtTable.getPlayerId(), playerAtTable.getSessionId(), playerAtTable.getName());
        com.yazino.game.api.Command command = new com.yazino.game.api.Command(gamePlayer, table.getTableId(), table.getGameId(), "FooBar", com.yazino.game.api.Command.CommandType.GetStatus.name());
        List<HostDocument> documents = commandExecutor.execute(table, command);
        assertEquals(1, documents.size());

    }

    private static com.yazino.game.api.GameStatus statusWithPhase(MockGameStatus.MockGamePhase phase) {
        return new GameStatus(MockGameStatus.emptyStatus(new HashMap<String, String>()).withPhase(phase));
    }
}
