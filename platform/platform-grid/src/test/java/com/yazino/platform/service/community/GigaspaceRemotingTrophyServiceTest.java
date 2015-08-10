package com.yazino.platform.service.community;

import com.yazino.platform.community.Trophy;
import com.yazino.platform.persistence.SequenceGenerator;
import com.yazino.platform.repository.community.TrophyRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GigaspaceRemotingTrophyServiceTest {

    @Mock
    private TrophyRepository trophyRepository;
    @Mock
    private SequenceGenerator sequenceGenerator;

    private GigaspaceRemotingTrophyService trophyService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        trophyService = new GigaspaceRemotingTrophyService(
                trophyRepository, sequenceGenerator);
    }

    @Test
    public void findAllShouldQueryRepositoryForTrophies() {
        List<Trophy> trophies = new LinkedList<Trophy>();
        when(trophyRepository.findAll()).thenReturn(trophies);
        assertEquals(trophies, trophyService.findAll());
    }

    @Test
    public void findByIdShouldQueryRepositoryForTrophy() {
        BigDecimal id = new BigDecimal(1);
        Trophy trophy = new Trophy();
        when(trophyRepository.findById(id)).thenReturn(trophy);
        assertEquals(trophy, trophyService.findById(id));
    }

    @Test
    public void updateShouldCallSaveOnRepository() {
        Trophy trophy = new Trophy();
        trophyService.update(trophy);
        verify(trophyRepository).save(trophy);
    }

    @Test
    public void createShouldAssignIdAndCallSaveOnRepository() {
        Trophy trophy = new Trophy();
        when(sequenceGenerator.next()).thenReturn(new BigDecimal(123));
        BigDecimal id = trophyService.create(trophy);
        verify(trophyRepository).save(trophy);

        assertEquals(new BigDecimal(123), id);
    }

    @Test
    public void findForGameTypeShouldCallRepository() {

        Collection<Trophy> returnValue = new LinkedList<Trophy>();
        returnValue.add(new Trophy());

        when(trophyRepository.findForGameType("BOB")).thenReturn(returnValue);

        assertEquals(returnValue, trophyService.findForGameType("BOB"));
    }

    private Trophy aTrophy() {
        return new Trophy(BigDecimal.valueOf(10), "aTrophy", "BLACKJACK", "an/image");
    }

}
