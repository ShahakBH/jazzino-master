package com.yazino.platform.persistence.table;

import com.yazino.platform.table.GameVariation;
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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
public class JDBCGameVariationDAOIntegrationTest {

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = true)
    private JDBCGameVariationDAO jdbcGameVariationDAO;

    @Before
    @After
    public void cleanUp() {
        jdbcTemplate.execute("DELETE FROM GAME_VARIATION_TEMPLATE_PROPERTY WHERE GAME_VARIATION_TEMPLATE_ID < 0");
    }

    @Test
    @Transactional
    public void templatePropertiesAreReadCorrectlyFromTheDatabase() {
        writeToDB(aVariation(1, 3));

        final Collection<GameVariation> gameTemplates = jdbcGameVariationDAO.retrieveAll();

        assertThat(gameTemplates, hasItem(aVariation(1, 3)));
    }

    @Test
    @Transactional
    public void multipleTemplatesAreReadCorrectlyFromTheDatabase() {
        writeToDB(aVariation(1, 4));
        writeToDB(aVariation(2, 3));

        final Collection<GameVariation> gameTemplates = jdbcGameVariationDAO.retrieveAll();

        assertThat(gameTemplates, hasItem(aVariation(1, 4)));
        assertThat(gameTemplates, hasItem(aVariation(2, 3)));
    }

    private void writeToDB(final GameVariation template) {
        jdbcTemplate.update("INSERT INTO GAME_VARIATION_TEMPLATE (GAME_VARIATION_TEMPLATE_ID,NAME,GAME_TYPE) VALUES (?,?,?)",
                template.getId(), template.getName(), template.getGameType());

        for (String propertyName : template.getProperties().keySet()) {
            createProperty(template.getId(), propertyName, template.getProperties().get(propertyName));
        }
    }

    private GameVariation aVariation(final int id,
                                     final int numberOfProperties) {
        final BigDecimal templateId;
        if (id < 0) {
            templateId = new BigDecimal(id);
        } else {
            templateId = new BigDecimal(0 - id);
        }

        final HashMap<String, String> properties = new HashMap<String, String>();
        for (int i = 0; i < numberOfProperties; ++i) {
            properties.put("property" + i, "value" + i);
        }
        return new GameVariation(templateId, id % 2 == 0 ? "BLACKJACK" : "ROULETTE", "template-" + id, properties);
    }

    private void createProperty(final BigDecimal id,
                                final String name,
                                final String value) {
        jdbcTemplate.update("INSERT INTO GAME_VARIATION_TEMPLATE_PROPERTY (GAME_VARIATION_TEMPLATE_ID,NAME,VALUE) VALUES (?,?,?)",
                id, name, value);
    }

}
