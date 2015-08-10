package com.yazino.platform.repository.table;

import com.yazino.platform.model.table.Countdown;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(defaultRollback = true)
public class GigaspaceCountdownRepositoryIntegrationTest {
    private static final String GAME_TYPE = "aGameType";

    @Autowired
    private GigaSpace gigaSpace;
    @Autowired
    private GigaspaceCountdownRepository underTest;

    private Countdown countdown;

    @Before
    @Transactional
    public void setUp() {
        final Calendar countdownCalendar = Calendar.getInstance();
        countdownCalendar.set(Calendar.MILLISECOND, 0);
        countdownCalendar.add(Calendar.DAY_OF_MONTH, -1);
        Long countdownUntil = countdownCalendar.getTime().getTime();
        countdown = new Countdown(GAME_TYPE, countdownUntil);
    }

    @Test
    public void findCountdownInSpace() {
        gigaSpace.clear(null);
        gigaSpace.write(countdown);
        final Collection<Countdown> countdownResult = underTest.find();
        Countdown found = countdownResult.iterator().next();

        assertTrue(countdownResult.size() == 1);
        assertEquals(countdown.getId(), found.getId());
        assertEquals(countdown.getCountdown(), found.getCountdown());
    }

    @Test
    public void findCountdownInSpaceById() {
        gigaSpace.clear(null);
        gigaSpace.write(countdown);
        final Countdown countdownResult = underTest.find(GAME_TYPE);

        assertEquals(countdown.getId(), countdownResult.getId());
        assertEquals(countdown.getCountdown(), countdownResult.getCountdown());
    }

    @Test
    public void publishCountdownShouldWritesToSpace() {
        gigaSpace.clear(null);
        underTest.publishIntoSpace(countdown);
        final Collection<Countdown> countdownResult = underTest.find();
        Countdown found = countdownResult.iterator().next();

        assertTrue(countdownResult.size() == 1);
        assertEquals(countdown.getId(), found.getId());
        assertEquals(countdown.getCountdown(), found.getCountdown());
    }

    @Test
    public void removeCountdownFromSpace() {
        gigaSpace.clear(null);
        gigaSpace.write(countdown);
        underTest.removeCountdownFromSpace(countdown);
        assertNull(gigaSpace.read(new Countdown()));
    }
}
