package com.yazino.platform.test;


import com.yazino.platform.model.table.Client;
import com.yazino.platform.persistence.table.ClientDAO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class InMemoryClientDAO implements ClientDAO {

    private final List<Client> clients = new ArrayList<Client>();


    @Override
    public Collection<Client> findAll() {
        return clients;
    }

    public void setClients(final Collection<Client> clients) {
        if (clients != null) {
            this.clients.addAll(clients);
        }
    }
}
