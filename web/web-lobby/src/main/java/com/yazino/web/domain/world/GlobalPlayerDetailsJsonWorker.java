package com.yazino.web.domain.world;

import com.yazino.platform.community.BasicProfileInformation;
import com.yazino.platform.community.PlayerService;
import com.yazino.platform.session.PlayerLocations;
import com.yazino.web.data.BalanceSnapshotRepository;
import com.yazino.web.data.LevelRepository;
import com.yazino.web.data.LocationDetailsRepository;
import com.yazino.web.domain.LocationDetails;
import com.yazino.web.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notNull;

@Service("globalPlayerDetailsJsonWorker")
public class GlobalPlayerDetailsJsonWorker {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalPlayerDetailsJsonWorker.class);

    private final PlayerService playerService;
    private final LocationDetailsRepository locationDetailsRepository;
    private final LevelRepository levelRepository;
    private final BalanceSnapshotRepository balanceSnapshotRepository;

    @Autowired
    public GlobalPlayerDetailsJsonWorker(final PlayerService playerService,
                                         final LocationDetailsRepository locationDetailsRepository,
                                         final LevelRepository levelRepository,
                                         final BalanceSnapshotRepository balanceSnapshotRepository) {
        notNull(playerService, "playerService may not be null");
        notNull(locationDetailsRepository, "locationDetailsRepository may not be null");
        notNull(levelRepository, "levelRepository may not be null");
        notNull(balanceSnapshotRepository, "balanceSnapshotRepository may not be null");

        this.playerService = playerService;
        this.locationDetailsRepository = locationDetailsRepository;
        this.levelRepository = levelRepository;
        this.balanceSnapshotRepository = balanceSnapshotRepository;
    }

    public String buildJson(final Set<PlayerLocations> locations) {
        final Map<BigDecimal, PlayerDetailsJson> toSerialize = new HashMap<BigDecimal, PlayerDetailsJson>();
        for (PlayerLocations player : locations) {
            toSerialize.put(player.getPlayerId(), processPlayer(player));
        }
        return new JsonHelper().serialize(toSerialize);
    }

    private PlayerDetailsJson processPlayer(final PlayerLocations player) {
        final BasicProfileInformation basicProfileInformation
                = playerService.getBasicProfileInformation(player.getPlayerId());
        final BigDecimal balanceSnapshot = balanceSnapshotRepository.getBalanceSnapshot(player.getPlayerId());
        final PlayerDetailsJson.Builder builder = new PlayerDetailsJson.Builder(
                basicProfileInformation, balanceSnapshot);
        processLocations(player, builder);
        return builder.build();
    }

    private void processLocations(final PlayerLocations player,
                                  final PlayerDetailsJson.Builder builder) {
        for (BigDecimal locationId : player.getLocationIds()) {
            final LocationDetails locationDetails = locationDetailsRepository.getLocationDetails(locationId);
            if (locationDetails != null) {
                builder.addLevel(locationDetails.getGameType(),
                        levelRepository.getLevel(player.getPlayerId(), locationDetails.getGameType()));
                builder.addLocation(locationDetails);
            } else {
                LOG.debug("Couldn't find location details for {}", locationId);
            }
        }
    }
}
