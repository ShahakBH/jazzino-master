package com.yazino.platform.repository.session;

import com.yazino.platform.grid.Routing;
import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.model.session.InboxMessagePersistenceRequest;
import com.yazino.platform.model.session.InboxMessageReceived;
import com.yazino.platform.persistence.session.InboxMessageDAO;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

@Repository
public class GigaspaceInboxMessageRepository implements InboxMessageRepository {

    private final GigaSpace localGigaSpace;
    private final GigaSpace globalGigaSpace;
    private final InboxMessageDAO inboxMessageDAO;
    private final Routing routing;

    @Autowired
    public GigaspaceInboxMessageRepository(@Qualifier("gigaSpace") final GigaSpace localGigaSpace,
                                           @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace,
                                           final Routing routing,
                                           final InboxMessageDAO inboxMessageDAO) {
        notNull(localGigaSpace, "localGigaSpace is null");
        notNull(globalGigaSpace, "globalGigaSpace is null");
        notNull(routing, "routing is null");
        notNull(inboxMessageDAO, "inboxMessageDAO is null");

        this.localGigaSpace = localGigaSpace;
        this.globalGigaSpace = globalGigaSpace;
        this.routing = routing;
        this.inboxMessageDAO = inboxMessageDAO;
    }

    @Override
    public void send(final InboxMessage message) {
        notNull(message, "message may not be null");

        spaceFor(message.getPlayerId()).writeMultiple(new Object[]{new InboxMessagePersistenceRequest(message), new InboxMessageReceived(message)});
    }

    @Override
    public void save(final InboxMessage message) {
        notNull(message, "message is null");

        spaceFor(message.getPlayerId()).write(new InboxMessagePersistenceRequest(message));
    }

    @Override
    public List<InboxMessage> findUnreadMessages(final BigDecimal playerId) {
        notNull(playerId, "playerId is null");

        return inboxMessageDAO.findUnreadMessages(playerId);
    }

    @Override
    public void messageReceived(final InboxMessage message) {
        notNull(message, "message is null");

        spaceFor(message.getPlayerId()).write(new InboxMessageReceived(message));
    }

    private GigaSpace spaceFor(final BigDecimal playerId) {
        if (routing.isRoutedToCurrentPartition(playerId)) {
            return localGigaSpace;
        }
        return globalGigaSpace;
    }
}
