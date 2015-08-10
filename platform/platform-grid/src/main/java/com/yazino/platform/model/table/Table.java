package com.yazino.platform.model.table;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.game.api.*;
import com.yazino.game.api.time.TimeSource;
import com.yazino.platform.model.conversion.PlayerAtTableIdExtractor;
import com.yazino.platform.table.*;
import com.yazino.platform.util.BigDecimals;
import com.yazino.platform.xml.XMLSerialiser;
import org.apache.commons.lang3.builder.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.collect.Collections2.transform;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * this class encapsulates table operation, command and event execution. the
 * class is not thread safe so it should not be used concurrently from several
 * threads. it is intended to be used in a dedicated processing thread
 */
@SpaceClass
public class Table implements GameInformation, Serializable {
    private static final long serialVersionUID = 4L;

    private static final Logger LOG = LoggerFactory.getLogger(Table.class);

    private static final PlayerAtTableIdExtractor TRANSFORM_TO_PLAYER_ID = new PlayerAtTableIdExtractor();

    private GameStatus currentGame;
    private GameStatus lastGame;
    private BigDecimal tableId;
    private BigDecimal ownerId;
    private Long gameId;
    private Boolean full;
    private Boolean hasPlayers;
    private Long increment;
    private BigDecimal templateId;
    private String templateName;
    private GameType gameType;
    private String gameTypeId;
    private TableStatus tableStatus;
    private Boolean showInLobby;
    private Boolean hasOwner;
    private String tableName;
    private DateTime createdDateTime;
    private String monitoringMessage;
    private String clientId;
    private Client client;
    private Long incrementOfGameStart;
    private Integer freeSeats;
    private SortedSet<ScheduledEventWrapper> scheduledEventWrappers;
    private Map<String, String> variationProperties;
    private PlayerInformationCache playerInformationCache;
    private Long lastUpdated;
    private Set<String> tags;

    private Boolean availableForPlayersJoining;

    /*
      Constuctor for Gigaspaces use only
       */
    public Table() {
    }

    public Table(final GameType gameType,
                 final BigDecimal templateId,
                 final String clientId,
                 final boolean showInLobby) {
        this(gameType, templateId, clientId, showInLobby, null);
    }

    public Table(final GameType gameType,
                 final BigDecimal templateId,
                 final String clientId,
                 final boolean showInLobby,
                 final Set<String> tags) {
        notNull(gameType, "gameTypeId may not be null");

        this.gameType = gameType;
        this.gameTypeId = gameType.getId(); // for GS template searches
        this.templateId = templateId;
        this.showInLobby = showInLobby;
        this.tableStatus = TableStatus.closed;
        this.gameId = 0L;
        this.increment = 0L;
        this.incrementOfGameStart = 0L;
        this.clientId = clientId;
        this.full = false;
        this.tags = tags;

        initialiseCollectionsIfRequired();
    }

    public void reset() {
        setCurrentGame(null);
        setLastGame(null);
        setTableStatus(TableStatus.open);
        setMonitoringMessage("Reset at " + DateFormat.getDateTimeInstance().format(new Date()));
    }

    public boolean isAddingPlayersPossible() {
        return tableStatus == TableStatus.open;
    }

    public String getMonitoringMessage() {
        return monitoringMessage;
    }

    public void setMonitoringMessage(final String monitoringMessage) {
        this.monitoringMessage = monitoringMessage;
    }

    public Long getNextEventTimestamp() {
        if (scheduledEventWrappers == null) {
            return null;
        }
        if (!scheduledEventWrappers.isEmpty()) {
            return scheduledEventWrappers.first().getWhenMillis();
        }
        return null;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setNextEventTimestamp(final Long nextEventTimestamp) {
        // required by gigaspaces, ignore!
        // next event timestamp is a calculated property, see
        // calculateNextEventTimestamp
    }

    public ObservableStatus buildObservableStatus(final GameRules gameRules,
                                                  final GamePlayer player,
                                                  final BigDecimal balance) {
        if (currentGame == null) {
            return null;
        }
        return gameRules.getObservableStatus(currentGame, new ObservableContext(player, balance));
    }

    @SpaceIndex
    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(final Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @SpaceIndex
    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public Map<String, String> getVariationProperties() {
        return variationProperties;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(final String tableName) {
        this.tableName = tableName;
    }

    public DateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(final DateTime createdTimeStamp) {
        this.createdDateTime = createdTimeStamp;
    }

    public GameStatus getCurrentGame() {
        return currentGame;
    }

    public GameStatus getLastGame() {
        return lastGame;
    }

    @SpaceIndex
    public String getGameTypeId() {
        return gameTypeId;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(final GameType gameType) {
        this.gameType = gameType;
        if (gameType != null) {
            this.gameTypeId = gameType.getId();
        } else {
            this.gameTypeId = null;
        }
    }

    public void setCurrentGame(final GameStatus status) {
        currentGame = status;
    }

    public void setLastGame(final GameStatus lastGame) {
        this.lastGame = lastGame;
    }

    @SpaceIndex
    public TableStatus getTableStatus() {
        return tableStatus;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(final Long gameId) {
        this.gameId = gameId;
    }

    @SpaceIndex
    public Boolean getShowInLobby() {
        return showInLobby;
    }

    public void setShowInLobby(final Boolean showInLobby) {
        this.showInLobby = showInLobby;
    }

    public Long getIncrement() {
        return increment;
    }

    public Long getIncrementOfGameStart() {
        return incrementOfGameStart;
    }

    public void setIncrementOfGameStart(final Long incrementOfGameStart) {
        this.incrementOfGameStart = incrementOfGameStart;
    }

    public long incrementOfGameStartDefaultedToOne() {
        if (incrementOfGameStart == null) {
            return 1;
        }
        return incrementOfGameStart;
    }

    public long incrementDefaultedToOne() {
        if (increment == null) {
            return 1;
        }
        return increment;
    }

    public void setIncrement(final Long increment) {
        this.increment = increment;
    }

    @SpaceIndex
    public Boolean getHasPlayers() {
        return hasPlayers;
    }

    public void setHasPlayers(final Boolean hasPlayers) {
        this.hasPlayers = hasPlayers;
    }

    @SpaceId
    @SpaceRouting
    public BigDecimal getTableId() {
        return tableId;
    }

    @SpaceIndex
    public Boolean getAvailableForPlayersJoining() {
        return availableForPlayersJoining;
    }


    public void setTableId(final BigDecimal tableId) {
        this.tableId = tableId;
    }

    public void setTemplateId(final BigDecimal templateId) {
        this.templateId = templateId;
    }

    public void setVariationProperties(final Map<String, String> variationProperties) {
        if (variationProperties != null && !(variationProperties instanceof ConcurrentHashMap)) {
            this.variationProperties = new ConcurrentHashMap<>(variationProperties);
        } else {
            this.variationProperties = variationProperties;
        }
    }

    public void setGameTypeId(final String gameTypeId) {
        this.gameTypeId = gameTypeId;
    }

    public void setScheduledEventWrappers(final SortedSet<ScheduledEventWrapper> scheduledEventWrappers) {
        this.scheduledEventWrappers = toConcurrentSortedSet(scheduledEventWrappers);
    }

    public SortedSet<ScheduledEventWrapper> getScheduledEventWrappers() {
        return scheduledEventWrappers;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(final Set<String> tags) {
        this.tags = toConcurrentSet(tags);
    }

    private <T> SortedSet<T> toConcurrentSortedSet(final SortedSet<T> items) {
        if (items != null && !(items instanceof ConcurrentSkipListSet)) {
            return new ConcurrentSkipListSet<>(items);
        }
        return items;
    }

    private <T> Set<T> toConcurrentSet(final Set<T> items) {
        if (items != null && !items.isEmpty()) {
            return new CopyOnWriteArraySet<>(items);
        }
        return null;
    }

    public void close() {
        tableStatus = calculateNextStatus(TableStatus.closed);
    }

    public boolean isTableOpenOrClosing() {
        return TableStatus.open.equals(tableStatus)
                || TableStatus.closing.equals(tableStatus);
    }

    public boolean isTableOpen() {
        return TableStatus.open.equals(tableStatus);
    }

    public void setTableStatus(final TableStatus status) {
        tableStatus = status;
    }

    public BigDecimal getTemplateId() {
        return templateId;
    }

    public boolean isClosed() {
        return TableStatus.closed.equals(tableStatus);
    }

    @SpaceIndex
    public Boolean getFull() {
        return full;
    }

    public void setFull(final Boolean full) {
        this.full = full;
    }

    @SpaceIndex
    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(final String templateName) {
        this.templateName = templateName;
    }

    @SpaceIndex
    public Boolean getOpenOrClosing() {
        if (tableStatus == null) {
            return null;
        }

        return isTableOpenOrClosing();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setOpenOrClosing(final Boolean ignored) {
        // ignored, merely required for GigaSpace indexing
    }

    @SpaceIndex
    public Boolean getOpen() {
        if (tableStatus == null) {
            return null;
        }

        return tableStatus == TableStatus.open;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setOpen(final Boolean ignored) {
        // ignored, merely required for GigaSpace indexing
    }

    @SpaceIndex
    public Boolean getHasOwner() {
        return hasOwner;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setHasOwner(final Boolean hasOwner) {
        this.hasOwner = hasOwner;
    }

    public void playerAcknowledgesIncrement(final BigDecimal playerId,
                                            final Long changesFrom) {
        if (playerId == null) {
            return;
        }
        final PlayerInformation playerInformation = playerAtTable(playerId);
        if (playerInformation != null) {
            playerInformation.setAcknowledgedIncrement(changesFrom);
        }
    }

    public void open() {
        if (tableStatus == TableStatus.open) {
            return;
        }
        tableStatus = calculateNextStatus(TableStatus.open);
        if (tableStatus == TableStatus.open) {
            currentGame = null;
            lastGame = null;
        }
    }

    private void initialiseCollectionsIfRequired() {
        if (scheduledEventWrappers == null) {
            scheduledEventWrappers = new ConcurrentSkipListSet<>();
        }
    }

    public PlayerInformationCache getPlayerInformationCache() {
        return playerInformationCache;
    }

    public void setPlayerInformationCache(final PlayerInformationCache playerInformationCache) {
        this.playerInformationCache = playerInformationCache;
    }

    public PlayerInformationCache playerInfoCache() {
        if (playerInformationCache == null) {
            playerInformationCache = new PlayerInformationCache();
        }
        return playerInformationCache;
    }

    public PlayerInformation playerAtTable(final BigDecimal playerId) {
        notNull(playerId, "Player ID may not be null");

        if (playerInformationCache != null) {
            return playerInformationCache.get(playerId);
        }
        return null;
    }

    public void addPlayerToTable(final PlayerInformation playerInformation) {
        notNull(playerInformation, "playerInformation may not be null");

        playerInfoCache().add(playerInformation);
    }

    public GamePlayer cachedPlayer(final BigDecimal playerId) {
        notNull(playerId, "Player ID may not be null");

        if (playerInformationCache != null) {
            final PlayerInformation playerInformation = playerInformationCache.get(playerId);
            if (playerInformation != null) {
                return new GamePlayer(playerInformation.getPlayerId(), playerInformation.getSessionId(), playerInformation.getName());
            }
        }

        return null;
    }

    public PlayerInformation playerWithAccountId(final BigDecimal accountId) {
        notNull(accountId, "Account ID may not be null");

        if (playerInformationCache != null) {
            for (PlayerInformation playerInformation : playerInformationCache.getAll()) {
                if (playerInformation.getAccountId().equals(accountId)) {
                    return playerInformation;
                }
            }
        }

        return null;
    }

    private TableStatus calculateNextStatus(final TableStatus requestedStatus) {
        switch (requestedStatus) {
            case closed:
                if (TableStatus.open.equals(tableStatus)) {
                    return TableStatus.closing;
                }
                break;
            case open:
                if (TableStatus.closing.equals(tableStatus)
                        || TableStatus.closed.equals(tableStatus)) {
                    return TableStatus.open;
                }
                break;
            default:
                // ignored
                break;
        }
        return tableStatus;
    }

    public void updateFreeSeats(final int numberOfSeatsTaken) {
        int maxSeats = 0;
        if (client != null && client.getNumberOfSeats() != null) {
            maxSeats = client.getNumberOfSeats();
        }

        final int newFreeSeats;
        if (numberOfSeatsTaken >= maxSeats) {
            newFreeSeats = 0;
        } else {
            newFreeSeats = maxSeats - numberOfSeatsTaken;
        }
        this.freeSeats = newFreeSeats;
        setFull(newFreeSeats == 0);
        setHasPlayers(numberOfSeatsTaken != 0);
    }

    public Client getClient() {
        return client;
    }

    public void setClient(final Client client) {
        this.client = client;
    }


    @SpaceIndex
    public Integer getFreeSeats() {
        return freeSeats;
    }

    public void setFreeSeats(final Integer freeSeats) {
        this.freeSeats = freeSeats;
    }

    public int scheduledEventCount() {
        return getScheduledEvents().size();
    }

    List<ScheduledEvent> getScheduledEvents() {
        final List<ScheduledEvent> returnList = new ArrayList<>();
        if (scheduledEventWrappers == null) {
            return returnList;
        }
        for (ScheduledEventWrapper scheduledEventWrapper : scheduledEventWrappers) {
            returnList.add(scheduledEventWrapper.getEvent());
        }
        return Collections.unmodifiableList(returnList);
    }

    public void addEvents(final TimeSource timeSource,
                          final List<ScheduledEvent> extraScheduledEvents) {
        initialiseCollectionsIfRequired();
        for (ScheduledEvent extraScheduledEvent : extraScheduledEvents) {
            addEvent(timeSource, extraScheduledEvent);
        }
    }

    public void addEvent(final TimeSource timeSource,
                         final ScheduledEvent scheduledEvent) {
        initialiseCollectionsIfRequired();
        final ScheduledEventWrapper scheduledEventWrapper = new ScheduledEventWrapper(
                timeSource, scheduledEvent);
        scheduledEventWrappers.add(scheduledEventWrapper);
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Added event for processing: %s, due for processing at: %s",
                    scheduledEventWrapper.getEvent().getEventSimpleName(),
                    new DateTime(scheduledEventWrapper.whenMillis)));
        }
    }

    public List<ScheduledEvent> getPendingEvents(final TimeSource timeSource) {
        final long currentTimeStamp = timeSource.getCurrentTimeStamp();
        final List<ScheduledEvent> pendingEvents = new ArrayList<>();
        if (scheduledEventWrappers == null) {
            return pendingEvents;
        }
        for (final Iterator<ScheduledEventWrapper> scheduledEventWrapperIterator = scheduledEventWrappers.iterator();
             scheduledEventWrapperIterator.hasNext(); ) {
            final ScheduledEventWrapper scheduledEventWrapper = scheduledEventWrapperIterator
                    .next();
            if (scheduledEventWrapper.getWhenMillis() <= currentTimeStamp
                    || scheduledEventWrapper.getEvent().getDelayInMillis() == 0) {
                pendingEvents.add(scheduledEventWrapper.getEvent());
                if (LOG.isDebugEnabled()) {
                    LOG.debug(String.format(
                            "Returning event for processing: %s, due for processing at: %s",
                            scheduledEventWrapper.getEvent().getEventSimpleName(),
                            new DateTime(scheduledEventWrapper.whenMillis)));
                }
                scheduledEventWrapperIterator.remove();
            }
        }
        return pendingEvents;
    }

    public int numberOfPlayers(final GameRules gameRules) {
        LOG.debug("entering numberOfPlayers");

        if (currentGame == null) {
            return 0;
        }

        return gameRules.getPlayerInformation(currentGame).size();
    }


    public Set<BigDecimal> playerIds(final GameRules gameRules) {
        LOG.debug("entering getPlayerIds");

        final Set<BigDecimal> playerSet = new HashSet<>();
        if (currentGame == null) {
            return playerSet;
        }
        playerSet.addAll(transform(playersAtTableInformation(gameRules), TRANSFORM_TO_PLAYER_ID));
        return playerSet;
    }

    public Map<BigDecimal, BigDecimal> playerIdsToSessions(final GameRules gameRules) {
        LOG.debug("entering playerIdsToSessions");

        final Map<BigDecimal, BigDecimal> playersToSessions = new HashMap<>();
        if (currentGame == null) {
            return playersToSessions;
        }
        for (PlayerAtTableInformation playerAtTable : playersAtTableInformation(gameRules)) {
            playersToSessions.put(playerAtTable.getPlayer().getId(), playerAtTable.getPlayer().getSessionId());
        }
        return playersToSessions;
    }

    public Collection<PlayerAtTableInformation> playersAtTableInformation(final GameRules gameRules) {
        if (currentGame == null) {
            return Collections.emptyList();
        }
        return gameRules.getPlayerInformation(currentGame);
    }

    public void clearEvents() {
        if (scheduledEventWrappers != null) {
            this.scheduledEventWrappers.clear();
        }
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
        final Table rhs = (Table) obj;
        return new EqualsBuilder()
                .append(gameId, rhs.gameId)
                .append(increment, rhs.increment)
                .append(templateId, rhs.templateId)
                .append(variationProperties, rhs.variationProperties)
                .append(gameType, rhs.gameType)
                .append(currentGame, rhs.currentGame)
                .append(lastGame, rhs.lastGame)
                .append(tableStatus, rhs.tableStatus)
                .append(showInLobby, rhs.showInLobby)
                .append(tableName, rhs.tableName)
                .append(client, rhs.client)
                .append(clientId, rhs.clientId)
                .append(createdDateTime, rhs.createdDateTime)
                .append(monitoringMessage, rhs.monitoringMessage)
                .append(scheduledEventWrappers, rhs.scheduledEventWrappers)
                .append(tags, rhs.tags)
                .isEquals()
                && BigDecimals.equalByComparison(tableId, rhs.tableId)
                && BigDecimals.equalByComparison(ownerId, rhs.ownerId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(tableId))
                .append(gameId)
                .append(BigDecimals.strip(ownerId))
                .append(increment)
                .append(templateId)
                .append(variationProperties)
                .append(gameType)
                .append(currentGame)
                .append(lastGame)
                .append(tableStatus)
                .append(showInLobby)
                .append(tableName)
                .append(createdDateTime)
                .append(monitoringMessage)
                .append(clientId)
                .append(client)
                .append(scheduledEventWrappers)
                .append(tags)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public void removePlayersNoLongerAtTable(final GameRules gameRules) {
        if (currentGame == null) {
            playerInfoCache().clear();
            return;
        }

        playerInfoCache().retainOnly(transform(gameRules.getPlayerInformation(currentGame), TRANSFORM_TO_PLAYER_ID));
    }

    public void nextIncrement() {
        if (increment == null) {
            increment = 0L;
        }

        increment++;
    }

    public boolean readyToBeClosed(final GameRules gameRules) {
        return tableStatus == TableStatus.closing && (currentGame == null || gameRules.canBeClosed(currentGame));
    }

    @SpaceIndex
    public BigDecimal getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(final BigDecimal ownerId) {
        this.ownerId = ownerId;
        this.hasOwner = ownerId != null;
    }

    /* intentionally not getXXX to avoid gigaspace serialization */

    public Collection<PlayerInformation> allPlayersAtTable() {
        return playerInfoCache().getAll();
    }

    public void removeAllPlayers() {
        playerInfoCache().clear();
    }

    public void setAvailableForPlayersJoining(final Boolean availableForPlayersJoining) {
        this.availableForPlayersJoining = availableForPlayersJoining;
    }

    public TableGameSummary summariseGame() {
        return new TableGameSummary(getTableId(),
                getTableName(),
                getGameId(),
                gameType,
                XMLSerialiser.toXML(getCurrentGame()),
                incrementDefaultedToOne());
    }

    public TableSummary summarise(final GameRules gameRules) {
        String clientFile = null;
        if (getClient() != null) {
            clientFile = getClient().getClientFile();
        }

        return new TableSummary(getTableId(),
                getTableName(),
                getTableStatus(),
                getGameTypeId(),
                gameType,
                getOwnerId(),
                getClientId(),
                clientFile,
                getTemplateName(),
                getMonitoringMessage(),
                playerIds(gameRules),
                getTags());
    }

    public TableType resolveType() {
        if (ownerId != null) {
            return TableType.PRIVATE;
        }
        if (showInLobby != null && !showInLobby) {
            return TableType.TOURNAMENT;
        }
        return TableType.PUBLIC;
    }

    public int getJoiningDesirability(final GameRules gameRules) {
        if (currentGame == null) {
            return Integer.MIN_VALUE;
        }
        return gameRules.getJoiningDesirability(currentGame);
    }

    private class ScheduledEventWrapper implements
            Comparable<ScheduledEventWrapper>, Serializable {
        private static final long serialVersionUID = -2485764189597196237L;
        private final ScheduledEvent event;
        private final Long whenMillis;

        public ScheduledEventWrapper(final TimeSource timeSource,
                                     final ScheduledEvent event) {
            if (event == null) {
                throw new IllegalArgumentException("Event may not be null");
            }
            this.event = event;
            final long currentTimeStamp = timeSource.getCurrentTimeStamp();
            this.whenMillis = currentTimeStamp + event.getDelayInMillis();
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Event %s added at: %s and is scheduled for: %s",
                        event.getEventSimpleName(), new DateTime(currentTimeStamp), new DateTime(this.whenMillis)));
            }
        }

        public ScheduledEvent getEvent() {
            return event;
        }

        public Long getWhenMillis() {
            return whenMillis;
        }

        public int compareTo(final ScheduledEventWrapper o) {
            final int comparisonResult = new CompareToBuilder().append(
                    whenMillis, o.whenMillis).append(event.hashCode(),
                    o.event.hashCode()).toComparison();
            if (comparisonResult == 0) { // catch equal events & log
                // as we don't override hashcode these should always be unique
                return Integer.valueOf(hashCode()).compareTo(o.hashCode());
            }
            return comparisonResult;
        }

        @Override
        public String toString() {
            return new ReflectionToStringBuilder(this).toString();
        }
    }

    public Map<String, String> getCombinedGameProperties() {
        final HashMap<String, String> map = new HashMap<>();
        if (variationProperties != null) {
            map.putAll(variationProperties);
        }
        if (client != null && client.getClientProperties() != null) {
            map.putAll(client.getClientProperties());
        }
        return map;
    }
}
