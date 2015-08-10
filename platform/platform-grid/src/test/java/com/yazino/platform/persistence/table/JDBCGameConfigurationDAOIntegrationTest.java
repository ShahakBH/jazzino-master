package com.yazino.platform.persistence.table;

import com.yazino.platform.table.GameConfiguration;
import com.yazino.platform.table.GameConfigurationProperty;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true, transactionManager = "jdbcTransactionManager")
@Transactional
public class JDBCGameConfigurationDAOIntegrationTest {
    private static final String SQL_INSERT_GAME_CONFIGURATION =
            "INSERT INTO GAME_CONFIGURATION (GAME_ID,SHORT_NAME,DISPLAY_NAME,ALIASES,ORD) VALUES (?,?,?,?,?)";
    private static final String SQL_INSERT_GAME_CONFIGURATION_PROPERTY =
            "INSERT INTO GAME_CONFIGURATION_PROPERTY (GAME_PROPERTY_ID,GAME_ID,PROPERTY_NAME,PROPERTY_VALUE) VALUES (?,?,?,?)";

    private static final String GAME_ID = "GAME_ID";
    private static final BigDecimal GAME_CONFIGURATION_PROPERTY_ONE = BigDecimal.valueOf(-1);

    private static final GameConfigurationProperty GCP = new GameConfigurationProperty(GAME_CONFIGURATION_PROPERTY_ONE, GAME_ID, "aPropertyName", "aPropertyValue");
    private static final GameConfiguration GC = new GameConfiguration(GAME_ID, "shortName", "displayName", new HashSet<String>(Arrays.asList("aGame", "myGame")), 0).withProperties(Arrays.asList(GCP));

    @Autowired(required = true)
    @Qualifier("jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = true)
    private JDBCGameConfigurationDAO underTest;

    @Before
    public void setup() {
        jdbcTemplate.update(SQL_INSERT_GAME_CONFIGURATION, new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement stmt) throws SQLException {
                stmt.setString(1, GC.getGameId());
                stmt.setString(2, GC.getShortName());
                stmt.setString(3, GC.getDisplayName());
                stmt.setString(4, StringUtils.join(GC.getAliases(), ","));
                stmt.setInt(5, GC.getOrder());
            }
        });
        jdbcTemplate.update(SQL_INSERT_GAME_CONFIGURATION_PROPERTY, new PreparedStatementSetter() {
            @Override
            public void setValues(final PreparedStatement stmt) throws SQLException {
                stmt.setBigDecimal(1, GCP.getPropertyId());
                stmt.setString(2, GCP.getGameId());
                stmt.setString(3, GCP.getPropertyName());
                stmt.setString(4, GCP.getPropertyValue());
            }
        });
    }

    @Test
    public void shouldReadGameConfigurations() throws Exception {
        Collection<GameConfiguration> gameConfigurations = underTest.retrieveAll();

        assertTrue(gameConfigurations.contains(GC));
    }
}
