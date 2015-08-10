package com.yazino.host.chat;

import com.yazino.platform.chat.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yazino.platform.repository.chat.ChatRepository;
import com.yazino.platform.service.chat.GigaspaceRemotingChatService;
import com.yazino.platform.repository.community.PlayerRepository;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
public class StandaloneChatService implements ChatService {
    private static final Logger LOG = LoggerFactory.getLogger(StandaloneChatService.class);

    private ChatService delegate;

    @Autowired
    public StandaloneChatService(final ChatRepository chatRepository,
                                 final PlayerRepository playerRepository) {
//        Notice: even though the delegate is named Gigaspace, it does not use GigaSpace directly, so it's
//        safe to use this implementation here.
        this.delegate = new GigaspaceRemotingChatService(chatRepository, playerRepository);
    }

    @Override
    public void processCommand(final BigDecimal playerId, final String... chatCommand) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Processing commands for player %s: %s", playerId, Arrays.toString(chatCommand)));
        }
        delegate.processCommand(playerId, chatCommand);
    }

    @Override
    public void asyncProcessCommand(final BigDecimal playerId, final String... chatCommand) {
        processCommand(playerId, chatCommand);
    }

}
