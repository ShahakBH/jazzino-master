package com.yazino.web.service;

import com.yazino.web.domain.LobbyInformation;

public interface LobbyInformationService {
    LobbyInformation getLobbyInformation(final String gameType);
}
