package strata.server.lobby.controlcentre.form

import scala.beans.BeanProperty
import org.joda.time.{Duration, Interval, DateTime}
import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import java.util
import TrophyLeaderboardForm._
import com.yazino.platform.tournament.{TrophyLeaderboardView, TrophyLeaderboardPosition, TrophyLeaderboardDefinition}
import scala.collection.JavaConversions._
import java.lang.Integer

object TrophyLeaderboardForm {
    private val ONE_DAY = 86400000
}

class TrophyLeaderboardForm(@BeanProperty var id: BigDecimal,
                            @BeanProperty var name: String,
                            @BeanProperty var gameType: String,
                            @BeanProperty var enabled: Boolean,
                            @BeanProperty var pointBonus: Long,
                            @BeanProperty var startDate: util.Date,
                            @BeanProperty var endDate: util.Date,
                            @BeanProperty var cycle: TimePeriod,
                            @BeanProperty var positions: util.List[TrophyLeaderboardPositionForm]) {

    def this() {
        this(null, null, null, true, 0, new DateTime().toDate, new DateTime().plusWeeks(1).toDate,
            TimePeriod(ONE_DAY), new util.ArrayList[TrophyLeaderboardPositionForm]())
    }

    def this(leaderboard: TrophyLeaderboardDefinition) {
        this(null, leaderboard.getName,
            leaderboard.getGameType, leaderboard.getActive, leaderboard.getPointBonusPerPlayer,
            leaderboard.getValidInterval.getStart.toDate, leaderboard.getValidInterval.getEnd.toDate,
            TimePeriod(leaderboard.getCycle.getMillis),
            leaderboard.getPositionData.values().map(new TrophyLeaderboardPositionForm(_)).toSeq.sortBy(_.getPosition)
        )
    }

    def this(leaderboard: TrophyLeaderboardView) {
        this(leaderboard.getId, leaderboard.getName,
            leaderboard.getGameType, leaderboard.getActive, leaderboard.getPointBonusPerPlayer,
            leaderboard.getStartTime.toDate, leaderboard.getEndTime.toDate,
            TimePeriod(leaderboard.getCycle.getMillis),
            leaderboard.getPositionData.values().map(new TrophyLeaderboardPositionForm(_)).toSeq.sortBy(_.getPosition))
    }

    def toDefinition: TrophyLeaderboardDefinition = new TrophyLeaderboardDefinition(name, gameType,
        new Interval(new DateTime(startDate), new DateTime(endDate)),
        new Duration(cycle.milliseconds), pointBonus, positionsAsMap)

    def toView: TrophyLeaderboardView = new TrophyLeaderboardView(id.underlying(), name, enabled, gameType, pointBonus,
        new DateTime(startDate), new DateTime(endDate), new DateTime(endDate),
        new Duration(cycle.milliseconds), positionsAsMap, null)

    private def positionsAsMap = {
        val positionMap = new util.HashMap[Integer, TrophyLeaderboardPosition]()
        positions.foreach(position => positionMap.put(position.position, position.toPosition))
        positionMap
    }

    override def toString: String = new ToStringBuilder(this)
        .append(name)
        .append(gameType)
        .append(enabled)
        .append(pointBonus)
        .append(startDate)
        .append(endDate)
        .append(cycle)
        .append(positions)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: TrophyLeaderboardForm => other.getClass == getClass &&
            new EqualsBuilder()
                .append(name, other.name)
                .append(gameType, other.gameType)
                .append(enabled, other.enabled)
                .append(pointBonus, other.pointBonus)
                .append(startDate, other.startDate)
                .append(endDate, other.endDate)
                .append(cycle, other.cycle)
                .append(positions, other.positions)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(name)
        .append(gameType)
        .append(enabled)
        .append(pointBonus)
        .append(startDate)
        .append(endDate)
        .append(cycle)
        .append(positions)
        .hashCode


}
