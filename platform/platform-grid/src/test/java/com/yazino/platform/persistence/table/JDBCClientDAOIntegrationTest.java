package com.yazino.platform.persistence.table;

import com.yazino.platform.model.table.Client;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.hasItem;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class JDBCClientDAOIntegrationTest {
    private static final String CLIENT_ID = "UnitTest Client";

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = true)
    private JDBCClientDAO jdbcClientDAO;

    @Before
    @After
    public void cleanUp() {
        jdbcTemplate.update("DELETE FROM CLIENT_PROPERTY WHERE CLIENT_ID LIKE ?", CLIENT_ID + "%");
        jdbcTemplate.update("DELETE FROM CLIENT WHERE CLIENT_ID LIKE ?", CLIENT_ID + "%");
    }

    @Test
    @Transactional
    public void aSingleClientIsReadCorrectlyFromTable() {
        final Client expectedClient = createClientInDatabase(CLIENT_ID, "BLACKJACK");

        final Collection<Client> clientsInDatabase = jdbcClientDAO.findAll();

        assertThat(clientsInDatabase, hasItem(expectedClient));
    }

    @Test
    @Transactional
    public void multipleClientsAreReadCorrectlyFromDatabase() {
        final Client expectedClient1 = createClientInDatabase(CLIENT_ID + 1, "BLACKJACK");
        final Client expectedClient2 = createClientInDatabase(CLIENT_ID + 2, "TEXAS_HOLDEM");
        final Client expectedClient3 = createClientInDatabase(CLIENT_ID + 3, "ROULETTE");

        final Collection<Client> clientsInDatabase = jdbcClientDAO.findAll();

        assertThat(clientsInDatabase, hasItem(expectedClient1));
        assertThat(clientsInDatabase, hasItem(expectedClient2));
        assertThat(clientsInDatabase, hasItem(expectedClient3));
    }

    private Client createClientInDatabase(final String clientId, final String gameType) {
        jdbcTemplate.update("INSERT INTO CLIENT (CLIENT_ID,CLIENT_FILE,GAME_TYPE,NUMBER_OF_SEATS) VALUES (?,?,?,?)",
                clientId, clientId + ".swf", gameType, 5);

        final Map<String, String> properties = new HashMap<String, String>();
        properties.put("property1", "value1");
        properties.put("property2", "value2");
        properties.put("property3", "value3");

        for (final String propertyName : properties.keySet()) {
            jdbcTemplate.update("INSERT INTO CLIENT_PROPERTY (CLIENT_ID,PROPERTY_NAME,PROPERTY_VALUE) VALUES (?,?,?)",
                    clientId, propertyName, properties.get(propertyName));
        }

        return new Client(clientId, 5, clientId + ".swf", gameType, properties);
    }
}
