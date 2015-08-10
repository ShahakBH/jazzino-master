package com.yazino.platform.repository.tournament;

import com.yazino.platform.model.tournament.TournamentSchedule;
import com.yazino.platform.tournament.TournamentRegistrationInfo;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GigaspaceTournamentInfoRepositoryIntegrationTest {

    private static final String BLACKJACK = "BLACKJACK";

    @Autowired
    private GigaSpace gigaSpace;
    private GigaspaceTournamentInfoRepository unit;

    @Before
    public void setUp() {
        gigaSpace.clear(null);
        unit = new GigaspaceTournamentInfoRepository(gigaSpace);
    }

    @Test
    public void shouldReturnTournamentScheduleMatchingGameTypeAndPartnerId() throws Exception {
        TournamentSchedule schedule = buildTemplateSchedule();
        final TournamentRegistrationInfo registrationInfo = new TournamentRegistrationInfo(BigDecimal.TEN,
                new DateTime(), BigDecimal.ONE, BigDecimal.ZERO,
                "aName", "aDescription", "aTemplate", Collections.<BigDecimal>emptySet());
        schedule.addRegistrationInfo(registrationInfo);
        gigaSpace.write(schedule);
        assertEquals(schedule, unit.findTournamentSchedule(BLACKJACK));
    }

    @Test
    public void shouldReturnTemplateTournament() throws Exception {
        assertEquals(buildTemplateSchedule(), unit.findTournamentSchedule(BLACKJACK));
    }

    private TournamentSchedule buildTemplateSchedule() {
        TournamentSchedule schedule = new TournamentSchedule();
        schedule.setGameType(BLACKJACK);
        return schedule;
    }

}
