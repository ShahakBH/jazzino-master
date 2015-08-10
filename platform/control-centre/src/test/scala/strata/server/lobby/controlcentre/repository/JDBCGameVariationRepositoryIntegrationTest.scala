package strata.server.lobby.controlcentre.repository

import mapper.{GameVariationRepository, GameVariationMapper, GameVariationPropertyMapper}
import scala.collection.JavaConversions._
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.transaction.TransactionConfiguration
import org.springframework.test.context.ContextConfiguration
import org.junit.runner.RunWith
import org.scalatest.junit.AssertionsForJUnit
import org.springframework.beans.factory.annotation.Autowired
import org.scalatest.matchers.ShouldMatchers
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.junit.{After, Before, Test}
import strata.server.lobby.controlcentre.model.{GameVariationProperty, GameVariation}
import scala.math.min

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration
@TransactionConfiguration
class JDBCGameVariationRepositoryIntegrationTest extends AssertionsForJUnit with ShouldMatchers {

    @Autowired private var underTest: GameVariationRepository = null
    @Autowired private val jdbcTemplate: JdbcTemplate = null

    @Before @After def cleanUpDatabase() {
        jdbcTemplate.execute("DELETE FROM GAME_VARIATION_TEMPLATE_PROPERTY WHERE GAME_VARIATION_TEMPLATE_ID < 0")
        jdbcTemplate.execute("DELETE FROM GAME_VARIATION_TEMPLATE WHERE GAME_VARIATION_TEMPLATE_ID < 0")
    }

    @Transactional @Test def findingANonExistentVariationReturnsNone() {
        underTest.findById(BigDecimal("-1")) should equal (None)
    }

    @Transactional @Test def findingAVariationReturnsTheVariation() {
        insert(aGameVariation(BigDecimal("-1")))

        underTest.findById(BigDecimal("-1")).get should equal (aGameVariation(BigDecimal("-1")))
    }

    @Transactional @Test def findingAVariationWithPropertiesReturnsTheVariationAndProperties() {
        insert(aGameVariationWithProperties(BigDecimal(-1)))

        underTest.findById(BigDecimal("-1")).get should equal (aGameVariationWithProperties(BigDecimal(-1)))
    }

    @Transactional @Test def listVariationsReturnsAMapOfIdsToNames() {
        insert(aGameVariation(BigDecimal("-1")))

        val items = underTest.listFor("BLACKJACK")

        items should contain(BigDecimal("-1") -> "TEST-TEMPLATE-1")
    }

    @Transactional @Test def findingAllVariationsReturnsAPageOfVariations() {
        val initialCount = jdbcTemplate.queryForInt("SELECT COUNT(*) FROM GAME_VARIATION_TEMPLATE")

        insert(aGameVariationWithProperties(BigDecimal("-1")))
        insert(aGameVariationWithProperties(BigDecimal("-5")))
        insert(aGameVariationWithProperties(BigDecimal("-10")))

        val pageZero = underTest.findAll(0, 20)

        pageZero.getData should contain (aGameVariationWithProperties(BigDecimal("-1")))
        pageZero.getData should contain (aGameVariationWithProperties(BigDecimal("-5")))
        pageZero.getData should contain (aGameVariationWithProperties(BigDecimal("-10")))
        pageZero.getStartPosition should equal (0)
        pageZero.getSize should equal (min(3 + initialCount, 20))
        pageZero.getTotalSize should equal (3 + initialCount)
    }

    @Transactional @Test def aNewVariationWithNoPropertiesCanBeSavedAndReturnsTheID() {
        val variation = aGameVariation()

        var savedVariation = underTest.save(variation)

        savedVariation.id should not equal (null)
        savedVariation.name should equal (variation.name)
        savedVariation.gameType should equal (variation.gameType)

        val variationInDb = getVariationFromDB(savedVariation.id)
        variationInDb should equal (savedVariation)
    }

    @Transactional @Test def aVariationWithNoPropertiesCanBeUpdated() {
        val variation = aGameVariation(BigDecimal("-1"))
        insert(variation)

        val updatedVariation = new GameVariation(variation.id, variation.gameType, "aNewName", variation.properties)
        var savedVariation = underTest.save(updatedVariation)

        savedVariation should equal (updatedVariation)

        val variationInDb = getVariationFromDB(savedVariation.id)
        variationInDb should equal (savedVariation)
    }

    @Transactional @Test def aNewVariationWithPropertiesCanBeSavedAndReturnsTheID() {
        val variation = aGameVariationWithProperties()

        var savedVariation = underTest.save(variation)

        savedVariation.properties.foreach { property => property.id should not be (null) }

        val variationInDb = getVariationFromDB(savedVariation.id)
        variationInDb should equal (savedVariation)
    }

    @Transactional @Test def aVariationWithPropertiesCanBeUpdated() {
        val variation = aGameVariationWithProperties(BigDecimal("-1"))
        insert(variation)

        val updatedVariation = new GameVariation(variation.id, variation.gameType, "aNewName",
            variation.properties.map {property => new GameVariationProperty(property.id, property.name, property.value + "1")})
        var savedVariation = underTest.save(updatedVariation)

        val variationInDb = getVariationFromDB(savedVariation.id)
        variationInDb should equal (savedVariation)
    }

    @Transactional @Test def aVariationWithANewPropertiesCanBeUpdated() {
        val variation = aGameVariationWithProperties(BigDecimal("-1"))
        insert(variation)

        val updatedProperties = variation.properties.map {property => new GameVariationProperty(property.id, property.name, property.value + "1")}
        val updatedVariation = new GameVariation(variation.id, variation.gameType, "aNewName",  new GameVariationProperty("aNewName", "aNewValue") :: updatedProperties)
        var savedVariation = underTest.save(updatedVariation)

        savedVariation.properties.foreach { property => property.id should not be (null) }

        val variationInDb = getVariationFromDB(savedVariation.id)
        variationInDb should equal (savedVariation)
    }

    @Transactional @Test def deleteShouldRemoveAVariationFromTheDB() {
        val variation = aGameVariation(BigDecimal("-1"))
        insert(variation)

        underTest.delete(BigDecimal("-1"))

        getVariationFromDB(BigDecimal("-1")) should equal (null)
    }

    @Transactional @Test def deleteShouldRemoveAVariationAndItsPropertiesFromTheDB() {
        val variation = aGameVariationWithProperties(BigDecimal("-1"))
        insert(variation)

        underTest.delete(BigDecimal("-1"))

        getVariationFromDB(BigDecimal("-1")) should equal (null)
        val numberOfPropertiesInDb = jdbcTemplate.queryForInt("SELECT COUNT(*) FROM GAME_VARIATION_TEMPLATE_PROPERTY WHERE GAME_VARIATION_TEMPLATE_ID=-1")
        numberOfPropertiesInDb should equal (0)
    }
    
    private def getVariationFromDB(id: BigDecimal) = {
        val gameVariations = jdbcTemplate.query("SELECT * FROM GAME_VARIATION_TEMPLATE WHERE GAME_VARIATION_TEMPLATE_ID=?",
                new GameVariationMapper(), id.underlying())
        if (gameVariations != null && !gameVariations.isEmpty) {
            val properties = jdbcTemplate.query(
                    "SELECT * FROM GAME_VARIATION_TEMPLATE_PROPERTY WHERE GAME_VARIATION_TEMPLATE_ID=?",
                    new GameVariationPropertyMapper(), id.underlying())
            gameVariations.get(0).withProperties(properties.toList)
        } else {
            null
        }
    }

    private def aGameVariation(id: BigDecimal = null) =
        new GameVariation(id, "BLACKJACK", "TEST-TEMPLATE" + id, List())

    private def aGameVariationWithProperties(id: BigDecimal = null) =
        if (id == null) {
            new GameVariation(id, "BLACKJACK", "TEST-TEMPLATE", List(
                new GameVariationProperty("Property1", "Value1"),
                new GameVariationProperty("Property2", "Value2"),
                new GameVariationProperty("Property3", "Value3")
            ))
        } else {
            new GameVariation(id, "BLACKJACK", "TEST-TEMPLATE" + id, List(
                new GameVariationProperty(id - BigDecimal(1), "Property1", "Value1"),
                new GameVariationProperty(id - BigDecimal(2), "Property2", "Value2"),
                new GameVariationProperty(id - BigDecimal(3), "Property3", "Value3")
            ))
        }

    private def insert(variation: GameVariation) {
        jdbcTemplate.update("INSERT INTO GAME_VARIATION_TEMPLATE (GAME_VARIATION_TEMPLATE_ID,GAME_TYPE,NAME) VALUES (?,?,?)",
            variation.id.underlying(), variation.gameType, variation.name)
        variation.properties.foreach { property =>
            jdbcTemplate.update("""INSERT INTO GAME_VARIATION_TEMPLATE_PROPERTY
                    (GAME_VARIATION_TEMPLATE_PROPERTY_ID,GAME_VARIATION_TEMPLATE_ID,NAME,VALUE)
                    VALUES (?,?,?,?)""",
                    property.id.underlying(), variation.id.underlying(), property.name, property.value)
        }
    }

}