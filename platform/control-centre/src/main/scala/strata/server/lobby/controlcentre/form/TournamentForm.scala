package strata.server.lobby.controlcentre.form

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import com.yazino.platform.tournament.TournamentStatus
import scala.beans.BeanProperty
import strata.server.lobby.controlcentre.model.Tournament
import java.util
import org.joda.time.DateTime
import TournamentForm._

object TournamentForm {
    private val CANNOT_CANCEL_STATUSES = Set(TournamentStatus.ERROR, TournamentStatus.CLOSED,
        TournamentStatus.CANCELLED, TournamentStatus.SETTLED, TournamentStatus.FINISHED)

    def cannotCancelStatuses: Set[TournamentStatus] = CANNOT_CANCEL_STATUSES
}

class TournamentForm(@BeanProperty var id: BigDecimal,
                     @BeanProperty var name: String,
                     @BeanProperty var variationId: BigDecimal,
                     @BeanProperty var signupStart: util.Date,
                     @BeanProperty var signupEnd: util.Date,
                     @BeanProperty var start: util.Date,
                     @BeanProperty var status: TournamentStatus,
                     @BeanProperty var partnerId: String,
                     @BeanProperty var description: String) {

    def this() {
        this(null, null, null, new DateTime().toDate, new DateTime().plusDays(1).toDate,
            new DateTime().plusDays(1).toDate, TournamentStatus.ANNOUNCED, "PLAY_FOR_FUN", null)
    }

    def this(tournament: Tournament) {
        this(tournament.id, tournament.name, tournament.variationId, tournament.signupStart.toDate,
            tournament.signupEnd.toDate, tournament.start.toDate, tournament.status,
            tournament.partnerId, tournament.description)
    }

    def toTournament: Tournament = new Tournament(id, name, variationId, toDateTime(signupStart),
        toDateTime(signupEnd), toDateTime(start), status, partnerId, description)

    def canCancel: Boolean = status != null && !CANNOT_CANCEL_STATUSES.contains(status)

    private def toDateTime(date: util.Date): DateTime = if (date != null) new DateTime(date.getTime) else null

    override def toString: String = new ToStringBuilder(this)
        .append(id)
        .append(name)
        .append(variationId)
        .append(signupStart)
        .append(signupEnd)
        .append(start)
        .append(status)
        .append(partnerId)
        .append(description)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: TournamentForm => other.getClass == getClass &&
            new EqualsBuilder()
                .append(id, other.id)
                .append(name, other.name)
                .append(variationId, other.variationId)
                .append(signupStart, other.signupStart)
                .append(signupEnd, other.signupEnd)
                .append(start, other.start)
                .append(status, other.status)
                .append(partnerId, other.partnerId)
                .append(description, other.description)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(id)
        .append(name)
        .append(variationId)
        .append(signupStart)
        .append(signupEnd)
        .append(start)
        .append(status)
        .append(partnerId)
        .append(description)
        .hashCode

}
