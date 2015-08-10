package com.yazino.platform.repository.table;

import com.yazino.platform.model.table.Client;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaSpaceClientRepositoryIntegrationTest {
    @Autowired
    ClientRepository clientRepository;
    @Autowired
    GigaSpace referenceSpace;

    @Test
    public void clients_are_available_in_space() {
        referenceSpace.write(new Client("TestClient"));
        Assert.assertNotNull(clientRepository.findById("TestClient"));
    }
}
