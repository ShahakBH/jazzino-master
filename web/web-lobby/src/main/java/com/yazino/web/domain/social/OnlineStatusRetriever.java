package com.yazino.web.domain.social;

import com.yazino.web.data.SessionStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Qualifier("playerInformationRetriever")
public class OnlineStatusRetriever implements PlayerInformationRetriever {
    private final SessionStatusRepository repository;

    @Autowired
    public OnlineStatusRetriever(final SessionStatusRepository repository) {
        this.repository = repository;
    }

    @Override
    public PlayerInformationType getType() {
        return PlayerInformationType.ONLINE;
    }

    @Override
    public Object retrieveInformation(final BigDecimal playerId, final String gameType) {
        return repository.getStatus(playerId) != null;
    }


}
