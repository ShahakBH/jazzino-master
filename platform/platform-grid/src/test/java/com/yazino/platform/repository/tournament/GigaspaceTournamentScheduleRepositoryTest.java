package com.yazino.platform.repository.tournament;

import com.gigaspaces.client.ReadModifiers;
import com.gigaspaces.client.WriteModifiers;
import com.yazino.platform.model.tournament.TournamentSchedule;
import net.jini.core.lease.Lease;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openspaces.core.GigaSpace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GigaspaceTournamentScheduleRepositoryTest {
    @Mock
    private GigaSpace gigaSpace;

    private GigaspaceTournamentScheduleRepository underTest;

    @Before
    public void setUp() {
        underTest = new GigaspaceTournamentScheduleRepository(gigaSpace);
    }

    @Test(expected = NullPointerException.class)
    public void repositoryCannotBeCreatedWithANullGigaspace() {
        new GigaspaceTournamentScheduleRepository(null);
    }

    @Test(expected = NullPointerException.class)
    public void aNullTournamentScheduleThrowsAnExceptionOnSave() {
        underTest.save(null);
    }

    @Test
    public void aSavedTournamentScheduleIsWrittenToTheSpace() {
        underTest.save(aSchedule());

        verify(gigaSpace).write(aSchedule(), Lease.FOREVER, 3000, WriteModifiers.UPDATE_OR_WRITE);
    }

    @Test(expected = NullPointerException.class)
    public void aTournamentScheduleThrowsAnExceptionIfTheGameTypeIsNull() {
        underTest.findByGameType(null);
    }

    @Test
    public void aTournamentScheduleMayBeFoundByGameType() {
        when(gigaSpace.readById(TournamentSchedule.class, "aGameType", "aGameType", 0, ReadModifiers.DIRTY_READ)).thenReturn(aSchedule());

        final TournamentSchedule result = underTest.findByGameType("aGameType");

        assertThat(result, is(equalTo(aSchedule())));
    }

    @Test
    public void anAbsentTournamentScheduleCreatesAnEmptyTournamentScheduleAndReturnsIt() {
        final TournamentSchedule result = underTest.findByGameType("aNewGameType");

        assertThat(result, is(equalTo(anEmptyScheduleFor("aNewGameType"))));
        verify(gigaSpace).write(anEmptyScheduleFor("aNewGameType"), Lease.FOREVER, 3000, WriteModifiers.UPDATE_OR_WRITE);
    }

    private TournamentSchedule aSchedule() {
        return anEmptyScheduleFor("aGameType");
    }

    private TournamentSchedule anEmptyScheduleFor(final String gameType) {
        final TournamentSchedule tournamentSchedule = new TournamentSchedule();
        tournamentSchedule.setGameType(gameType);
        return tournamentSchedule;
    }
}
