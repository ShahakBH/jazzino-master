package com.yazino.platform.service.tournament;

import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.model.conversion.TrophyLeaderboardViewTransformer;
import com.yazino.platform.model.tournament.TrophyLeaderboard;
import com.yazino.platform.model.tournament.TrophyLeaderboardCreationRequest;
import com.yazino.platform.model.tournament.TrophyLeaderboardCreationResponse;
import com.yazino.platform.repository.tournament.TrophyLeaderboardRepository;
import com.yazino.platform.tournament.TrophyLeaderboardDefinition;
import com.yazino.platform.tournament.TrophyLeaderboardException;
import com.yazino.platform.tournament.TrophyLeaderboardService;
import com.yazino.platform.tournament.TrophyLeaderboardView;
import net.jini.core.lease.Lease;
import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.RemotingService;
import org.openspaces.remoting.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.Validate.notNull;

@RemotingService
public class GigaspaceRemotingTrophyLeaderboardService implements TrophyLeaderboardService {
    private static final Logger LOG = LoggerFactory.getLogger(GigaspaceRemotingTrophyLeaderboardService.class);

    private static final TrophyLeaderboardViewTransformer LEADERBOARD_VIEW_TRANSFORMER
            = new TrophyLeaderboardViewTransformer();
    private static final int TIMEOUT = 5000;

    private final TrophyLeaderboardRepository leaderboardRepository;
    private final GigaSpace globalGigaSpace;

    @Autowired(required = true)
    public GigaspaceRemotingTrophyLeaderboardService(@Qualifier("trophyLeaderboardRepository") final TrophyLeaderboardRepository leaderboardRepository,
                                                     @Qualifier("globalGigaSpace") final GigaSpace globalGigaSpace) {
        notNull(leaderboardRepository, "leaderboardRepository is null");
        notNull(globalGigaSpace, "globalGigaSpace may not be null");

        this.leaderboardRepository = leaderboardRepository;
        this.globalGigaSpace = globalGigaSpace;
    }

    @Override
    @Transactional
    public BigDecimal create(final TrophyLeaderboardDefinition trophyLeaderboardDefinition)
            throws TrophyLeaderboardException {
        notNull(trophyLeaderboardDefinition, "trophyLeaderboardDefinition may not be null");

        final TrophyLeaderboard template = new TrophyLeaderboard();
        template.setActive(true);
        template.setGameType(trophyLeaderboardDefinition.getGameType());
        if (globalGigaSpace.readIfExists(template) != null) {
            throw new TrophyLeaderboardException("Could not create Trophy Leaderboard, an existing active board exists for gametype "
                    + trophyLeaderboardDefinition.getGameType());
        }

        final TrophyLeaderboardCreationRequest request = new TrophyLeaderboardCreationRequest(
                new TrophyLeaderboard(trophyLeaderboardDefinition));

        globalGigaSpace.write(request, Lease.FOREVER, TIMEOUT, WriteModifiers.WRITE_ONLY);

        final TrophyLeaderboardCreationResponse templateResponse = new TrophyLeaderboardCreationResponse();
        templateResponse.setRequestSpaceId(request.getSpaceId());

        final TrophyLeaderboardCreationResponse response = globalGigaSpace.take(templateResponse, TIMEOUT);

        LOG.debug("Received create tournament processing response {}", response);
        if (response != null) {
            return response.getTrophyLeaderboardId();
        }

        throw new TrophyLeaderboardException("Could not create null response");
    }

    @Override
    @Transactional
    public void update(@Routing("getId") final TrophyLeaderboardView trophyLeaderboardView) {
        final TrophyLeaderboard leaderboard = leaderboardRepository.findById(trophyLeaderboardView.getId());
        if (leaderboard == null) {
            throw new IllegalArgumentException("Invalid leaderboard: " + trophyLeaderboardView.getId());
        }

        leaderboard.setName(trophyLeaderboardView.getName());
        leaderboard.setGameType(trophyLeaderboardView.getGameType());
        leaderboard.setStartTime(trophyLeaderboardView.getStartTime());
        leaderboard.setEndTime(trophyLeaderboardView.getEndTime());
        leaderboard.setCycle(trophyLeaderboardView.getCycle());
        if (trophyLeaderboardView.getCurrentCycleEnd() != null) {
            leaderboard.setCurrentCycleEnd(trophyLeaderboardView.getCurrentCycleEnd());
        }
        leaderboard.setPointBonusPerPlayer(trophyLeaderboardView.getPointBonusPerPlayer());
        leaderboard.setPositionData(trophyLeaderboardView.getPositionData());

        leaderboardRepository.save(leaderboard);
    }

    @Override
    @Transactional
    public void setIsActive(@Routing final BigDecimal trophyLeaderboardId,
                            final boolean isActive) {
        final TrophyLeaderboard trophyLeaderboard = leaderboardRepository.findById(trophyLeaderboardId);
        trophyLeaderboard.setActive(isActive);
        leaderboardRepository.save(trophyLeaderboard);
    }


    @Override
    @Transactional
    public Collection<TrophyLeaderboardView> findAll() {
        return newArrayList(transform(asList(leaderboardRepository.findAll()), LEADERBOARD_VIEW_TRANSFORMER));
    }

    @Override
    @Transactional
    public TrophyLeaderboardView findById(@Routing final BigDecimal trophyLeaderboardId) {
        return LEADERBOARD_VIEW_TRANSFORMER.apply(leaderboardRepository.findById(trophyLeaderboardId));
    }

    @Override
    public TrophyLeaderboardView findByGameType(final String gameType) {
        return LEADERBOARD_VIEW_TRANSFORMER.apply(leaderboardRepository.findByGameType(gameType));
    }
}
