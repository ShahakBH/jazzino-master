package com.yazino.platform.messaging.host;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Collection;

import static org.apache.commons.lang3.Validate.notEmpty;

@SpaceClass(replicate = false)
public class HostDocumentWrapper implements Serializable {
    private static final long serialVersionUID = 3830990725524160669L;

    private String requestId;
    private Collection<HostDocument> hostDocuments;

    public HostDocumentWrapper() {
    }

    public HostDocumentWrapper(final Collection<HostDocument> hostDocuments) {
        notEmpty(hostDocuments, "Documents may not be null/empty");

        this.hostDocuments = hostDocuments;
    }

    @SpaceId(autoGenerate = true)
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    public Collection<HostDocument> getHostDocuments() {
        return hostDocuments;
    }

    public void setHostDocuments(final Collection<HostDocument> hostDocuments) {
        this.hostDocuments = hostDocuments;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final HostDocumentWrapper rhs = (HostDocumentWrapper) obj;
        return new EqualsBuilder()
                .append(requestId, rhs.requestId)
                .append(hostDocuments, rhs.hostDocuments)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 19)
                .append(requestId)
                .append(hostDocuments)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(requestId)
                .append(hostDocuments)
                .toString();
    }
}
