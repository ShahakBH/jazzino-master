package com.yazino.novomatic.cgs;

import com.yazino.configuration.YazinoConfiguration;
import com.yazino.novomatic.cgs.message.NovomaticGameDefinition;
import com.yazino.novomatic.cgs.msgpack.MessagePackMapper;
import com.yazino.novomatic.cgs.transport.ClientSocketConnectionPool;
import com.yazino.novomatic.cgs.transport.ClientTransport;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

public class ClientExternalIntegrationTest {
    private NovomaticClient novomaticClient;

    @Before
    public void setUp() throws IOException {
        final ClientTransport transport = new ClientTransport(new ClientSocketConnectionPool(new YazinoConfiguration()));
        novomaticClient = new NovomaticClient(transport, new MessagePackMapper());
    }

    @Test
    public void shouldGetListOfAvailableGames() throws IOException, NovomaticError {
        final List<NovomaticGameDefinition> availableGames = novomaticClient.getAvailableGames();
        assertNotNull(availableGames);
        assertThat(availableGames.size(), is(1));
        assertThat(availableGames.get(0).getName(), is(equalTo("Just Jewels")));
    }

    @Test
    public void shouldAllowDecreasingLines() throws NovomaticError, IOException {
        final NovomaticGameState gameState = novomaticClient.initGame(1000l);
        assertThat(recordsFrom(gameState), hasItem((NovomaticEvent) new GameParameters(new SelectableValue(asList(1l, 3l, 5l, 7l, 9l), 9l), new SelectableValue(asList(1l, 2l, 3l, 4l, 5l, 10l, 20l, 50l, 100l), 1l),
                                                                                       new SelectableValue(asList(1L, 2L, 5L, 10L, 20L, 50L, 100L), 1l),
                                                                                       1000l, 1000l)));
        final NovomaticGameState gameState2 = novomaticClient.sendCommand(gameState.getInternalState(), NovomaticCommand.DecreaseLines);
        assertThat(recordsFrom(gameState2), hasItem((NovomaticEvent) new GameParameters(new SelectableValue(asList(1l, 3l, 5l, 7l, 9l), 7l), new SelectableValue(asList(1l, 2l, 3l, 4l, 5l, 10l, 20l, 50l, 100l), 1l),
                                                                                        new SelectableValue(asList(1L, 2L, 5L, 10L, 20L, 50L, 100L), 1l),
                                                                                        1000l, 1000l)));
        final NovomaticGameState gameState3 = novomaticClient.sendCommand(gameState.getInternalState(), NovomaticCommand.Spin);
        assertNotNull(gameState3);
    }

    @Test
    public void shouldAllowIncreaseInBet() throws NovomaticError, IOException {
        final NovomaticGameState gameState = novomaticClient.initGame(1000l);
        assertThat(recordsFrom(gameState), hasItem((NovomaticEvent) new GameParameters(new SelectableValue(asList(1l, 3l, 5l, 7l, 9l), 9l), new SelectableValue(asList(1l, 2l, 3l, 4l, 5l, 10l, 20l, 50l, 100l), 1l),
                                                                                       new SelectableValue(asList(1L, 2L, 5L, 10L, 20L, 50L, 100L), 1l),
                                                                                       1000l, 1000l)));
        final NovomaticGameState gameState2 = novomaticClient.sendCommand(gameState.getInternalState(), NovomaticCommand.IncreaseBetPlacement);
        assertThat(recordsFrom(gameState2), hasItem((NovomaticEvent) new GameParameters(new SelectableValue(asList(1l, 3l, 5l, 7l, 9l), 9l), new SelectableValue(asList(1l, 2l, 3l, 4l, 5l, 10l, 20l, 50l, 100l), 2l),
                                                                                        new SelectableValue(asList(1L, 2L, 5L, 10L, 20L, 50L), 1l),
                                                                                        1000l, 1000l)));
        final NovomaticGameState gameState3 = novomaticClient.sendCommand(gameState.getInternalState(), NovomaticCommand.Spin);
        assertNotNull(gameState3);
    }

    @Test
    public void shouldAllowSpin() throws NovomaticError {
        NovomaticGameState gameState = novomaticClient.initGame(1000l);
        gameState = novomaticClient.sendCommand(gameState.getInternalState(), NovomaticCommand.Spin);
        boolean spinHappened = false;
        for (NovomaticEvent novomaticEvent : gameState.getEvents()) {
            if (novomaticEvent.getNovomaticEventType().equals(NovomaticEventType.EventCreditChange.getNovomaticEventType()) || novomaticEvent.getNovomaticEventType().equals(NovomaticEventType.EventCreditWin.getNovomaticEventType())) {
                spinHappened = true;
            }
        }
        assertThat(spinHappened, is(true));
    }

    @Test
//    @Ignore("Should really use CheatClient instead.")
    public void shouldPlayGambler() throws NovomaticError {
        NovomaticGameState gameState = novomaticClient.initGame(1000l);
        int attempt = 0, maxAttempts = 500;
        while (!recordsFrom(gameState).contains(NovomaticEventType.EventGamblerStart) && attempt < maxAttempts) {
            attempt++;
            gameState = novomaticClient.sendCommand(gameState.getInternalState(), NovomaticCommand.Spin);
        }
        checkAttempts(attempt, maxAttempts);
        assertThat(recordsFrom(gameState), hasItem((NovomaticEvent) NovomaticEventType.EventGamblerStart));
        attempt = 0;
        while (!recordsFrom(gameState).contains(NovomaticEventType.EventGameStart) && attempt < maxAttempts) {
            gameState = novomaticClient.sendCommand(gameState.getInternalState(), NovomaticCommand.GambleOnRed);
        }
        assertThat(recordsFrom(gameState), hasItem((NovomaticEvent) NovomaticEventType.EventGameStart));
    }

    @Test
//    @Ignore("Should really use CheatClient instead.")
    public void shouldCollectInsteadOfGamble() throws NovomaticError {
        NovomaticGameState gameState = novomaticClient.initGame(1000l);
        int attempt = 0, maxAttempts = 500;
        while (!recordsFrom(gameState).contains(NovomaticEventType.EventGamblerStart) && attempt < maxAttempts) {
            attempt++;
            gameState = novomaticClient.sendCommand(gameState.getInternalState(), NovomaticCommand.Spin);
        }
        checkAttempts(attempt, maxAttempts);
        assertThat(recordsFrom(gameState), hasItem((NovomaticEvent) NovomaticEventType.EventGamblerStart));
        attempt = 0;
        while (!recordsFrom(gameState).contains(NovomaticEventType.EventGameStart) && attempt < maxAttempts) {
            gameState = novomaticClient.sendCommand(gameState.getInternalState(), NovomaticCommand.CollectGamble);
        }
        assertThat(recordsFrom(gameState), hasItem((NovomaticEvent) NovomaticEventType.EventGameStart));
    }


    private void checkAttempts(int attempt, int maxAttempts) {
        if (attempt == maxAttempts) {
            fail("Could not get to desired state after " + maxAttempts + " attempts.");
        }
    }

    private List<NovomaticEvent> recordsFrom(NovomaticGameState gameState) {
        List<NovomaticEvent> novomaticEvents = new LinkedList<NovomaticEvent>();
        for (NovomaticEvent novomaticEvent : gameState.getEvents()) {
            novomaticEvents.add(novomaticEvent);
        }
        return novomaticEvents;
    }
}
