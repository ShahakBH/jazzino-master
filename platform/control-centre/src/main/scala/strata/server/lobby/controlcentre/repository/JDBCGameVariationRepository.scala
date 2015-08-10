package strata.server.lobby.controlcentre.repository

import mapper.{GameVariationRepository, GameVariationPropertyMapper, GameVariationMapper}
import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import org.apache.commons.lang3.Validate.notNull
import com.yazino.platform.model.PagedData
import strata.server.lobby.controlcentre.model.{GameVariationProperty, GameVariation}
import org.springframework.stereotype.Repository
import org.springframework.jdbc.core.{PreparedStatementCreator, JdbcTemplate}
import java.sql.{PreparedStatement, Statement, Connection}
import org.springframework.transaction.annotation.Transactional
import JDBCGameVariationRepository._

object JDBCGameVariationRepository {
    private val SQL_SELECT_BY_ID = """
            SELECT GAME_VARIATION_TEMPLATE_ID,GAME_TYPE,NAME
              FROM GAME_VARIATION_TEMPLATE
             WHERE GAME_VARIATION_TEMPLATE_ID=?"""
    private val SQL_SELECT_PROPERTIES = """
            SELECT GAME_VARIATION_TEMPLATE_PROPERTY_ID,NAME,VALUE
              FROM GAME_VARIATION_TEMPLATE_PROPERTY
             WHERE GAME_VARIATION_TEMPLATE_ID=?"""
    private val SQL_SELECT_ALL = """
            SELECT GAME_VARIATION_TEMPLATE_ID,GAME_TYPE,NAME
              FROM GAME_VARIATION_TEMPLATE
             ORDER BY GAME_VARIATION_TEMPLATE_ID ASC"""
    private val SQL_LIST_FOR_GAMETYPE =
        "SELECT GAME_VARIATION_TEMPLATE_ID,NAME FROM GAME_VARIATION_TEMPLATE WHERE GAME_TYPE=? ORDER BY NAME ASC"
    private val SQL_INSERT = "INSERT INTO GAME_VARIATION_TEMPLATE (GAME_TYPE,NAME) VALUES (?,?)"
    private val SQL_INSERT_PROPERTY = """
            INSERT INTO GAME_VARIATION_TEMPLATE_PROPERTY (GAME_VARIATION_TEMPLATE_ID,NAME,VALUE)
            VALUES (?,?,?)"""
    private val SQL_UPDATE = "UPDATE GAME_VARIATION_TEMPLATE SET NAME=? WHERE GAME_VARIATION_TEMPLATE_ID=?"
    private val SQL_UPDATE_PROPERTY = """
            UPDATE GAME_VARIATION_TEMPLATE_PROPERTY SET VALUE=?
            WHERE GAME_VARIATION_TEMPLATE_ID=? AND NAME=?"""
    private val SQL_DELETE = "DELETE FROM GAME_VARIATION_TEMPLATE WHERE GAME_VARIATION_TEMPLATE_ID=?"
    private val SQL_DELETE_PROPERTY = "DELETE FROM GAME_VARIATION_TEMPLATE_PROPERTY WHERE GAME_VARIATION_TEMPLATE_ID=?"
}

@Repository
class JDBCGameVariationRepository @Autowired()(val jdbcTemplate: JdbcTemplate) extends GameVariationRepository with MySQLPaging[GameVariation] {

    private val gameVariationMapper = new GameVariationMapper()
    private val gameVariationPropertyMapper = new GameVariationPropertyMapper()

    def findById(id: BigDecimal): Option[GameVariation] = {
        notNull(id, "gameId may not be null")

        val gameVariations = jdbcTemplate.query(JDBCGameVariationRepository.SQL_SELECT_BY_ID, gameVariationMapper, id.underlying())
        if (gameVariations != null && !gameVariations.isEmpty) {
            Some(gameVariations.get(0).withProperties(propertiesFor(id)))
        } else {
            None
        }
    }

    def listFor(gameType: String): Map[BigDecimal, String] = {
        var templateMap = collection.mutable.Map[BigDecimal, String]()
        jdbcTemplate.queryForList(SQL_LIST_FOR_GAMETYPE, gameType).foreach{
            row => templateMap += BigDecimal(row.get("GAME_VARIATION_TEMPLATE_ID").toString) -> row.get("NAME").toString
        }
        templateMap.toMap
    }

    private def propertiesFor(id: scala.BigDecimal) = jdbcTemplate.query(
            JDBCGameVariationRepository.SQL_SELECT_PROPERTIES, gameVariationPropertyMapper, id.underlying()).toList

    @Transactional
    def findAll(page: Int = 0, pageSize: Int = 20): PagedData[GameVariation] = {
        val pagedVariations = selectWithPaging(jdbcTemplate, JDBCGameVariationRepository.SQL_SELECT_ALL, gameVariationMapper, page, pageSize)
        new PagedData[GameVariation](pagedVariations.getStartPosition, pagedVariations.getSize, pagedVariations.getTotalSize,
                pagedVariations.getData.map { variation => variation.withProperties(propertiesFor(variation.id)) })
    }

    def save(gameVariation: GameVariation): GameVariation = {
        notNull(gameVariation, "gameVariation may not be null")

        if (gameVariation.id == null) {
            insert(gameVariation)
        } else {
            update(gameVariation)
        }
    }

    private def insert(gameVariation: GameVariation): GameVariation = {
        val variationId = jdbcTemplate.execute(new PreparedStatementCreator {
            def createPreparedStatement(conn: Connection): PreparedStatement = {
                val stmt = conn.prepareStatement(JDBCGameVariationRepository.SQL_INSERT, Statement.RETURN_GENERATED_KEYS)
                stmt.setString(1, gameVariation.gameType)
                stmt.setString(2, gameVariation.name)
                stmt
            }
        }, new PrimaryKeyLoader())
        saveProperties(gameVariation.withId(BigDecimal(variationId)))
    }

    private def update(gameVariation: GameVariation): GameVariation = {
        jdbcTemplate.update(JDBCGameVariationRepository.SQL_UPDATE, gameVariation.name, gameVariation.id.underlying())
        saveProperties(gameVariation)
    }

    def delete(gameVariationId: BigDecimal) {
        notNull(gameVariationId, "gameVariationId may not be null")

        jdbcTemplate.update(JDBCGameVariationRepository.SQL_DELETE_PROPERTY, gameVariationId.underlying())
        jdbcTemplate.update(JDBCGameVariationRepository.SQL_DELETE, gameVariationId.underlying())
    }

    private def saveProperties(gameVariation: GameVariation): GameVariation = {
        val updatedProperties = gameVariation.properties.map { property =>
            if (property.id == null) {
                insertProperties(gameVariation, property)
            } else {
                updateProperties(property, gameVariation)
            }
        }

        gameVariation.withProperties(updatedProperties)
    }

    private def insertProperties(gameVariation: GameVariation, property: GameVariationProperty): GameVariationProperty = {
        val propertyId = jdbcTemplate.execute(new PreparedStatementCreator {
            def createPreparedStatement(conn: Connection): PreparedStatement = {
                val stmt = conn.prepareStatement(JDBCGameVariationRepository.SQL_INSERT_PROPERTY, Statement.RETURN_GENERATED_KEYS)
                stmt.setBigDecimal(1, gameVariation.id.underlying())
                stmt.setString(2, property.name)
                stmt.setString(3, property.value)
                stmt
            }
        }, new PrimaryKeyLoader())
        new GameVariationProperty(BigDecimal(propertyId), property.name, property.value)
    }

    private def updateProperties(property: GameVariationProperty, gameVariation: GameVariation): GameVariationProperty = {
        jdbcTemplate.update(JDBCGameVariationRepository.SQL_UPDATE_PROPERTY, property.value,
            gameVariation.id.underlying(), property.name)
        property
    }

}
