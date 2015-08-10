package com.yazino.platform.messaging;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

@SpaceClass(replicate = false)
public class DocumentWrapper implements Serializable {
    private static final long serialVersionUID = 2974977586851441029L;

    private String spaceId;
    private Set<BigDecimal> recipients;
    private Document document;
    private BigDecimal playerId;

    public DocumentWrapper() {
    }

    public DocumentWrapper(final Document document) {
        this.document = document;
    }

    public DocumentWrapper(final Document document,
                           final Set<BigDecimal> recipients) {
        this.recipients = recipients;
        this.document = document;
        if (recipients != null && recipients.size() == 1) {
            this.playerId = recipients.iterator().next();
        }
    }

    @SpaceId(autoGenerate = true)
    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(final String spaceId) {
        this.spaceId = spaceId;
    }

    public Set<BigDecimal> getRecipients() {
        return recipients;
    }

    public void setRecipients(final Set<BigDecimal> recipients) {
        this.recipients = recipients;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(final Document document) {
        this.document = document;
    }

    @SpaceRouting
    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
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
        final DocumentWrapper rhs = (DocumentWrapper) obj;
        return new EqualsBuilder()
                .append(spaceId, rhs.spaceId)
                .append(playerId, rhs.playerId)
                .append(recipients, rhs.recipients)
                .append(document, rhs.document)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(spaceId)
                .append(playerId)
                .append(recipients)
                .append(document)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(spaceId)
                .append(playerId)
                .append(recipients)
                .append(document)
                .toString();
    }
}
