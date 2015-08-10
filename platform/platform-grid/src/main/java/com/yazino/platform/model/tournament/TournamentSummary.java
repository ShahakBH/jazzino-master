package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.yazino.platform.tournament.Summary;
import com.yazino.platform.tournament.TournamentPlayerSummary;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Provides a summary of a past tournament.
 */
@SpaceClass
public class TournamentSummary implements Serializable {
    private static final long serialVersionUID = -8193105096852055049L;

    private BigDecimal tournamentId;
    private BigDecimal variationId;
    private String tournamentName;
    private Date startDateTime;
    private Date finishDateTime;
    private String gameType;
    private List<TournamentPlayerSummary> players;

    public void addPlayer(final TournamentPlayerSummary playerSummary) {
        notNull(playerSummary, "Player summary may not be null");
        players().add(playerSummary);
    }

    private List<TournamentPlayerSummary> players() {
        if (players == null) {
            players = new LinkedList<>();
        }
        return players;
    }

    @SpaceId
    @SpaceRouting
    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(final BigDecimal tournamentId) {
        this.tournamentId = tournamentId;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    public void setTournamentName(final String tournamentName) {
        this.tournamentName = tournamentName;
    }

    @SpaceIndex
    public Date getFinishDateTime() {
        return finishDateTime;
    }

    public void setFinishDateTime(final Date finishDateTime) {
        this.finishDateTime = finishDateTime;
    }

    public List<TournamentPlayerSummary> getPlayers() {
        return players;
    }

    public void setPlayers(final List<TournamentPlayerSummary> players) {
        this.players = players;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public BigDecimal getVariationId() {
        return variationId;
    }

    public void setVariationId(final BigDecimal variationId) {
        this.variationId = variationId;
    }

    public Date getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(final Date startDateTime) {
        this.startDateTime = startDateTime;
    }

    @SuppressWarnings("unchecked")
    public List<TournamentPlayerSummary> playerSummaries() {
        final ArrayList<TournamentPlayerSummary> result = new ArrayList<>(players());
        Collections.sort(result);
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<TournamentPlayerSummary> filteredSummaries(final Set<BigDecimal> filterIds) {
        final ArrayList<TournamentPlayerSummary> result = Lists.newArrayList(
                Iterables.filter(players(), new Predicate<TournamentPlayerSummary>() {
                    @Override
                    public boolean apply(final TournamentPlayerSummary playerSummary) {
                        return playerSummary != null && filterIds.contains(playerSummary.getId());
                    }
                }));
        Collections.sort(result);
        return result;
    }

    public Summary asSummary() {
        return new Summary(tournamentId, tournamentName, finishDateTime, gameType, players());
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj.getClass() != getClass()) {
            return false;
        }

        final TournamentSummary rhs = (TournamentSummary) obj;
        return new EqualsBuilder()
                .append(variationId, rhs.variationId)
                .append(tournamentName, rhs.tournamentName)
                .append(startDateTime, rhs.startDateTime)
                .append(finishDateTime, rhs.finishDateTime)
                .append(players, rhs.players)
                .append(gameType, rhs.gameType)
                .isEquals()
                && BigDecimals.equalByComparison(tournamentId, rhs.tournamentId);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(tournamentId))
                .append(variationId)
                .append(tournamentName)
                .append(startDateTime)
                .append(finishDateTime)
                .append(players)
                .append(gameType)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
