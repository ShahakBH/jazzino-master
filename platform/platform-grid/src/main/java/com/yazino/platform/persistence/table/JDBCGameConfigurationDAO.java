package com.yazino.platform.persistence.table;

import com.yazino.platform.table.GameConfiguration;
import com.yazino.platform.table.GameConfigurationProperty;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class JDBCGameConfigurationDAO {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCGameConfigurationDAO.class);
    private static final String SELECT_ALL_GAME_CONFIGURATIONS = "SELECT * FROM GAME_CONFIGURATION WHERE ENABLED=1";
    private static final String SELECT_PROPERTIES_BY_GAME_ID =
            "SELECT GAME_PROPERTY_ID, GAME_ID, PROPERTY_NAME, PROPERTY_VALUE "
                    + "FROM GAME_CONFIGURATION_PROPERTY "
                    + "WHERE GAME_ID=?";
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JDBCGameConfigurationDAO(@Qualifier("jdbcTemplate") final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Set<GameConfiguration> retrieveAll() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading all from GAME_CONFIGURATION table");
        }
        Set<GameConfiguration> gameConfigurations = new HashSet<GameConfiguration>(
                jdbcTemplate.query(SELECT_ALL_GAME_CONFIGURATIONS, new RowMapper<GameConfiguration>() {
                    @Override
                    public GameConfiguration mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        final String gameId = rs.getString("GAME_ID");
                        final String shortName = rs.getString("SHORT_NAME");
                        final String displayName = rs.getString("DISPLAY_NAME");
                        final String allAliases = rs.getString("ALIASES");
                        final Set<String> aliases = new HashSet<String>();
                        if (!StringUtils.isBlank(allAliases)) {
                            aliases.addAll(Arrays.asList(allAliases.split(",")));
                        }
                        final int order = rs.getInt("ORD");
                        return new GameConfiguration(gameId, shortName, displayName, aliases, order);
                    }
                }));
        final Set<GameConfiguration> result = new HashSet<GameConfiguration>();
        for (GameConfiguration gameConfiguration : gameConfigurations) {
            result.add(gameConfiguration.withProperties(readPropertiesForGameId(gameConfiguration.getGameId())));
        }
        return result;
    }

    private Collection<GameConfigurationProperty> readPropertiesForGameId(final String gameId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading all properties from GAME_CONFIGURATION_PROPERTY");
        }
        return new ArrayList<GameConfigurationProperty>(jdbcTemplate.query(SELECT_PROPERTIES_BY_GAME_ID,
                new RowMapper<GameConfigurationProperty>() {
                    @Override
                    public GameConfigurationProperty mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        final BigDecimal propertyId = rs.getBigDecimal("GAME_PROPERTY_ID");
                        final String gameId = rs.getString("GAME_ID");
                        final String propertyName = rs.getString("PROPERTY_NAME");
                        final String propertyValue = rs.getString("PROPERTY_VALUE");
                        return new GameConfigurationProperty(propertyId, gameId, propertyName, propertyValue);
                    }
                }, gameId));
    }
}
