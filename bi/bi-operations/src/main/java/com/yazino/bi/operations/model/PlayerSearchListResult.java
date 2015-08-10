package com.yazino.bi.operations.model;

import com.yazino.platform.model.PagedData;
import com.yazino.platform.player.PlayerSearchResult;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.Validate.notBlank;

/**
 * Results of the players search, when the search returns a list
 */
public class PlayerSearchListResult extends PlayerSearchRequest {
    private final PagedData<PlayerSearchResult> playersList;
    private final String message;

    public PlayerSearchListResult(final DashboardParameters source,
                                  final String message) {
        notBlank(message, "message may not be null/blank");

        setFirstRecord(source.getFirstRecord());
        setPageSize(source.getPageSize());
        setQuery(source.getQuery());

        this.message = message;
        this.playersList = PagedData.empty();
    }

    public PlayerSearchListResult(final DashboardParameters source,
                                  final PagedData<PlayerSearchResult> playersList) {
        setFirstRecord(source.getFirstRecord());
        setPageSize(source.getPageSize());
        setQuery(source.getQuery());

        if (playersList != null) {
            this.playersList = playersList;
        } else {
            this.playersList = PagedData.empty();
        }

        this.message = null;
    }

    public PagedData<PlayerSearchResult> getPlayersList() {
        return playersList;
    }

    public String getMessage() {
        return message;
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
        final PlayerSearchListResult rhs = (PlayerSearchListResult) obj;
        return new EqualsBuilder()
                .append(playersList, rhs.playersList)
                .append(message, rhs.message)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(playersList)
                .append(message)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append(playersList)
                .append(message)
                .toString();
    }
}
