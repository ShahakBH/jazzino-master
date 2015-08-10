package com.yazino.platform.processor.table;

import com.yazino.platform.audit.AuditService;
import com.yazino.platform.audit.message.CommandAudit;
import com.yazino.platform.model.table.CommandAuditWrapper;
import com.yazino.platform.repository.community.PlayerRepository;
import org.apache.commons.lang3.ArrayUtils;
import org.joda.time.DateTime;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;
import org.openspaces.events.polling.ReceiveHandler;
import org.openspaces.events.polling.receive.MultiTakeReceiveOperationHandler;
import org.openspaces.events.polling.receive.ReceiveOperationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.Validate.notNull;

@EventDriven
@Polling(gigaSpace = "gigaSpace", passArrayAsIs = true)
public class CommandAuditProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(CommandAuditProcessor.class);

    private static final CommandAuditWrapper TEMPLATE = new CommandAuditWrapper();
    private static final int BATCH_SIZE = 100;
    public static final String BET = "Bet";

    private final AuditService auditService;
    private final PlayerRepository playerRepository;

    @Autowired
    public CommandAuditProcessor(final AuditService auditService,
                                 final PlayerRepository playerRepository) {
        notNull(auditService, "auditService may not be null");
        notNull(playerRepository, "playerRepository may not be null");

        this.auditService = auditService;
        this.playerRepository = playerRepository;
    }

    @EventTemplate
    public CommandAuditWrapper receivedTemplate() {
        return TEMPLATE;
    }

    @ReceiveHandler
    public ReceiveOperationHandler receiveHandler() {
        final MultiTakeReceiveOperationHandler receiveHandler = new MultiTakeReceiveOperationHandler();
        receiveHandler.setMaxEntries(BATCH_SIZE);
        return receiveHandler;
    }

    @SpaceDataEvent
    public void store(final CommandAuditWrapper[] auditRequests) {
        if (auditRequests == null || auditRequests.length == 0) {
            return;
        }

        try {
            final List<CommandAudit> commandsToBeAudited = new ArrayList<>(auditRequests.length);
            final Map<BigDecimal, DateTime> lastPlayedMap = new HashMap<>();

            for (CommandAuditWrapper auditRequest : auditRequests) {

                if (BET.equalsIgnoreCase(auditRequest.getCommand().getType())) {
                    lastPlayedMap.put(auditRequest.getCommand().getPlayer().getId(), new DateTime(auditRequest.getAuditContext().getAuditDate()));
                }
                commandsToBeAudited.add(toWorkerFormat(auditRequest));
            }

            auditService.auditCommands(commandsToBeAudited);
            PlayerLastPlayedUpdateRequest[] entries = transformLastPlayedMapToArray(lastPlayedMap);
            if (entries.length > 0) {
                playerRepository.requestLastPlayedUpdates(entries);
            }

        } catch (Throwable t) {
            LOG.error("error storing audit commands: " + ArrayUtils.toString(auditRequests), t);
        }
    }

    private PlayerLastPlayedUpdateRequest[] transformLastPlayedMapToArray(final Map<BigDecimal, DateTime> lastPlayed) {
        final PlayerLastPlayedUpdateRequest[] playerLastPlayed = new PlayerLastPlayedUpdateRequest[lastPlayed.size()];
        int index = 0;
        for (BigDecimal playerId : lastPlayed.keySet()) {
            playerLastPlayed[index] = new PlayerLastPlayedUpdateRequest(playerId, lastPlayed.get(playerId));
            ++index;
        }
        return playerLastPlayed;
    }


    private CommandAudit toWorkerFormat(final CommandAuditWrapper auditRequest) {
        return new CommandAudit(
                auditRequest.getAuditContext().getLabel(),
                auditRequest.getAuditContext().getHostname(),
                auditRequest.getAuditContext().getAuditDate(),
                auditRequest.getCommand().getTableId(),
                auditRequest.getCommand().getGameId(),
                auditRequest.getCommand().getType(),
                auditRequest.getCommand().getArgs(),
                auditRequest.getCommand().getPlayer().getId(),
                auditRequest.getCommand().getUuid());
    }
}
