package strata.server.lobby.controlcentre.repository

import mapper.{TournamentVariationPayoutMapper, TournamentVariationRoundMapper, TournamentVariationMapper}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.{PreparedStatementCreator, JdbcTemplate}
import strata.server.lobby.controlcentre.model.{TournamentVariationPayout, TournamentVariationRound, TournamentVariation}
import org.springframework.stereotype.Repository
import org.apache.commons.lang3.Validate._
import scala._
import com.yazino.platform.model.PagedData
import JDBCTournamentVariationRepository._
import java.sql.{PreparedStatement, Statement, Connection}
import scala.collection.JavaConversions._
import java.lang.{Integer => JavaInt, Long => JavaLong}
import scala.Some
import org.springframework.transaction.annotation.Transactional

object JDBCTournamentVariationRepository {
    private val SQL_SELECT = "SELECT * FROM TOURNAMENT_VARIATION_TEMPLATE WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=?"

    private val SQL_SELECT_ALL = "SELECT * FROM TOURNAMENT_VARIATION_TEMPLATE"

    private val SQL_SELECT_ROUNDS = "SELECT * FROM TOURNAMENT_VARIATION_ROUND WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=?"

    private val SQL_SELECT_PAYOUTS = "SELECT * FROM TOURNAMENT_VARIATION_PAYOUT WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=?"

    private val SQL_LIST = "SELECT TOURNAMENT_VARIATION_TEMPLATE_ID,NAME FROM TOURNAMENT_VARIATION_TEMPLATE"

    private val SQL_INSERT = """
            INSERT INTO TOURNAMENT_VARIATION_TEMPLATE
            (TOURNAMENT_TYPE,NAME,ENTRY_FEE,SERVICE_FEE,STARTING_CHIPS,MIN_PLAYERS,MAX_PLAYERS,
            GAME_TYPE,EXPIRY_DELAY,PRIZE_POOL,ALLOCATOR)
            VALUES
            (?,?,?,?,?,?,?,?,?,?,?)"""

    private val SQL_UPDATE = """
            UPDATE TOURNAMENT_VARIATION_TEMPLATE SET
                TOURNAMENT_TYPE=?,NAME=?,ENTRY_FEE=?,SERVICE_FEE=?,STARTING_CHIPS=?,MIN_PLAYERS=?,
                MAX_PLAYERS=?,GAME_TYPE=?,EXPIRY_DELAY=?,PRIZE_POOL=?,ALLOCATOR=?
            WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=?"""

    private val SQL_DELETE
            = "DELETE FROM TOURNAMENT_VARIATION_TEMPLATE WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=?"

    private val SQL_INSERT_ROUND = """
            INSERT INTO TOURNAMENT_VARIATION_ROUND
            (TOURNAMENT_VARIATION_TEMPLATE_ID,ROUND_NUMBER,ROUND_END_INTERVAL,ROUND_LENGTH,
            GAME_VARIATION_TEMPLATE_ID,CLIENT_PROPERTIES_ID,MINIMUM_BALANCE,DESCRIPTION)
            VALUES
            (?,?,?,?,?,?,?,?)"""

    private val SQL_UPDATE_ROUND = """
            UPDATE TOURNAMENT_VARIATION_ROUND SET
                ROUND_NUMBER=?,ROUND_END_INTERVAL=?,ROUND_LENGTH=?,
                GAME_VARIATION_TEMPLATE_ID=?,CLIENT_PROPERTIES_ID=?,MINIMUM_BALANCE=?,DESCRIPTION=?
            WHERE TOURNAMENT_VARIATION_ROUND_ID=?"""

    private val SQL_DELETE_OLD_ROUNDS =
        "DELETE FROM TOURNAMENT_VARIATION_ROUND WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=? AND TOURNAMENT_VARIATION_ROUND_ID NOT IN (%s)"

    private val SQL_DELETE_ROUNDS
            = "DELETE FROM TOURNAMENT_VARIATION_ROUND WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=?"

    private val SQL_INSERT_PAYOUT =
        "INSERT INTO TOURNAMENT_VARIATION_PAYOUT (TOURNAMENT_VARIATION_TEMPLATE_ID,RANK,PAYOUT) VALUES (?,?,?)"

    private val SQL_UPDATE_PAYOUT =
        "UPDATE TOURNAMENT_VARIATION_PAYOUT SET RANK=?,PAYOUT=? WHERE TOURNAMENT_VARIATION_PAYOUT_ID=?"

    private val SQL_DELETE_OLD_PAYOUTS =
        "DELETE FROM TOURNAMENT_VARIATION_PAYOUT WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=? AND TOURNAMENT_VARIATION_PAYOUT_ID NOT IN (%s)"

    private val SQL_DELETE_PAYOUTS =
        "DELETE FROM TOURNAMENT_VARIATION_PAYOUT WHERE TOURNAMENT_VARIATION_TEMPLATE_ID=?"

    private val DEFAULT_PAGE_SIZE = 20
}

@Repository
class JDBCTournamentVariationRepository @Autowired()(val jdbcTemplate: JdbcTemplate)
    extends TournamentVariationRepository with MySQLPaging[TournamentVariation] {

    private val variationMapper = new TournamentVariationMapper
    private val roundMapper = new TournamentVariationRoundMapper
    private val payoutMapper = new TournamentVariationPayoutMapper

    def findById(id: BigDecimal): Option[TournamentVariation] = {
        notNull(id, "id may not be null")

        val variations = jdbcTemplate.query(SQL_SELECT, variationMapper, id.underlying())
        if (variations != null && !variations.isEmpty) {
            Some(variations.get(0)).map(populateVariation(_))
        } else {
            None
        }
    }

    @Transactional(readOnly = true)
    def findAll(page: Int = 0, pageSize: Int = DEFAULT_PAGE_SIZE): PagedData[TournamentVariation] = {
        var pagedData = selectWithPaging(jdbcTemplate, SQL_SELECT_ALL, variationMapper, page, pageSize)
        new PagedData(pagedData.getStartPosition, pagedData.getSize, pagedData.getTotalSize,
            pagedData.getData.map(populateVariation(_)))
    }

    def save(variation: TournamentVariation): TournamentVariation = {
        notNull(variation, "variation may not be null")

        if (variation.id == null) {
            val variationId = BigDecimal(jdbcTemplate.execute(new PreparedStatementCreator {
                def createPreparedStatement(conn: Connection): PreparedStatement = {
                    val stmt = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)
                    stmt.setString(1, asString(variation.tournamentType))
                    stmt.setString(2, variation.name)
                    stmt.setBigDecimal(3, asJavaBigDecimal(variation.entryFee))
                    stmt.setBigDecimal(4, asJavaBigDecimal(variation.serviceFee))
                    stmt.setBigDecimal(5, asJavaBigDecimal(variation.startingChips))
                    stmt.setInt(6, variation.minPlayers)
                    stmt.setInt(7, variation.maxPlayers)
                    stmt.setString(8, variation.gameType)
                    stmt.setLong(9, variation.expiryDelay)
                    stmt.setBigDecimal(10, asJavaBigDecimal(variation.prizePool))
                    stmt.setString(11, asString(variation.allocator))
                    stmt
                }
            }, new PrimaryKeyLoader()))

            variation.withId(variationId)
                    .withPayouts(savePayouts(variationId, variation.payouts))
                    .withRounds(saveRounds(variationId, variation.rounds))

        } else {
            jdbcTemplate.update(SQL_UPDATE, asString(variation.tournamentType), variation.name,
                asJavaBigDecimal(variation.entryFee), asJavaBigDecimal(variation.serviceFee),
                asJavaBigDecimal(variation.startingChips), new JavaInt(variation.minPlayers),
                new JavaInt(variation.maxPlayers), variation.gameType, new JavaLong(variation.expiryDelay),
                asJavaBigDecimal(variation.prizePool), asString(variation.allocator),
                variation.id.underlying())

            variation.withPayouts(savePayouts(variation.id, variation.payouts))
                .withRounds(saveRounds(variation.id, variation.rounds))
        }
    }

    def delete(id: BigDecimal) {
        notNull(id, "id may not be null")

        jdbcTemplate.update(SQL_DELETE_PAYOUTS, id.underlying())
        jdbcTemplate.update(SQL_DELETE_ROUNDS, id.underlying())
        jdbcTemplate.update(SQL_DELETE, id.underlying())
    }

    def list(): Map[BigDecimal, String] = {
        var templateMap = collection.mutable.Map[BigDecimal, String]()
        jdbcTemplate.queryForList(SQL_LIST).foreach{
            row => templateMap += BigDecimal(
                row.get("TOURNAMENT_VARIATION_TEMPLATE_ID").toString) -> row.get("NAME").toString
        }
        templateMap.toMap
    }

    private def asString(value: Any) = if (value != null) value.toString else null

    private def asJavaBigDecimal(value: BigDecimal) = if (value != null) value.underlying() else null

    private def saveRounds(variationId: BigDecimal, rounds: Iterable[TournamentVariationRound]) = {
        val roundIds = rounds.filter(_.id != null).map(_.id)
        if (roundIds.isEmpty) {
            jdbcTemplate.update(SQL_DELETE_ROUNDS, variationId.underlying())
        } else {
            jdbcTemplate.update(SQL_DELETE_OLD_ROUNDS.format(roundIds.mkString(",")), variationId.underlying())
        }

        rounds.map{round =>
            if (round.id == null) {
                val roundId = BigDecimal(jdbcTemplate.execute(new PreparedStatementCreator {
                    def createPreparedStatement(conn: Connection): PreparedStatement = {
                        val stmt = conn.prepareStatement(SQL_INSERT_ROUND, Statement.RETURN_GENERATED_KEYS)
                        stmt.setBigDecimal(1, asJavaBigDecimal(variationId))
                        stmt.setInt(2, round.number)
                        stmt.setInt(3, round.endInterval)
                        stmt.setInt(4, round.length)
                        stmt.setBigDecimal(5, asJavaBigDecimal(round.gameVariationId))
                        stmt.setString(6, round.clientPropertiesId)
                        stmt.setBigDecimal(7, asJavaBigDecimal(round.minimumBalance))
                        stmt.setString(8, round.description)
                        stmt
                    }
                }, new PrimaryKeyLoader()))
                round.withId(roundId)

            } else {
                jdbcTemplate.update(SQL_UPDATE_ROUND, new JavaInt(round.number), new JavaInt(round.endInterval),
                    new JavaInt(round.length), asJavaBigDecimal(round.gameVariationId),
                    round.clientPropertiesId, asJavaBigDecimal(round.minimumBalance),
                    round.description, round.id.underlying())
                round
            }
        }
    }

    private def savePayouts(variationId: BigDecimal, payouts: Iterable[TournamentVariationPayout]) = {
        val payoutIds = payouts.filter(_.id != null).map(_.id)
        if (payoutIds.isEmpty) {
            jdbcTemplate.update(SQL_DELETE_PAYOUTS, variationId.underlying())
        } else {
            jdbcTemplate.update(SQL_DELETE_OLD_PAYOUTS.format(payoutIds.mkString(",")), variationId.underlying())
        }

        payouts.map{payout =>
            if (payout.id == null) {
                val payoutId = BigDecimal(jdbcTemplate.execute(new PreparedStatementCreator {
                    def createPreparedStatement(conn: Connection): PreparedStatement = {
                        val stmt = conn.prepareStatement(SQL_INSERT_PAYOUT, Statement.RETURN_GENERATED_KEYS)
                        stmt.setBigDecimal(1, asJavaBigDecimal(variationId))
                        stmt.setInt(2, payout.rank)
                        stmt.setBigDecimal(3, asJavaBigDecimal(payout.payout))
                        stmt
                    }
                }, new PrimaryKeyLoader()))
                payout.withId(payoutId)

            } else {
                jdbcTemplate.update(SQL_UPDATE_PAYOUT, new JavaInt(payout.rank),
                    asJavaBigDecimal(payout.payout), payout.id.underlying())
                payout
            }
        }
    }

    private def populateVariation(variation: TournamentVariation) = {
        val rounds = jdbcTemplate.query(SQL_SELECT_ROUNDS, roundMapper, variation.id.underlying())
        val payouts = jdbcTemplate.query(SQL_SELECT_PAYOUTS, payoutMapper, variation.id.underlying())
        variation
            .withRounds(rounds)
            .withPayouts(payouts)
    }

}
