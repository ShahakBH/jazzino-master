package com.yazino.platform.service.community;

import com.yazino.platform.community.Trophy;
import com.yazino.platform.community.TrophyCabinet;
import com.yazino.platform.community.TrophySummary;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PlayerTrophy;
import com.yazino.platform.repository.community.PlayerRepository;
import com.yazino.platform.repository.community.PlayerTrophyRepository;
import com.yazino.platform.repository.community.TrophyRepository;
import com.yazino.platform.tournament.TrophyWinner;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Tests the {@link com.yazino.platform.service.community.GigaspaceRemotingPlayerTrophyService} class.
 */
public class GigaspaceRemotingPlayerTrophyServiceTest {

    private static final BigDecimal PLAYER_ID_1 = BigDecimal.ONE;
    private static final BigDecimal PLAYER_ID_2 = BigDecimal.valueOf(2);
    private static final BigDecimal TROPHY_ID_1 = BigDecimal.valueOf(100);
    private static final BigDecimal TROPHY_ID_2 = BigDecimal.valueOf(200);

    private final PlayerTrophyRepository playerTrophyRepository = mock(PlayerTrophyRepository.class);
    private final PlayerRepository playerRepository = mock(PlayerRepository.class);
    private final TrophyRepository trophyRepository = mock(TrophyRepository.class);

    private final Player player1 = new Player(PLAYER_ID_1);
    private final Player player2 = new Player(PLAYER_ID_2);
    private final Trophy trophy1 = new Trophy(TROPHY_ID_1, "TROPHY_1", "TEST", "");
    private final Trophy trophy2 = new Trophy(TROPHY_ID_2, "TROPHY_2", "TEST", "");

    private final GigaspaceRemotingPlayerTrophyService underTest = new GigaspaceRemotingPlayerTrophyService(
            playerTrophyRepository, playerRepository, trophyRepository);
    private static final String GAME_TYPE = "GAME_TYPE";

    @Before
    public void setup() {
        player1.setName("PLAYER_1");
        player2.setName("PLAYER_2");
        when(playerRepository.findById(PLAYER_ID_1)).thenReturn(player1);
        when(playerRepository.findById(PLAYER_ID_2)).thenReturn(player2);
        when(trophyRepository.findById(TROPHY_ID_1)).thenReturn(trophy1);
        when(trophyRepository.findById(TROPHY_ID_2)).thenReturn(trophy2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionWhenAttemptingToFindCabinetForNonExistentPlayer() throws Exception {
        when(playerRepository.findById(PLAYER_ID_1)).thenThrow(new IllegalArgumentException("player1 was null"));
        underTest.findTrophyCabinetForPlayer("TEST", PLAYER_ID_1, asList("TEST"));
    }

    @Test
    public void ensureTrophyCabinetHasCorrectPlayerName() throws Exception {
        PlayerTrophy playerTrophy1 = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_1, new DateTime());
        when(playerTrophyRepository.findPlayersTrophies(PLAYER_ID_1)).thenReturn(asList(playerTrophy1));
        TrophyCabinet cabinet = underTest.findTrophyCabinetForPlayer("TEST", PLAYER_ID_1, asList("TEST"));
        assertEquals(player1.getName(), cabinet.getPlayerName());
    }

    @Test
    public void ensureTrophyCabinetHasCorrectGameType() throws Exception {
        String gameType = "TEST";
        PlayerTrophy playerTrophy1 = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_1, new DateTime());
        PlayerTrophy playerTrophy2 = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_2, new DateTime());
        when(playerTrophyRepository.findPlayersTrophies(PLAYER_ID_1)).thenReturn(asList(playerTrophy1, playerTrophy2));
        trophy1.setGameType(gameType);
        trophy2.setGameType("other game type");
        TrophyCabinet cabinet = underTest.findTrophyCabinetForPlayer(gameType, PLAYER_ID_1, asList(trophy2.getName(), trophy1.getName()));
        assertEquals(gameType, cabinet.getGameType());
        assertEquals(1, cabinet.getTotalTrophyCount());
    }

    @Test
    public void returnsEmptyCabinetWhenNoTrophiesMatchingName() throws Exception {
        TrophyCabinet cabinet = underTest.findTrophyCabinetForPlayer("TEST", PLAYER_ID_1, asList("TROPHY_3"));
        assertEquals(0, cabinet.getTrophySummaries().size());
    }

    @Test
    public void ensureSummaryReturnedForSingleTrophy() throws Exception {
        PlayerTrophy playerTrophy1 = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_1, new DateTime());
        when(playerTrophyRepository.findPlayersTrophies(PLAYER_ID_1)).thenReturn(asList(playerTrophy1));

        TrophyCabinet cabinet = underTest.findTrophyCabinetForPlayer("TEST", PLAYER_ID_1, asList(trophy1.getName()));
        assertEquals(1, cabinet.getTrophySummaries().size());
        TrophySummary summary = cabinet.getTrophySummary(trophy1.getName());
        assertEquals(1, summary.getCount());
    }

    @Test
    public void summaryCountUpdatedWhenMultiplePlayerTrophies() throws Exception {
        DateTime dateTime = new DateTime();
        PlayerTrophy playerTrophy1 = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_1, dateTime);
        PlayerTrophy playerTrophy2 = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_1, new DateTime(dateTime.getMillis() + 100));
        when(playerTrophyRepository.findPlayersTrophies(PLAYER_ID_1)).thenReturn(asList(playerTrophy1, playerTrophy2));
        TrophyCabinet cabinet = underTest.findTrophyCabinetForPlayer("TEST", PLAYER_ID_1, asList(trophy1.getName()));
        assertEquals(1, cabinet.getTrophySummaries().size());
        TrophySummary summary = cabinet.getTrophySummary(trophy1.getName());
        assertEquals(2, summary.getCount());
    }

    @Test
    public void ensureSummaryReturnedForEachDifferentTrophy() throws Exception {
        DateTime dateTime = new DateTime();
        PlayerTrophy playerTrophy1 = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_1, dateTime);
        PlayerTrophy playerTrophy2 = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_2, new DateTime(dateTime.getMillis() + 100));
        when(playerTrophyRepository.findPlayersTrophies(PLAYER_ID_1)).thenReturn(asList(playerTrophy1, playerTrophy2));

        TrophyCabinet cabinet = underTest.findTrophyCabinetForPlayer("TEST", PLAYER_ID_1, asList(trophy1.getName(), trophy2.getName()));

        assertEquals(2, cabinet.getTrophySummaries().size());
        TrophySummary summary1 = cabinet.getTrophySummary(trophy1.getName());
        assertEquals(1, summary1.getCount());
        TrophySummary summary2 = cabinet.getTrophySummary(trophy2.getName());
        assertEquals(1, summary2.getCount());
    }

    @Test
    public void ensureSummaryReturnedForAllValidTrophiesAndInvalidAreSkipped() throws Exception {
        DateTime dateTime = new DateTime();
        PlayerTrophy playerTrophy1 = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_1, dateTime);
        PlayerTrophy playerTrophy2 = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_2, new DateTime(dateTime.getMillis() + 100));
        when(playerTrophyRepository.findPlayersTrophies(PLAYER_ID_1))
                .thenReturn(asList(playerTrophy1, new PlayerTrophy(PLAYER_ID_1, BigDecimal.valueOf(999), dateTime), playerTrophy2));

        TrophyCabinet cabinet = underTest.findTrophyCabinetForPlayer("TEST", PLAYER_ID_1, asList(trophy1.getName(), trophy2.getName()));

        assertEquals(2, cabinet.getTrophySummaries().size());
        TrophySummary summary1 = cabinet.getTrophySummary(trophy1.getName());
        assertEquals(1, summary1.getCount());
        TrophySummary summary2 = cabinet.getTrophySummary(trophy2.getName());
        assertEquals(1, summary2.getCount());
    }

    @Test
    public void shouldUseDifferentMessagesInCabinetDependingOnTrophyCount() {
        trophy1.setMessageCabinet("%1$s has now %2$s trophies 1!");
        trophy2.setMessage("%1$s got his first trophy 2!");
        DateTime dateTime = new DateTime();
        PlayerTrophy playerTrophy1 = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_1, dateTime);
        PlayerTrophy playerTrophy2 = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_1, new DateTime(dateTime.getMillis() + 100));
        PlayerTrophy playerTrophy3 = new PlayerTrophy(PLAYER_ID_1, TROPHY_ID_2, new DateTime(dateTime.getMillis() + 300));
        when(playerTrophyRepository.findPlayersTrophies(PLAYER_ID_1)).thenReturn(asList(playerTrophy1, playerTrophy2, playerTrophy3));
        TrophyCabinet cabinet = underTest.findTrophyCabinetForPlayer("TEST", PLAYER_ID_1, asList(trophy1.getName(), trophy2.getName()));
        assertEquals("PLAYER_1 has now 2 trophies 1!", cabinet.getTrophySummary(trophy1.getName()).getMessage().toString());
        assertEquals("PLAYER_1 got his first trophy 2!", cabinet.getTrophySummary(trophy2.getName()).getMessage().toString());
    }

    @Test
    public void findWinnersByTrophyNameFindsWinners() {
        reset(playerRepository, playerTrophyRepository);

        final DateTime awardTime = new DateTime();
        final Player player1 = createPlayer(5);
        final Player player2 = createPlayer(7);
        final TrophyWinner tournamentPlayer1 = createTournamentPlayer(player1, awardTime);
        final TrophyWinner tournamentPlayer2 = createTournamentPlayer(player2, awardTime);
        final List<TrophyWinner> expectedPlayers = asList(tournamentPlayer1, tournamentPlayer2);
        when(trophyRepository.findByNameAndGameType("TestTrophyName", GAME_TYPE)).thenReturn(trophy1);
        final List<PlayerTrophy> playerTrophies = asList(createPlayerTrophy(awardTime, player1), createPlayerTrophy(awardTime, player2));
        when(playerTrophyRepository.findWinnersByTrophyId(trophy1.getId(), 500)).thenReturn(playerTrophies);

        Map<String, List<TrophyWinner>> winners = underTest.findWinnersByTrophyName("TestTrophyName", 500, GAME_TYPE);

        assertTrue(winners.containsKey(GAME_TYPE));
        List<TrophyWinner> tournamentPlayerList = winners.get(GAME_TYPE);
        assertEquals(expectedPlayers, tournamentPlayerList);
    }

    @Test(expected = NullPointerException.class)
    public void findWinnersWhenTrophyNotFound() {
        when(trophyRepository.findByNameAndGameType("TestTrophyName", GAME_TYPE)).thenReturn(null);
        underTest.findWinnersByTrophyName("TestTrophyName", Integer.MAX_VALUE, GAME_TYPE);
        verifyNoMoreInteractions(playerTrophyRepository);
        verifyNoMoreInteractions(playerRepository);
    }

    private PlayerTrophy createPlayerTrophy(DateTime awardTime, Player player) {
        return new PlayerTrophy(player.getPlayerId(), trophy1.getId(), awardTime);
    }

    private Player createPlayer(int playerId) {
        Player player = new Player(new BigDecimal(playerId));
        player.setName("Test Player Name " + playerId);
        player.setPictureUrl("pic");
        when(playerRepository.findById(player.getPlayerId())).thenReturn(player);
        return player;
    }

    private TrophyWinner createTournamentPlayer(Player player, DateTime awardTime) {
        return new TrophyWinner(player.getPlayerId(), player.getName(), player.getPictureUrl(), awardTime);
    }


}
