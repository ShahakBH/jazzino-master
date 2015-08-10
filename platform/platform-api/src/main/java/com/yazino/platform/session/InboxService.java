package com.yazino.platform.session;

import org.openspaces.remoting.Routing;

import java.math.BigDecimal;

public interface InboxService {

    void checkNewMessages(@Routing BigDecimal playerId);

}
