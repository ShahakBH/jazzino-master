package com.yazino.platform.model.community;

import com.yazino.platform.account.WalletServiceException;
import com.yazino.platform.community.PlayerInfo;
import com.yazino.platform.community.ProfileInformation;
import com.yazino.platform.community.Trophy;
import com.yazino.platform.repository.community.PlayerTrophyRepository;
import com.yazino.platform.repository.community.TrophyRepository;
import com.yazino.platform.service.account.InternalWalletService;
import org.joda.time.DateTime;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProfileInformationBuilderTest {
    private static final String GAME_TYPE = "gameType";

    private TrophyRepository trophyRepository = mock(TrophyRepository.class);
    private PlayerTrophyRepository playerTrophyRepository = mock(PlayerTrophyRepository.class);
    private InternalWalletService internalWalletService = mock(InternalWalletService.class);

    @Test
    public void shouldGetProfileInformation() throws WalletServiceException {
        final List<PlayerTrophy> player1Trophies = asList(
                aPlayerTrophy(1), aPlayerTrophy(2), aPlayerTrophy(3), aPlayerTrophy(4));
        when(playerTrophyRepository.findPlayersTrophies(BigDecimal.valueOf(1))).thenReturn(player1Trophies);

        when(trophyRepository.findByNameAndGameType("trophy_weeklyChamp", GAME_TYPE)).thenReturn(aTrophy(4));
        when(trophyRepository.findByNameAndGameType("medal_1", GAME_TYPE)).thenReturn(aTrophy(1));
        when(trophyRepository.findByNameAndGameType("medal_2", GAME_TYPE)).thenReturn(aTrophy(2));
        when(trophyRepository.findByNameAndGameType("medal_3", GAME_TYPE)).thenReturn(aTrophy(3));

        final Player player = player(1);

        when(internalWalletService.getBalance(player.getAccountId())).thenReturn(BigDecimal.TEN);

        ProfileInformationBuilder unit = new ProfileInformationBuilder(trophyRepository, playerTrophyRepository, internalWalletService);
        final ProfileInformation expected = new ProfileInformation(new PlayerInfo("player 1", null), 1, 3, BigDecimal.TEN);
        final ProfileInformation actual = unit.buildProfileInformation(player, GAME_TYPE);
        assertEquals(expected, actual);
    }

    private Player player(int id) {
        final Player player = new Player(BigDecimal.valueOf(id));
        player.setName("player " + id);
        return player;
    }

    private Trophy aTrophy(int id) {
        return new Trophy(BigDecimal.valueOf(id), "medal_" + id, GAME_TYPE, "image");
    }

    private PlayerTrophy aPlayerTrophy(int id) {
        return new PlayerTrophy(BigDecimal.valueOf(1), BigDecimal.valueOf(id), new DateTime(id * 1000));
    }
}
