package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.game.api.NewsEvent;
import com.yazino.game.api.NewsEventType;
import com.yazino.game.api.ParameterisedMessage;
import com.yazino.game.api.time.TimeSource;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.Trophy;
import com.yazino.platform.model.session.InboxMessage;
import com.yazino.platform.processor.tournament.TrophyLeaderboardPayoutCalculator;
import com.yazino.platform.processor.tournament.TrophyLeaderboardResultContext;
import com.yazino.platform.tournament.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class TrophyLeaderboard implements Serializable {
    private static final long serialVersionUID = 4134202844053612215L;

    private static final Logger LOG = LoggerFactory.getLogger(TrophyLeaderboard.class);
    private static final int ROUND_UP_TO = 100;

    private Map<Integer, TrophyLeaderboardPosition> positionData;
    private TrophyLeaderboardPlayers players;

    private BigDecimal id;
    private String name;
    private Boolean active;
    private String gameType;
    private Long pointBonusPerPlayer;
    private DateTime startTime;
    private DateTime endTime;
    private DateTime currentCycleEnd;
    private Duration cycle;
    private TrophyLeaderboardPayoutCalculator trophyLeaderboardPayoutCalculator;

    public TrophyLeaderboard() {
    }

    public TrophyLeaderboard(final BigDecimal id) {
        notNull(id, "ID may not be null");

        this.id = id;
    }

    public TrophyLeaderboard(final BigDecimal id,
                             final String name,
                             final String gameType,
                             final Interval validInterval,
                             final Duration cycle) {
        this(name, gameType, validInterval, cycle);
        notNull(id, "ID may not be null");
        this.id = id;
    }

    public TrophyLeaderboard(final TrophyLeaderboardDefinition trophyLeaderboardDefinition) {
        this(trophyLeaderboardDefinition.getName(),
                trophyLeaderboardDefinition.getGameType(),
                trophyLeaderboardDefinition.getValidInterval(),
                trophyLeaderboardDefinition.getCycle());

        setPointBonusPerPlayer(trophyLeaderboardDefinition.getPointBonusPerPlayer());
        setPositionData(trophyLeaderboardDefinition.getPositionData());
    }

    private TrophyLeaderboard(final String name,
                              final String gameType,
                              final Interval validInterval,
                              final Duration cycle) {
        notBlank(name, "Name may not be null/blank");
        notBlank(gameType, "Game Type may not be null/blank");
        notNull(validInterval, "Valid Interval may not be null");
        notNull(cycle, "Cycle may not be null");


        this.name = name;
        this.gameType = gameType;
        this.startTime = validInterval.getStart();
        this.endTime = validInterval.getEnd();
        this.cycle = cycle;

        if (this.startTime.plus(cycle).isAfter(endTime)) {
            throw new IllegalArgumentException("End time must be at least one period after the start time");
        }

        this.active = true;
        this.currentCycleEnd = startTime.plus(cycle);
    }

    public void addPosition(final TrophyLeaderboardPosition position) {
        notNull(position, "Position may not be null");

        positionData().put(position.getPosition(), position);
    }

    public TrophyLeaderboardPlayerUpdateResult update(final TrophyLeaderboardPlayerUpdateRequest request) {
        notNull(request, "request may not be null");

        LOG.debug("Leaderboard {}: Updating with player request {}", id, request);

        int previousPosition = 0;
        long previousPoints = 0;
        final TrophyLeaderboardPlayer leaderboardPlayer = players().findPlayer(request.getPlayerId());
        if (leaderboardPlayer != null) {
            previousPosition = leaderboardPlayer.getLeaderboardPosition();
            previousPoints = leaderboardPlayer.getPoints();
        }

        final long tournamentPoints = calculateTournamentPointsFor(request.getLeaderboardPosition(), request.getTournamentPlayerCount());
        final long bonusPoints = updateForPlayer(request.getPlayerId(),
                request.getPlayerName(),
                request.getPlayerPictureUrl(),
                request.getLeaderboardPosition(),
                request.getTournamentPlayerCount(),
                tournamentPoints);

        players().updatePlayersPositions();

        final TrophyLeaderboardPlayer updatedLeaderboardPlayer = players().findPlayer(request.getPlayerId());
        return new TrophyLeaderboardPlayerUpdateResult(request.getTrophyLeaderboardId(),
                request.getTournamentId(),
                previousPosition,
                updatedLeaderboardPlayer.getLeaderboardPosition(),
                previousPoints,
                tournamentPoints,
                bonusPoints);
    }

    private long calculateTournamentPointsFor(final Integer leaderboardPosition,
                                              final int numberOfPlayers) {
        if (leaderboardPosition != null) {
            long bonusPointsMultiplier = 1;
            if (pointBonusPerPlayer != null) {
                bonusPointsMultiplier = pointBonusPerPlayer;
            }
            return bonusPointsMultiplier * (numberOfPlayers - leaderboardPosition);
        }
        return 0;
    }

    private long updateForPlayer(final BigDecimal playerId,
                                 final String playerName,
                                 final String pictureUrl,
                                 final int tournamentLeaderboardPosition,
                                 final int numberOfPlayers,
                                 final long tournamentPoints) {
        TrophyLeaderboardPlayer leaderboardPlayer = players().findPlayer(playerId);
        if (leaderboardPlayer == null) {
            LOG.debug("Leaderboard {}: Adding player {} ({})", id, playerId, playerName);
            leaderboardPlayer = new TrophyLeaderboardPlayer(playerId, playerName, pictureUrl);
            players().addPlayer(leaderboardPlayer);
        }

        final TrophyLeaderboardPosition position = positionData().get(tournamentLeaderboardPosition);
        LOG.debug("Leaderboard {}: Player {} finished {} and will be applied rule {}", id, playerId, tournamentLeaderboardPosition, position);

        final long bonusPoints = bonusPointsFor(position, numberOfPlayers);
        if (bonusPoints > 0) {
            LOG.debug("Leaderboard {}: Player {} is awarded {} tournament + {} bonus", id, playerId, tournamentPoints, bonusPoints);
            leaderboardPlayer.incrementPoints(bonusPoints + tournamentPoints);
            return bonusPoints;
        }

        return 0;
    }

    private long bonusPointsFor(final TrophyLeaderboardPosition position, final int numberOfPlayers) {
        if (position != null) {
            final int multiplier = (numberOfPlayers + ROUND_UP_TO - 1) / ROUND_UP_TO;
            return position.getAwardPoints() * multiplier;
        }
        return 0;
    }

    public TrophyLeaderboardResult result(final TrophyLeaderboardResultContext trophyLeaderboardResultContext)
            throws WalletServiceException {
        notNull(trophyLeaderboardResultContext, "trophyLeaderboardResultContext may not be null");

        LOG.debug("Leaderboard {}: Resulting", id);

        final List<TrophyLeaderboardPlayerResult> leaderboardPlayerResults = new ArrayList<>();

        trophyLeaderboardPayoutCalculator().payout(
                this.id,
                players(),
                positionData(),
                trophyLeaderboardResultContext.getInternalWalletService(),
                trophyLeaderboardResultContext.getPlayerRepository(),
                trophyLeaderboardResultContext.getAuditor());

        final TimeSource timeSource = trophyLeaderboardResultContext.getTimeSource();
        final DateTime now = new DateTime(timeSource.getCurrentTimeStamp());

        for (final TrophyLeaderboardPlayer leaderboardPlayer : players().getOrderedByPosition()) {
            final BigDecimal trophyId = awardTrophy(trophyLeaderboardResultContext, leaderboardPlayer);
            if (trophyId != null) {
                sendTrophyNews(trophyLeaderboardResultContext, leaderboardPlayer, trophyId);
            }
            leaderboardPlayerResults.add(new TrophyLeaderboardPlayerResult(
                    leaderboardPlayer.getPlayerId(),
                    leaderboardPlayer.getPlayerName(),
                    leaderboardPlayer.getPoints(),
                    leaderboardPlayer.getFinalPayoutWithDefault(BigDecimal.ZERO),
                    leaderboardPlayer.getLeaderboardPosition()));
        }

        TrophyLeaderboardResult result = null;
        if (leaderboardPlayerResults.size() > 0) {
            final DateTime expiry = now.plus(getCycle());
            final TrophyLeaderboardResult leaderboardResult = new TrophyLeaderboardResult(
                    id, now, expiry, leaderboardPlayerResults);
            trophyLeaderboardResultContext.getTrophyLeaderboardResultRepository().save(leaderboardResult);
            LOG.debug("Leaderboard {}: resulted with output {}", id, leaderboardResult);
            result = leaderboardResult;
        }

        reset(timeSource);

        return result;
    }

    private BigDecimal awardTrophy(final TrophyLeaderboardResultContext trophyLeaderboardResultContext,
                                   final TrophyLeaderboardPlayer leaderboardPlayer) {
        final int position = leaderboardPlayer.getLeaderboardPosition();
        final TrophyLeaderboardPosition leaderboardPosition = positionData().get(position);
        if (leaderboardPosition == null) {
            return null;
        }

        final BigDecimal trophyId = leaderboardPosition.getTrophyId();
        if (trophyId == null) {
            return null;
        }

        trophyLeaderboardResultContext.getAwardTrophyService().awardTrophy(leaderboardPlayer.getPlayerId(), trophyId);

        return trophyId;

    }

    private void sendTrophyNews(final TrophyLeaderboardResultContext trophyLeaderboardResultContext,
                                final TrophyLeaderboardPlayer leaderboardPlayer,
                                final BigDecimal trophyId) {
        notNull(trophyId, "trophy id is null");
        final Trophy trophy = trophyLeaderboardResultContext.getTrophyRepository().findById(trophyId);
        if (trophy == null) {
            return;
        }
        final String message = trophy.getMessage();
        final String shortDescription = trophy.getShortDescription();
        if (message == null || shortDescription == null) {
            LOG.warn("Trophy {} did not have a message or short description so no email was sent", trophyId);
            return;
        }
        final ParameterisedMessage parameterisedMessage = new ParameterisedMessage(
                message, leaderboardPlayer.getPlayerName());
        final ParameterisedMessage parameterisedShortDescription = new ParameterisedMessage(
                shortDescription, leaderboardPlayer.getPlayerName());
        final String image = trophy.getImage();
        final NewsEvent newsEvent = new NewsEvent.Builder(leaderboardPlayer.getPlayerId(), parameterisedMessage)
                .setType(NewsEventType.TROPHY)
                .setShortDescription(parameterisedShortDescription)
                .setImage(image)
                .setGameType(gameType)
                .build();
        trophyLeaderboardResultContext.getInboxMessageGlobalRepository().send(
                new InboxMessage(leaderboardPlayer.getPlayerId(), newsEvent, new DateTime()));
    }


    private TrophyLeaderboardPayoutCalculator trophyLeaderboardPayoutCalculator() {
        if (trophyLeaderboardPayoutCalculator == null) {
            trophyLeaderboardPayoutCalculator = new TrophyLeaderboardPayoutCalculator();
        }
        return trophyLeaderboardPayoutCalculator;
    }

    public void suspend() {
        LOG.debug("Leaderboard {}: Suspending", id);
        this.active = false;
    }

    public void resume() {
        LOG.debug("Leaderboard {}: Resuming", id);
        this.active = true;
    }

    private void reset(final TimeSource timeSource) {
        LOG.debug("Resetting leaderboard {}", id);

        players().clear();

        final DateTime currentTime = new DateTime(timeSource.getCurrentTimeStamp());
        if (currentTime.isBefore(endTime)) {
            this.currentCycleEnd = currentCycleEnd.plus(cycle);
            LOG.debug("Leaderboard {}: Another cycle required, next period end is {}", id, currentCycleEnd);

        } else {
            this.currentCycleEnd = null;
            suspend();

            LOG.debug("Leaderboard {}: No more periods required, suspending.", id);
        }
    }

    private Map<Integer, TrophyLeaderboardPosition> positionData() {
        if (positionData == null) {
            positionData = new ConcurrentHashMap<>();
        }

        return positionData;
    }

    private TrophyLeaderboardPlayers players() {
        if (players == null) {
            players = new TrophyLeaderboardPlayers();
        }

        return players;
    }

    @SpaceId
    @SpaceRouting
    public BigDecimal getId() {
        return id;
    }

    public void setId(final BigDecimal id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @SpaceIndex
    public Boolean getActive() {
        return active;
    }

    public void setActive(final Boolean active) {
        this.active = active;
    }

    @SpaceIndex
    public String getGameType() {
        return gameType;
    }

    public void setGameType(final String gameType) {
        this.gameType = gameType;
    }

    public Long getPointBonusPerPlayer() {
        return pointBonusPerPlayer;
    }

    public void setPointBonusPerPlayer(final Long pointBonusPerPlayer) {
        this.pointBonusPerPlayer = pointBonusPerPlayer;
    }

    @SpaceIndex
    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final DateTime startTime) {
        this.startTime = startTime;
    }

    @SpaceIndex
    public DateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(final DateTime endTime) {
        this.endTime = endTime;
    }

    public DateTime getCurrentCycleEnd() {
        return currentCycleEnd;
    }

    public void setCurrentCycleEnd(final DateTime currentCycleEnd) {
        this.currentCycleEnd = currentCycleEnd;
    }

    public Duration getCycle() {
        return cycle;
    }

    public void setCycle(final Duration cycle) {
        this.cycle = cycle;
    }

    public Map<Integer, TrophyLeaderboardPosition> getPositionData() {
        return positionData;
    }

    public void setPositionData(final Map<Integer, TrophyLeaderboardPosition> positionData) {
        if (positionData != null) {
            positionData().clear();
            positionData().putAll(positionData);
        } else {
            this.positionData = null;
        }
    }

    public TrophyLeaderboardPlayers getPlayers() {
        return players;
    }

    public void setPlayers(final TrophyLeaderboardPlayers players) {
        this.players = players;
    }

    public List<TrophyLeaderboardPlayer> getOrderedByPosition() {
        return players().getOrderedByPosition();
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

        final TrophyLeaderboard rhs = (TrophyLeaderboard) obj;
        return new EqualsBuilder()
                .append(id, rhs.id)
                .append(name, rhs.name)
                .append(active, rhs.active)
                .append(gameType, rhs.gameType)
                .append(pointBonusPerPlayer, rhs.pointBonusPerPlayer)
                .append(startTime, rhs.startTime)
                .append(endTime, rhs.endTime)
                .append(currentCycleEnd, rhs.currentCycleEnd)
                .append(cycle, rhs.cycle)
                .isEquals();
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(id)
                .append(name)
                .append(active)
                .append(gameType)
                .append(pointBonusPerPlayer)
                .append(startTime)
                .append(endTime)
                .append(currentCycleEnd)
                .append(cycle)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
