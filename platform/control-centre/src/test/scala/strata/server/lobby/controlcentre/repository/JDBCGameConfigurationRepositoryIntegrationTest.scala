package strata.server.lobby.controlcentre.repository

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TransactionConfiguration
import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.junit.{After, Before, Test}
import java.math.BigDecimal
import com.yazino.platform.table.{GameConfigurationProperty, GameConfiguration}
import java.util.{Arrays, ArrayList => JavaArrayList, HashSet => JavaHashSet}
import java.util


@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true)
class JDBCGameConfigurationRepositoryIntegrationTest extends AssertionsForJUnit with ShouldMatchers {

  @Autowired private val underTest: GameConfigurationRepository = null
  @Autowired private val jdbcTemplate: JdbcTemplate = null
  val GAME_ONE_ID: String = "GAME_TYPE_1"
  val GAME_TWO_ID: String = "GAME_TYPE_2"
  val GAME_PROPERTY_ONE_ID: BigDecimal = BigDecimal.valueOf(-1)
  val GAME_PROPERTY_TWO_ID: BigDecimal = BigDecimal.valueOf(-2)
  val SHORT_NAME: String = "shortName"
  val DISPLAY_NAME: String = "Display Name"
  val PROPERTY_NAME: String = "PROPERTY_NAME"
  val PROPERTY_VALUE: String = "Property value"
  val ALIASES = Set("g1", "g2")

  val gameOneConfiguration = new GameConfiguration(GAME_ONE_ID, SHORT_NAME, DISPLAY_NAME, new util.HashSet[String](), 0)
  val gameTwoConfiguration = new GameConfiguration(GAME_TWO_ID, SHORT_NAME, DISPLAY_NAME,  new util.HashSet[String](), 0)
  val aGameConfigurationProperty = new GameConfigurationProperty(GAME_PROPERTY_ONE_ID, GAME_ONE_ID, PROPERTY_NAME, PROPERTY_VALUE)

  @Before
  @After def cleanUpDatabase() {
    jdbcTemplate.execute("DELETE FROM GAME_CONFIGURATION_PROPERTY WHERE GAME_ID IN ('GAME_TYPE_1', 'GAME_TYPE_2')")
    jdbcTemplate.execute("DELETE FROM GAME_CONFIGURATION WHERE GAME_ID IN ('GAME_TYPE_1', 'GAME_TYPE_2')")
  }

  @Transactional
  @Test def findGameConfigurationIncludingPropertyById() {
    jdbcTemplate.execute(String.format("INSERT INTO GAME_CONFIGURATION (GAME_ID, SHORT_NAME, DISPLAY_NAME, ALIASES, ORD) VALUES ('%s', '%s', '%s', '%s', %s)", GAME_ONE_ID, SHORT_NAME, DISPLAY_NAME, ALIASES.mkString(","), "0"))
    jdbcTemplate.execute(String.format("INSERT INTO GAME_CONFIGURATION_PROPERTY (GAME_PROPERTY_ID, GAME_ID, PROPERTY_NAME, PROPERTY_VALUE) VALUES (%s, '%s', '%s', '%s')", GAME_PROPERTY_ONE_ID, GAME_ONE_ID, PROPERTY_NAME, PROPERTY_VALUE))

    val gameConfiguration = underTest.findGameById(GAME_ONE_ID)
    gameConfiguration.get.getGameId should equal(GAME_ONE_ID)
    gameConfiguration.get.getShortName should equal(SHORT_NAME)
    gameConfiguration.get.getDisplayName should equal(DISPLAY_NAME)
    gameConfiguration.get.getProperties should equal(new util.HashSet[GameConfigurationProperty](util.Arrays.asList(aGameConfigurationProperty)))
  }

  @Transactional
  @Test def shouldSaveGameConfiguration() {
    val aGame: GameConfiguration = new GameConfiguration("A_GAME", "shortName", "Display Name", new util.HashSet[String](), 0)

    val gameConfiguration = underTest.save(aGame)
    val savedGameConfiguration = underTest.findGameById(aGame.getGameId).get

    savedGameConfiguration should equal(aGame)
    gameConfiguration should equal(aGame)
  }

  @Transactional
  @Test def shouldSaveNewGameProperty() {
    jdbcTemplate.execute(String.format("INSERT INTO GAME_CONFIGURATION (GAME_ID, SHORT_NAME, DISPLAY_NAME, ALIASES, ORD) VALUES ('%s', '%s', '%s', '%s', %s)", GAME_ONE_ID, SHORT_NAME, DISPLAY_NAME, ALIASES.mkString(","), "0"))
    val aNewGameProperty = new GameConfigurationProperty(GAME_PROPERTY_TWO_ID, GAME_ONE_ID, PROPERTY_NAME, PROPERTY_VALUE)
    var gameConfiguration = gameOneConfiguration.withProperties(new util.HashSet[GameConfigurationProperty](util.Arrays.asList(aGameConfigurationProperty, aNewGameProperty)))
    gameConfiguration = underTest.save(gameConfiguration)
    gameConfiguration.getProperties.size should equal(2)
  }

  @Transactional
  @Test def shouldUpdateAConfigurationProperty() {
    jdbcTemplate.execute(String.format("INSERT INTO GAME_CONFIGURATION (GAME_ID, SHORT_NAME, DISPLAY_NAME, ALIASES, ORD) VALUES ('%s', '%s', '%s', '%s', %s)", GAME_ONE_ID, SHORT_NAME, DISPLAY_NAME, ALIASES.mkString(","), "0"))
    jdbcTemplate.execute(String.format("INSERT INTO GAME_CONFIGURATION_PROPERTY (GAME_PROPERTY_ID, GAME_ID, PROPERTY_NAME, PROPERTY_VALUE) VALUES (%s, '%s', '%s', '%s')", GAME_PROPERTY_ONE_ID, GAME_ONE_ID, PROPERTY_NAME, PROPERTY_VALUE))

    val updatedGameConfiguration = new GameConfiguration(GAME_ONE_ID, "sn", "dn", Set("a1", "a2"), 3)
    val updatedGameConfigurationProperty = new GameConfigurationProperty(GAME_PROPERTY_ONE_ID, GAME_ONE_ID, PROPERTY_NAME, "updatedValue")

    var gameConfiguration = underTest.findGameById(GAME_ONE_ID).get
    gameConfiguration = updatedGameConfiguration.withProperties(util.Arrays.asList(updatedGameConfigurationProperty))
    gameConfiguration = underTest.save(gameConfiguration)

    val gameConfigurationAfterUpdate = underTest.findGameById(GAME_ONE_ID).get
    gameConfigurationAfterUpdate should equal (gameConfiguration)
    assert(gameConfigurationAfterUpdate.getProperties.contains(updatedGameConfigurationProperty))
  }
}
