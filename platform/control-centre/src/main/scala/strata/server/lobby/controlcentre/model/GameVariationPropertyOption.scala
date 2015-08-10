package strata.server.lobby.controlcentre.model

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import scala.beans.BeanProperty
import java.util.{Map => JavaMap, HashMap => JavaHashMap}

class GameVariationPropertyOption(val propertyName: String) {

    @BeanProperty var availableOptions: JavaMap[String, String] = new JavaHashMap()
    @BeanProperty var defaultValue: String = null
    @BeanProperty var required: Boolean = false
    @BeanProperty var valueType: String = "string"
    @BeanProperty var multiple: Boolean = false
    @BeanProperty var toolTip: String = ""

    override def toString: String = new ToStringBuilder(this)
        .append(propertyName)
        .append(defaultValue)
        .append(availableOptions)
        .append(required)
        .append(toolTip)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: GameVariationPropertyOption => other.getClass == getClass &&
            new EqualsBuilder()
                .append(propertyName, other.propertyName)
                .append(defaultValue, other.defaultValue)
                .append(availableOptions, other.availableOptions)
                .append(required, other.required)
                .append(toolTip, other.toolTip)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(propertyName)
        .append(defaultValue)
        .append(availableOptions)
        .append(required)
        .append(toolTip)
        .hashCode

}
