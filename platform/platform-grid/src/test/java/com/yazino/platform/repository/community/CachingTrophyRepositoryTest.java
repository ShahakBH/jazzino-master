package com.yazino.platform.repository.community;

import com.yazino.platform.community.Trophy;
import com.yazino.platform.persistence.community.TrophyDAO;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;

public class CachingTrophyRepositoryTest {
    private static final BigDecimal INVALID_TROPHY_ID = BigDecimal.valueOf(-1);
    private static final String GAME_TYPE = "gameType";
    private static final String ANOTHER_GAME_TYPE = "gameType2";
    private static final String GAME_TYPE_WITHOUT_TROPHIES = "gameTypeTrophyLess";
    private static final Trophy TROPHY1 = new Trophy(BigDecimal.valueOf(1), "trophyOne", GAME_TYPE, "imageOne");
    private static final Trophy TROPHY2 = new Trophy(BigDecimal.valueOf(2), "trophyTwo", GAME_TYPE, "imageTwo");
    private static final Trophy TROPHY3 = new Trophy(BigDecimal.valueOf(3), "trophyThree", ANOTHER_GAME_TYPE, "imageThree");

    private TrophyDAO trophyDAO;

    private CachingTrophyRepository underTest;

    @Before
    public void setUp() {
        trophyDAO = mock(TrophyDAO.class);
        when(trophyDAO.retrieveAll()).thenReturn(asList(TROPHY1, TROPHY2, TROPHY3));

        underTest = new CachingTrophyRepository(trophyDAO);
        underTest.refreshTrophies();
    }

    @Test
    public void findsAndReturnsAllTrophiesForAGivenGameType() {
        final Collection<Trophy> trophies = underTest.findForGameType(GAME_TYPE);

        assertThat(trophies.size(), is(equalTo(2)));
        assertThat(trophies, hasItem(TROPHY1));
        assertThat(trophies, hasItem(TROPHY2));
    }

    @Test
    public void findsAndReturnsEmptyCollectionIfNoTrophiesArePossessedByAGivenGameType() {
        final Collection<Trophy> trophies = underTest.findForGameType(GAME_TYPE_WITHOUT_TROPHIES);

        assertThat(trophies.size(), is(equalTo(0)));
    }

    @Test
    public void findByIdReturnsMatchingTrophy() {
        final Trophy trophy = underTest.findById(TROPHY1.getId());

        assertThat(trophy, is(equalTo(TROPHY1)));
    }

    @Test
    public void findByIdReturnsNullIfNoMatchFound() {
        final Trophy trophy = underTest.findById(INVALID_TROPHY_ID);

        assertThat(trophy, is(nullValue()));
    }

    @Test
    public void findAllReadsTheInitialisedTrophies() {
        assertThat(underTest.findAll(), allOf(hasItem(TROPHY1), hasItem(TROPHY2), hasItem(TROPHY3)));
    }

    @Test
    public void findAllReturnsEmptyListIfNoTrophies() {
        reset(trophyDAO);
        when(trophyDAO.retrieveAll()).thenReturn(Collections.<Trophy>emptyList());
        underTest.refreshTrophies();

        assertEquals(Collections.EMPTY_LIST, underTest.findAll());
    }

    @Test
    public void saveShouldWriteTheTrophyToTheDAO() {
        final Trophy trophy = new Trophy();
        trophy.setId(new BigDecimal(666));

        underTest.save(trophy);

        verify(trophyDAO).save(trophy);
    }

    @Test
    public void findByNameReturnsEmptyCollectionWhenNoEntries() throws Exception {
        final Collection<Trophy> results = underTest.findByName("Foo");
        assertTrue(results.isEmpty());
    }

    @Test
    public void findByNameReturnsCollectionOfTrophiesMatchingName() throws Exception {
        Collection<Trophy> results = underTest.findByName("trophyTwo");
        assertEquals(1, results.size());
        assertEquals(TROPHY2, results.iterator().next());
    }

}
