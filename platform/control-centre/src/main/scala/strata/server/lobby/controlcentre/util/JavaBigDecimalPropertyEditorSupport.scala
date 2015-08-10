package strata.server.lobby.controlcentre.util

import java.beans.PropertyEditorSupport
import java.math.BigDecimal

class JavaBigDecimalPropertyEditorSupport extends PropertyEditorSupport {

  override def getAsText: String = getValue.toString

  override def setAsText(text: String) {
    setValue(new BigDecimal(text))
  }

}
