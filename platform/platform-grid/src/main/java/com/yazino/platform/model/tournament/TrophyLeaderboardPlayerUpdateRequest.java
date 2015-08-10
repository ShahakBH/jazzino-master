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
public class TrophyLeaderboardPlayerUpdateRequest implements Serializable {
    private static final long serialVersionUID = 8303942300348880066L;

    private BigDecimal trophyLeaderboardId;
    private BigDecimal tournamentId;
    private BigDecimal playerId;
    private String playerName;
    private String playerPictureUrl;
    private Integer leaderboardPosition;
    private Integer tournamentPlayerCount;

    public TrophyLeaderboardPlayerUpdateRequest() {
    }

    public TrophyLeaderboardPlayerUpdateRequest(final BigDecimal trophyLeaderboardId,
                                                final BigDecimal tournamentId,
                                                final BigDecimal playerId,
                                                final String playerName,
                                                final String playerPictureUrl,
                                                final int leaderboardPosition,
                                                final int tournamentPlayerCount) {
        notNull(trophyLeaderboardId, "trophyLeaderboardId may not be null");
        notNull(tournamentId, "tournamentId may not be null");
        notNull(playerId, "playerId may not be null");

        this.trophyLeaderboardId = trophyLeaderboardId;
        this.tournamentId = tournamentId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.playerPictureUrl = playerPictureUrl;
        this.leaderboardPosition = leaderboardPosition;
        this.tournamentPlayerCount = tournamentPlayerCount;
    }

    @SpaceId
    public String getSpaceId() {
        if (trophyLeaderboardId != null || tournamentId != null || playerId != null) {
            return ObjectUtils.toString(trophyLeaderboardId) + "-" + ObjectUtils.toString(tournamentId) + "-" + ObjectUtils.toString(playerId);
        }
        return null;
    }

    public void setSpaceId(final String ignored) {
        // intentionally empty for GigaSpaces
    }

    @SpaceRouting
    public BigDecimal getTrophyLeaderboardId() {
        return trophyLeaderboardId;
    }

    public void setTrophyLeaderboardId(final BigDecimal trophyLeaderboardId) {
        this.trophyLeaderboardId = trophyLeaderboardId;
    }

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

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(final String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerPictureUrl() {
        return playerPictureUrl;
    }

    public void setPlayerPictureUrl(final String playerPictureUrl) {
        this.playerPictureUrl = playerPictureUrl;
    }

    public Integer getLeaderboardPosition() {
        return leaderboardPosition;
    }

    public void setLeaderboardPosition(final Integer leaderboardPosition) {
        this.leaderboardPosition = leaderboardPosition;
    }

    public Integer getTournamentPlayerCount() {
        return tournamentPlayerCount;
    }

    public void setTournamentPlayerCount(final Integer tournamentPlayerCount) {
        this.tournamentPlayerCount = tournamentPlayerCount;
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
        TrophyLeaderboardPlayerUpdateRequest rhs = (TrophyLeaderboardPlayerUpdateRequest) obj;
        return new EqualsBuilder()
                .append(this.trophyLeaderboardId, rhs.trophyLeaderboardId)
                .append(this.tournamentId, rhs.tournamentId)
                .append(this.playerId, rhs.playerId)
                .append(this.playerName, rhs.playerName)
                .append(this.playerPictureUrl, rhs.playerPictureUrl)
                .append(this.leaderboardPosition, rhs.leaderboardPosition)
                .append(this.tournamentPlayerCount, rhs.tournamentPlayerCount)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(trophyLeaderboardId)
                .append(tournamentId)
                .append(playerId)
                .append(playerName)
                .append(playerPictureUrl)
                .append(leaderboardPosition)
                .append(tournamentPlayerCount)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("trophyLeaderboardId", trophyLeaderboardId)
                .append("tournamentId", tournamentId)
                .append("playerId", playerId)
                .append("playerName", playerName)
                .append("playerPictureUrl", playerPictureUrl)
                .append("leaderboardPosition", leaderboardPosition)
                .append("tournamentPlayerCount", tournamentPlayerCount)
                .toString();
    }
}
