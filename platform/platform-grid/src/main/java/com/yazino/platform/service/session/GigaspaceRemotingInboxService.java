package com.yazino.platform.service.session;

import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.repository.session.InboxMessageRepository;
import com.yazino.platform.session.InboxService;
import org.openspaces.remoting.RemotingService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingInboxService implements InboxService {
    private final InboxMessageRepository inboxMessageRepository;

    @Autowired
    public GigaspaceRemotingInboxService(final InboxMessageRepository inboxMessageRepository) {
        notNull(inboxMessageRepository, "inboxMessageRepository is null");

        this.inboxMessageRepository = inboxMessageRepository;
    }

    @Override
    public void checkNewMessages(final BigDecimal playerId) {
        notNull(playerId, "playerId is null");

        final List<InboxMessage> messages = inboxMessageRepository.findUnreadMessages(playerId);
        for (final InboxMessage message : messages) {
            inboxMessageRepository.messageReceived(message);
        }
    }

}
