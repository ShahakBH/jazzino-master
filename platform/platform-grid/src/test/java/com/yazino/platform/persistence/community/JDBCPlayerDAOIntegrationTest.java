package com.yazino.platform.persistence.community;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.yazino.platform.community.PaymentPreferences;
import com.yazino.platform.community.Relationship;
import com.yazino.platform.community.RelationshipType;
import com.yazino.platform.model.PagedData;
import com.yazino.platform.model.community.Player;
import com.yazino.platform.reference.Currency;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static com.google.common.collect.Sets.newHashSet;
import static java.math.BigDecimal.valueOf;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
@TransactionConfiguration
public class JDBCPlayerDAOIntegrationTest {
    private static final String PLAYER_SELECT = "SELECT * FROM PLAYER WHERE PLAYER_ID=?";

    private static final String PICTURE_LOCATION = "pictureUrl";
    private static final BigDecimal PLAYER_ID = valueOf(-10);
    private static final BigDecimal PLAYER_TWO_ID = valueOf(-20);
    private static final BigDecimal ACCOUNT_ID = BigDecimal.valueOf(-1);
    public static final DateTime CREATION_TIME = new DateTime(2010, 10, 15, 12, 0, 0, 0);

    @Autowired
    private JDBCPlayerDAO underTest;

    @Autowired
    private JdbcTemplate jdbc;

    @Before
    public void setup() {
        jdbc.update("DELETE FROM ACCOUNT WHERE ACCOUNT_ID < 0");
        createAccount(ACCOUNT_ID, "player test account 1");
    }

    @Test
    public void aPlayerIsFindableByTheirPLAYER_ID() {
        final Player expectedPlayer = aPlayer(PLAYER_ID, "aPlayerName");
        underTest.save(expectedPlayer);

        final Player foundPlayer = underTest.findById(PLAYER_ID);

        assertThat(foundPlayer, is(org.hamcrest.Matchers.equalTo(expectedPlayer)));
    }

    @Test
    public void aCollectionOfPlayersAreFindableByTheirPlayerIds() {
        final Player expectedOne = aPlayer(PLAYER_ID, "aPlayerName");
        final Player expectedTwo = aPlayer(PLAYER_TWO_ID, "playerTwo");
        underTest.save(expectedOne);
        underTest.save(expectedTwo);

        final Collection<Player> foundPlayers = underTest.findByIds(newHashSet(PLAYER_ID, PLAYER_TWO_ID));

        assertThat(foundPlayers, containsInAnyOrder(expectedOne, expectedTwo));
    }

    @Test
    public void aSearchForANonExistentPLAYER_IDReturnsNull() {
        final Player foundPlayer = underTest.findById(valueOf(4345435433454L));

        assertThat(foundPlayer, is(nullValue()));
    }

    @Test(expected = NullPointerException.class)
    public void findingAPlayerByANullPLAYER_IDThrowsANullPointerException() {
        underTest.findById(null);
    }

    @Test(expected = NullPointerException.class)
    public void findingAPlayerByANullUserProfileIdThrowsANullPointerException() {
        underTest.findById(null);
    }

    @Test
    public void whenNoPlayersMatchAnEmptyPagedDataIsReturned() {
        underTest.save(aPlayer(valueOf(9999999991L), "JDBCPlayerDaoTest-Jim"));
        underTest.save(aPlayer(valueOf(9999999992L), "JDBCPlayerDaoTest-Dave"));

        final PagedData<Player> results = underTest.findByName("JDBCPlayerDaoTest-Fred", 0, null);

        assertThat(results.getSize(), is(equalTo(0)));
        assertThat(results.getData(), is(equalTo(Collections.<Player>emptyList())));
    }

    @Test(expected = NullPointerException.class)
    public void aNullPlayerNameSearchStringIsRejectedWithANullPointerException() {
        underTest.findByName(null, 0, null);
    }

    @Test
    public void playersCanBeFoundByName() {
        final Player expectedPlayer = aPlayer(valueOf(9999999991L), "JDBCPlayerDaoTest-Jim");
        underTest.save(expectedPlayer);
        underTest.save(aPlayer(valueOf(9999999992L), "JDBCPlayerDaoTest-Dave"));

        final PagedData<Player> results = underTest.findByName("JDBCPlayerDaoTest-Jim", 0, null);

        assertThat(results.getSize(), is(equalTo(1)));
        assertThat(results.getTotalSize(), is(equalTo(1)));
        assertThat(results.getData(), hasItem(expectedPlayer));
    }

    @Test
    public void multiplePlayersCanBeFoundWhereTheStartOfTheNameMatches() {
        final Player expectedPlayer1 = aPlayer(valueOf(9999999991L), "JDBCPlayerDaoTest-Jima");
        final Player expectedPlayer2 = aPlayer(valueOf(9999999992L), "JDBCPlayerDaoTest-Jimb");
        underTest.save(expectedPlayer1);
        underTest.save(expectedPlayer2);
        underTest.save(aPlayer(valueOf(9999999993L), "JDBCPlayerDaoTest-Dave"));

        final PagedData<Player> results = underTest.findByName("JDBCPlayerDaoTest-Jim", 0, null);

        assertThat(results.getSize(), is(equalTo(2)));
        assertThat(results.getData(), hasItem(expectedPlayer1));
        assertThat(results.getData(), hasItem(expectedPlayer2));
    }

    @Test
    public void whereTheNumberOfResultsExceedsThePageSizeOnlyTheFirstPageIsReturned() {
        underTest.setPageSize(2);

        final Player expectedPlayer1 = aPlayer(valueOf(9999999991L), "JDBCPlayerDaoTest-Jima");
        final Player expectedPlayer2 = aPlayer(valueOf(9999999992L), "JDBCPlayerDaoTest-Jimb");
        underTest.save(expectedPlayer1);
        underTest.save(expectedPlayer2);
        underTest.save(aPlayer(valueOf(9999999993L), "JDBCPlayerDaoTest-Jimc"));
        underTest.save(aPlayer(valueOf(9999999994L), "JDBCPlayerDaoTest-Jimd"));

        final PagedData<Player> results = underTest.findByName("JDBCPlayerDaoTest-Jim", 0, null);

        assertThat(results.getSize(), is(equalTo(2)));
        assertThat(results.getTotalSize(), is(equalTo(4)));
        assertThat(results.getStartPosition(), is(equalTo(0)));
        assertThat(results.getData(), hasItem(expectedPlayer1));
        assertThat(results.getData(), hasItem(expectedPlayer2));
    }

    @Test
    public void whereTheNumberOfResultsExceedsThePageSizeAndTheSecondPageIsRequestedOnlyTheSecondPageIsReturned() {
        final int pageSize = 2;
        underTest.setPageSize(pageSize);

        underTest.save(aPlayer(valueOf(9999999991L), "JDBCPlayerDaoTest-Jima"));
        underTest.save(aPlayer(valueOf(9999999992L), "JDBCPlayerDaoTest-Jimb"));
        final Player expectedPlayer3 = aPlayer(valueOf(9999999993L), "JDBCPlayerDaoTest-Jimc");
        final Player expectedPlayer4 = aPlayer(valueOf(9999999994L), "JDBCPlayerDaoTest-Jimd");
        underTest.save(expectedPlayer3);
        underTest.save(expectedPlayer4);

        final PagedData<Player> results = underTest.findByName("JDBCPlayerDaoTest-Jim", 1, null);

        assertThat(results.getSize(), is(equalTo(2)));
        assertThat(results.getTotalSize(), is(equalTo(4)));
        assertThat(results.getStartPosition(), is(equalTo(pageSize)));
        assertThat(results.getData(), hasItem(expectedPlayer3));
        assertThat(results.getData(), hasItem(expectedPlayer4));
    }

    @Test
    public void whereAPLAYER_IDIsSuppliedTheMatchingPlayerIsOmittedFromTheSearchResults() {
        final Player expectedPlayer = aPlayer(valueOf(9999999992L), "JDBCPlayerDaoTest-Jimb");
        underTest.save(expectedPlayer);
        underTest.save(aPlayer(valueOf(9999999991L), "JDBCPlayerDaoTest-Jima"));
        underTest.save(aPlayer(valueOf(9999999993L), "JDBCPlayerDaoTest-Dave"));

        final PagedData<Player> results = underTest.findByName("JDBCPlayerDaoTest-Jim", 0, valueOf(9999999991L));

        assertThat(results.getSize(), is(equalTo(1)));
        assertThat(results.getData(), hasItem(expectedPlayer));
    }

    @Test(expected = IllegalArgumentException.class)
    public void thePageSizeMustBeGreaterThatZero() {
        underTest.setPageSize(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void thePageSizeMustBePositive() {
        underTest.setPageSize(-1);
    }

    @Test
    public void testSaveNewPlayer() {
        final String name = "John Doe";
        final Player player = aPlayer(PLAYER_ID, name);
        underTest.save(player);
        final Map map = queryForPlayer(PLAYER_ID);

        playerAssertions(map, name);
    }

    @Test
    public void testSavePlayerWithTags() {
        final Player player = aPlayer(PLAYER_ID, "John Doe");
        player.setTags(new HashSet<>(Arrays.asList("tag 1", "tag 2")));
        underTest.save(player);
        final Map map = queryForPlayer(PLAYER_ID);
        assertEquals("tag 1,tag 2", map.get("TAGS"));
    }

    @Test
    public void testUpdatePlayerWithTags() {
        final Player player = aPlayer(PLAYER_ID, "John Doe");
        player.setTags(new HashSet<>(Arrays.asList("tag 1", "tag 2")));
        underTest.save(player);
        player.setTags(new HashSet<>(Arrays.asList("tag 3", "tag 4")));
        underTest.save(player);
        final Map map = queryForPlayer(PLAYER_ID);
        assertEquals("tag 3,tag 4", map.get("TAGS"));
    }

    @Test
    public void testUpdatePlayerWithNullTags() {
        final Player player = aPlayer(PLAYER_ID, "John Doe");
        player.setTags(null);
        underTest.save(player);
        final Map map = queryForPlayer(PLAYER_ID);
        assertEquals(null, map.get("TAGS"));
    }

    @Test
    public void testLoadPlayerWithTags() {
        final Player player = aPlayer(PLAYER_ID, "John Doe");
        player.setTags(new HashSet<>(Arrays.asList("tag 1", "tag 2")));
        underTest.save(player);
        final Player actualPlayer = underTest.findById(PLAYER_ID);
        assertEquals(new HashSet<>(Arrays.asList("tag 1", "tag 2")), actualPlayer.getTags());
    }

    private Player aPlayer(final BigDecimal PLAYER_ID, final String name) {
        final PaymentPreferences paymentPreferences = new PaymentPreferences(Currency.EUR);
        return new Player(PLAYER_ID, name, ACCOUNT_ID, PICTURE_LOCATION,
                paymentPreferences, CREATION_TIME, null);
    }

    private void playerAssertions(final Map map, final String name) {
        assertEquals(PLAYER_ID, valueOf(((BigDecimal) map.get("PLAYER_ID")).longValue()));
        assertEquals(name, map.get("NAME"));
        assertEquals(PICTURE_LOCATION, map.get("PICTURE_LOCATION"));
        assertEquals(ACCOUNT_ID, valueOf(((BigDecimal) map.get("ACCOUNT_ID")).intValue()));
        assertEquals(Currency.EUR, Currency.valueOf(map.get("PREFERRED_CURRENCY").toString()));
        assertNotNull(map.get("TSCREATED").toString());
    }

    @Test
    public void testUpdateExistingPlayer() {
        final Player player = new Player(PLAYER_ID, "John Doe", ACCOUNT_ID, "aPicture", null, new DateTime(), null);
        underTest.save(player);

        final PaymentPreferences paymentPreferences = new PaymentPreferences(Currency.GBP);
        final String newName = "John Smith";
        player.setName(newName);
        player.setPictureUrl(PICTURE_LOCATION);
        player.setPaymentPreferences(paymentPreferences);
        underTest.save(player);

        final Map map = queryForPlayer(PLAYER_ID);
        assertEquals(newName, map.get("NAME"));
        assertEquals(PICTURE_LOCATION, map.get("PICTURE_LOCATION"));
        assertEquals(Currency.GBP, Currency.valueOf(map.get("PREFERRED_CURRENCY").toString()));
    }

    @Test
    public void testSaveNewPlayerWithRelationships() {
        final BigDecimal friendId = valueOf(9893333332L);
        final BigDecimal ignoredFriendId = valueOf(98933321111L);
        final Player player = new Player(PLAYER_ID, "John Doe", ACCOUNT_ID, "aPicture", null, new DateTime(), null);
        final Player friend = new Player(friendId, "John Smith", ACCOUNT_ID, "aPicture", null, new DateTime(), null);
        final Player ignoredFriend = new Player(ignoredFriendId, "John Thompson",
                ACCOUNT_ID, "aPicture", null, new DateTime(), null);
        player.setRelationship(friendId, new Relationship(friend.getName(), RelationshipType.FRIEND));
        player.setRelationship(ignoredFriendId, new Relationship(ignoredFriend.getName(), RelationshipType.IGNORED));

        underTest.save(ignoredFriend);
        underTest.save(friend);
        underTest.save(player);

        final String rel1String = friendId + "\t" + friend.getName() + "\t" + RelationshipType.FRIEND;
        final String rel2String = ignoredFriendId + "\t" + ignoredFriend.getName() + "\t" + RelationshipType.IGNORED;

        checkRelationships(PLAYER_ID, rel1String, rel2String);
    }

    @Test
    public void testUpdateExistingPlayerWithRelationships() {
        final Player player = new Player(PLAYER_ID, "John Doe",
                ACCOUNT_ID, "aPicture", null, new DateTime(), null);
        final Player friend1 = new Player(valueOf(9893333332L), "John Smith",
                ACCOUNT_ID, "aPicture", null, new DateTime(), null);
        final Player friend2 = new Player(valueOf(98933321111L), "John Thompson",
                ACCOUNT_ID, "aPicture", null, new DateTime(), null);
        underTest.save(friend1);
        underTest.save(friend2);

        player.setRelationship(friend1.getPlayerId(), new Relationship("m1", RelationshipType.FRIEND));
        underTest.save(player);

        player.getRelationships().clear();
        player.setRelationship(friend2.getPlayerId(), new Relationship("m2", RelationshipType.FRIEND));
        underTest.save(player);

        checkRelationships(PLAYER_ID, friend2.getPlayerId() + "\t" + "m2" + "\t" + RelationshipType.FRIEND);

    }

    @Test
    public void testLoadAllPlayersWithRelationships() {
        final Set<Player> expected = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            final BigDecimal id = PLAYER_ID.add(valueOf(i));
            final BigDecimal accountId = id.subtract(BigDecimal.valueOf(1000));
            createAccount(accountId, "acct " + id);
            final Player player = new Player(id, "player " + id, accountId, "", null, CREATION_TIME, null);
            if (i > 0) {
                final BigDecimal friendId = id.subtract(valueOf(1));
                player.setRelationship(friendId, new Relationship("player " + friendId, RelationshipType.FRIEND));
            }
            player.setPaymentPreferences(new PaymentPreferences(Currency.GBP));
            underTest.save(player);
            expected.add(player);
        }
        final Set<Player> result = underTest.findAll();
        // We cannot rely here on the empty database in integration, the maximum is
        // to expect that we have nothing more than what we add, and at least what we add
        assertTrue(expected.size() <= result.size());
        for (final Player player : expected) {
            assertThat(expected, hasItems(player));
            assertTrue(result.contains(player));
        }
    }

    @Test
    public void unrecoginisedRelationShipTypesAreIgnoredAndDoNotCauseException() {
        final Player player = new Player(PLAYER_ID, "John Doe",
                ACCOUNT_ID, "aPicture", null, new DateTime(), null);
        final Player friend1 = new Player(valueOf(9893333332L), "John Smith",
                ACCOUNT_ID, "aPicture", null, new DateTime(), null);
        final Player friend2 = new Player(valueOf(98933321111L), "John Thompson",
                ACCOUNT_ID, "aPicture", null, new DateTime(), null);
        underTest.save(friend1);
        underTest.save(friend2);
        player.setRelationship(friend1.getPlayerId(), new Relationship(friend1.getName(), RelationshipType.FRIEND));
        player.setRelationship(friend2.getPlayerId(), new Relationship(friend2.getName(), RelationshipType.IGNORED));
        underTest.save(player);
        jdbc.update("UPDATE PLAYER set RELATIONSHIPS = REPLACE(RELATIONSHIPS, 'IGNORED', ?) where PLAYER_ID = ?",
                "foo", PLAYER_ID);
        final Set<Player> after = underTest.findAll();
        final BigDecimal id = PLAYER_ID;
        final Player result = Iterables.find(after, new Predicate<Player>() {
            @Override
            public boolean apply(final Player player) {
                return id.compareTo(player.getPlayerId()) == 0;
            }
        });
        assertNotNull(result);
        assertEquals(1, result.getRelationships().size());
        final Relationship friend1Relationship = result.getRelationships().get(friend1.getPlayerId());
        assertNotNull(friend1Relationship);
        assertEquals(RelationshipType.FRIEND, friend1Relationship.getType());
    }

    @Test
    public void saveShouldSaveNullIfLastPlayedIsNull() {
        final Player player = new Player(PLAYER_ID, "John Doe", ACCOUNT_ID, "aPicture", null, new DateTime(), null);
        underTest.save(player);
        Player actual = underTest.findById(PLAYER_ID);
        Assert.assertNull(actual.getLastPlayed());
    }

    @Test
    public void saveShouldUpdateLastPlayedTs() {
        DateTime expected = new DateTime().withMillisOfSecond(0);
        final Player player = new Player(PLAYER_ID, "John Doe", ACCOUNT_ID, "aPicture", null, new DateTime(), expected);
        underTest.save(player);
        Player actual = underTest.findById(PLAYER_ID);
        Assert.assertThat(actual.getLastPlayed(), is(equalTo(expected)));
    }

    @Test
    public void updateShouldUpdateLastPlayedTs() {
        DateTime expected = new DateTime().withMillisOfSecond(0);
        final Player player = new Player(PLAYER_ID, "John Doe", ACCOUNT_ID, "aPicture", null, new DateTime(), expected.minusDays(5));
        underTest.save(player);
        player.setLastPlayed(expected);
        underTest.save(player);
        Player actual = underTest.findById(PLAYER_ID);
        Assert.assertThat(actual.getLastPlayed(), is(equalTo(expected)));
    }

    @Test
    public void updateLastPlayedTsShouldUpdateLastPlayedTs() {
        final DateTime now = new DateTime().withMillisOfSecond(0);
        final Player player = new Player(PLAYER_ID, "John Doe", ACCOUNT_ID, "aPicture", null, now, now);
        final Player player2 = new Player(BigDecimal.ONE, "Player 2", BigDecimal.TEN, "aPicture", null, now, now);
        underTest.save(player);
        underTest.save(player2);

        DateTime expectedLastPlayedTs = now.plusHours(1);
        underTest.updateLastPlayedTs(PLAYER_ID, expectedLastPlayedTs);
        Player actual = underTest.findById(PLAYER_ID);
        Assert.assertThat(actual.getLastPlayed(), is(equalTo(expectedLastPlayedTs)));
        Assert.assertThat(underTest.findById(BigDecimal.ONE).getLastPlayed(), is(equalTo(now)));
    }

    private Map queryForPlayer(final BigDecimal PLAYER_ID) {
        return jdbc.queryForMap(PLAYER_SELECT, PLAYER_ID);
    }

    private void checkRelationships(final BigDecimal PLAYER_ID, final String... relString) {
        final Map map = queryForPlayer(PLAYER_ID);
        final String relationshipsString = (String) map.get("RELATIONSHIPS");
        final List<String> relationships = Arrays.asList(relationshipsString.split("\n"));
        for (final String relationship : relString) {
            assertTrue(relationships.contains(relationship));
        }
    }

    private void createAccount(final BigDecimal id,
                               final String name) {
        jdbc.update("INSERT INTO ACCOUNT (ACCOUNT_ID,NAME) values (?,?)", id, name);
    }
}
