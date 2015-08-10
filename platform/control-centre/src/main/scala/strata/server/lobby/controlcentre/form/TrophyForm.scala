package strata.server.lobby.controlcentre.form

import scala.beans.BeanProperty
import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import com.yazino.platform.community.Trophy
import java.math.{BigDecimal => JavaBigDecimal}
import scala._

class TrophyForm(@BeanProperty var id: BigDecimal,
                 @BeanProperty var image: String,
                 @BeanProperty var name: String,
                 @BeanProperty var gameType: String,
                 @BeanProperty var message: String,
                 @BeanProperty var shortDescription: String,
                 @BeanProperty var cabinetDescription: String) {

    def this() {
        this(null, null, null, "BLACKJACK", null, null, null)
    }

    def this(trophy: Trophy) {
        this(if (trophy.getId != null) {BigDecimal(trophy.getId)} else null,
            trophy.getImage, trophy.getName, trophy.getGameType, trophy.getMessage,
            trophy.getShortDescription, trophy.getMessageCabinet)
    }

    def toTrophy: Trophy = {
        val trophy = new Trophy()
        if (id != null) {
            trophy.setId(id.underlying())
        }
        trophy.setName(name)
        trophy.setImage(image)
        trophy.setGameType(gameType)
        trophy.setMessage(message)
        trophy.setShortDescription(shortDescription)
        trophy.setMessageCabinet(cabinetDescription)
        trophy
    }

    override def toString: String = new ToStringBuilder(this)
        .append(id)
        .append(image)
        .append(name)
        .append(gameType)
        .append(message)
        .append(shortDescription)
        .append(cabinetDescription)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: TrophyForm => other.getClass == getClass &&
            new EqualsBuilder()
                .append(id, other.id)
                .append(image, other.image)
                .append(name, other.name)
                .append(gameType, other.gameType)
                .append(message, other.message)
                .append(shortDescription, other.shortDescription)
                .append(cabinetDescription, other.cabinetDescription)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(id)
        .append(image)
        .append(name)
        .append(gameType)
        .append(message)
        .append(shortDescription)
        .append(cabinetDescription)
        .hashCode

}
