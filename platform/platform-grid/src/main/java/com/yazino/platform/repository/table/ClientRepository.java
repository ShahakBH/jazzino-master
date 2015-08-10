package com.yazino.platform.repository.table;

import com.yazino.platform.model.table.Client;

public interface ClientRepository {
    Client findById(String clientId);

    Client[] findAll(String gameType);
}
