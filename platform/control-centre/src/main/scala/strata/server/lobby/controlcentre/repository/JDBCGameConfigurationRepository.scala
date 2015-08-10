package strata.server.lobby.controlcentre.repository

import mapper.{GameConfigurationPropertyMapper, GameConfigurationMapper}
import scala.collection.JavaConversions._
import java.math.BigDecimal
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.lang3.Validate.notNull
import org.springframework.stereotype.Repository
import org.apache.commons.logging.LogFactory
import org.springframework.jdbc.core.{PreparedStatementSetter, JdbcTemplate}
import java.sql.PreparedStatement
import com.yazino.platform.table.{GameConfigurationProperty, GameConfiguration}
import java.util


object JDBCGameConfigurationRepository {
    private val SQL_SELECT_GAME_BY_ID = """
            SELECT GAME_ID, SHORT_NAME, DISPLAY_NAME, ALIASES, ORD
            FROM GAME_CONFIGURATION
            WHERE GAME_ID = ?"""
    private val SQL_SELECT_PROPERTIES_BY_GAME_ID = """
            SELECT GAME_PROPERTY_ID, GAME_ID, PROPERTY_NAME, PROPERTY_VALUE
            FROM GAME_CONFIGURATION_PROPERTY
            WHERE GAME_ID=?"""
    private val SQL_SELECT_ALL_GAMES = """
            SELECT * FROM GAME_CONFIGURATION
            WHERE ENABLED=1
            ORDER BY GAME_ID"""
    private val SQL_MERGE_GAME_CONFIGURATION =
        """INSERT INTO GAME_CONFIGURATION (GAME_ID,SHORT_NAME,DISPLAY_NAME,ALIASES,ORD) VALUES (?,?,?,?,?)
        ON DUPLICATE KEY UPDATE
          SHORT_NAME = VALUES(SHORT_NAME),
          DISPLAY_NAME = VALUES(DISPLAY_NAME),
          ALIASES = VALUES(ALIASES),
          ORD = VALUES(ORD)
        """
    private val SQL_DELETE_ALL_GAME_CONFIGURATION_PROPERTIES =
        """DELETE FROM GAME_CONFIGURATION_PROPERTY WHERE GAME_ID=?"""
    private val SQL_DELETE_GAME_CONFIGURATION =
        """DELETE FROM GAME_CONFIGURATION WHERE GAME_ID=?"""
    private val SQL_INSERT_GAME_CONFIGURATION_PROPERTY =
        """INSERT INTO GAME_CONFIGURATION_PROPERTY (GAME_ID,PROPERTY_NAME,PROPERTY_VALUE) VALUES (?,?,?)"""
    private val SQL_UPDATE_GAME_CONFIGURATION_PROPERTY =
        """UPDATE GAME_CONFIGURATION_PROPERTY SET GAME_ID=?,PROPERTY_NAME=?,PROPERTY_VALUE=? WHERE GAME_PROPERTY_ID=?"""
    private val SQL_DELETE_GAME_CONFIGURATION_PROPERTY =
        """DELETE FROM GAME_CONFIGURATION_PROPERTY WHERE GAME_ID=? AND GAME_PROPERTY_ID=?"""
}

@Repository
class JDBCGameConfigurationRepository @Autowired()(val jdbcTemplate: JdbcTemplate)
    extends GameConfigurationRepository with MySQLPaging[GameConfiguration] {

    private val LOG = LogFactory.getLog(classOf[JDBCGameConfigurationRepository])
    private val gameConfigurationMapper = new GameConfigurationMapper()
    private val gameConfigurationPropertyMapper = new GameConfigurationPropertyMapper()

    def findAllGames(): util.List[GameConfiguration] = {
        if (LOG.isDebugEnabled) {
            LOG.debug("Fetching all game configuration")
        }
        jdbcTemplate.query(JDBCGameConfigurationRepository.SQL_SELECT_ALL_GAMES, gameConfigurationMapper).toList
    }

    def findGameById(gameId: String): Option[GameConfiguration] = {
        notNull(gameId, "gameId may not be null")
        if (LOG.isDebugEnabled) {
            LOG.debug(String.format("Reading configuration for game with ID", gameId))
        }
        val gameConfigurations = jdbcTemplate.query(JDBCGameConfigurationRepository.SQL_SELECT_GAME_BY_ID, gameConfigurationMapper, gameId)
        if (gameConfigurations != null && !gameConfigurations.isEmpty) {
            Some(gameConfigurations.get(0).withProperties(propertiesFor(gameId)))
        } else {
            None
        }
    }

    def save(gameConfiguration: GameConfiguration): GameConfiguration = {
        notNull(gameConfiguration, "gameConfiguration may not be null")
        if (LOG.isDebugEnabled) {
            LOG.debug(String.format("Saving game configuration %s", gameConfiguration))
        }

        jdbcTemplate.update(JDBCGameConfigurationRepository.SQL_MERGE_GAME_CONFIGURATION,
            gameConfiguration.getGameId.toUpperCase,
            gameConfiguration.getShortName,
            gameConfiguration.getDisplayName,
            gameConfiguration.getAliases.mkString(","),
            Int.box(gameConfiguration.getOrder))
        saveProperties(gameConfiguration)
    }

    def delete(gameId: String) {
        notNull(gameId, "gameId may not be null")
        if (LOG.isDebugEnabled) {
            LOG.debug(String.format("Deleting GAME_CONFIGURAITON where GAME_ID %s", gameId))
        }
        jdbcTemplate.update(JDBCGameConfigurationRepository.SQL_DELETE_ALL_GAME_CONFIGURATION_PROPERTIES, new PreparedStatementSetter {
            def setValues(stmt: PreparedStatement) {
                stmt.setString(1, gameId)
            }
        })
        jdbcTemplate.update(JDBCGameConfigurationRepository.SQL_DELETE_GAME_CONFIGURATION, new PreparedStatementSetter {
            def setValues(stmt: PreparedStatement) {
                stmt.setString(1, gameId)
            }
        })
    }

    def deleteProperty(gameId: String, propertyId: BigDecimal) {
        notNull(propertyId, "gameVariationId may not be null")
        if (LOG.isDebugEnabled) {
            LOG.debug(String.format("Deleting property from GAME_CONFIGURAITON_PROPERTY where GAME_ID %s and GAME_PROPERTY_ID %s", gameId, propertyId))
        }
        jdbcTemplate.update(JDBCGameConfigurationRepository.SQL_DELETE_GAME_CONFIGURATION_PROPERTY, new PreparedStatementSetter {
            def setValues(stmt: PreparedStatement) {
                stmt.setString(1, gameId)
                stmt.setBigDecimal(2, propertyId)
            }
        })
    }

    private def propertiesFor(gameId: String) = new util.HashSet[GameConfigurationProperty](jdbcTemplate.query(
        JDBCGameConfigurationRepository.SQL_SELECT_PROPERTIES_BY_GAME_ID, gameConfigurationPropertyMapper, gameId).toList)

    private def saveProperties(gameConfiguration: GameConfiguration): GameConfiguration = {
        LOG.debug("Saving properties for game configuration with ID '{}'. Properties are {}",
            gameConfiguration.getGameId, gameConfiguration.getProperties)
        val updatedProperties = new util.HashSet[GameConfigurationProperty](gameConfiguration.getProperties.map {
            property =>
                if (property.getPropertyId == null) {
                    insertProperties(gameConfiguration, property)
                } else {
                    updateProperties(gameConfiguration, property)
                }
        })
        gameConfiguration.withProperties(updatedProperties)
    }

    private def insertProperties(gameConfiguration: GameConfiguration, property: GameConfigurationProperty): GameConfigurationProperty = {
        notNull(property, "property may not be null")
        if (LOG.isDebugEnabled) {
            LOG.debug(String.format("Inserting property '%s' for game configuration with ID %s", property, gameConfiguration.getGameId))
        }
        val propertyId = jdbcTemplate.update(JDBCGameConfigurationRepository.SQL_INSERT_GAME_CONFIGURATION_PROPERTY, new PreparedStatementSetter {
            def setValues(stmt: PreparedStatement) {
                stmt.setString(1, gameConfiguration.getGameId)
                stmt.setString(2, property.getPropertyName)
                stmt.setString(3, property.getPropertyValue)
            }
        })
        if (LOG.isDebugEnabled) {
            LOG.debug(String.format("Property assigned an ID of %s", String.valueOf(propertyId)))
        }
        new GameConfigurationProperty(BigDecimal.valueOf(propertyId), property.getGameId, property.getPropertyName, property.getPropertyValue)
    }

    private def updateProperties(gameConfiguration: GameConfiguration, property: GameConfigurationProperty): GameConfigurationProperty = {
        if (LOG.isDebugEnabled) {
            LOG.debug(String.format("Updating property '%s' for game configuration with ID %s", property, gameConfiguration.getGameId))
        }
        jdbcTemplate.update(JDBCGameConfigurationRepository.SQL_UPDATE_GAME_CONFIGURATION_PROPERTY, new PreparedStatementSetter {
            def setValues(stmt: PreparedStatement) {
                stmt.setString(1, gameConfiguration.getGameId)
                stmt.setString(2, property.getPropertyName)
                stmt.setString(3, property.getPropertyValue)
                stmt.setBigDecimal(4, property.getPropertyId)
            }
        })
        property
    }
}
