package strata.server.lobby.controlcentre.repository

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TransactionConfiguration
import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.matchers.ShouldMatchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.junit.{Test, Before, After}
import com.yazino.platform.tournament.DayPeriod
import strata.server.lobby.controlcentre.model.RecurringTournamentDefinition
import org.joda.time.DateTime
import org.springframework.transaction.annotation.Transactional

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration
@TransactionConfiguration
class JDBCRecurringTournamentDefinitionRepositoryIntegrationTest extends AssertionsForJUnit with ShouldMatchers {

    @Autowired private var underTest: RecurringTournamentDefinitionRepository = null
    @Autowired private val jdbcTemplate: JdbcTemplate = null

    @After def cleanUpDatabase() {
        jdbcTemplate.update("DELETE FROM RECURRING_TOURNAMENT_DEFINITION WHERE DEFINITION_ID < 0")
    }

    @Before def createTestDefinition() {
        jdbcTemplate.update(
            """INSERT INTO RECURRING_TOURNAMENT_DEFINITION
               (DEFINITION_ID,INITIAL_SIGNUP_TIME,SIGNUP_PERIOD,FREQUENCY,
               TOURNAMENT_VARIATION_TEMPLATE_ID,TOURNAMENT_NAME,TOURNAMENT_DESCRIPTION,
               PARTNER_ID,EXCLUSION_PERIODS, ENABLED)
               VALUES
               (-1,'2012-02-20 10:00:00',14400000,3600000,1,'aTestTournament','aDescription',
               'PLAY_FOR_FUN','Monday@00:00-00:01,Tuesday@00:00-00:01', 1),
               (-2,'2012-02-20 10:00:00',14400000,3600000,1,'aTestTournament','aDescription',
               'PLAY_FOR_FUN','Monday@00:00-00:01,Tuesday@00:00-00:01', 1)""")
    }

    @Transactional
    @Test def findingANonExistentDefinitionReturnsNone() {
        underTest.findById(BigDecimal("-3")) should equal(None)
    }

    @Transactional
    @Test def findingADefinitionReturnsTheDefinition() {
        underTest.findById(BigDecimal(-1)).getOrElse(None) should equal(recurringTournamentOne)
    }

    @Transactional
    @Test def findingAllDefinitionReturnsKnownDefinition() {
        val tournaments = underTest.findAll(0, Integer.MAX_VALUE)

        tournaments.getData should contain(recurringTournamentOne)
        tournaments.getData should contain(recurringTournamentTwo)
    }

    @Transactional
    @Test def savingADefinitionShouldWriteItToTheRepository() {
        val definition = recurringTournamentOne.withId(null)

        val savedDefinition = underTest.save(definition)

        val definitionMap = jdbcTemplate.queryForMap(
            "SELECT * FROM RECURRING_TOURNAMENT_DEFINITION WHERE DEFINITION_ID=?", savedDefinition.id.underlying())
        definitionMap.get("DEFINITION_ID") should equal(savedDefinition.id)
        definitionMap.get("TOURNAMENT_NAME") should equal(savedDefinition.tournamentName)
        definitionMap.get("TOURNAMENT_DESCRIPTION") should equal(savedDefinition.tournamentDescription)
        definitionMap.get("PARTNER_ID") should equal(savedDefinition.partnerId)
        new DateTime(definitionMap.get("INITIAL_SIGNUP_TIME")) should equal(definition.initialSignupTime)
        definitionMap.get("SIGNUP_PERIOD") should equal(savedDefinition.signupPeriod)
        definitionMap.get("FREQUENCY") should equal(savedDefinition.frequency)
        definitionMap.get("TOURNAMENT_VARIATION_TEMPLATE_ID") should equal(savedDefinition.variationId)
        definitionMap.get("ENABLED") should equal(savedDefinition.enabled)
        definitionMap.get("EXCLUSION_PERIODS") should equal(savedDefinition.exclusionPeriods.mkString(","))
    }

    @Transactional
    @Test def updateADefinitionShouldWriteTheUpdatesToTheRepository() {
        val savedDefinition = underTest.save(recurringTournamentOne)
        val definitionToUpdate = new RecurringTournamentDefinition(savedDefinition.id,
            "anUpdatedName",
            "anUpdatedDescription",
            "anUpdatedPartner",
            new DateTime(2011, 10, 26, 10, 0, 0, 0),
            3000L, 6000L, BigDecimal(1), false,
            Seq(new DayPeriod("Tuesday@00:00-00:01"), new DayPeriod("Wednesday@12:00-14:30")))

        underTest.save(definitionToUpdate)

        val definitionMap = jdbcTemplate.queryForMap(
            "SELECT * FROM RECURRING_TOURNAMENT_DEFINITION WHERE DEFINITION_ID=?", savedDefinition.id.underlying())
        definitionMap.get("DEFINITION_ID") should equal(definitionToUpdate.id)
        definitionMap.get("TOURNAMENT_NAME") should equal(definitionToUpdate.tournamentName)
        definitionMap.get("TOURNAMENT_DESCRIPTION") should equal(definitionToUpdate.tournamentDescription)
        definitionMap.get("PARTNER_ID") should equal(definitionToUpdate.partnerId)
        new DateTime(definitionMap.get("INITIAL_SIGNUP_TIME")) should equal(definitionToUpdate.initialSignupTime)
        definitionMap.get("SIGNUP_PERIOD") should equal(definitionToUpdate.signupPeriod)
        definitionMap.get("FREQUENCY") should equal(definitionToUpdate.frequency)
        definitionMap.get("TOURNAMENT_VARIATION_TEMPLATE_ID") should equal(definitionToUpdate.variationId)
        definitionMap.get("ENABLED") should equal(definitionToUpdate.enabled)
        definitionMap.get("EXCLUSION_PERIODS") should equal(definitionToUpdate.exclusionPeriods.mkString(","))
    }

    private def recurringTournamentOne = new RecurringTournamentDefinition(
        BigDecimal(-1), "aTestTournament", "aDescription", "PLAY_FOR_FUN",
        new DateTime(2012, 2, 20, 10, 0, 0, 0), 14400000L, 3600000L, BigDecimal(1),
        true, Seq(new DayPeriod("Monday@00:00-00:01"), new DayPeriod("Tuesday@00:00-00:01")))

    private def recurringTournamentTwo = new RecurringTournamentDefinition(
        BigDecimal(-2), "aTestTournament", "aDescription", "PLAY_FOR_FUN",
        new DateTime(2012, 2, 20, 10, 0, 0, 0), 14400000L, 3600000L, BigDecimal(1),
        true, Seq(new DayPeriod("Monday@00:00-00:01"), new DayPeriod("Tuesday@00:00-00:01")))

}
