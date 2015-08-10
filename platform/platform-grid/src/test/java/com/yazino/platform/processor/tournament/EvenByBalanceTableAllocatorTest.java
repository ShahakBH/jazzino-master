package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.PlayerGroup;
import com.yazino.platform.model.tournament.TournamentPlayer;
import com.yazino.platform.model.tournament.TournamentPlayerStatus;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class EvenByBalanceTableAllocatorTest {

    private static final int TABLE_SIZE = 5;

    private final AtomicLong idSource = new AtomicLong(0);

    private EvenByBalanceTableAllocator allocator;

    @Before
    public void setUp() {
        allocator = new EvenByBalanceTableAllocator();
    }

    @Test
    public void test2Players() {
        final List<TournamentPlayer> players = createPlayerGroup(2);
        final List<TournamentPlayer> players1 = createSublist(players, 1, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(1, allocatedPlayers.size());
        assertEquals(new PlayerGroup(players1), allocatedPlayers.iterator().next());
    }

    @Test
    public void test3Players() {
        final List<TournamentPlayer> players = createPlayerGroup(3);
        final List<TournamentPlayer> players1 = createSublist(players, 2, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(1, allocatedPlayers.size());
        assertEquals(new PlayerGroup(players1), allocatedPlayers.iterator().next());
    }

    @Test
    public void test4Players() {
        final List<TournamentPlayer> players = createPlayerGroup(4);
        final List<TournamentPlayer> players1 = createSublist(players, 3, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(1, allocatedPlayers.size());
        assertEquals(new PlayerGroup(players1), allocatedPlayers.iterator().next());
    }

    @Test
    public void test5Players() {
        final List<TournamentPlayer> players = createPlayerGroup(5);
        final List<TournamentPlayer> players1 = createSublist(players, 4, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(1, allocatedPlayers.size());
        assertEquals(new PlayerGroup(players1), allocatedPlayers.iterator().next());
    }

    @Test
    public void test6Players() {
        final List<TournamentPlayer> players = createPlayerGroup(6);

        final List<TournamentPlayer> players1 = createSublist(players, 5, 3);
        final List<TournamentPlayer> players2 = createSublist(players, 2, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(2, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
    }

    @Test
    public void test7Players() {
        final List<TournamentPlayer> players = createPlayerGroup(7);

        final List<TournamentPlayer> players1 = createSublist(players, 6, 3);
        final List<TournamentPlayer> players2 = createSublist(players, 2, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(2, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
    }

    @Test
    public void test8Players() {
        final List<TournamentPlayer> players = createPlayerGroup(8);

        final List<TournamentPlayer> players1 = createSublist(players, 7, 4);
        final List<TournamentPlayer> players2 = createSublist(players, 3, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(2, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
    }

    @Test
    public void test9Players() {
        final List<TournamentPlayer> players = createPlayerGroup(9);

        final List<TournamentPlayer> players1 = createSublist(players, 8, 4);
        final List<TournamentPlayer> players2 = createSublist(players, 3, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(2, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
    }

    @Test
    public void test10Players() {
        final List<TournamentPlayer> players = createPlayerGroup(10);

        final List<TournamentPlayer> players1 = createSublist(players, 9, 5);
        final List<TournamentPlayer> players2 = createSublist(players, 4, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(2, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
    }

    @Test
    public void test11Players() {
        final List<TournamentPlayer> players = createPlayerGroup(11);

        final List<TournamentPlayer> players1 = createSublist(players, 10, 7);
        final List<TournamentPlayer> players2 = createSublist(players, 6, 3);
        final List<TournamentPlayer> players3 = createSublist(players, 2, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(3, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players3), allocatedPlayerIterator.next());
    }

    @Test
    public void test12Players() {
        final List<TournamentPlayer> players = createPlayerGroup(12);

        final List<TournamentPlayer> players1 = createSublist(players, 11, 8);
        final List<TournamentPlayer> players2 = createSublist(players, 7, 4);
        final List<TournamentPlayer> players3 = createSublist(players, 3, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(3, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players3), allocatedPlayerIterator.next());
    }

    @Test
    public void test13Players() {
        final List<TournamentPlayer> players = createPlayerGroup(13);

        final List<TournamentPlayer> players1 = createSublist(players, 12, 8);
        final List<TournamentPlayer> players2 = createSublist(players, 7, 4);
        final List<TournamentPlayer> players3 = createSublist(players, 3, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(3, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players3), allocatedPlayerIterator.next());
    }

    @Test
    public void test14Players() {
        final List<TournamentPlayer> players = createPlayerGroup(14);

        final List<TournamentPlayer> players1 = createSublist(players, 13, 9);
        final List<TournamentPlayer> players2 = createSublist(players, 8, 4);
        final List<TournamentPlayer> players3 = createSublist(players, 3, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(3, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players3), allocatedPlayerIterator.next());
    }

    @Test
    public void test15Players() {
        final List<TournamentPlayer> players = createPlayerGroup(15);

        final List<TournamentPlayer> players1 = createSublist(players, 14, 10);
        final List<TournamentPlayer> players2 = createSublist(players, 9, 5);
        final List<TournamentPlayer> players3 = createSublist(players, 4, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(3, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players3), allocatedPlayerIterator.next());
    }

    @Test
    public void test16Players() {
        final List<TournamentPlayer> players = createPlayerGroup(16);

        final List<TournamentPlayer> players1 = createSublist(players, 15, 12);
        final List<TournamentPlayer> players2 = createSublist(players, 11, 8);
        final List<TournamentPlayer> players3 = createSublist(players, 7, 4);
        final List<TournamentPlayer> players4 = createSublist(players, 3, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(4, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players3), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players4), allocatedPlayerIterator.next());
    }

    @Test
    public void test17Players() {
        final List<TournamentPlayer> players = createPlayerGroup(17);

        final List<TournamentPlayer> players1 = createSublist(players, 16, 12);
        final List<TournamentPlayer> players2 = createSublist(players, 11, 8);
        final List<TournamentPlayer> players3 = createSublist(players, 7, 4);
        final List<TournamentPlayer> players4 = createSublist(players, 3, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(4, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players3), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players4), allocatedPlayerIterator.next());
    }

    @Test
    public void test18Players() {
        final List<TournamentPlayer> players = createPlayerGroup(18);

        final List<TournamentPlayer> players1 = createSublist(players, 17, 13);
        final List<TournamentPlayer> players2 = createSublist(players, 12, 8);
        final List<TournamentPlayer> players3 = createSublist(players, 7, 4);
        final List<TournamentPlayer> players4 = createSublist(players, 3, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(4, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players3), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players4), allocatedPlayerIterator.next());
    }

    @Test
    public void test19Players() {
        final List<TournamentPlayer> players = createPlayerGroup(19);

        final List<TournamentPlayer> players1 = createSublist(players, 18, 14);
        final List<TournamentPlayer> players2 = createSublist(players, 13, 9);
        final List<TournamentPlayer> players3 = createSublist(players, 8, 4);
        final List<TournamentPlayer> players4 = createSublist(players, 3, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(4, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players3), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players4), allocatedPlayerIterator.next());
    }

    @Test
    public void test20Players() {
        final List<TournamentPlayer> players = createPlayerGroup(20);

        final List<TournamentPlayer> players1 = createSublist(players, 19, 15);
        final List<TournamentPlayer> players2 = createSublist(players, 14, 10);
        final List<TournamentPlayer> players3 = createSublist(players, 9, 5);
        final List<TournamentPlayer> players4 = createSublist(players, 4, 0);

        final Collection<PlayerGroup> allocatedPlayers = allocator.allocate(players, TABLE_SIZE);

        assertEquals(4, allocatedPlayers.size());
        final Iterator<PlayerGroup> allocatedPlayerIterator = allocatedPlayers.iterator();
        assertEquals(new PlayerGroup(players1), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players2), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players3), allocatedPlayerIterator.next());
        assertEquals(new PlayerGroup(players4), allocatedPlayerIterator.next());
    }

    private List<TournamentPlayer> createPlayerGroup(final int size) {
        final List<TournamentPlayer> players = new ArrayList<TournamentPlayer>();

        for (int i = 0, j = size; i < size; ++i, --j) {
            players.add(createPlayer(i, j));
        }

        return players;
    }

    private List<TournamentPlayer> createSublist(final List<TournamentPlayer> list, final int last, final int first) {
        List<TournamentPlayer> sublist = new ArrayList<TournamentPlayer>();
        if (last >= first) {
            for (int i = last; i >= first; i--) {
                sublist.add(list.get(i));
            }
        } else {
            throw new IllegalArgumentException(String.format("Indexes for creating sublist are invalid, begining index: %s is greater than end index: %s.", first, last));
        }
        return sublist;
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

}
