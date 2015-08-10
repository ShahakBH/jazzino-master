package strata.server.lobby.controlcentre.form

import scala.beans.BeanProperty
import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import com.yazino.platform.tournament.TrophyLeaderboardPosition
import java.math

class TrophyLeaderboardPositionForm(@BeanProperty var position: Int,
                                    @BeanProperty var awardPoints: Long,
                                    @BeanProperty var awardPayout: Long,
                                    @BeanProperty var trophyId: BigDecimal) {

    def this() {
        this(1, 0, 0, null)
    }

    def this(position: TrophyLeaderboardPosition) {
        this(position.getPosition, position.getAwardPoints, position.getAwardPayout,
            if (position.getTrophyId != null) position.getTrophyId else null)
    }

    def toPosition: TrophyLeaderboardPosition = new TrophyLeaderboardPosition(position, awardPoints, awardPayout, filterToJava(trophyId))

    private def filterToJava(number: BigDecimal): math.BigDecimal =
        if (number != null && number >= 0) number.underlying() else null

    override def toString: String = new ToStringBuilder(this)
        .append(position)
        .append(awardPoints)
        .append(awardPayout)
        .append(trophyId)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: TrophyLeaderboardPositionForm => other.getClass == getClass &&
            new EqualsBuilder()
                .append(position, other.position)
                .append(awardPoints, other.awardPoints)
                .append(awardPayout, other.awardPayout)
                .append(trophyId, other.trophyId)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(position)
        .append(awardPoints)
        .append(awardPayout)
        .append(trophyId)
        .hashCode

}
