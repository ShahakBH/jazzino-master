package com.yazino.novomatic.cgs;

import com.yazino.novomatic.cgs.message.NovomaticGameDefinition;
import com.yazino.novomatic.cgs.message.RequestGameInit;
import com.yazino.novomatic.cgs.message.RequestGameList;
import com.yazino.novomatic.cgs.message.UserInput;
import com.yazino.novomatic.cgs.msgpack.MessagePackMapper;
import com.yazino.novomatic.cgs.message.conversion.ErrorBuilder;
import com.yazino.novomatic.cgs.message.conversion.GameListBuilder;
import com.yazino.novomatic.cgs.message.conversion.GameStateBuilder;
import com.yazino.novomatic.cgs.transport.ClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class NovomaticClient {

    private static final Logger LOG = LoggerFactory.getLogger(NovomaticClient.class);
    private final ClientTransport transport;
    private final MessagePackMapper mapper;

    private final GameListBuilder gameListBuilder = new GameListBuilder();
    private final GameStateBuilder gameStateBuilder = new GameStateBuilder();
    private final ErrorBuilder errorBuilder = new ErrorBuilder();

    @Autowired
    public NovomaticClient(ClientTransport transport, MessagePackMapper mapper) {
        this.transport = transport;
        this.mapper = mapper;
    }

    public List<NovomaticGameDefinition> getAvailableGames() throws NovomaticError {
        try {
            final Map responseAsMap = executeRequest(new RequestGameList().toMap());
            return gameListBuilder.buildFromMap(responseAsMap);
        } catch (IOException e) {
            LOG.error("Error listing available games", e);
            throw new RuntimeException(e);
        }
    }

    public NovomaticGameState initGame(long initialBalance) throws NovomaticError {
        LOG.debug("Initializing game {} with balance {}", NovomaticGameDefinition.JUST_JEWELS, initialBalance);
        try {
            final Map request = new RequestGameInit(NovomaticGameDefinition.JUST_JEWELS.getId(), initialBalance).toMap();
            final Map responseAsMap = executeRequest(request);
            return gameStateBuilder.buildFromMap(responseAsMap);
        } catch (IOException e) {
            LOG.error("Error listing available games", e);
            throw new RuntimeException(e);
        }
    }

    public NovomaticGameState sendCommand(byte[] internalState, NovomaticCommand command) throws NovomaticError {
        LOG.debug("Sending command {} (game state={})", command, internalState);
        final String buttonCode = command.getNovomaticButtonCode();
        final Map request = new UserInput(internalState, buttonCode).toMap();
        return sendUserInput(request);
    }

    protected NovomaticGameState sendUserInput(Map request) throws NovomaticError {
        try {
            final Map responseAsMap = executeRequest(request);
            return gameStateBuilder.buildFromMap(responseAsMap);
        } catch (IOException e) {
            LOG.error("Error listing available games", e);
            throw new RuntimeException(e);
        }
    }

    private Map executeRequest(Map request) throws IOException, NovomaticError {
        LOG.debug("Sending  {}", request);
        final byte[] payload = mapper.write(request);
        final byte[] responseRaw = transport.sendRequest(payload);
        final Map responseAsMap = mapper.read(responseRaw, Map.class, "gmstate");
        LOG.debug("Received {}", responseAsMap);
        if (NovomaticError.TYPE.equals(responseAsMap.get("type"))) {
            throw errorBuilder.buildFromMap(responseAsMap);
        }
        return responseAsMap;
    }
}
