package strata.server.lobby.controlcentre.repository

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.AssertionsForJUnit
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TransactionConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.junit.{Test, After}
import org.springframework.transaction.annotation.Transactional
import strata.server.lobby.controlcentre.model.{TournamentVariationPayout, TournamentVariationRound, TournamentVariation}
import com.yazino.platform.tournament.TournamentType
import strata.server.lobby.controlcentre.model.Allocator
import scala.collection.JavaConversions._

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration
@Transactional
@TransactionConfiguration
class JDBCTournamentVariationRepositoryIntegrationTest extends AssertionsForJUnit with ShouldMatchers {

    @Autowired private var underTest: TournamentVariationRepository = null
    @Autowired private val jdbcTemplate: JdbcTemplate = null

    @After def cleanUpDatabase() {
        jdbcTemplate.update("DELETE FROM TOURNAMENT_VARIATION_PAYOUT WHERE TOURNAMENT_VARIATION_TEMPLATE_ID < 0")
        jdbcTemplate.update("DELETE FROM TOURNAMENT_VARIATION_PAYOUT WHERE TOURNAMENT_VARIATION_TEMPLATE_ID IN "
            + "(SELECT TOURNAMENT_VARIATION_TEMPLATE_ID FROM TOURNAMENT_VARIATION_TEMPLATE WHERE NAME IN (?,?))",
            "aTestTournament", "aTestTournament2")
        jdbcTemplate.update("DELETE FROM TOURNAMENT_VARIATION_ROUND WHERE TOURNAMENT_VARIATION_TEMPLATE_ID < 0")
        jdbcTemplate.update("DELETE FROM TOURNAMENT_VARIATION_ROUND WHERE TOURNAMENT_VARIATION_TEMPLATE_ID IN "
            + "(SELECT TOURNAMENT_VARIATION_TEMPLATE_ID FROM TOURNAMENT_VARIATION_TEMPLATE WHERE NAME IN (?,?))",
            "aTestTournament", "aTestTournament2")
        jdbcTemplate.update("DELETE FROM TOURNAMENT_VARIATION_TEMPLATE WHERE TOURNAMENT_VARIATION_TEMPLATE_ID < 0")
        jdbcTemplate.update("DELETE FROM TOURNAMENT_VARIATION_TEMPLATE WHERE NAME IN (?,?)",
            "aTestTournament", "aTestTournament2")
    }

    @Test def listingVariationsReturnsAKnownVariation() {
        val variation = underTest.save(aVariation())

        val variationList = underTest.list()

        variationList should contain key(variation.id)
        variationList.get(variation.id).getOrElse(null) should equal(variation.name)
    }

    @Test def savingANewVariationCreatesItInTheDatabase() {
        val variation = underTest.save(aVariation())

        val userMap = jdbcTemplate.queryForMap(
            "SELECT * FROM TOURNAMENT_VARIATION_TEMPLATE WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=?",
            variation.id.underlying())
        userMap.get("TOURNAMENT_TYPE") should equal(TournamentType.PRESET.name())
        userMap.get("NAME") should equal("aTestTournament")
        userMap.get("ENTRY_FEE") should equal(BigDecimal("100.0000").underlying())
        userMap.get("SERVICE_FEE") should equal(BigDecimal("200.0000").underlying())
        userMap.get("STARTING_CHIPS") should equal(BigDecimal("1000.0000").underlying())
        userMap.get("MIN_PLAYERS") should equal(3)
        userMap.get("MAX_PLAYERS") should equal(10)
        userMap.get("GAME_TYPE") should equal("BLACKJACK")
        userMap.get("EXPIRY_DELAY") should equal(60 * 60 * 24)
        userMap.get("PRIZE_POOL") should equal(BigDecimal("1500.0000").underlying())
        userMap.get("ALLOCATOR") should equal("EVEN_BY_BALANCE")
    }

    @Test def savingANewVariationCreatesAttachedPayoutsInTheDatabase() {
        val variation = underTest.save(aVariation()
            .withPayouts(List(aPayout(1), aPayout(2))))

        val payoutIds = jdbcTemplate.queryForList(
            "SELECT TOURNAMENT_VARIATION_PAYOUT_ID FROM TOURNAMENT_VARIATION_PAYOUT "
                + "WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=? ORDER BY RANK ASC",
            variation.id.underlying())
        payoutIds.size should equal(2)
        var index = 1
        payoutIds.map(_.get("TOURNAMENT_VARIATION_PAYOUT_ID")).foreach {
            payoutId =>
                val payoutMap = jdbcTemplate.queryForMap(
                    "SELECT * FROM TOURNAMENT_VARIATION_PAYOUT WHERE TOURNAMENT_VARIATION_PAYOUT_ID=?",
                    payoutId)
                payoutMap.get("RANK") should equal(index)
                payoutMap.get("PAYOUT") should equal(BigDecimal(index * 0.1).underlying().setScale(8))
                index += 1
        }
    }

    @Test def savingANewVariationCreatesAttachedRoundsInTheDatabase() {
        val variation = underTest.save(aVariation()
            .withRounds(List(aRound(1), aRound(2))))

        val roundIds = jdbcTemplate.queryForList(
            "SELECT TOURNAMENT_VARIATION_ROUND_ID FROM TOURNAMENT_VARIATION_ROUND "
                + "WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=? ORDER BY ROUND_NUMBER ASC",
            variation.id.underlying())
        roundIds.size should equal(2)
        var index = 1
        roundIds.map(_.get("TOURNAMENT_VARIATION_ROUND_ID")).foreach {
            roundId =>
                val roundMap = jdbcTemplate.queryForMap(
                    "SELECT * FROM TOURNAMENT_VARIATION_ROUND WHERE TOURNAMENT_VARIATION_ROUND_ID=?",
                    roundId)
                roundMap.get("ROUND_NUMBER") should equal(index)
                roundMap.get("ROUND_END_INTERVAL") should equal(30)
                roundMap.get("ROUND_LENGTH") should equal(300)
                roundMap.get("GAME_VARIATION_TEMPLATE_ID") should equal(5)
                roundMap.get("CLIENT_PROPERTIES_ID") should equal("Blue Blackjack")
                roundMap.get("MINIMUM_BALANCE") should equal(BigDecimal("100.0000").underlying())
                roundMap.get("DESCRIPTION") should equal("aTestRound")

                index += 1
        }
    }

    @Test def savingAnExistingVariationUpdatesItInTheDatabase() {
        val variation = underTest.save(aVariation())
        val updatedVariation = new TournamentVariation(variation.id, variation.tournamentType,
            "aTestTournament2", BigDecimal(400), BigDecimal(800), BigDecimal(2000),
            6, 20, "ROULETTE", 60 * 60 * 48, BigDecimal(3000), Allocator.EVEN_RANDOM,
            variation.rounds, variation.payouts)

        underTest.save(updatedVariation)

        val userMap = jdbcTemplate.queryForMap(
            "SELECT * FROM TOURNAMENT_VARIATION_TEMPLATE WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=?",
            updatedVariation.id.underlying())
        userMap.get("TOURNAMENT_TYPE") should equal(TournamentType.PRESET.name())
        userMap.get("NAME") should equal("aTestTournament2")
        userMap.get("ENTRY_FEE") should equal(BigDecimal("400.0000").underlying())
        userMap.get("SERVICE_FEE") should equal(BigDecimal("800.0000").underlying())
        userMap.get("STARTING_CHIPS") should equal(BigDecimal("2000.0000").underlying())
        userMap.get("MIN_PLAYERS") should equal(6)
        userMap.get("MAX_PLAYERS") should equal(20)
        userMap.get("GAME_TYPE") should equal("ROULETTE")
        userMap.get("EXPIRY_DELAY") should equal(60 * 60 * 48)
        userMap.get("PRIZE_POOL") should equal(BigDecimal("3000.0000").underlying())
        userMap.get("ALLOCATOR") should equal("EVEN_RANDOM")
    }

    @Test def savingAnExistingVariationUpdatesAttachedPayoutsInTheDatabase() {
        val variation = underTest.save(aVariation()
            .withPayouts(List(aPayout(1), aPayout(2))))
        val payout1 = variation.payouts.get(0)
        val updatedPayout1 = new TournamentVariationPayout(payout1.id, payout1.rank, BigDecimal(0.2))
        val payout2 = variation.payouts.get(1)
        val updatedPayout2 = new TournamentVariationPayout(payout2.id, payout2.rank, BigDecimal(0.4))
        val updatedVariation = variation.withPayouts(List(updatedPayout1, updatedPayout2))

        underTest.save(updatedVariation)

        val payoutIds = jdbcTemplate.queryForList(
            "SELECT TOURNAMENT_VARIATION_PAYOUT_ID FROM TOURNAMENT_VARIATION_PAYOUT "
                + "WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=? ORDER BY RANK ASC",
            variation.id.underlying())
        payoutIds.size should equal(2)
        var index = 1
        payoutIds.map(_.get("TOURNAMENT_VARIATION_PAYOUT_ID")).foreach {
            payoutId =>
                val payoutMap = jdbcTemplate.queryForMap(
                    "SELECT * FROM TOURNAMENT_VARIATION_PAYOUT WHERE TOURNAMENT_VARIATION_PAYOUT_ID=?",
                    payoutId)
                payoutMap.get("RANK") should equal(index)
                payoutMap.get("PAYOUT") should equal(BigDecimal((index * 2) * 0.1)
                    .setScale(8, BigDecimal.RoundingMode.HALF_EVEN).underlying())
                index += 1
        }
    }

    @Test def savingAnExistingVariationAddsNewPayoutsToTheDatabase() {
        val variation = underTest.save(aVariation()
            .withPayouts(List(aPayout(1), aPayout(2))))
        val updatedVariation = variation.withPayouts(aPayout(3) :: variation.payouts.toList)

        underTest.save(updatedVariation)

        val payoutIds = jdbcTemplate.queryForList(
            "SELECT TOURNAMENT_VARIATION_PAYOUT_ID FROM TOURNAMENT_VARIATION_PAYOUT "
                + "WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=? ORDER BY RANK ASC",
            variation.id.underlying())
        payoutIds.size should equal(3)
        var index = 1
        payoutIds.map(_.get("TOURNAMENT_VARIATION_PAYOUT_ID")).foreach {
            payoutId =>
                val payoutMap = jdbcTemplate.queryForMap(
                    "SELECT * FROM TOURNAMENT_VARIATION_PAYOUT WHERE TOURNAMENT_VARIATION_PAYOUT_ID=?",
                    payoutId)
                payoutMap.get("RANK") should equal(index)
                payoutMap.get("PAYOUT") should equal(BigDecimal(index * 0.1)
                    .setScale(8, BigDecimal.RoundingMode.HALF_EVEN).underlying())
                index += 1
        }
    }

    @Test def savingAnExistingVariationDeletesRemovedPayoutsFromTheDatabase() {
        val variation = underTest.save(aVariation()
            .withPayouts(List(aPayout(1), aPayout(2), aPayout(3))))
        val updatedVariation = variation.withPayouts(variation.payouts.slice(1, 3))

        underTest.save(updatedVariation)

        val payoutIds = jdbcTemplate.queryForList(
            "SELECT TOURNAMENT_VARIATION_PAYOUT_ID FROM TOURNAMENT_VARIATION_PAYOUT "
                + "WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=? ORDER BY RANK ASC",
            variation.id.underlying())
        payoutIds.size should equal(2)
        var index = 2
        payoutIds.map(_.get("TOURNAMENT_VARIATION_PAYOUT_ID")).foreach {
            payoutId =>
                val payoutMap = jdbcTemplate.queryForMap(
                    "SELECT * FROM TOURNAMENT_VARIATION_PAYOUT WHERE TOURNAMENT_VARIATION_PAYOUT_ID=?",
                    payoutId)
                payoutMap.get("RANK") should equal(index)
                payoutMap.get("PAYOUT") should equal(BigDecimal(index * 0.1)
                    .setScale(8, BigDecimal.RoundingMode.HALF_EVEN).underlying())
                index += 1
        }
    }

    @Test def savingAnExistingVariationUpdatesAttachedRoundsInTheDatabase() {
        val variation = underTest.save(aVariation()
            .withRounds(List(aRound(1), aRound(2))))
        val round1 = variation.rounds.get(0)
        val updatedRound1 = new TournamentVariationRound(round1.id, 3, 60, 500, 5,
            "Red Blackjack", BigDecimal(200), "anotherTestRound")
        val round2 = variation.rounds.get(1)
        val updatedRound2 = new TournamentVariationRound(round2.id, 4, 60, 500, 5,
            "Red Blackjack", BigDecimal(200), "anotherTestRound")

        underTest.save(variation.withRounds(List(updatedRound1, updatedRound2)))

        val roundIds = jdbcTemplate.queryForList(
            "SELECT TOURNAMENT_VARIATION_ROUND_ID FROM TOURNAMENT_VARIATION_ROUND "
                + "WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=? ORDER BY ROUND_NUMBER ASC",
            variation.id.underlying())
        roundIds.size should equal(2)
        var index = 3
        roundIds.map(_.get("TOURNAMENT_VARIATION_ROUND_ID")).foreach {
            roundId =>
                val roundMap = jdbcTemplate.queryForMap(
                    "SELECT * FROM TOURNAMENT_VARIATION_ROUND WHERE TOURNAMENT_VARIATION_ROUND_ID=?",
                    roundId)
                roundMap.get("ROUND_NUMBER") should equal(index)
                roundMap.get("ROUND_END_INTERVAL") should equal(60)
                roundMap.get("ROUND_LENGTH") should equal(500)
                roundMap.get("GAME_VARIATION_TEMPLATE_ID") should equal(5)
                roundMap.get("CLIENT_PROPERTIES_ID") should equal("Red Blackjack")
                roundMap.get("MINIMUM_BALANCE") should equal(BigDecimal("200.0000").underlying())
                roundMap.get("DESCRIPTION") should equal("anotherTestRound")

                index += 1
        }
    }

    @Test def savingAnExistingVariationAddsNewRoundsToTheDatabase() {
        val variation = underTest.save(aVariation()
            .withRounds(List(aRound(1), aRound(2))))
        val updatedVariation = variation.withRounds(aRound(3) :: variation.rounds.toList)

        underTest.save(updatedVariation)

        val roundIds = jdbcTemplate.queryForList(
            "SELECT TOURNAMENT_VARIATION_ROUND_ID FROM TOURNAMENT_VARIATION_ROUND "
                + "WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=? ORDER BY ROUND_NUMBER ASC",
            variation.id.underlying())
        roundIds.size should equal(3)
        var index = 1
        roundIds.map(_.get("TOURNAMENT_VARIATION_ROUND_ID")).foreach {
            roundId =>
                val roundMap = jdbcTemplate.queryForMap(
                    "SELECT * FROM TOURNAMENT_VARIATION_ROUND WHERE TOURNAMENT_VARIATION_ROUND_ID=?",
                    roundId)
                roundMap.get("ROUND_NUMBER") should equal(index)
                roundMap.get("ROUND_END_INTERVAL") should equal(30)
                roundMap.get("ROUND_LENGTH") should equal(300)
                roundMap.get("GAME_VARIATION_TEMPLATE_ID") should equal(5)
                roundMap.get("CLIENT_PROPERTIES_ID") should equal("Blue Blackjack")
                roundMap.get("MINIMUM_BALANCE") should equal(BigDecimal("100.0000").underlying())
                roundMap.get("DESCRIPTION") should equal("aTestRound")

                index += 1
        }
    }

    @Test def savingAnExistingVariationDeletesRemovedRoundsFromTheDatabase() {
        val variation = underTest.save(aVariation()
            .withRounds(List(aRound(1), aRound(2), aRound(3))))
        val updatedVariation = variation.withRounds(variation.rounds.slice(1, 3))

        underTest.save(updatedVariation)

        val roundIds = jdbcTemplate.queryForList(
            "SELECT TOURNAMENT_VARIATION_ROUND_ID FROM TOURNAMENT_VARIATION_ROUND "
                + "WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=? ORDER BY ROUND_NUMBER ASC",
            variation.id.underlying())
        roundIds.size should equal(2)
        var index = 2
        roundIds.map(_.get("TOURNAMENT_VARIATION_ROUND_ID")).foreach {
            roundId =>
                val roundMap = jdbcTemplate.queryForMap(
                    "SELECT * FROM TOURNAMENT_VARIATION_ROUND WHERE TOURNAMENT_VARIATION_ROUND_ID=?",
                    roundId)
                roundMap.get("ROUND_NUMBER") should equal(index)
                roundMap.get("ROUND_END_INTERVAL") should equal(30)
                roundMap.get("ROUND_LENGTH") should equal(300)
                roundMap.get("GAME_VARIATION_TEMPLATE_ID") should equal(5)
                roundMap.get("CLIENT_PROPERTIES_ID") should equal("Blue Blackjack")
                roundMap.get("MINIMUM_BALANCE") should equal(BigDecimal("100.0000").underlying())
                roundMap.get("DESCRIPTION") should equal("aTestRound")

                index += 1
        }
    }

    @Test def findingANonExistentVariationReturnsNone() {
        val result = underTest.findById(BigDecimal(-1))

        result should equal(None)
    }

    @Test def findingAnExistingVariationReturnsTheVariation() {
        val variation = underTest.save(aVariation()
            .withRounds(List(aRound(1)))
            .withPayouts(List(aPayout(1), aPayout(2))))

        val result = underTest.findById(variation.id)

        result should equal(Some(variation))
    }

    @Test def findingAllVariationsReturnsAKnownVariation() {
        val variation = underTest.save(aVariation())

        val allUsers = underTest.findAll(0, Integer.MAX_VALUE)

        allUsers.getTotalSize should (be >= 1)
        allUsers.getData should contain(variation)
    }

    @Test def deletingAVariationRemovesItFromTheDatabase() {
        val variation = underTest.save(aVariation())

        underTest.delete(variation.id)

        val recordCount = jdbcTemplate.queryForInt(
            "SELECT COUNT(*) FROM TOURNAMENT_VARIATION_TEMPLATE WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=?",
            variation.id.underlying())
        recordCount should equal(0)
    }

    private def aVariation() =
        new TournamentVariation(null, TournamentType.PRESET, "aTestTournament", BigDecimal(100),
            BigDecimal(200), BigDecimal(1000), 3, 10, "BLACKJACK", 60 * 60 * 24, BigDecimal(1500),
            Allocator.EVEN_BY_BALANCE, List(), List())

    private def aRound(round: Int) =
        new TournamentVariationRound(null, round, 30, 300, BigDecimal(5),
            "Blue Blackjack", BigDecimal(100), "aTestRound")

    private def aPayout(rank: Int) =
        new TournamentVariationPayout(null, rank, BigDecimal(rank * 0.1))
}
