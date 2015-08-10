package com.yazino.host.table.game;

import com.yazino.platform.model.table.Client;
import org.springframework.stereotype.Component;
import com.yazino.platform.repository.table.ClientRepository;

@Component
public class StandaloneClientRepository implements ClientRepository {
    @Override
    public Client findById(final String clientId) {
        return null;
    }

    @Override
    public Client[] findAll(final String gameType) {
        return new Client[0];
    }
}
