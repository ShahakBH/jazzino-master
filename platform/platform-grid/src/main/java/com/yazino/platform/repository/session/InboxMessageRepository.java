package com.yazino.platform.repository.session;


import com.yazino.platform.model.session.InboxMessage;

import java.math.BigDecimal;
import java.util.List;

public interface InboxMessageRepository {
    void send(InboxMessage message);

    void save(InboxMessage message);

    List<InboxMessage> findUnreadMessages(BigDecimal playerId);

    void messageReceived(InboxMessage message);
}
