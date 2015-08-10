package com.yazino.platform.processor.external;

import com.yazino.game.api.ExternalCallResult;
import com.yazino.novomatic.cgs.NovomaticClient;
import com.yazino.novomatic.cgs.NovomaticCommand;
import com.yazino.novomatic.cgs.NovomaticGameState;
import com.yazino.platform.gamehost.external.NovomaticRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class NovomaticRequestExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(NovomaticRequestExecutor.class);
    private final NovomaticClient novomaticClient;

    @Autowired
    public NovomaticRequestExecutor(final NovomaticClient novomaticClient) {
        this.novomaticClient = novomaticClient;
    }

    public ExternalCallResult execute(final NovomaticRequest request) {
        ExternalCallResult externalCallResult;
        try {

            if (NovomaticCommand.valueOf(request.getCallName()) == NovomaticCommand.Join) {
                LOG.debug("Sending init to Novomatic game...");
                final long balance = ((BigDecimal) request.getCallContext()).longValue();
                NovomaticGameState initialState = novomaticClient.initGame(balance);
                externalCallResult = new ExternalCallResult(request.getRequestId(), request.getPlayerId(), true, request.getCallName(), initialState);
            } else {
                LOG.debug("Sending command to Novomatic game...");
                final byte[] currentState = (byte[]) request.getCallContext();
                final NovomaticGameState nextState = novomaticClient.sendCommand(currentState, NovomaticCommand.valueOf(request.getCallName()));
                externalCallResult = new ExternalCallResult(request.getRequestId(), request.getPlayerId(), true, request.getCallName(), nextState);
            }

        } catch (Exception e) {
            LOG.error("Error processing novomatic request", e);
            externalCallResult = new ExternalCallResult(request.getRequestId(), request.getPlayerId(), false, request.getCallName(), null);
        }
        return externalCallResult;
    }
}
