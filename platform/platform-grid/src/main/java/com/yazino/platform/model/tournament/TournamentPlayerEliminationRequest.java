package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass(replicate = false)
public class TournamentPlayerEliminationRequest implements Serializable {
    private static final long serialVersionUID = 8303942300348880066L;

    private BigDecimal tournamentId;
    private BigDecimal playerId;
    private String gameType;
    private Integer numberOfPlayers;
    private Integer leaderBoardPosition;

    public TournamentPlayerEliminationRequest() {
    }

    public TournamentPlayerEliminationRequest(final BigDecimal tournamentId,
                                              final BigDecimal playerId,
                                              final String gameType,
                                              final int numberOfPlayers,
                                              final int leaderBoardPosition) {
        notNull(tournamentId, "tournamentId may not be null");
        notNull(playerId, "playerId may not be null");

        this.tournamentId = tournamentId;
        this.playerId = playerId;
        this.gameType = gameType;
        this.numberOfPlayers = numberOfPlayers;
        this.leaderBoardPosition = leaderBoardPosition;
    }

    @SpaceId
    public String getSpaceId() {
        if (tournamentId != null || playerId != null) {
            return ObjectUtils.toString(tournamentId) + "-" + ObjectUtils.toString(playerId);
        }
        return null;
    }

    public void setSpaceId(final String ignored) {
        // intentionally empty for GigaSpaces
    }

    @SpaceRouting
    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(final BigDecimal tournamentId) {
        this.tournamentId = tournamentId;
    }

    public BigDecimal getPlayerId() {
        return playerId;
    }

    public void setPlayerId(final BigDecimal playerId) {
        this.playerId = playerId;
    }

    public Integer getLeaderBoardPosition() {
        return leaderBoardPosition;
    }

    public void setLeaderBoardPosition(final Integer leaderBoardPosition) {
        this.leaderBoardPosition = leaderBoardPosition;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public Integer getNumberOfPlayers() {
        return numberOfPlayers;
    }

    public void setNumberOfPlayers(final Integer numberOfPlayers) {
        this.numberOfPlayers = numberOfPlayers;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        TournamentPlayerEliminationRequest rhs = (TournamentPlayerEliminationRequest) obj;
        return new EqualsBuilder()
                .append(this.tournamentId, rhs.tournamentId)
                .append(this.playerId, rhs.playerId)
                .append(this.gameType, rhs.gameType)
                .append(this.numberOfPlayers, rhs.numberOfPlayers)
                .append(this.leaderBoardPosition, rhs.leaderBoardPosition)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(tournamentId)
                .append(playerId)
                .append(gameType)
                .append(numberOfPlayers)
                .append(leaderBoardPosition)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("tournamentId", tournamentId)
                .append("playerId", playerId)
                .append("gameType", gameType)
                .append("numberOfPlayers", numberOfPlayers)
                .append("leaderBoardPosition", leaderBoardPosition)
                .toString();
    }
}
