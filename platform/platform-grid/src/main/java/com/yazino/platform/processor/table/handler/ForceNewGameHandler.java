package com.yazino.platform.processor.table.handler;

import com.yazino.platform.gamehost.GameHost;
import com.yazino.platform.messaging.host.HostDocument;
import com.yazino.platform.model.table.ForceNewGameRequest;
import com.yazino.platform.model.table.Table;
import com.yazino.platform.model.table.TableRequestType;
import com.yazino.platform.repository.table.ClientRepository;
import com.yazino.platform.repository.table.GameVariationRepository;
import com.yazino.platform.table.GameVariation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.Validate.notNull;

@Service
@Qualifier("tableRequestHandler")
public class ForceNewGameHandler extends TablePersistingRequestHandler<ForceNewGameRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(ForceNewGameHandler.class);

    private ClientRepository clientRepository;
    private GameVariationRepository gameTemplateRepository;

    @Autowired
    public ForceNewGameHandler(final ClientRepository clientRepository,
                               final GameVariationRepository gameTemplateRepository) {
        notNull(clientRepository, "clientRepository may not be null");
        notNull(gameTemplateRepository, "gameTemplateRepository may not be null");

        this.clientRepository = clientRepository;
        this.gameTemplateRepository = gameTemplateRepository;
    }

    @Override
    public boolean accepts(final TableRequestType requestType) {
        return requestType == TableRequestType.FORCE_NEW_GAME;
    }

    protected List<HostDocument> execute(final ForceNewGameRequest forceNewGameRequest,
                                         final GameHost gameHost,
                                         final Table table) {
        LOG.debug("execute {}", forceNewGameRequest);
        table.setClientId(forceNewGameRequest.getClientId());
        table.setClient(clientRepository.findById(forceNewGameRequest.getClientId()));

        final GameVariation gameVariation = gameTemplateRepository.findById(
                forceNewGameRequest.getVariationTemplateId());
        if (gameVariation == null) {
            throw new IllegalStateException("No game template exists for ID "
                    + forceNewGameRequest.getVariationTemplateId());
        }

        table.setVariationProperties(new ConcurrentHashMap<String, String>(gameVariation.getProperties()));
        table.setTemplateId(forceNewGameRequest.getVariationTemplateId());

        return gameHost.forceNewGame(table, forceNewGameRequest.getPlayersInformation(),
                new ConcurrentHashMap<BigDecimal, BigDecimal>(forceNewGameRequest.getOverriddenAccountIds()));
    }
}
