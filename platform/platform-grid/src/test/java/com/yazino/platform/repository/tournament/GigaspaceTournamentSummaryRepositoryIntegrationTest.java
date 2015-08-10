package com.yazino.platform.repository.tournament;

import com.yazino.platform.model.tournament.TournamentSummary;
import com.yazino.platform.persistence.tournament.TournamentSummaryDao;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaspaceTournamentSummaryRepositoryIntegrationTest {

    @Autowired(required = true)
    private GigaSpace gigaSpace;

    @Mock
    private TournamentSummaryDao tournamentSummaryDao;

    private GigaspaceTournamentSummaryRepository underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        gigaSpace.clear(null);

        underTest = new GigaspaceTournamentSummaryRepository(gigaSpace, tournamentSummaryDao);
    }

    @Test
    @Transactional
    public void findMostRecentReturnsClosestToCurrentTime() {
        final TournamentSummary summary1 = createSummary(BigDecimal.valueOf(1), new DateTime(2008, 1, 1, 1, 1, 1, 1).toDate());
        final TournamentSummary summary2 = createSummary(BigDecimal.valueOf(2), new DateTime(2009, 1, 1, 1, 1, 1, 1).toDate());
        final TournamentSummary summary3 = createSummary(BigDecimal.valueOf(3), new DateTime(2007, 1, 1, 1, 1, 1, 1).toDate());

        gigaSpace.writeMultiple(new Object[]{summary1, summary2, summary3});

        final TournamentSummary result = underTest.findMostRecent("BLACKJACK");

        assertNotNull(result);
        assertEquals(summary2, result);
    }

    @Test
    @Transactional
    public void findMostRecentReturnsCorrectGameType() {
        final TournamentSummary summary1 = createSummary(BigDecimal.valueOf(1), new DateTime(2008, 1, 1, 1, 1, 1, 1).toDate());

        final TournamentSummary wrongSummary2 = createSummary(BigDecimal.valueOf(4), new DateTime(2009, 1, 1, 1, 1, 1, 1).toDate());
        wrongSummary2.setGameType("TEXAS_HOLDEM");

        gigaSpace.writeMultiple(new Object[]{summary1, wrongSummary2});

        final TournamentSummary result = underTest.findMostRecent("BLACKJACK");

        assertNotNull(result);
        assertEquals(summary1, result);
    }

    @Test
    @Transactional
    public void deletingASummaryDelegatesToTheDAO() {
        underTest.delete(BigDecimal.valueOf(100));

        verify(tournamentSummaryDao).delete(BigDecimal.valueOf(100));
    }

    @Test(expected = NullPointerException.class)
    @Transactional
    public void deletingASummaryWithANullIDThrowsANullPointerException() {
        underTest.delete(null);
    }

    private TournamentSummary createSummary(final BigDecimal id, final Date finishDate) {
        final TournamentSummary summary = new TournamentSummary();
        summary.setTournamentId(id);
        summary.setGameType("BLACKJACK");
        summary.setFinishDateTime(finishDate);
        return summary;
    }
}
