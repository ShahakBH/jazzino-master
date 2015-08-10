package com.yazino.platform.messaging.destination;

import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.Validate.notNull;

public class ObserversDestination implements Destination {
    private static final long serialVersionUID = 6776980190678219841L;

    private static final Logger LOG = LoggerFactory.getLogger(ObserversDestination.class);

    @Override
    public void send(final Document document,
                     final DocumentDispatcher documentDispatcher) {
        notNull(document, "Document may not be null");
        notNull(documentDispatcher, "documentDispatcher may not be null");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Dispatching document to observers: "
                    + ToStringBuilder.reflectionToString(document));
        }

        documentDispatcher.dispatch(document);
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
        return true;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).toString();
    }

}
