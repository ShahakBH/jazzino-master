package com.yazino.platform.service.tournament.transactional;


import com.yazino.platform.model.tournament.Tournament;
import com.yazino.platform.model.tournament.TournamentVariationTemplateBuilder;
import com.yazino.platform.repository.tournament.TournamentRepository;
import com.yazino.platform.tournament.TournamentStatus;
import com.yazino.platform.tournament.TournamentType;
import com.yazino.platform.tournament.TournamentVariationTemplate;
import com.yazino.test.ThreadLocalDateTimeUtils;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static com.yazino.platform.tournament.TournamentStatus.CANCELLING;
import static com.yazino.platform.tournament.TournamentStatus.RUNNING;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class TransactionalTournamentServiceTest {
    private static final BigDecimal TOURNAMENT_ID = BigDecimal.valueOf(2000);

    @Mock
    private TournamentRepository tournamentRepository;

    private TransactionalTournamentService underTest;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        underTest = new TransactionalTournamentService(tournamentRepository);

        ThreadLocalDateTimeUtils.setCurrentMillisFixed(new DateTime().getMillis());
    }

    @After
    public void cleanUp() {
        ThreadLocalDateTimeUtils.setCurrentMillisSystem();
    }

    @Test(expected = IllegalStateException.class)
    public void methodCallsAreAbortedOnAnInstanceCreatedWithTheEmptyConstructor() {
        new TransactionalTournamentService().cancelTournament(TOURNAMENT_ID);
    }

    @Test(expected = NullPointerException.class)
    public void cancellationShouldThrowAnExceptionForANullSetOfIds() throws Exception {
        underTest.cancelTournament(null);
    }

    @Test
    public void cancellationShouldChangeStatusOfTournamentAndWriteNonPersistently() throws Exception {
        when(tournamentRepository.lock(bd(1))).thenReturn(aTournamentWithStatus(1, RUNNING));
        when(tournamentRepository.lock(bd(2))).thenReturn(aTournamentWithStatus(2, RUNNING));

        underTest.cancelTournament(bd(1));
        underTest.cancelTournament(bd(2));

        verify(tournamentRepository).nonPersistentSave(aTournamentWithStatus(1, CANCELLING));
        verify(tournamentRepository).nonPersistentSave(aTournamentWithStatus(2, CANCELLING));
    }

    @Test
    public void cancellationShouldReturnTheIdsOfAllTournamentsSuccessfullyCancelled() throws Exception {
        when(tournamentRepository.lock(bd(1))).thenReturn(aTournamentWithStatus(1, RUNNING));
        when(tournamentRepository.lock(bd(2))).thenReturn(aTournamentWithStatus(2, RUNNING));
        doThrow(new RuntimeException("aRuntimeException"))
                .when(tournamentRepository).nonPersistentSave(aTournamentWithStatus(1, CANCELLING));

        assertFalse(underTest.cancelTournament(bd(1)));
        assertTrue(underTest.cancelTournament(bd(2)));
    }

    private BigDecimal bd(final int i) {
        return BigDecimal.valueOf(i);
    }

    private TournamentVariationTemplate aTournamentTemplate() {
        return new TournamentVariationTemplateBuilder()
                .setTournamentVariationTemplateId(BigDecimal.valueOf(4325454354L))
                .setTournamentType(TournamentType.PRESET)
                .setTemplateName("aTournamentTemplate")
                .setGameType("BLACKJACK")
                .toTemplate();
    }

    private Tournament aTournamentWithStatus(final Integer id, final TournamentStatus status) {
        final Tournament tournament = new Tournament();
        if (id != null) {
            tournament.setTournamentId(BigDecimal.valueOf(id));
        }
        tournament.setName("aTournament");
        tournament.setSignupStartTimeStamp(new DateTime());
        tournament.setStartTimeStamp(new DateTime().plusMinutes(10));
        tournament.setTournamentStatus(status);
        tournament.setTournamentVariationTemplate(aTournamentTemplate());
        tournament.setPot(BigDecimal.ZERO);
        return tournament;
    }

}
