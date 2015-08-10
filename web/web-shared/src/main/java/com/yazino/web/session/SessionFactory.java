package com.yazino.web.session;

import com.yazino.platform.Platform;
import com.yazino.platform.player.LoginResult;

import java.math.BigDecimal;
import java.util.Map;

public interface SessionFactory {

    LobbySessionCreationResponse registerNewSession(BigDecimal playerId,
                                                    PartnerSession partnerSession,
                                                    Platform platform,
                                                    LoginResult loginResult,
                                                    final Map<String, Object> clientContext);

}
