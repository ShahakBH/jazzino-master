package com.yazino.web.domain;

import com.yazino.web.service.TournamentViewDocumentWorker;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;

public class TournamentDocumentRequest {
    private final TournamentViewDocumentWorker.DocumentType requestType;
    private final Integer pageNumber;
    private final Integer pageSize;
    private final BigDecimal playerId;

    public TournamentDocumentRequest(final TournamentViewDocumentWorker.DocumentType requestType,
                                     final BigDecimal playerId,
                                     final Integer pageNumber,
                                     final Integer pageSize) {
        this.requestType = requestType;
        this.playerId = playerId;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public TournamentViewDocumentWorker.DocumentType getRequestType() {
        return requestType;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
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
        final TournamentDocumentRequest rhs = (TournamentDocumentRequest) obj;
        return new EqualsBuilder()
                .append(requestType, rhs.requestType)
                .append(playerId, rhs.playerId)
                .append(pageNumber, rhs.pageNumber)
                .append(pageSize, rhs.pageSize)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(requestType)
                .append(playerId)
                .append(pageNumber)
                .append(pageSize)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(requestType)
                .append(playerId)
                .append(pageNumber)
                .append(pageSize)
                .toString();
    }
}
