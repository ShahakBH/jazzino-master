package com.yazino.novomatic;

import com.yazino.novomatic.cgs.NovomaticClient;
import com.yazino.novomatic.cgs.NovomaticCommand;
import com.yazino.novomatic.cgs.NovomaticError;
import com.yazino.novomatic.cgs.NovomaticGameState;
import com.yazino.novomatic.cgs.message.NovomaticGameDefinition;
import com.yazino.novomatic.cgs.msgpack.MessagePackMapper;
import com.yazino.novomatic.cgs.transport.ClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class NovomaticFakeClient extends NovomaticClient {

    private static final Logger LOG = LoggerFactory.getLogger(NovomaticFakeClient.class);

    private final FakeNovomaticStateSerialization converter = new FakeNovomaticStateSerialization();

    public NovomaticFakeClient() {
        super(new ClientTransport(new DummyPool()), new MessagePackMapper());
    }

    @Override
    public List<NovomaticGameDefinition> getAvailableGames() throws NovomaticError {
        return Arrays.asList(new NovomaticGameDefinition(90101l, "Just Jewels"));
    }

    @Override
    public NovomaticGameState initGame(long initialBalance) throws NovomaticError {
        LOG.debug("Initializing fake game!");
        final FakeNovomaticState state = FakeEngineRulesForStandardGame.initialState(initialBalance);
        return new NovomaticGameState(converter.toBytes(state), state.getEvents());
    }

    @Override
    public NovomaticGameState sendCommand(byte[] internalState, NovomaticCommand command) throws NovomaticError {
        LOG.debug("Processing command {}", command);
        final FakeNovomaticState currentState = converter.fromBytes(internalState);
        LOG.debug("Previous game= {}", currentState);
        final FakeNovomaticState stateAfterCommand = currentState.resolveEngine().process(currentState, command);
        LOG.debug("Next game= {}", stateAfterCommand);
        return new NovomaticGameState(converter.toBytes(stateAfterCommand), stateAfterCommand.getEvents());
    }
}
