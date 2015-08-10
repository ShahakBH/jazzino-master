package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.PlayerGroup;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.model.tournament.TournamentPlayerStatus;
import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class EvenRandomTableAllocatorTest {

    private static final int TABLE_SIZE = 5;

    private final AtomicLong idSource = new AtomicLong(0);

    private EvenRandomTableAllocator allocator;

    @Before
    public void setUp() {
        allocator = new EvenRandomTableAllocator();
    }

    @Test
    public void playersShouldBeAssignedInARandomOrder() {
        Collection<PlayerGroup> lastRun = null;
        boolean allEquals = true;

        final List<TournamentPlayer> players = createPlayerGroup(TABLE_SIZE);
        for (int i = 0; i < 10; ++i) {
            final Collection<PlayerGroup> currentRun = allocator.allocate(players, TABLE_SIZE);

            if (lastRun != null) {
                allEquals = lastRun.equals(currentRun);
                if (!allEquals) {
                    break;
                }
            }
            lastRun = currentRun;
        }

        assertFalse("Players have been repeatedly allocated in the same order", allEquals);
    }

    @Test
    public void test2Players() {
        final List<TournamentPlayer> players = createPlayerGroup(2);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(1, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(2));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test3Players() {
        final List<TournamentPlayer> players = createPlayerGroup(3);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(1, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(3));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test4Players() {
        final List<TournamentPlayer> players = createPlayerGroup(4);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(1, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(4));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test5Players() {
        final List<TournamentPlayer> players = createPlayerGroup(5);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(1, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(5));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test6Players() {
        final List<TournamentPlayer> players = createPlayerGroup(6);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(2, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(3, 3));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test7Players() {
        final List<TournamentPlayer> players = createPlayerGroup(7);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(2, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(4, 3));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test8Players() {
        final List<TournamentPlayer> players = createPlayerGroup(8);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(2, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(4, 4));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test9Players() {
        final List<TournamentPlayer> players = createPlayerGroup(9);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(2, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(5, 4));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test10Players() {
        final List<TournamentPlayer> players = createPlayerGroup(10);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(2, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(5, 5));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test11Players() {
        final List<TournamentPlayer> players = createPlayerGroup(11);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(3, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(4, 4, 3));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test12Players() {
        final List<TournamentPlayer> players = createPlayerGroup(12);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(3, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(4, 4, 4));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test13Players() {
        final List<TournamentPlayer> players = createPlayerGroup(13);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(3, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(5, 4, 4));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test14Players() {
        final List<TournamentPlayer> players = createPlayerGroup(14);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(3, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(5, 5, 4));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test15Players() {
        final List<TournamentPlayer> players = createPlayerGroup(15);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(3, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(5, 5, 5));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test16Players() {
        final List<TournamentPlayer> players = createPlayerGroup(16);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(4, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(4, 4, 4, 4));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test17Players() {
        final List<TournamentPlayer> players = createPlayerGroup(17);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(4, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(5, 4, 4, 4));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test18Players() {
        final List<TournamentPlayer> players = createPlayerGroup(18);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(4, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(5, 5, 4, 4));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test19Players() {
        final List<TournamentPlayer> players = createPlayerGroup(19);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(4, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(5, 5, 5, 4));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    @Test
    public void test20Players() {
        final List<TournamentPlayer> players = createPlayerGroup(20);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(4, allocatedPlayers.size());
        assertThat(allocatedPlayers, containsGroupsWithPlayers(5, 5, 5, 5));
        assertThat(allocatedPlayers, containsInAnyOrder(players));
    }

    private List<TournamentPlayer> createPlayerGroup(final int size) {
        final List<TournamentPlayer> players = new ArrayList<TournamentPlayer>();

        for (int i = 0, j = size; i < size; ++i, --j) {
            players.add(createPlayer(i, j));
        }

        return players;
    }

    private TournamentPlayer createPlayer(final int i, final int leaderBoardPosition) {
        final TournamentPlayer tournamentPlayer = new TournamentPlayer(
                BigDecimal.valueOf(idSource.getAndIncrement()),
                "player " + i,
                BigDecimal.valueOf(idSource.getAndIncrement()),
                TournamentPlayerStatus.ADDITION_PENDING);
        tournamentPlayer.setLeaderboardPosition(leaderBoardPosition);

        return tournamentPlayer;
    }

    private Matcher<Collection<PlayerGroup>> containsGroupsWithPlayers(final int... playersForGroup) {
        return new TypeSafeMatcher<Collection<PlayerGroup>>() {
            @Override
            public boolean matchesSafely(final Collection<PlayerGroup> playerGroups) {
                int actualGroups = 0;
                for (PlayerGroup playerGroup : playerGroups) {
                    if (playerGroup.asList().size() != playersForGroup[actualGroups]) {
                        return false;
                    }
                    ++actualGroups;
                }

                final int expectedGroups = playersForGroup.length;
                return actualGroups == expectedGroups;
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("contains " + playersForGroup.length + " groups with player counts of")
                        .appendValue(ArrayUtils.toString(playersForGroup));
            }
        };
    }

    private Matcher<Collection<PlayerGroup>> containsInAnyOrder(final Collection<TournamentPlayer> expectedPlayers) {
        return new TypeSafeMatcher<Collection<PlayerGroup>>() {
            @Override
            public boolean matchesSafely(final Collection<PlayerGroup> targetPlayers) {
                final Set<TournamentPlayer> allPlayers = new HashSet<TournamentPlayer>();
                for (final PlayerGroup playerGroup : targetPlayers) {
                    allPlayers.addAll(playerGroup.asList());
                }

                final Set<TournamentPlayer> unmatchedPlayers = new HashSet<TournamentPlayer>(expectedPlayers);
                return unmatchedPlayers.equals(allPlayers);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("contains all of").appendValue(expectedPlayers);
            }
        };
    }

}
