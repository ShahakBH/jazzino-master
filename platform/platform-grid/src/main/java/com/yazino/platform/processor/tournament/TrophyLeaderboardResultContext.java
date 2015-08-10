package com.yazino.platform.processor.tournament;

import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.TrophyRepository;
import com.yazino.platform.repository.session.InboxMessageRepository;
import com.yazino.platform.repository.tournament.TrophyLeaderboardResultRepository;
import com.yazino.platform.service.account.InternalWalletService;
import com.yazino.platform.service.audit.AuditLabelFactory;
import com.yazino.platform.service.tournament.AwardTrophyService;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.yazino.game.api.time.TimeSource;

import java.io.Serializable;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * This class contains the various services and repositories that are required to process leaderboard results.
 */
public class TrophyLeaderboardResultContext implements Serializable {
    private static final long serialVersionUID = -1691579215916347515L;

    private final TrophyLeaderboardResultRepository trophyLeaderboardResultRepository;
    private final InternalWalletService internalWalletService;
    private final PlayerRepository playerRepository;
    private final AwardTrophyService awardTrophyService;
    private final TrophyRepository trophyRepository;
    private final InboxMessageRepository inboxMessageGlobalRepository;
    private final AuditLabelFactory auditor;
    private final TimeSource timeSource;

    public TrophyLeaderboardResultContext(final TrophyLeaderboardResultRepository trophyLeaderboardResultRepository,
                                          final InternalWalletService internalWalletService,
                                          final PlayerRepository playerRepository,
                                          final AwardTrophyService awardTrophyService,
                                          final InboxMessageRepository inboxMessageGlobalRepository,
                                          final TrophyRepository trophyRepository,
                                          final AuditLabelFactory auditor,
                                          final TimeSource timeSource) {
        notNull(trophyLeaderboardResultRepository, "trophyLeaderboardResultRepository must not be null");
        notNull(internalWalletService, "internalWalletService must not be null");
        notNull(playerRepository, "playerRepository must not be null");
        notNull(awardTrophyService, "awardTrophyService must not be null");
        notNull(trophyRepository, "trophyRepository must not be null");
        notNull(inboxMessageGlobalRepository, "inboxMessageGlobalRepository must not be null");
        notNull(auditor, "auditor must not be null");
        notNull(timeSource, "timeSource must not be null");

        this.trophyLeaderboardResultRepository = trophyLeaderboardResultRepository;
        this.internalWalletService = internalWalletService;
        this.playerRepository = playerRepository;
        this.awardTrophyService = awardTrophyService;
        this.trophyRepository = trophyRepository;
        this.inboxMessageGlobalRepository = inboxMessageGlobalRepository;
        this.auditor = auditor;
        this.timeSource = timeSource;
    }

    public TimeSource getTimeSource() {
        return timeSource;
    }

    public TrophyLeaderboardResultRepository getTrophyLeaderboardResultRepository() {
        return trophyLeaderboardResultRepository;
    }

    public InternalWalletService getInternalWalletService() {
        return internalWalletService;
    }

    public PlayerRepository getPlayerRepository() {
        return playerRepository;
    }

    public AwardTrophyService getAwardTrophyService() {
        return awardTrophyService;
    }

    public TrophyRepository getTrophyRepository() {
        return trophyRepository;
    }

    public InboxMessageRepository getInboxMessageGlobalRepository() {
        return inboxMessageGlobalRepository;
    }

    public AuditLabelFactory getAuditor() {
        return auditor;
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

        final TrophyLeaderboardResultContext rhs = (TrophyLeaderboardResultContext) obj;
        return new EqualsBuilder()
                .append(trophyLeaderboardResultRepository, rhs.trophyLeaderboardResultRepository)
                .append(internalWalletService, rhs.internalWalletService)
                .append(playerRepository, rhs.playerRepository)
                .append(awardTrophyService, rhs.awardTrophyService)
                .append(trophyRepository, rhs.trophyRepository)
                .append(inboxMessageGlobalRepository, rhs.inboxMessageGlobalRepository)
                .append(auditor, rhs.auditor)
                .append(timeSource, rhs.timeSource)
                .isEquals();

    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 41)
                .append(trophyLeaderboardResultRepository)
                .append(internalWalletService)
                .append(playerRepository)
                .append(awardTrophyService)
                .append(trophyRepository)
                .append(inboxMessageGlobalRepository)
                .append(auditor)
                .append(timeSource)
                .toHashCode();
    }
}
