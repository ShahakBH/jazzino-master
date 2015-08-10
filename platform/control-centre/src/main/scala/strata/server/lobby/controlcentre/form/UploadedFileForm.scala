package strata.server.lobby.controlcentre.form

import org.apache.commons.lang3.builder.{HashCodeBuilder, EqualsBuilder, ToStringBuilder}
import scala.beans.BeanProperty
import org.springframework.web.multipart.MultipartFile

class UploadedFileForm(@BeanProperty var name: String,
                       @BeanProperty var fileData: MultipartFile) {

    def this() {
        this(null, null)
    }

    override def toString: String = new ToStringBuilder(this)
        .append(name)
        .append(fileData)
        .toString

    override def equals(obj: Any): Boolean = obj match {
        case other: UploadedFileForm => other.getClass == getClass &&
            new EqualsBuilder()
                .append(name, other.name)
                .append(fileData, other.fileData)
                .isEquals
        case _ => false
    }

    override def hashCode: Int = new HashCodeBuilder()
        .append(name)
        .append(fileData)
        .hashCode
}
