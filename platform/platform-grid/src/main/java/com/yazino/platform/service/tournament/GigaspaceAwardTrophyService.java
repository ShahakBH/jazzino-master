package com.yazino.platform.service.tournament;

import com.yazino.platform.community.Trophy;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PlayerTrophy;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.PlayerTrophyRepository;
import com.yazino.platform.repository.community.TrophyRepository;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yazino.game.api.time.TimeSource;

import java.math.BigDecimal;

import static org.apache.commons.lang3.Validate.notNull;

/**
 * TODO: this should probably be converted to a PU for efficiency
 */
@Service
public class GigaspaceAwardTrophyService implements AwardTrophyService {
    private final TimeSource timeSource;
    private final PlayerTrophyRepository playerTrophyRepository;
    private final TrophyRepository trophyRepository;
    private final PlayerRepository playerRepository;

    @Autowired
    public GigaspaceAwardTrophyService(final PlayerTrophyRepository playerTrophyGlobalRepository,
                                       final TrophyRepository trophyRepository,
                                       final PlayerRepository playerRepository,
                                       final TimeSource timeSource) {
        notNull(playerTrophyGlobalRepository, "playerTrophyGlobalRepository may not be null");
        notNull(trophyRepository, "trophyRepository may not be null");
        notNull(playerRepository, "playerRepository may not be null");
        notNull(timeSource, "timeSource may not be null");

        this.timeSource = timeSource;
        this.trophyRepository = trophyRepository;
        this.playerTrophyRepository = playerTrophyGlobalRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    public void awardTrophy(final BigDecimal playerId,
                            final BigDecimal trophyId) {
        notNull(playerId, "playerId must not null");
        notNull(trophyId, "trophyId must not null");

        final Player player = playerRepository.findById(playerId);
        notNull(player, "player with playerId " + playerId + " does not exist");

        final Trophy trophy = trophyRepository.findById(trophyId);
        notNull(trophy, "trophy with trophyId " + trophyId + " does not exist");

        final PlayerTrophy playerTrophy = new PlayerTrophy(
                playerId, trophyId, new DateTime(timeSource.getCurrentTimeStamp()));
        playerTrophyRepository.save(playerTrophy);
    }
}
