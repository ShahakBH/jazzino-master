package com.yazino.platform.processor.tournament;


import com.yazino.game.api.time.TimeSource;
import com.yazino.platform.messaging.DocumentDispatcher;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.session.PlayerSession;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.session.PlayerSessionRepository;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.tournament.TournamentTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * Container for static references required by domain business logic.
 */
@Component
public class TournamentHost {

    private static final long FIVE_SECONDS = 5000L;
    private static final long TWO_HOURS = 7200000L;
    private static final long FIVE_MINUTES = 300000L;

    private final TimeSource timeSource;
    private final InternalWalletService internalWalletService;
    private final TournamentTableService tableService;
    private final TournamentRepository tournamentRepository;
    private final TableAllocatorFactory tableAllocatorFactory;
    private final PlayerRepository playerRepository;
    private final PlayerSessionRepository playerSessionRepository;
    private final DocumentDispatcher documentDispatcher;
    private final TournamentPlayerStatisticPublisher tournamentPlayerStatisticPublisher;

    private long pollDelay = FIVE_SECONDS;
    private long cancellationExpiryDelay = TWO_HOURS;
    private Long warningBeforeStartMillis = FIVE_MINUTES;

    @Autowired(required = true)
    public TournamentHost(final TimeSource timeSource,
                          final InternalWalletService internalWalletService,
                          final TournamentTableService tableService,
                          final TournamentRepository tournamentRepository,
                          final TableAllocatorFactory tableAllocatorFactory,
                          final PlayerRepository playerRepository,
                          final PlayerSessionRepository playerSessionRepository,
                          @Qualifier("documentDispatcher") final DocumentDispatcher documentDispatcher,
                          final TournamentPlayerStatisticPublisher tournamentPlayerStatisticPublisher) {
        notNull(timeSource, "Time Source may not be null");
        notNull(internalWalletService, "internalWalletService may not be null");
        notNull(tableService, "Table Service may not be null");
        notNull(tournamentRepository, "Tournament Repository may not be null");
        notNull(tableAllocatorFactory, "Table Allocator Factory may not be null");
        notNull(playerRepository, "playerRepository may not be null");
        notNull(playerSessionRepository, "playerSessionRepository repository may not be null");
        notNull(documentDispatcher, "documentDispatcher may not be null");
        notNull(tournamentPlayerStatisticPublisher, "tournamentPlayerStatisticPublisher is null");

        this.documentDispatcher = documentDispatcher;
        this.timeSource = timeSource;
        this.internalWalletService = internalWalletService;
        this.tableService = tableService;
        this.tournamentRepository = tournamentRepository;
        this.tableAllocatorFactory = tableAllocatorFactory;
        this.playerRepository = playerRepository;
        this.playerSessionRepository = playerSessionRepository;
        this.tournamentPlayerStatisticPublisher = tournamentPlayerStatisticPublisher;
    }

    /**
     * Get the current time source. This should be used to determine the current time
     * rather than relying on Date/DateTime/System.currentTimeMillis().
     *
     * @return the current time source.
     */
    public TimeSource getTimeSource() {
        return timeSource;
    }

    public InternalWalletService getInternalWalletService() {
        return internalWalletService;
    }

    public TournamentTableService getTableService() {
        return tableService;
    }

    public TournamentRepository getTournamentRepository() {
        return tournamentRepository;
    }

    public TableAllocator getTableAllocator(final String allocatorId) {
        return tableAllocatorFactory.byId(allocatorId);
    }

    public long getPollDelay() {
        return pollDelay;
    }

    @Value("${strata.tournament.poll-delay}")
    public void setPollDelay(final long pollDelay) {
        this.pollDelay = pollDelay;
    }

    public long getCancellationExpiryDelay() {
        return cancellationExpiryDelay;
    }

    @Value("${strata.tournament.cancellation-expiry-delay}")
    public void setCancellationExpiryDelay(final long cancellationExpiryDelay) {
        this.cancellationExpiryDelay = cancellationExpiryDelay;
    }

    public BigDecimal getPlayerAccountId(final TournamentPlayer tournamentPlayer) {
        notNull(tournamentPlayer, "tournamentPlayer may not be null");
        final Player player = playerRepository.findById(tournamentPlayer.getPlayerId());
        if (player == null) {
            throw new IllegalArgumentException("Invalid player: " + tournamentPlayer.getPlayerId());
        }
        return player.getAccountId();
    }

    public PlayerSession sessionFor(final BigDecimal playerId) {
        final Collection<PlayerSession> allSessions = playerSessionRepository.findAllByPlayer(playerId);
        if (allSessions != null && allSessions.size() == 1) {
            return allSessions.iterator().next();
        }
        return null; // we can't determine which we're using at present
    }

    public boolean isPlayerOnline(final BigDecimal playerId) {
        return playerSessionRepository.isOnline(playerId);
    }

    public DocumentDispatcher getDocumentDispatcher() {
        return documentDispatcher;
    }

    public Long getWarningBeforeStartMillis() {
        return warningBeforeStartMillis;
    }

    public void setWarningBeforeStartMillis(final Long warningBeforeStartMillis) {
        this.warningBeforeStartMillis = warningBeforeStartMillis;
    }

    public TournamentPlayerStatisticPublisher getTournamentPlayerStatisticPublisher() {
        return tournamentPlayerStatisticPublisher;
    }
}
