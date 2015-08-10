package strata.server.lobby.controlcentre.form

import scala.collection.JavaConversions._
import scala.beans.BeanProperty
import org.apache.commons.lang.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import com.yazino.platform.table.{GameConfigurationProperty, GameConfiguration}
import java.util

class GameConfigurationForm(@BeanProperty var gameId: String,
                            @BeanProperty var shortName: String,
                            @BeanProperty var displayName: String,
                            @BeanProperty var properties: util.List[GameConfigurationPropertyForm],
                            @BeanProperty var aliases: String,
                            @BeanProperty var order: Int) {

    def this() {
        this(null, null, null, new util.ArrayList[GameConfigurationPropertyForm](), null, 0)
    }

    def this(gameConfiguration: GameConfiguration) {
        this(gameConfiguration.getGameId,
            gameConfiguration.getShortName,
            gameConfiguration.getDisplayName,
            gameConfiguration.getProperties.map {
                new GameConfigurationPropertyForm(_)
            }.toList,
            gameConfiguration.getAliases.mkString(","),
            Int.box(gameConfiguration.getOrder))
    }

    def toGameConfiguration: GameConfiguration = {
        if (shortName == null) {
            shortName = "temporary"
        }
        new GameConfiguration(gameId, shortName, displayName, convert(aliases), order)
            .withProperties(new util.HashSet[GameConfigurationProperty](properties.map({
            _.toGameConfigurationProperty
        }).toList))
    }


    def convert(aliases: String): util.List[String] = {
        if (aliases == null || aliases.isEmpty) {
            util.Collections.emptyList()
        } else {
            new util.ArrayList[String](aliases.split(",").toList)
        }
    }

    override def toString: String = new ToStringBuilder(this)
        .append(gameId)
        .append(shortName)
        .append(displayName)
        .append(properties)
        .append(aliases)
        .append(order)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: GameConfigurationForm => other.getClass == getClass &&
            new EqualsBuilder()
                .append(gameId, other.gameId)
                .append(shortName, other.shortName)
                .append(displayName, other.displayName)
                .append(properties, other.properties)
                .append(aliases, other.aliases)
                .append(order, other.order)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(gameId)
        .append(shortName)
        .append(displayName)
        .append(properties)
        .append(aliases)
        .append(order)
        .hashCode
}
