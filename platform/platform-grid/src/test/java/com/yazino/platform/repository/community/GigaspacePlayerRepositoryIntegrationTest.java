package com.yazino.platform.repository.community;

import com.yazino.platform.model.community.Player;
import com.yazino.platform.model.community.PlayerPersistenceRequest;
import com.yazino.platform.persistence.player.StubPlayerDAO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ConcurrentModificationException;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaspacePlayerRepositoryIntegrationTest {
    private static final BigDecimal PLAYER_ID = BigDecimal.valueOf(200435L);
    private static final BigDecimal PLAYER_TWO_ID = BigDecimal.valueOf(200436L);
    private static final BigDecimal PLAYER_THREE_ID = BigDecimal.valueOf(200437L);

    @Autowired
    private GigaSpace gigaSpace;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private StubPlayerDAO playerDao;

    private BigDecimal accountId = new BigDecimal("13455");
    private BigDecimal playerId = new BigDecimal("13454");
    private BigDecimal secondPlayerId = new BigDecimal("13877");
    private Player player = new Player(playerId, "Name", accountId, "aPicture", null, null, null);

    @Before
    public void clean() {
        playerDao.clear();
        gigaSpace.clear(null);
        gigaSpace.write(player);
        ((GigaspacePlayerRepository) playerRepository).setTimeout(10);
    }

    @Test
    public void findByIdReturnsAllPresentPlayersInTheSpace() {
        gigaSpace.write(playerWithId(PLAYER_ID));
        gigaSpace.write(playerWithId(PLAYER_TWO_ID));

        final Set<Player> foundPlayers = playerRepository.findLocalByIds(newHashSet(PLAYER_ID, PLAYER_TWO_ID));

        assertThat(foundPlayers, containsInAnyOrder(playerWithId(PLAYER_ID), playerWithId(PLAYER_TWO_ID)));
    }

    @Test
    public void findByIdLoadsPlayersThatAreNotInTheSpaceFromTheDatabase() {
        gigaSpace.write(playerWithId(PLAYER_ID));
        playerDao.save(playerWithId(PLAYER_TWO_ID));
        playerDao.save(playerWithId(PLAYER_THREE_ID));

        final Set<Player> foundPlayers = playerRepository.findLocalByIds(newHashSet(PLAYER_ID, PLAYER_TWO_ID, PLAYER_THREE_ID));

        assertThat(foundPlayers, containsInAnyOrder(playerWithId(PLAYER_ID), playerWithId(PLAYER_TWO_ID), playerWithId(PLAYER_THREE_ID)));
    }

    @Test
    public void findByIdLoadsIgnoresPlayersThatDoNotExistInTheSpaceOrDatabase() {
        gigaSpace.write(playerWithId(PLAYER_ID));
        playerDao.save(playerWithId(PLAYER_THREE_ID));

        final Set<Player> foundPlayers = playerRepository.findLocalByIds(newHashSet(PLAYER_ID, PLAYER_TWO_ID, PLAYER_THREE_ID));

        assertThat(foundPlayers, containsInAnyOrder(playerWithId(PLAYER_ID), playerWithId(PLAYER_THREE_ID)));
    }

    @Test
    public void findByIdReturnsAnEmptyCollectionWhereNoMatchesAreFound() {
        final Set<Player> foundPlayers = playerRepository.findLocalByIds(newHashSet(PLAYER_ID, PLAYER_TWO_ID, PLAYER_THREE_ID));

        assertThat(foundPlayers, is(empty()));
    }

    @Test
    public void findByIdLoadsAnObjectFromTheDatabaseIfItIsNotPresentInTheSpace() {
        final Player expectedPlayer = playerWithId(PLAYER_ID);
        playerDao.save(expectedPlayer);

        final Player foundPlayer = playerRepository.findById(PLAYER_ID);

        assertThat(foundPlayer, is(equalTo(expectedPlayer)));
    }

    @Test
    public void aPlayerLoadedByPlayerIdFromTheDBIsAddedToTheSpace() {
        final Player expectedPlayer = playerWithId(PLAYER_ID);
        playerDao.save(expectedPlayer);

        playerRepository.findById(PLAYER_ID);

        final Player spacePlayer = gigaSpace.readById(Player.class, expectedPlayer.getPlayerId());
        assertThat(spacePlayer, is(equalTo(expectedPlayer)));
    }

    @Test
    public void findByIdReturnsNullIfAPlayerDoesNotExistInEitherTheSpaceOrTheDB() {
        final Player foundPlayer = playerRepository.findById(PLAYER_ID);

        assertThat(foundPlayer, is(nullValue()));
    }

    @Test
    @Transactional
    public void lock_retrieves_objects_from_space() {
        assertEquals(playerId, playerRepository.lock(playerId).getPlayerId());
    }

    @Test(expected = ConcurrentModificationException.class)
    @Transactional
    public void lock_throws_exception_when_no_object() {
        playerRepository.lock(secondPlayerId);
        fail("no exception thrown");
    }

    @Test
    public void find_by_id_retrieves_objects_from_space_and_null_otherwise() {
        assertEquals(playerId, playerRepository.findById(playerId).getPlayerId());
        assertEquals(null, playerRepository.findById(secondPlayerId));
    }

    @Test
    public void save_writes_into_space() {
        playerRepository.save(new Player(secondPlayerId, "Name", accountId, "aPicture", null, null, null));
        assertEquals(secondPlayerId, playerRepository.findById(secondPlayerId).getPlayerId());
        assertEquals(secondPlayerId, gigaSpace.take(new PlayerPersistenceRequest()).getPlayerId());
    }

    private Player playerWithId(final BigDecimal playerId) {
        return new Player(playerId, "aName", accountId, "aPicture", null, null, null);
    }

}
