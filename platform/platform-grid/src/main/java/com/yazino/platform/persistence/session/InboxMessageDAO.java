package com.yazino.platform.persistence.session;


import com.yazino.platform.model.session.InboxMessage;

import java.math.BigDecimal;
import java.util.List;

public interface InboxMessageDAO {
    void save(InboxMessage message);

    List<InboxMessage> findUnreadMessages(BigDecimal playerId);
}
