package com.yazino.platform.service.table;

import com.yazino.platform.model.table.Countdown;
import com.yazino.platform.repository.table.CountdownRepository;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

@SuppressWarnings({"UnusedDeclaration"})
public class GigaspaceRemotingCountdownServiceTest {

    private final CountdownRepository repository = mock(CountdownRepository.class);

    private GigaspaceRemotingCountdownService underTest;

    @Before
    public void setUp() {
        underTest = new GigaspaceRemotingCountdownService(repository);
    }

    @Test
    public void findCountdownInCommunitySpace() {
        Countdown countdown = new Countdown("aGameType", new Date().getTime());
        when(repository.find()).thenReturn(Arrays.asList(countdown));

        assertSame(underTest.findAll().get("aGameType"), countdown.getCountdown());
        verify(repository).find();
    }

}
