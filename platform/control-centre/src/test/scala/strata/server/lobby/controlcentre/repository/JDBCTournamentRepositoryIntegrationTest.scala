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
import org.springframework.transaction.annotation.Transactional
import strata.server.lobby.controlcentre.model.Tournament
import org.joda.time.DateTime
import com.yazino.platform.tournament.TournamentStatus

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration
@TransactionConfiguration
class JDBCTournamentRepositoryIntegrationTest extends AssertionsForJUnit with ShouldMatchers {

    @Autowired private var underTest: TournamentRepository = null
    @Autowired private val jdbcTemplate: JdbcTemplate = null


    @After def cleanUpDatabase() {
        jdbcTemplate.update("DELETE FROM TOURNAMENT WHERE TOURNAMENT_ID < 0")
    }

    @Before def createTestTournaments() {
        jdbcTemplate.update(
            """INSERT INTO TOURNAMENT
               (TOURNAMENT_ID,TOURNAMENT_VARIATION_TEMPLATE_ID,TOURNAMENT_SIGNUP_START_TS,
                TOURNAMENT_SIGNUP_END_TS,TOURNAMENT_START_TS,TOURNAMENT_STATUS,TOURNAMENT_NAME,
                PARTNER_ID,TOURNAMENT_DESCRIPTION)
               VALUES
               (-1,1,'2012-02-20 10:00:00','2012-02-21 10:00:00','2012-02-21 10:00:00',
                'R','aTestTournament1','PLAY_FOR_FUN','aDescription1'),
               (-2,1,'2012-02-20 11:00:00','2012-02-21 11:00:00','2012-02-21 11:00:00',
                'R','aTestTournament2','PLAY_FOR_FUN','aDescription2')""")
    }

    @Transactional
    @Test def findingANonExistentTournamentReturnsNone() {
        underTest.findById(BigDecimal("-3")) should equal(None)
    }

    @Transactional
    @Test def findingATournamentReturnsTheTournament() {
        underTest.findById(BigDecimal(-1)).getOrElse(None) should equal(tournamentOne)
    }

    @Transactional
    @Test def findingAllTournamentsReturnsKnownTournaments() {
        val tournaments = underTest.findAll(0, Integer.MAX_VALUE)

        tournaments.getData should contain(tournamentOne)
        tournaments.getData should contain(tournamentTwo)
    }

    private def tournamentOne = new Tournament(BigDecimal(-1), "aTestTournament1", BigDecimal(1),
        new DateTime(2012, 2, 20, 10, 0, 0, 0), new DateTime(2012, 2, 21, 10, 0, 0, 0),
        new DateTime(2012, 2, 21, 10, 0, 0, 0), TournamentStatus.RUNNING, "PLAY_FOR_FUN", "aDescription1")

    private def tournamentTwo = new Tournament(BigDecimal(-2), "aTestTournament2", BigDecimal(1),
        new DateTime(2012, 2, 20, 11, 0, 0, 0), new DateTime(2012, 2, 21, 11, 0, 0, 0),
        new DateTime(2012, 2, 21, 11, 0, 0, 0), TournamentStatus.RUNNING, "PLAY_FOR_FUN", "aDescription2")

}
