package com.yazino.platform.model.tournament;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.yazino.platform.account.TransactionContext;
import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.table.Client;
import com.yazino.platform.processor.tournament.TableAllocator;
import com.yazino.platform.processor.tournament.TournamentHost;
import com.yazino.platform.processor.tournament.TournamentPayoutCalculator;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.tournament.*;
import com.yazino.platform.util.BigDecimals;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static com.yazino.platform.account.TransactionContext.transactionContext;
import static org.apache.commons.lang3.Validate.notNull;

@SpaceClass
public class Tournament implements Serializable {
    private static final long serialVersionUID = 4791462212187948716L;

    private static final Logger LOG = LoggerFactory.getLogger(Tournament.class);

    private static final String PLAYER_ACCOUNT_SUFFIX = ":TOURNAMENT";
    private static final String TOURNAMENT_PAYOUT_TYPE = "Tournament Payout";

    private BigDecimal tournamentId;
    private BigDecimal pot;
    private DateTime startTimeStamp;
    private DateTime signupStartTimeStamp;
    private DateTime signupEndTimeStamp;
    private TournamentStatus tournamentStatus;
    private TournamentVariationTemplate tournamentVariationTemplate;
    private String name;
    private String description;
    private Long nextEvent;
    private String partnerId;
    private Integer currentRoundIndex;
    private List<BigDecimal> startingTables;
    private List<BigDecimal> tables;
    private String monitoringMessage;
    private BigDecimal settledPrizePot;

    private TournamentPlayers players;
    private TournamentLeaderboard tournamentLeaderboard;
    private TournamentPayoutCalculator tournamentPayoutCalculator;

    public Tournament() {
    }

    public Tournament(final BigDecimal tournamentId) {
        this.tournamentId = tournamentId;
    }

    // for rehydration only

    public Tournament(final BigDecimal tournamentId,
                      final Set<TournamentPlayer> players) {
        this.tournamentId = tournamentId;
        retrievePlayers().addAll(players);
    }

    public Tournament(final Tournament tournament) {
        notNull(tournament, "Tournament may not be null");

        this.tournamentId = tournament.getTournamentId();

        this.startTimeStamp = tournament.getStartTimeStamp();
        this.signupStartTimeStamp = tournament.getSignupStartTimeStamp();
        this.signupEndTimeStamp = tournament.getSignupEndTimeStamp();
        this.tournamentStatus = tournament.getTournamentStatus();
        this.tournamentVariationTemplate = tournament.getTournamentVariationTemplate();
        this.name = tournament.getName();
        this.description = tournament.getDescription();
        this.pot = tournament.getPot();
        this.nextEvent = tournament.getNextEvent();
        this.tables = tournament.getTables();
        this.partnerId = tournament.getPartnerId();
        this.settledPrizePot = tournament.getSettledPrizePot();
        this.players = tournament.players;
    }

    public Tournament(final TournamentDefinition tournamentDefinition) {
        notNull(tournamentDefinition, "tournamentDefinition may not be null");

        this.tournamentId = tournamentDefinition.getId();
        this.name = tournamentDefinition.getName();
        this.description = tournamentDefinition.getDescription();
        this.signupStartTimeStamp = tournamentDefinition.getSignUpStart();
        this.signupEndTimeStamp = tournamentDefinition.getSignUpEnd();
        this.startTimeStamp = tournamentDefinition.getStart();
        this.tournamentStatus = tournamentDefinition.getStatus();
        this.tournamentVariationTemplate = tournamentDefinition.getTemplate();
        this.partnerId = tournamentDefinition.getPartnerId();
    }

    public BigDecimal getPot() {
        return pot;
    }

    public void setPot(final BigDecimal pot) {
        this.pot = pot;
    }

    @SpaceId
    @SpaceRouting
    public BigDecimal getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(final BigDecimal tournamentId) {
        this.tournamentId = tournamentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Long getNextEvent() {
        return nextEvent;
    }

    public void setNextEvent(final Long nextEvent) {
        this.nextEvent = nextEvent;
    }

    public DateTime getStartTimeStamp() {
        return startTimeStamp;
    }

    public void setStartTimeStamp(final DateTime startTimeStamp) {
        this.startTimeStamp = startTimeStamp;
    }

    public TournamentStatus getTournamentStatus() {
        return tournamentStatus;
    }

    public void setTournamentStatus(final TournamentStatus tournamentStatus) {
        this.tournamentStatus = tournamentStatus;
    }

    public TournamentVariationTemplate getTournamentVariationTemplate() {
        return tournamentVariationTemplate;
    }

    public void setTournamentVariationTemplate(final TournamentVariationTemplate tournamentVariationTemplate) {
        this.tournamentVariationTemplate = tournamentVariationTemplate;
    }

    public DateTime getSignupStartTimeStamp() {
        return signupStartTimeStamp;
    }

    public void setSignupStartTimeStamp(final DateTime signupStartTimeStamp) {
        this.signupStartTimeStamp = signupStartTimeStamp;
    }

    public DateTime getSignupEndTimeStamp() {
        return signupEndTimeStamp;
    }

    public void setSignupEndTimeStamp(final DateTime signupEndTimeStamp) {
        this.signupEndTimeStamp = signupEndTimeStamp;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<BigDecimal> getTables() {
        return tables;
    }

    public void setTables(final List<BigDecimal> tables) {
        this.tables = tables;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(final String partnerId) {
        this.partnerId = partnerId;
    }

    public Integer getCurrentRoundIndex() {
        return currentRoundIndex;
    }

    public void setCurrentRoundIndex(final Integer currentRoundIndex) {
        this.currentRoundIndex = currentRoundIndex;
    }

    public String getMonitoringMessage() {
        return monitoringMessage;
    }

    public void setMonitoringMessage(final String monitoringMessage) {
        this.monitoringMessage = monitoringMessage;
    }

    public List<BigDecimal> getStartingTables() {
        return startingTables;
    }

    public void setStartingTables(final List<BigDecimal> startingTables) {
        this.startingTables = startingTables;
    }

    public int activePlayerCount() {
        return retrievePlayers().size(TournamentPlayerStatus.ACTIVE);
    }

    public int playerCount() {
        return retrievePlayers().size();
    }

    public Set<BigDecimal> players() {
        final Set<BigDecimal> playerIds = new HashSet<>();
        for (final TournamentPlayer player : retrievePlayers()) {
            playerIds.add(player.getPlayerId());
        }
        return playerIds;
    }

    public Set<TournamentPlayer> tournamentPlayers() {
        final Set<TournamentPlayer> tournamentPlayers = new HashSet<>();
        for (final TournamentPlayer player : retrievePlayers()) {
            tournamentPlayers.add(new TournamentPlayer(player));
        }
        return tournamentPlayers;
    }

    public TournamentPlayer findPlayer(final BigDecimal playerId) {
        notNull(playerId, "Player ID may not be null");
        final TournamentPlayer player = retrievePlayers().getByPlayerId(playerId);
        if (player != null) {
            return new TournamentPlayer(player);
        }
        return null;
    }

    TournamentPlayers retrievePlayers() {
        if (players == null) {
            players = new TournamentPlayers();
        }
        return players;
    }

    // for GS use only

    public TournamentPlayers getPlayers() {
        return players;
    }

    // for GS use only

    public void setPlayers(final TournamentPlayers players) {
        this.players = players;
    }

    private TournamentLeaderboard retrieveLeaderboard(final TournamentHost tournamentHost) {
        if (tournamentLeaderboard == null) {
            LOG.debug("Creating new tournament leaderboard for tournament {}", tournamentId);
            tournamentLeaderboard = new TournamentLeaderboard(tournamentId);
            tournamentLeaderboard.updatePlayersAndBalances(tournamentHost, retrievePlayers());
        }
        return tournamentLeaderboard;
    }

    public void setTournamentLeaderboard(final TournamentLeaderboard tournamentLeaderboard) {
        this.tournamentLeaderboard = tournamentLeaderboard;
    }

    public TournamentLeaderboard getTournamentLeaderboard() {
        return tournamentLeaderboard;
    }

    @SpaceIndex
    public Boolean getLeaderboardUpdatesRequired() {
        if (tournamentStatus == null) {
            return null;
        }

        switch (tournamentStatus) {
            case RUNNING:
            case WAITING_FOR_CLIENTS:
                return Boolean.TRUE;

            default:
                return Boolean.FALSE;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setLeaderboardUpdatesRequired(final Boolean leaderboardUpdatesRequired) {
        // required by Gigaspaces for indexing
    }

    private BigDecimal retrieveCurrentRoundGameVariationTemplateId() {
        if (currentRoundIndex != null) {
            return retrieveCurrentRound().getGameVariationTemplateId();
        }
        return null;
    }

    TournamentVariationRound retrieveCurrentRound() {
        if (currentRoundIndex == null
                || currentRoundIndex < 0
                || currentRoundIndex >= tournamentVariationTemplate.getTournamentRounds().size()) {
            return null;
        }
        return tournamentVariationTemplate.getTournamentRounds().get(currentRoundIndex);
    }

    TournamentVariationRound retrieveNextRound() {
        if (currentRoundIndex == null
                || currentRoundIndex < 0
                || (currentRoundIndex + 1) >= tournamentVariationTemplate.getTournamentRounds().size()) {
            return null;
        }
        return tournamentVariationTemplate.getTournamentRounds().get(currentRoundIndex + 1);
    }

    private void incrementRoundCounter() {
        if (currentRoundIndex == null) {
            throw new IllegalStateException("Round counter should have been initialized at Tournament start.");
        }
        if (currentRoundIndex + 1 >= tournamentVariationTemplate.getTournamentRounds().size()) {
            final String errorString = String.format(
                    "Tournament %s is trying to incremented past max number of rounds (%s).",
                    tournamentId, tournamentVariationTemplate.getTournamentRounds().size()
            );
            LOG.error(errorString);
            throw new IllegalStateException(errorString);
        }
        currentRoundIndex++;
    }

    private void initialiseRoundCounter() {
        if (tournamentVariationTemplate.getTournamentRounds() == null
                || tournamentVariationTemplate.getTournamentRounds().size() == 0) {
            final String errorString = String.format("Tournament %s has been created with no rounds.", tournamentId);
            LOG.error(errorString);
            throw new IllegalStateException(errorString);
        }
        if (currentRoundIndex == null) {
            currentRoundIndex = 0;
        }
    }

    public String retrieveTournamentGameType() {
        return tournamentVariationTemplate.getGameType();
    }

    public BigDecimal calculateUnpaidPrizePool() {
        final int numberPlayers = playerCount();
        final BigDecimal unpaidPrizePool = retrievePayoutCalculator().calculateUnpaidPrizePool(
                numberPlayers, tournamentVariationTemplate);
        LOG.debug("Tournament [{}], players [{}] has an unpaid prize pool of [{}]",
                tournamentId, numberPlayers, unpaidPrizePool);
        return unpaidPrizePool;
    }

    /**
     * Process any un-processed and pending events on the tournament.
     * <p/>GS
     * For example, if the tournament start time has passed and the tournamnet has not yet started running,
     * it will start the tournament.
     *
     * @param tournamentHost the current tournament host.
     */
    public void processEvent(final TournamentHost tournamentHost) {
        notNull(tournamentHost, "Tournament Host may not be null");
        LOG.debug("Handling event for tournament {} in status {}", tournamentId, tournamentStatus);

        switch (tournamentStatus) {
            case FINISHED:
            case CANCELLING:
            case CLOSED:
                // events are not responsible for changes in these states
                break;

            case SETTLED:
            case CANCELLED:
                handleSettledAndCancelled(tournamentHost);
                break;

            case ANNOUNCED:
                handleAnnounced(tournamentHost);
                break;

            case REGISTERING:
                handleRegistering(tournamentHost);
                break;

            case RUNNING:
                handleRunning(tournamentHost);
                break;

            case WAITING_FOR_CLIENTS:
                handleWaitingForClients(tournamentHost);
                break;

            case ON_BREAK:
                handleOnBreak(tournamentHost);
                break;

            default:
                throw new IllegalStateException("Unknown state: " + tournamentStatus);
        }

        LOG.debug("Completed event for tournament {} now in status {}", tournamentId, tournamentStatus);
    }

    public void validateNewTournament() {
        if (tournamentStatus != TournamentStatus.ANNOUNCED) {
            throw new IllegalStateException("Tournament is in an invalid state: " + tournamentStatus);
        }

        if (signupStartTimeStamp == null) {
            throw new IllegalStateException("Tournament has no signup start time");
        }
        setNextEvent(signupStartTimeStamp.getMillis());
    }

    /**
     * Settle the tournament.
     *
     * @param tournamentHost the tournament host
     * @throws IllegalStateException if the tournament is in an invalid state to be settled.
     */
    public void settle(final TournamentHost tournamentHost) {
        if (tournamentStatus != TournamentStatus.FINISHED) {
            // any other transition to ANNOUNCED will be handled by events
            throw new IllegalStateException("Tournament is in an invalid state: " + tournamentStatus);
        }

        LOG.debug("Updating leaderboard for the last time for tournament {} (tournamentId={})", name, tournamentId);
        retrieveLeaderboard(tournamentHost).updateLeaderboard(this, tournamentHost, false, false);

        LOG.debug("Settling {} (tournamentId={})", name, tournamentId);
        final InternalWalletService walletService = tournamentHost.getInternalWalletService();
        final BigDecimal prizePot = calculateUnpaidPrizePool();

        final Set<TournamentPlayer> playerSet = toSet(this.players);
        final Map<TournamentPlayer, BigDecimal> individualPayouts = retrievePayoutCalculator().calculatePayouts(
                playerSet, tournamentVariationTemplate);
        LOG.debug("Payouts for Tournament [{}] are [{}]", tournamentId, individualPayouts);
        final Set<TournamentPlayer> winners = individualPayouts.keySet();
        final Map<BigDecimal, BigDecimal> idToPlayer = fetchPlayers(tournamentHost, winners);
        try {
            for (TournamentPlayer player : winners) {
                final BigDecimal playerId = player.getPlayerId();
                final BigDecimal accountId = idToPlayer.get(playerId);
                final BigDecimal amount = individualPayouts.get(player);
                player.setSettledPrize(amount);
                if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    final String reference = "Tournament (" + tournamentId + ") payout for player " + playerId;
                    walletService.postTransaction(accountId, amount, TOURNAMENT_PAYOUT_TYPE, reference,
                            transactionContextFor(player.getPlayerId(), tournamentHost));
                }
                LOG.debug("Preparing to pay {} to {}", amount, player);
            }
        } catch (WalletServiceException e) {
            throw new IllegalStateException(e);
        }
        this.settledPrizePot = prizePot;

        final long currentTime = tournamentHost.getTimeSource().getCurrentTimeStamp();
        nextEvent = currentTime + tournamentVariationTemplate.getExpiryDelay();

        changeStatus(TournamentStatus.SETTLED, tournamentHost);
        LOG.debug("Tournament {} finished settlement", tournamentId);
    }

    private static Set<TournamentPlayer> toSet(final TournamentPlayers players) {
        final Set<TournamentPlayer> setOfPlayers = new HashSet<>(players.size());
        for (TournamentPlayer player : players) {
            setOfPlayers.add(player);
        }
        return setOfPlayers;
    }

    private TournamentPayoutCalculator retrievePayoutCalculator() {
        if (tournamentPayoutCalculator == null) {
            tournamentPayoutCalculator = new TournamentPayoutCalculator();
        }
        return tournamentPayoutCalculator;
    }

    /**
     * Change the status of the tournament to the new value.
     *
     * @param newStatus      the new status. Not null.
     * @param tournamentHost the host
     */
    void changeStatus(final TournamentStatus newStatus,
                      final TournamentHost tournamentHost) {
        notNull(newStatus, "Status may not be null");

        if (tournamentStatus == null || tournamentStatus.isValidSuccessor(newStatus)) {
            final boolean shouldCloseAccounts = (tournamentStatus != null && !tournamentStatus.isCloseAccounts())
                    && newStatus.isCloseAccounts();
            this.tournamentStatus = newStatus;
            if (shouldCloseAccounts) {
                for (TournamentPlayer player : retrievePlayers()) {
                    tournamentHost.getInternalWalletService().closeAccount(player.getAccountId());
                }
            }
        } else {
            throw new IllegalStateException(String.format("Status %s is not a valid successor status to %s",
                    newStatus, tournamentStatus));
        }
    }

    private boolean isTableClosureComplete(final TournamentHost tournamentHost) {
        assert tournamentHost != null;

        return tournamentHost.getTableService().getOpenTableCount(new HashSet<>(tables)) == 0;
    }

    /**
     * Transition a tournment from a waiting state into a new round.
     * <p/>
     * This will re-sort players, assign tables and move into a running state.
     *
     * @param tournamentHost the current tournament host.
     * @throws IllegalStateException if the tournament is in an invaid state.
     */
    public void startRound(final TournamentHost tournamentHost) {
        LOG.debug("Starting round for tournament {}", tournamentId);

        notNull(tournamentHost, "Tournament Host may not be null");
        assert isTableClosureComplete(tournamentHost) : "Tables remain announce";

        final boolean finished = retrieveLeaderboard(tournamentHost).updateLeaderboard(
                this, tournamentHost, false, true);
        if (finished) {
            finish(tournamentHost);
            return;
        }

        incrementRoundCounter();

        changeStatus(TournamentStatus.RUNNING, tournamentHost);
        final Set<TournamentPlayer> activePlayers = retrieveLeaderboard(tournamentHost).getActivePlayers();

        final Client client = tournamentHost.getTableService().findClientById(
                retrieveCurrentRound().getClientPropertiesId());
        final int tableSize = client.getNumberOfSeats();

        final TableAllocator tableAllocator = tournamentHost.getTableAllocator(
                getTournamentVariationTemplate().getAllocator());
        final Collection<PlayerGroup> playersForTables = tableAllocator.allocate(activePlayers, tableSize);
        assert playersForTables.size() <= tables.size()
                : String.format("More tables have been allocated than are available (%s > %s)",
                playersForTables.size(), tables.size());

        restartTables(tournamentHost, playersForTables, retrieveCurrentRound().getClientPropertiesId(),
                retrieveCurrentRoundGameVariationTemplateId());

        LOG.debug("Tournament {} is now {}", tournamentId, tournamentStatus);
    }

    /**
     * Transition a tournment from a running state to waiting.
     * <p/>
     * This will ask all tables to close and move the state to waiting. The
     * caller should then use {@link #checkIfRoundFinished(TournamentHost)}
     * to determine when the round is actually complete.
     *
     * @param tournamentHost the current tournament host.
     * @throws IllegalStateException if the tournament is in an invaid state.
     */
    public void finishRound(final TournamentHost tournamentHost) {
        LOG.debug("Finishing round for tournament {}", tournamentId);

        notNull(tournamentHost, "Tournament Host may not be null");
        if (TournamentStatus.RUNNING == tournamentStatus) {
            final boolean insufficientPlayers = retrieveLeaderboard(tournamentHost).isInsufficientPlayersPresent();

            tournamentHost.getTableService().requestClosing(new HashSet<>(tables));

            if (insufficientPlayers) {
                LOG.debug("Tournament {} has insufficient players, moving to {}", tournamentId, TournamentStatus.ON_BREAK);
                changeStatus(TournamentStatus.ON_BREAK, tournamentHost);
                handleOnBreak(tournamentHost);

            } else {
                LOG.debug("Tournament {} has sufficient players, moving to {}", tournamentId, TournamentStatus.WAITING_FOR_CLIENTS);
                changeStatus(TournamentStatus.WAITING_FOR_CLIENTS, tournamentHost);

                final DateTime currentTime = new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp());
                nextEvent = currentTime.getMillis() + tournamentHost.getPollDelay();
            }

            LOG.debug("Tournament {} is now {}", tournamentId, tournamentStatus);
        } else {
            LOG.debug("Tournament {} is already not running ({})", tournamentId, tournamentStatus);
        }
    }

    /**
     * Determine if the current round is finished.
     *
     * @param tournamentHost the current tournament host.
     * @return true if the round is finished, otherwise false.
     */
    private boolean checkIfRoundFinished(final TournamentHost tournamentHost) {
        LOG.debug("Checking if round finished for tournament {}", tournamentId);

        notNull(tournamentHost, "Tournament Host may not be null");

        if (tournamentStatus != TournamentStatus.WAITING_FOR_CLIENTS) {
            throw new IllegalStateException("Tournamnet is not in waiting state: " + tournamentId);
        }

        return isTableClosureComplete(tournamentHost);
    }

    /**
     * Transition a tournment from a waiting state to finished.
     * <p/>
     * Table closure must have been requested (or no tables may exist) before this method is called,
     * and the status must be ON_BREAK.
     *
     * @param tournamentHost the current tournament host.
     * @throws IllegalStateException if the tournament is in an invaid state.
     */
    public void finish(final TournamentHost tournamentHost) {
        LOG.debug("Finishing tournament {}", tournamentId);

        assert tournamentStatus == TournamentStatus.ON_BREAK;
        assert isTableClosureComplete(tournamentHost) : "Tables remain announce";

        notNull(tournamentHost, "tournamentHost may not be null");

        changeStatus(TournamentStatus.FINISHED, tournamentHost);
        nextEvent = null;

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Tournament %s is now %s", tournamentId, tournamentStatus));
        }
    }

    /**
     * Transition the tournment into the RUNNING state.
     * <p/>
     * This will create tables, assign players and start the games.
     *
     * @param tournamentHost the current tournament host.
     * @throws IllegalStateException if the tournament is in an invaid state for starting.
     */
    public void start(final TournamentHost tournamentHost) {
        LOG.debug("Starting tournament {}", tournamentId);

        notNull(tournamentHost, "Tournament Host may not be null");

        if (tournamentVariationTemplate == null) {
            throw new IllegalStateException("Template may not be null");
        }

        if (startTimeStamp != null && startTimeStamp.isAfter(tournamentHost.getTimeSource().getCurrentTimeStamp())) {
            throw new IllegalStateException("Cannot start tournament: start time is still in the future: "
                    + startTimeStamp);
        }

        final Collection<TournamentPlayer> activePlayers = retrievePlayers().getByStatus(TournamentPlayerStatus.ACTIVE);
        assert activePlayers != null;

        if (tournamentVariationTemplate.getMinPlayers() != null
                && activePlayers.size() < tournamentVariationTemplate.getMinPlayers()) {
            LOG.debug("Tournament will be cancelled due to insufficient players at start: {}", tournamentId);
            changeStatus(TournamentStatus.CANCELLING, tournamentHost);
            return;
        }

        if (tournamentVariationTemplate.getMaxPlayers() != null
                && activePlayers.size() > tournamentVariationTemplate.getMaxPlayers()) {
            throw new IllegalStateException(String.format("Cannot start tournament: require max %s players, found %s",
                    tournamentVariationTemplate.getMaxPlayers(), activePlayers.size()));
        }

        initialiseRoundCounter();
        changeStatus(TournamentStatus.RUNNING, tournamentHost);

        if (tables == null) {
            tables = new ArrayList<>();
        } else {
            tables.clear();
        }

        if (startingTables == null) {
            startingTables = new ArrayList<>();
        } else {
            startingTables.clear();
        }

        final String clientId = retrieveCurrentRound().getClientPropertiesId();
        final Client client = tournamentHost.getTableService().findClientById(clientId);
        if (client == null) {
            throw new IllegalStateException("Client not found " + clientId);
        }

        final TableAllocator tableAllocator = tournamentHost.getTableAllocator(
                getTournamentVariationTemplate().getAllocator());
        final Collection<PlayerGroup> playersForTables = tableAllocator.allocate(
                activePlayers, client.getNumberOfSeats());
        final int numberOfTables = playersForTables.size();

        final List<BigDecimal> newTableIds = tournamentHost.getTableService().createTables(
                numberOfTables, tournamentVariationTemplate.getGameType(),
                retrieveCurrentRoundGameVariationTemplateId(),
                clientId, partnerId, name);
        assert newTableIds != null && newTableIds.size() == numberOfTables;
        tables.addAll(newTableIds);
        startingTables.addAll(newTableIds);
        restartTables(tournamentHost, playersForTables, clientId, retrieveCurrentRoundGameVariationTemplateId());

        LOG.debug("Tournament {} is now {}", tournamentId, tournamentStatus);
    }

    public void restartTables(final TournamentHost tournamentHost,
                              final Collection<PlayerGroup> playersForTables,
                              final String clientId,
                              final BigDecimal variationTemplateId) {
        final Iterator<BigDecimal> currentTableIterator = tables.iterator();
        final List<BigDecimal> usedTables = new ArrayList<>();
        for (final PlayerGroup playersForTable : playersForTables) {
            final BigDecimal tableId = currentTableIterator.next();
            tournamentHost.getTableService().reopenAndStartNewGame(
                    tableId, playersForTable, variationTemplateId, clientId);
            usedTables.add(tableId);

            for (TournamentPlayer tournamentPlayer : playersForTable) {
                retrievePlayers().getByPlayerId(tournamentPlayer.getPlayerId()).setTableId(tableId);
            }

            LOG.debug("Started table {} for tournament {}", tableId, tournamentId);
        }

        tables.clear();
        tables.addAll(usedTables);
    }

    /**
     * Add a player to the tournament.
     *
     * @param player         the player to add.
     * @param tournamentHost the current tournament host.
     * @throws TournamentException if the player cannot be added.
     */
    public void addPlayer(final Player player,
                          final TournamentHost tournamentHost)
            throws TournamentException {
        notNull(player, "Player may not be null");
        notNull(tournamentHost, "Tournament Host may not be null");

        LOG.debug("Tournament {}: Adding player {}", tournamentId, player);

        if (tournamentStatus == null || tournamentStatus != TournamentStatus.REGISTERING) {
            throw new TournamentException(TournamentOperationResult.INVALID_SIGNUP_STATE);
        }

        final DateTime currentTime = new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp());
        if (signupStartTimeStamp.isAfter(currentTime)) {
            throw new TournamentException(TournamentOperationResult.BEFORE_SIGNUP_TIME);
        }
        if (signupEndTimeStamp != null && signupEndTimeStamp.isBefore(currentTime)) {
            throw new TournamentException(TournamentOperationResult.AFTER_SIGNUP_TIME);
        }

        final Integer maxPlayers = tournamentVariationTemplate.getMaxPlayers();
        final TournamentPlayers tournamentPlayers = retrievePlayers();
        if (maxPlayers != null && tournamentPlayers.size() >= maxPlayers) {
            throw new TournamentException(TournamentOperationResult.MAX_PLAYERS_EXCEEDED);
        }

        if (tournamentPlayers.contains(player.getPlayerId())) {
            throw new TournamentException(TournamentOperationResult.PLAYER_ALREADY_REGISTERED);
        }

        final String concatenatedName = player.getName();
        final BigDecimal accountId;
        try {
            accountId = tournamentHost.getInternalWalletService().createAccount(concatenatedName + PLAYER_ACCOUNT_SUFFIX);
        } catch (WalletServiceException e) {
            LOG.error("Account creation failed", e);
            throw new TournamentException(TournamentOperationResult.UNKNOWN);
        }

        final TournamentPlayer newPlayer = new TournamentPlayer(
                player.getPlayerId(), player.getName(), accountId, TournamentPlayerStatus.ADDITION_PENDING);
        tournamentPlayers.add(newPlayer);

        try {
            processFees(player, TournamentPlayerTransferType.CHARGE, tournamentHost);
            newPlayer.changeStatus(TournamentPlayerStatus.ACTIVE, tournamentHost);

        } catch (TournamentException e) {
            LOG.info("Unable to add player {}; removing", newPlayer.getPlayerId(), e);
            tournamentPlayers.removeByPlayerId(newPlayer.getPlayerId());
            throw e;
        }

        LOG.debug("Tournament {}: Added player {}, total players {}", tournamentId, newPlayer, tournamentPlayers.size());
    }

    /**
     * Remove a player from a tournament.
     *
     * @param player         the player to remove.
     * @param tournamentHost the current tournament host.
     * @throws TournamentException if the player cannot be removed.
     */
    public void removePlayer(final Player player,
                             final TournamentHost tournamentHost)
            throws TournamentException {
        notNull(player, "Player may not be null");
        notNull(tournamentHost, "Tournament Host may not be null");

        LOG.debug("Tournament {}: Removing player {}", tournamentId, player);

        final TournamentPlayer tournamentPlayer = retrievePlayers().getByPlayerId(player.getPlayerId());
        if (tournamentPlayer == null) {
            throw new TournamentException(TournamentOperationResult.PLAYER_NOT_REGISTERED);
        }

        if (tournamentStatus == null || tournamentStatus != TournamentStatus.REGISTERING) {
            throw new TournamentException(TournamentOperationResult.INVALID_SIGNUP_STATE);
        }

        final DateTime currentTime = new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp());
        if (signupStartTimeStamp.isAfter(currentTime)) {
            throw new TournamentException(TournamentOperationResult.BEFORE_SIGNUP_TIME);
        }
        if (signupEndTimeStamp != null && signupEndTimeStamp.isBefore(currentTime)) {
            throw new TournamentException(TournamentOperationResult.AFTER_SIGNUP_TIME);
        }

        if (tournamentPlayer.getStatus() != TournamentPlayerStatus.REFUNDED) {
            if (tournamentPlayer.getStatus() != TournamentPlayerStatus.REMOVAL_PENDING) {
                tournamentPlayer.changeStatus(TournamentPlayerStatus.REMOVAL_PENDING, tournamentHost);
            }
            processFees(player, TournamentPlayerTransferType.REFUND, tournamentHost);
        }
        retrievePlayers().removeByPlayerId(player.getPlayerId());
    }

    public int getNextBreakLength() {
        final List<TournamentVariationRound> rounds = getTournamentVariationTemplate().getTournamentRounds();
        final int startFrom;
        if (tournamentStatus == TournamentStatus.ON_BREAK || tournamentStatus == TournamentStatus.WAITING_FOR_CLIENTS) {
            startFrom = currentRoundIndex + 1;
        } else {
            startFrom = currentRoundIndex;
        }
        for (int i = startFrom; i < rounds.size(); i++) {
            final TournamentVariationRound round = rounds.get(i);
            final long breakLengthInMillis = round.getRoundEndInterval();
            if (breakLengthInMillis != 0L) {
                return new Duration(breakLengthInMillis).toPeriod().getMinutes();
            }
        }
        return 0;
    }

    public long retrieveEndTimeOfCurrentRound() {
        if (currentRoundIndex == null) {
            throw new IllegalStateException("No round is in progress");
        }

        final TournamentVariationRound currentRound = retrieveCurrentRound();
        return getElapsedTime() + currentRound.getRoundLength();
    }

    public long retrieveNextBreakTime() {
        if (currentRoundIndex == null) {
            throw new IllegalStateException("No round is in progress");
        }
        long millisUntilBreak = 0;
        final int startFrom;
        if (tournamentStatus == TournamentStatus.ON_BREAK
                || tournamentStatus == TournamentStatus.WAITING_FOR_CLIENTS) {
            startFrom = currentRoundIndex + 1;
        } else {
            startFrom = currentRoundIndex;
        }
        final List<TournamentVariationRound> rounds = tournamentVariationTemplate.getTournamentRounds();
        for (int i = startFrom; i < rounds.size(); i++) {
            final TournamentVariationRound round = rounds.get(i);
            millisUntilBreak += round.getRoundLength();
            if (round.getRoundEndInterval() != 0L) {
                break;
            }
        }
        return getElapsedTime() + millisUntilBreak;
    }

    /**
     * Cancel a tournament, refunding all players.
     *
     * @param tournamentHost the current host.
     * @throws TournamentException if the refund fails.
     */
    public void cancel(final TournamentHost tournamentHost) throws TournamentException {
        notNull(tournamentHost, "Tournament Host may not be null");

        if (tournamentStatus == null || tournamentStatus != TournamentStatus.CANCELLING) {
            throw new IllegalStateException("Invalid tournament state: " + tournamentStatus);
        }

        final BigDecimal entryFee = tournamentVariationTemplate.getEntryFee();
        if (entryFee != null && entryFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(String.format(
                    "Tournament entry fee is invalid: %s = %s", tournamentId, entryFee));
        }

        final BigDecimal serviceFee = tournamentVariationTemplate.getServiceFee();
        if (serviceFee != null && serviceFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(String.format(
                    "Tournament service fee is invalid: %s = %s", tournamentId, serviceFee));
        }

        final TournamentPlayers tournamentPlayers = retrievePlayers();
        final Map<BigDecimal, BigDecimal> idToPlayer = fetchPlayers(tournamentHost, tournamentPlayers);

        for (final TournamentPlayer tournamentPlayer : tournamentPlayers) {
            final BigDecimal accountId = idToPlayer.get(tournamentPlayer.getPlayerId());
            if (accountId == null) {
                throw new IllegalArgumentException("Player does not exist: " + tournamentPlayer.getPlayerId());
            }
            try {
                tournamentHost.getInternalWalletService().postTransaction(accountId, getTotalFeeAmount(entryFee, serviceFee),
                        "Tournament Fees Refund", "Refund of Fees for tournament " + name, TransactionContext.EMPTY);

            } catch (WalletServiceException e) {
                throw new TournamentException(TournamentOperationResult.TRANSFER_FAILED);
            }
        }


        final long currentTime = tournamentHost.getTimeSource().getCurrentTimeStamp();
        nextEvent = currentTime + tournamentHost.getCancellationExpiryDelay();

        changeStatus(TournamentStatus.CANCELLED, tournamentHost);
    }

    private Map<BigDecimal, BigDecimal> fetchPlayers(final TournamentHost tournamentHost,
                                                     final Iterable<TournamentPlayer> tournamentPlayers) {
        final Map<BigDecimal, BigDecimal> result = new HashMap<>();
        for (final TournamentPlayer tournamentPlayer : tournamentPlayers) {
            result.put(tournamentPlayer.getPlayerId(), tournamentHost.getPlayerAccountId(tournamentPlayer));
        }
        return result;
    }

    /**
     * Charge entry and service fees to the given player.
     *
     * @param player         the player to charge to. They must have been added to the tournament.
     * @param processType    the type of processing to conduct.
     * @param tournamentHost the current host.
     * @throws TournamentException if the postTransactions fails.
     */
    private void processFees(final Player player,
                             final TournamentPlayerTransferType processType,
                             final TournamentHost tournamentHost)
            throws TournamentException {
        notNull(player, "Player may not be null");
        notNull(tournamentHost, "Tournament Host may not be null");

        final BigDecimal entryFee = tournamentVariationTemplate.getEntryFee();
        if (entryFee != null && entryFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(String.format(
                    "Tournament entry fee is invalid: %s = %s", tournamentId, entryFee));
        }

        final BigDecimal serviceFee = tournamentVariationTemplate.getServiceFee();
        if (serviceFee != null && serviceFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(String.format(
                    "Tournament service fee is invalid: %s = %s", tournamentId, serviceFee));
        }

        final TournamentPlayer tournamentPlayer = retrievePlayers().getByPlayerId(player.getPlayerId());
        if (tournamentPlayer == null) {
            throw new IllegalArgumentException(String.format("Player %s is not valid for tournament %s",
                    player.getPlayerId(), tournamentId));
        }

        if (pot == null) {
            pot = BigDecimal.ZERO;
        }

        final TournamentPlayerStatus pendingStatus;
        try {
            final BigDecimal totalFeeAmount = getTotalFeeAmount(entryFee, serviceFee);
            if (processType == TournamentPlayerTransferType.REFUND) { // return money to player
                pendingStatus = TournamentPlayerStatus.REFUNDED;
                ensureValidSuccessorState(tournamentPlayer, pendingStatus);
                if (entryFee != null) {
                    pot = pot.subtract(entryFee);
                }

                if (totalFeeAmount.compareTo(BigDecimal.ZERO) != 0) {
                    tournamentHost.getInternalWalletService().postTransaction(
                            player.getAccountId(), getTotalFeeAmount(entryFee, serviceFee),
                            "Tournament Fees Refund", "Refund of Fees for tournament " + name,
                            transactionContextFor(player.getPlayerId(), tournamentHost));
                }
            } else if (processType == TournamentPlayerTransferType.CHARGE) {
                pendingStatus = TournamentPlayerStatus.CHARGED;
                ensureValidSuccessorState(tournamentPlayer, pendingStatus);

                if (totalFeeAmount.compareTo(BigDecimal.ZERO) != 0) {
                    tournamentHost.getInternalWalletService().postTransaction(
                            player.getAccountId(), BigDecimal.ZERO.subtract(totalFeeAmount),
                            "Tournament Fees", "Fees for tournament " + name,
                            transactionContextFor(player.getPlayerId(), tournamentHost));
                }

                if (entryFee != null) {
                    pot = pot.add(entryFee);
                }

            } else {
                throw new IllegalArgumentException("Unsupported process type: " + processType);
            }
            tournamentPlayer.changeStatus(pendingStatus, tournamentHost);
            processStartingChips(tournamentPlayer, processType, tournamentHost);

        } catch (WalletServiceException e) {
            LOG.debug("Received wallet service exception during registration", e);
            throw new TournamentException(TournamentOperationResult.TRANSFER_FAILED);
        }
    }

    private TransactionContext transactionContextFor(final BigDecimal playerId,
                                                     final TournamentHost tournamentHost) {
        final PlayerSession session = tournamentHost.sessionFor(playerId);
        if (session != null) {
            return transactionContext().withSessionId(session.getSessionId()).build();
        }
        return TransactionContext.EMPTY;
    }

    private void ensureValidSuccessorState(final TournamentPlayer tournamentPlayer,
                                           final TournamentPlayerStatus pendingStatus) {
        notNull(tournamentPlayer, "Tournament Player may not be null");
        notNull(tournamentPlayer.getStatus(), "Tournament Player - Status may not be null");
        notNull(pendingStatus, "Status may not be null");

        if (!tournamentPlayer.getStatus().isValidSuccessor(pendingStatus)) {
            throw new IllegalStateException(String.format("Player %s is not in valid state to move to %s",
                    tournamentPlayer, pendingStatus));
        }
    }

    /**
     * Request a leaderboard update.
     *
     * @param tournamentHost the current tournament host.
     */
    public void updateLeaderboard(final TournamentHost tournamentHost) {
        notNull(tournamentHost, "Tournament Host may not be null");
        final boolean finished = retrieveLeaderboard(tournamentHost).updateLeaderboard(
                this, tournamentHost, true, false);
        if (finished) {
            LOG.debug("Tournament {} round will be finished at instruction of leaderboard", tournamentId);

            finishRound(tournamentHost);
        }
    }

    private void processStartingChips(final TournamentPlayer tournamentPlayer,
                                      final TournamentPlayerTransferType processType,
                                      final TournamentHost tournamentHost) throws WalletServiceException {
        BigDecimal startingChips = tournamentVariationTemplate.getStartingChips();

        if (startingChips != null && startingChips.compareTo(BigDecimal.ZERO) > 0) {
            if (processType == TournamentPlayerTransferType.REFUND) {
                startingChips = BigDecimal.ZERO.subtract(startingChips);
            }
            tournamentHost.getInternalWalletService().postTransaction(tournamentPlayer.getAccountId(),
                    startingChips, "Tournament Chips", "Initial chips for tournament " + tournamentId,
                    transactionContextFor(tournamentPlayer.getPlayerId(), tournamentHost));
        }
    }

    private BigDecimal getTotalFeeAmount(final BigDecimal entryFee,
                                         final BigDecimal serviceFee) {
        if (entryFee != null && serviceFee != null) {
            return entryFee.add(serviceFee);
        } else if (entryFee != null) {
            return entryFee;
        } else if (serviceFee != null) {
            return serviceFee;
        }
        return BigDecimal.ZERO;
    }


    public void handleAnnounced(final TournamentHost tournamentHost) {
        assert tournamentHost != null;
        assert tournamentStatus == TournamentStatus.ANNOUNCED;
        assert startTimeStamp != null;

        final DateTime currentTime = new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp());

        if (currentTime.isAfter(startTimeStamp)) {
            start(tournamentHost);

            if (retrieveCurrentRound() != null) { // rounds do not exist in some states, e.g. CANCELLING
                final long roundLength = retrieveCurrentRound().getRoundLength();
                nextEvent = startTimeStamp.getMillis() + roundLength;
            }

        } else if (currentTime.isAfter(signupStartTimeStamp)) {
            changeStatus(TournamentStatus.REGISTERING, tournamentHost);
            if (signupEndTimeStamp != null) {
                nextEvent = signupEndTimeStamp.getMillis();
            } else {
                nextEvent = startTimeStamp.getMillis();
            }
            setWarningAsNextEvent(currentTime, tournamentHost);
        }
    }

    public boolean calculateShouldSendWarningOfImpendingStart(final TournamentHost tournamentHost) {
        LOG.debug("start calculateShouldSendWarningOfImpendingStart  warningBeforeStartMillis={}",
                tournamentHost.getWarningBeforeStartMillis());
        final long warningTimeStamp = startTimeStamp.getMillis() - tournamentHost.getWarningBeforeStartMillis();
        LOG.debug("BEFORE warningTimeStamp={} nextEvent={}", warningTimeStamp, nextEvent);
        return nextEvent != null && nextEvent == warningTimeStamp;
    }

    public void warningWasSent(final TournamentHost tournamentHost) {
        LOG.debug("warning was sent");
        final DateTime currentTime = new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp());

        final boolean afterStartTime = startTimeStamp != null && currentTime.isAfter(startTimeStamp);
        final boolean afterSignupTime = signupEndTimeStamp != null && currentTime.isAfter(signupEndTimeStamp);

        if (!afterSignupTime && signupEndTimeStamp != null) {
            nextEvent = signupEndTimeStamp.getMillis();
            LOG.debug("setting nextevent to signupEndTimeStamp {}", nextEvent);
        } else if (!afterStartTime && startTimeStamp != null) {
            nextEvent = startTimeStamp.getMillis();
            LOG.debug("setting nextevent to startTimeStamp {}", nextEvent);
        }
    }

    private void setWarningAsNextEvent(final DateTime currentTime,
                                       final TournamentHost tournamentHost) {
        LOG.debug("start setWarningAsNextEvent currentTime={} warningBeforeStartMillis={}",
                currentTime, tournamentHost.getWarningBeforeStartMillis());
        final long warningTimeStamp = startTimeStamp.getMillis() - tournamentHost.getWarningBeforeStartMillis();
        LOG.debug("BEFORE warningTimeStamp={} nextEvent={}", warningTimeStamp, nextEvent);
        if (currentTime.isBefore(warningTimeStamp)) {
            if (nextEvent == null) {
                nextEvent = warningTimeStamp;
            } else {
                nextEvent = Math.min(warningTimeStamp, nextEvent);
            }
            LOG.debug("AFTER warningTimeStamp={} nextEvent={}", warningTimeStamp, nextEvent);
        }
    }

    private void handleRegistering(final TournamentHost tournamentHost) {
        assert tournamentHost != null;
        assert tournamentStatus == TournamentStatus.REGISTERING;

        final DateTime currentTime = new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp());

        final boolean afterStartTime = startTimeStamp != null && currentTime.isAfter(startTimeStamp);
        final boolean afterSignupTime = signupEndTimeStamp != null && currentTime.isAfter(signupEndTimeStamp);

        if ((afterStartTime || afterSignupTime) && !verifySignupCriteria(tournamentHost)) {
            changeStatus(TournamentStatus.CANCELLING, tournamentHost);
            nextEvent = null;
            return;
        }

        if (afterStartTime) {
            start(tournamentHost);
            nextEvent = startTimeStamp.getMillis() + retrieveCurrentRound().getRoundLength();
        } else if (afterSignupTime) {
            changeStatus(TournamentStatus.ANNOUNCED, tournamentHost);
            nextEvent = startTimeStamp.getMillis();
            setWarningAsNextEvent(currentTime, tournamentHost);
        }
    }

    private boolean verifySignupCriteria(final TournamentHost tournamentHost) {
        assert tournamentHost != null;

        final Collection<TournamentPlayer> activePlayers = retrievePlayers().getByStatus(TournamentPlayerStatus.ACTIVE);
        assert activePlayers != null;

        if (tournamentVariationTemplate.getMinPlayers() != null
                && activePlayers.size() < tournamentVariationTemplate.getMinPlayers()) {
            LOG.debug("Tournament {} will be cancelled due to insufficient players at signup closure", tournamentId);
            return false;
        }

        return true;
    }

    private void handleSettledAndCancelled(final TournamentHost tournamentHost) {
        assert tournamentHost != null;
        assert tournamentStatus == TournamentStatus.SETTLED || tournamentStatus == TournamentStatus.CANCELLED;

        LOG.debug("Closing tournament {} in status {}", tournamentId, tournamentStatus);

        changeStatus(TournamentStatus.CLOSED, tournamentHost);
    }

    private void handleRunning(final TournamentHost tournamentHost) {
        assert tournamentHost != null;
        assert tournamentStatus == TournamentStatus.RUNNING;

        final DateTime currentTime = new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp());

        if (isCurrentRoundEnded(currentTime)) {
            LOG.debug("Tournament {} round has ended", tournamentId);
            finishRound(tournamentHost);
        }
    }

    private void handleWaitingForClients(final TournamentHost tournamentHost) {
        assert tournamentHost != null;
        assert tournamentStatus == TournamentStatus.WAITING_FOR_CLIENTS;

        if (checkIfRoundFinished(tournamentHost)) {
            LOG.debug("Tournament {} has no open tables, moving to ON_BREAK", tournamentId);

            changeStatus(TournamentStatus.ON_BREAK, tournamentHost);
            handleOnBreak(tournamentHost); // no need to poll for this one
        } else {
            LOG.debug("Tournament {} has tables still open, will poll in {}", tournamentId, tournamentHost.getPollDelay());

            final DateTime currentTime = new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp());
            nextEvent = currentTime.getMillis() + tournamentHost.getPollDelay();
        }
    }

    private void handleOnBreak(final TournamentHost tournamentHost) {
        assert tournamentHost != null;
        assert tournamentStatus == TournamentStatus.ON_BREAK;

        final DateTime currentTime = new DateTime(tournamentHost.getTimeSource().getCurrentTimeStamp());

        if (!hasMoreRounds()) {
            LOG.debug("Tournament {} is finished (no more rounds)", tournamentId);
            finish(tournamentHost);

        } else if (isNextRoundStarted(currentTime)) {
            LOG.debug("Tournament {} is due to start another round", tournamentId);
            startRound(tournamentHost);
            nextEvent = retrieveEndTimeOfCurrentRound();

        } else {
            final boolean finished = retrieveLeaderboard(tournamentHost).updateLeaderboard(
                    this, tournamentHost, false, false);
            if (finished) {
                LOG.debug("Tournament {} will be finished at leaderboard's request", tournamentId);
                finish(tournamentHost);

            } else {
                LOG.debug("Tournament {} will sleep until next round start", tournamentId);
                nextEvent = getNextRoundStartTime();
            }
        }
    }

    private boolean hasMoreRounds() {
        if (currentRoundIndex == null) {
            throw new IllegalStateException("The tournament has no current round");
        }

        return (currentRoundIndex + 1) < tournamentVariationTemplate.getTournamentRounds().size();
    }

    private boolean isCurrentRoundEnded(final DateTime currentTime) {
        if (currentRoundIndex == null) {
            return true;
        }

        if (startTimeStamp == null) {
            throw new IllegalStateException("Cannot calculate round end time as start time is null");
        }

        final TournamentVariationRound currentRound = retrieveCurrentRound();
        final long roundEndTime = getElapsedTime() + currentRound.getRoundLength();

        return currentTime.isAfter(roundEndTime);
    }

    private boolean isNextRoundStarted(final DateTime currentTime) {
        if (currentRoundIndex == null) {
            return false;
        }

        if (startTimeStamp == null) {
            throw new IllegalStateException("Cannot calculate round end time as start time is null");
        }

        return currentTime.isAfter(getNextRoundStartTime());
    }

    public long getNextRoundStartTime() {
        if (currentRoundIndex == null) {
            throw new IllegalStateException("No round is in progress");
        }

        final TournamentVariationRound currentRound = retrieveCurrentRound();
        final long currentRoundDuration = currentRound.getRoundLength() + currentRound.getRoundEndInterval();

        return getElapsedTime() + currentRoundDuration;
    }

    private long getElapsedTime() {
        long elapsedTime = startTimeStamp.getMillis();
        for (int i = 0; i < currentRoundIndex; ++i) { // build elapsed round time
            final TournamentVariationRound round = tournamentVariationTemplate.getTournamentRounds().get(i);
            elapsedTime += round.getRoundLength() + round.getRoundEndInterval();
        }
        return elapsedTime;
    }

    public BigDecimal getSettledPrizePot() {
        return settledPrizePot;
    }

    public void setSettledPrizePot(final BigDecimal settledPrizePot) {
        this.settledPrizePot = settledPrizePot;
    }

    public TournamentPayoutCalculator getPayoutCalculator() {
        return retrievePayoutCalculator();
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

        final Tournament rhs = (Tournament) obj;
        return new EqualsBuilder()
                .append(pot, rhs.pot)
                .append(startTimeStamp, rhs.startTimeStamp)
                .append(signupStartTimeStamp, rhs.signupStartTimeStamp)
                .append(signupEndTimeStamp, rhs.signupEndTimeStamp)
                .append(tournamentStatus, rhs.tournamentStatus)
                .append(tournamentVariationTemplate, rhs.tournamentVariationTemplate)
                .append(name, rhs.name)
                .append(description, rhs.description)
                .append(nextEvent, rhs.nextEvent)
                .append(tables, rhs.tables)
                .append(partnerId, rhs.partnerId)
                .append(currentRoundIndex, rhs.currentRoundIndex)
                .append(settledPrizePot, rhs.settledPrizePot)
                .isEquals()
                && BigDecimals.equalByComparison(tournamentId, rhs.tournamentId);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(BigDecimals.strip(tournamentId))
                .append(pot)
                .append(startTimeStamp)
                .append(signupStartTimeStamp)
                .append(signupEndTimeStamp)
                .append(tournamentStatus)
                .append(tournamentVariationTemplate)
                .append(name)
                .append(description)
                .append(nextEvent)
                .append(tables)
                .append(partnerId)
                .append(currentRoundIndex)
                .append(settledPrizePot)
                .toHashCode();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
