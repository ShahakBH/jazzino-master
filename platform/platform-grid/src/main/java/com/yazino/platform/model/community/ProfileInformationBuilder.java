package com.yazino.platform.model.community;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerInfo;
import com.yazino.platform.community.ProfileInformation;
import com.yazino.platform.community.Trophy;
import com.yazino.platform.community.TrophyType;
import com.yazino.platform.repository.community.PlayerTrophyRepository;
import com.yazino.platform.repository.community.TrophyRepository;
import com.yazino.platform.service.account.InternalWalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.Validate.notBlank;
import static org.apache.commons.lang3.Validate.notNull;

@Service
public class ProfileInformationBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ProfileInformationBuilder.class);

    private static final Set<String> TROPHY_NAMES = TrophyType.WEEKLY_CHAMP.getNames();
    private static final Set<String> MEDAL_NAMES = TrophyType.MEDAL.getNames();

    private final TrophyRepository trophyRepository;
    private final PlayerTrophyRepository playerTrophyRepository;
    private final InternalWalletService internalWalletService;

    @Autowired
    public ProfileInformationBuilder(final TrophyRepository trophyRepository,
                                     final PlayerTrophyRepository playerTrophyRepository,
                                     final InternalWalletService internalWalletService) {
        this.trophyRepository = trophyRepository;
        this.playerTrophyRepository = playerTrophyRepository;
        this.internalWalletService = internalWalletService;
    }

    private int buildMedalInfo(final Player player,
                               final String gameType) {
        return getTrophyCountForPlayer(gameType, player.getPlayerId(), MEDAL_NAMES);
    }

    private int buildTrophiesInfo(final Player player,
                                  final String gameType) {
        return getTrophyCountForPlayer(gameType, player.getPlayerId(), TROPHY_NAMES);
    }

    public ProfileInformation buildProfileInformation(final Player player,
                                                      final String gameType) {
        notNull(player, "player is null");
        notBlank(gameType, "gameType is blank");
        final PlayerInfo playerInfo = new PlayerInfo(player.getName(), player.getPictureUrl());
        final int trophies = buildTrophiesInfo(player, gameType);
        final int medals = buildMedalInfo(player, gameType);
        final BigDecimal balance = getBalance(player);
        return new ProfileInformation(playerInfo, trophies, medals, balance);
    }

    private BigDecimal getBalance(final Player player) {
        try {
            return internalWalletService.getBalance(player.getAccountId());
        } catch (WalletServiceException e) {
            LOG.error("Couldn't get player's balance", e);
            return BigDecimal.ZERO;
        }
    }

    private int getTrophyCountForPlayer(final String gameType,
                                        final BigDecimal playerId,
                                        final Collection<String> trophyNames) {
        final Set<BigDecimal> trophyIds = new HashSet<BigDecimal>();
        for (String trophyName : trophyNames) {
            final Trophy trophy = trophyRepository.findByNameAndGameType(trophyName, gameType);
            if (trophy != null) {
                trophyIds.add(trophy.getId());
            }
        }
        final Collection<PlayerTrophy> playerTrophies = playerTrophyRepository.findPlayersTrophies(playerId);
        int count = 0;
        for (PlayerTrophy playerTrophy : playerTrophies) {
            if (trophyIds.contains(playerTrophy.getTrophyId())) {
                count++;
            }
        }
        return count;
    }
}
