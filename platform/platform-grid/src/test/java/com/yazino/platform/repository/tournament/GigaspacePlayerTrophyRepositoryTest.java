package com.yazino.platform.repository.tournament;

import com.yazino.platform.community.Trophy;
import com.yazino.platform.model.community.PlayerTrophy;
import com.yazino.platform.model.community.PlayerTrophyPersistenceRequest;
import com.yazino.platform.repository.community.GigaspacePlayerTrophyRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link com.yazino.platform.repository.community.GigaspacePlayerTrophyRepository} class.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaspacePlayerTrophyRepositoryTest {
    private static final DateTime DEFAULT_TIME = new DateTime();
    private static final DateTime FUTURE_TIME = new DateTime(DEFAULT_TIME.getMillis() + 500);
    private static final DateTime FUTURE_TIME_2 = new DateTime(DEFAULT_TIME.getMillis() + 1000);

    private final Trophy trophy1 = new Trophy(BigDecimal.valueOf(100), "HOLDEM_1", "TEXAS_HOLDEM", "");
    private final Trophy trophy2 = new Trophy(BigDecimal.valueOf(200), "HOLDEM_2", "TEXAS_HOLDEM", "");
    private final Trophy trophy3 = new Trophy(BigDecimal.valueOf(300), "HOLDEM_3", "TEXAS_HOLDEM", "");
    private final Trophy trophy4 = new Trophy(BigDecimal.valueOf(400), "HOLDEM_4", "TEXAS_HOLDEM", "");

    private final PlayerTrophy playerTrophy1 = new PlayerTrophy(BigDecimal.valueOf(1), trophy1.getId(), DEFAULT_TIME);
    private final PlayerTrophy playerTrophy2 = new PlayerTrophy(BigDecimal.valueOf(2), trophy2.getId(), DEFAULT_TIME);
    private final PlayerTrophy playerTrophy3 = new PlayerTrophy(BigDecimal.valueOf(1), trophy3.getId(), DEFAULT_TIME);
    private final PlayerTrophy playerTrophy4 = new PlayerTrophy(BigDecimal.valueOf(2), trophy4.getId(), DEFAULT_TIME);
    private final PlayerTrophy playerTrophy5 = new PlayerTrophy(BigDecimal.valueOf(1), trophy4.getId(), FUTURE_TIME);
    private final PlayerTrophy playerTrophy6 = new PlayerTrophy(BigDecimal.valueOf(3), trophy4.getId(), FUTURE_TIME_2);

    @Autowired
    private GigaSpace globalGigaSpace;
    @Autowired
    private GigaSpace gigaSpace;
    @Autowired
    private GigaspacePlayerTrophyRepository playerTrophyRepository;

    @Before
    public void setup() {
        globalGigaSpace.clear(null);
    }

    @Test
    public void ensurePlayerTrophyIsSavedToGigaSpace() throws Exception {
        PlayerTrophy playerTrophy = new PlayerTrophy(BigDecimal.ONE, BigDecimal.TEN, DEFAULT_TIME);

        playerTrophyRepository.save(playerTrophy);

        PlayerTrophy[] actualPlayerTrophies = gigaSpace.readMultiple(new PlayerTrophy(), 10);
        assertEquals(1, actualPlayerTrophies.length);
        assertEquals(playerTrophy, actualPlayerTrophies[0]);
    }

    @Test
    public void ensurePeristenceRequest() throws Exception {
        PlayerTrophy playerTrophy = new PlayerTrophy(BigDecimal.ONE, BigDecimal.TEN, DEFAULT_TIME);
        playerTrophyRepository.save(playerTrophy);

        PlayerTrophyPersistenceRequest request = new PlayerTrophyPersistenceRequest();
        request.setPlayerTrophy(playerTrophy);

        PlayerTrophyPersistenceRequest[] actualRequests = gigaSpace.readMultiple(request, 10);
        assertEquals(1, actualRequests.length);

        assertEquals(request.getPlayerTrophy(), actualRequests[0].getPlayerTrophy());
    }

    @Test
    public void returnsEmptyCabinetWhenNoTrophiesOrPlayerTrophiesInSpace() throws Exception {
        PlayerTrophy[] playerTrophies = gigaSpace.readMultiple(new PlayerTrophy(), 10000);
        assertEquals(0, playerTrophies.length);
        Trophy[] trophies = gigaSpace.readMultiple(new Trophy(), 10000);
        assertEquals(0, trophies.length);
        Collection<PlayerTrophy> trophyCollection = playerTrophyRepository.findPlayersTrophies(playerTrophy1.getPlayerId());
        assertTrue(trophyCollection.isEmpty());
    }

    @Test
    public void returnsEmptyListWhenNoPlayerTrophiesInSpace() throws Exception {
        Collection<PlayerTrophy> playerTrophies = playerTrophyRepository.findPlayersTrophies(playerTrophy1.getPlayerId());
        assertTrue(playerTrophies.isEmpty());
    }

    @Test
    public void returnsAllPlayerTrophiesForPlayer() throws Exception {
        globalGigaSpace.write(playerTrophy1);
        globalGigaSpace.write(playerTrophy2);
        globalGigaSpace.write(playerTrophy3);
        globalGigaSpace.write(playerTrophy4);
        globalGigaSpace.write(playerTrophy5);
        Collection<PlayerTrophy> trophyCollection = playerTrophyRepository.findPlayersTrophies(playerTrophy1.getPlayerId());
        assertEquals(3, trophyCollection.size());
        Set<BigDecimal> ids = extractTrophyIds(trophyCollection);
        assertTrue(ids.contains(trophy1.getId()));
        assertTrue(ids.contains(trophy3.getId()));
        assertTrue(ids.contains(trophy4.getId()));
    }


    @Test
    public void returnsOnlySelectedTrophiesForPlayer() throws Exception {
        globalGigaSpace.write(playerTrophy1);
        globalGigaSpace.write(playerTrophy2);
        globalGigaSpace.write(playerTrophy3);
        globalGigaSpace.write(playerTrophy4);
        globalGigaSpace.write(playerTrophy5);
        Collection<PlayerTrophy> playerTrophies = playerTrophyRepository.findPlayersTrophies(playerTrophy1.getPlayerId());
        assertEquals(3, playerTrophies.size());
        Set<BigDecimal> ids = extractTrophyIds(playerTrophies);
        assertTrue(ids.contains(trophy1.getId()));
        assertTrue(ids.contains(trophy3.getId()));
        assertTrue(ids.contains(trophy4.getId()));
    }

    @Test
    public void findWinnersByTrophyIdQueriesGigaspaceForWinners() {

        globalGigaSpace.write(playerTrophy1);
        globalGigaSpace.write(playerTrophy4);
        globalGigaSpace.write(playerTrophy5);

        List<PlayerTrophy> expectedTrophies = new ArrayList<PlayerTrophy>();
        expectedTrophies.add(playerTrophy5);
        expectedTrophies.add(playerTrophy4);

        List<PlayerTrophy> playerTrophys = playerTrophyRepository.findWinnersByTrophyId(trophy4.getId(), 500);

        assertEquals(expectedTrophies, playerTrophys);

    }

    @Test
    public void findWinnersByTrophyId_onlyRetrievesMaxiumResults() throws Exception {
        globalGigaSpace.write(playerTrophy1);
        globalGigaSpace.write(playerTrophy4);
        globalGigaSpace.write(playerTrophy5);

        List<PlayerTrophy> playerTrophys = playerTrophyRepository.findWinnersByTrophyId(trophy4.getId(), 500);

        assertEquals(2, playerTrophys.size());

        playerTrophys = playerTrophyRepository.findWinnersByTrophyId(trophy4.getId(), 1);
        assertEquals(1, playerTrophys.size());

    }

    @Test
    public void findWinnersByTrophyId_retrievesCorrectResults() throws Exception {
        globalGigaSpace.write(playerTrophy1);
        globalGigaSpace.write(playerTrophy4);
        globalGigaSpace.write(playerTrophy5);
        globalGigaSpace.write(playerTrophy6);

        List<PlayerTrophy> playerTrophys = playerTrophyRepository.findWinnersByTrophyId(trophy4.getId(), 2);

        assertEquals(2, playerTrophys.size());

        assertTrue(playerTrophys.contains(playerTrophy5));
        assertTrue(playerTrophys.contains(playerTrophy6));
    }

    private static Set<BigDecimal> extractTrophyIds(Collection<PlayerTrophy> trophyCollection) {
        Set<BigDecimal> ids = new HashSet<BigDecimal>();
        for (PlayerTrophy playerTrophy : trophyCollection) {
            ids.add(playerTrophy.getTrophyId());
        }
        return ids;
    }

}
