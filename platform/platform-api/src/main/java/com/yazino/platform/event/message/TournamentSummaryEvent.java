package com.yazino.platform.event.message;

import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TournamentSummaryEvent implements PlatformEvent, Serializable {

    private static final long serialVersionUID = 132379691384430214L;
    @JsonProperty("id")
    private BigDecimal tournamentId;
    @JsonProperty("srtTs")
    private DateTime startTs;
    @JsonProperty("finTs")
    private DateTime finishedTs;
    @JsonProperty("plys")
    private Collection<TournamentPlayerSummary> players;
    @JsonProperty("tid")
    private BigDecimal templateId;
    @JsonProperty("gt")
    private String gameType;
    @JsonProperty("tn")
    private String tournamentName;

    private TournamentSummaryEvent() {
    }

    public TournamentSummaryEvent(final BigDecimal tournamentId,
                                  final String tournamentName,
                                  final BigDecimal templateId,
                                  final String gameType,
                                  final DateTime startTs,
                                  final DateTime finishedTs,
                                  final Collection<TournamentPlayerSummary> players) {
        notNull(tournamentId, "tournamentId may not be null");
        notNull(templateId, "templateId may not be null");
        notNull(tournamentName, "tournamentName may not be null");
        notNull(gameType, "gameType may not be null");
        notNull(startTs, "startTs may not be null");
        notNull(finishedTs, "finishedTs may not be null");
        notNull(players, "players may not be null");
        this.tournamentId = tournamentId;
        this.tournamentName = tournamentName;
        this.templateId = templateId;
        this.gameType = gameType;
        this.startTs = startTs;
        this.finishedTs = finishedTs;
        this.players = players;
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public EventMessageType getMessageType() {
        return EventMessageType.TOURNAMENT_SUMMARY;
    }

    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public BigDecimal getTemplateId() {
        return templateId;
    }

    public String getGameType() {
        return gameType;
    }

    public DateTime getStartTs() {
        return startTs;
    }

    public DateTime getFinishedTs() {
        return finishedTs;
    }

    public Collection<TournamentPlayerSummary> getPlayers() {
        return players;
    }

    private void setTournamentId(final BigDecimal tournamentId) {
        this.tournamentId = tournamentId;
    }

    public String getTournamentName() {
        return tournamentName;
    }

    private void setTournamentName(final String tournamentName) {
        this.tournamentName = tournamentName;
    }

    private void setTemplateId(final BigDecimal templateId) {
        this.templateId = templateId;
    }

    private void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    private void setStartTs(final DateTime startTs) {
        this.startTs = startTs;
    }

    private void setFinishedTs(final DateTime finishedTs) {
        this.finishedTs = finishedTs;
    }

    private void setPlayers(final Collection<TournamentPlayerSummary> players) {
        this.players = players;
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
        final TournamentSummaryEvent rhs = (TournamentSummaryEvent) obj;
        return new EqualsBuilder()
                .append(gameType, rhs.gameType)
                .append(players, rhs.players)
                .append(templateId, rhs.templateId)
                .append(finishedTs, rhs.finishedTs)
                .append(tournamentName, rhs.tournamentName)
                .append(startTs, rhs.startTs)
                .isEquals()
                && BigDecimals.equalByComparison(tournamentId, rhs.tournamentId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(gameType)
                .append(players)
                .append(templateId)
                .append(finishedTs)
                .append(BigDecimals.strip(tournamentId))
                .append(tournamentName)
                .append(startTs)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "TournamentSummaryEvent{"
                + "tournamentId=" + tournamentId
                + ", tournamentStartTs=" + startTs
                + ", tournamentFinishedTs=" + finishedTs
                + ", players=" + players
                + ", templateId=" + templateId
                + ", gameType='" + gameType + '\''
                + ", tournamentName='" + tournamentName + '\''
                + '}';
    }
}
