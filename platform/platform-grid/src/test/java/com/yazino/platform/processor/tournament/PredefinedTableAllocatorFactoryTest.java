package com.yazino.platform.processor.tournament;

import com.yazino.platform.model.tournament.PlayerGroup;
import com.yazino.platform.model.tournament.TournamentPlayer;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertThat;

public class PredefinedTableAllocatorFactoryTest {
    private static final String DUMMY_ID = "DUMMY";

    private TableAllocatorFactory unit;

    @Before
    public void setUp() {
        unit = new PredefinedTableAllocatorFactory(Arrays.asList((TableAllocator) new DummyTableAllocator()));
    }

    @Test
    public void allocatorCanBeRegisteredAndThenFetchedById() {
        assertThat(unit.byId(DUMMY_ID), idIsEqualTo(DUMMY_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unregisteredIdThrowsException() {
        unit.byId("bob");
    }

    private class DummyTableAllocator implements TableAllocator {
        @Override
        public String getId() {
            return DUMMY_ID;
        }

        @Override
        public Collection<PlayerGroup> allocate(final Collection<TournamentPlayer> players, final int tableSize) {
            return null;
        }
    }

    private Matcher<TableAllocator> idIsEqualTo(final String id) {
        return new TypeSafeMatcher<TableAllocator>() {
            @Override
            public void describeTo(final Description description) {
                description.appendText("has id " + id);
            }

            @Override
            public boolean matchesSafely(final TableAllocator item) {
                return item != null && StringUtils.equals(item.getId(), id);
            }
        };
    }

}
