package strata.server.lobby.controlcentre.util

import java.beans.PropertyEditorSupport
import scala.BigDecimal
import org.apache.commons.lang3.StringUtils

class BigDecimalPropertyEditorSupport extends PropertyEditorSupport {

    override def getAsText: String = if (getValue != null) {
        getValue.asInstanceOf[BigDecimal].underlying().toPlainString
    }
    else {
        ""
    }

    override def setAsText(text: String) {
        setValue(asBigDecimal(text))
    }

    private def asBigDecimal(value: String) = if (StringUtils.isNotBlank(value)) BigDecimal(value) else null

}
