package com.yazino.platform.persistence.table;


import com.yazino.platform.model.table.Client;

import java.util.Collection;

public interface ClientDAO {


    Collection<Client> findAll();

}
