package com.yazino.platform.messaging.host;

import com.yazino.platform.community.TableInviteSummary;
import com.yazino.platform.messaging.Document;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.messaging.DocumentHeaderType;
import com.yazino.platform.messaging.DocumentType;
import com.yazino.platform.messaging.destination.Destination;
import com.yazino.platform.util.JsonHelper;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.Validate.notNull;

public class TableInviteHostDocument implements HostDocument {
    private static final long serialVersionUID = 7271638495429204483L;

    private static final JsonHelper JSON_HELPER = new JsonHelper();

    private final List<TableInviteSummary> invites = new ArrayList<TableInviteSummary>();
    private final Destination destination;

    public TableInviteHostDocument(final List<TableInviteSummary> invites,
                                   final Destination destination) {
        notNull(invites, "invites may not be null");
        notNull(destination, "destination may not be null");

        this.invites.addAll(invites);
        this.destination = destination;
    }

    @Override
    public void send(final DocumentDispatcher documentDispatcher) {
        notNull(documentDispatcher, "documentDispatcher may not be null");

        destination.send(new Document(DocumentType.TABLE_INVITES.getName(),
                JSON_HELPER.serialize(invites),
                singletonMap(DocumentHeaderType.IS_A_PLAYER.getHeader(), Boolean.toString(true))),
                documentDispatcher);
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
        final TableInviteHostDocument rhs = (TableInviteHostDocument) obj;
        return new EqualsBuilder()
                .append(invites, rhs.invites)
                .append(destination, rhs.destination)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(invites)
                .append(destination)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(invites)
                .append(destination)
                .toString();
    }

}
